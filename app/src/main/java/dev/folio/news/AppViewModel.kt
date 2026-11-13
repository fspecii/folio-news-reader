package dev.folio.news

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.folio.news.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import java.util.UUID
import java.util.concurrent.TimeUnit

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(18, TimeUnit.SECONDS)
        .cache(Cache(app.cacheDir.resolve("http"), 30L * 1024 * 1024))
        .build()
    private val repo = FeedRepository(client)
    private val profile = Profile.fromJson(prefs.profileJson)

    init { Catalog.load(app) }

    var sources by mutableStateOf(prefs.loadSources()); private set
    var articles by mutableStateOf(dedupe(prefs.loadArticles())); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var themeMode by mutableStateOf(prefs.themeMode); private set
    var fontScale by mutableStateOf(prefs.fontScale); private set
    var twoPage by mutableStateOf(prefs.twoPage); private set
    var smartFeed by mutableStateOf(prefs.smartFeed); private set
    var onboarded by mutableStateOf(prefs.onboarded); private set

    var homeIndex by mutableIntStateOf(0)
    var openArticle by mutableStateOf<Article?>(null)
    var showSources by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showWizard by mutableStateOf(!prefs.onboarded)
    /** true when the reader was entered by swiping past the end of the previous article */
    var continued by mutableStateOf(false)

    private val blockCache = HashMap<String, List<Block>>()
    private var sourcesDirty = false

    init { if (prefs.onboarded) refresh() }

    // ------------------------------------------------------------------ feed

    private fun topicOf(sourceId: String): String? =
        sources.firstOrNull { it.id == sourceId }?.let { it.topic ?: Catalog.topicOfUrl(it.url) }

    fun refresh() {
        if (loading) return
        loading = true; error = null
        viewModelScope.launch {
            val active = sources.filter { it.enabled }
            val gate = Semaphore(10)
            val results = withContext(Dispatchers.IO) {
                active.map { s -> async { gate.withPermit { runCatching { repo.fetchSource(s) } } } }.awaitAll()
            }
            val failures = results.count { it.isFailure }
            val viaFlipboard = active.filter { it.url.contains("flipboard.com/topic") }.map { it.id }.toSet()
            val perSource = (260 / active.size.coerceAtLeast(1)).coerceIn(6, 20)
            val pooled = dedupe(results.flatMap { it.getOrDefault(emptyList()) }
                .map { a -> if (a.sourceId in viaFlipboard) a.copy(sourceName = Catalog.publisherName(a.link)) else a })
                .groupBy { it.sourceId }.values
                .flatMap { it.sortedByDescending { a -> a.published }.take(perSource) }
                .map { a -> if (a.image == null) a.copy(image = imageMemo[a.id]) else a }
            when {
                pooled.isNotEmpty() -> {
                    Ranker.decay(profile)
                    articles = order(pooled); homeIndex = 0
                    prefs.saveArticles(articles); saveProfile()
                    enrichImages(articles)
                    if (failures > 0 && failures > active.size / 3) error = "$failures sources could not be loaded"
                }
                active.isEmpty() -> { articles = emptyList(); error = "No sources enabled" }
                else -> error = "Couldn't load news. Check your connection."
            }
            loading = false
        }
    }

    private fun order(list: List<Article>): List<Article> =
        if (smartFeed) Ranker.rank(list, profile, ::topicOf) else list.sortedByDescending { it.published }

    fun updateSmartFeed(v: Boolean) {
        smartFeed = v; prefs.smartFeed = v
        articles = order(articles); homeIndex = 0
    }

    fun resetLearning() {
        profile.topic.clear(); profile.opens.clear(); profile.skips.clear(); profile.keywords.clear(); profile.read.clear()
        saveProfile(); articles = order(articles); homeIndex = 0
    }

    private fun saveProfile() { prefs.profileJson = profile.toJson() }

    // ------------------------------------------------------------ learning

    private var lastShown = -1
    private var lastShownAt = 0L
    private var lastOpened = false

    /** The home pager settled on [idx]: if the previous story was flicked past quickly, count it as a skip. */
    fun onHomePage(idx: Int) {
        val now = System.currentTimeMillis()
        if (lastShown in articles.indices && lastShown != idx && !lastOpened && now - lastShownAt < 1600) {
            val a = articles[lastShown]; Ranker.learnSkip(profile, a, topicOf(a.sourceId))
        }
        lastShown = idx; lastShownAt = now; lastOpened = false
    }

    fun open(a: Article) {
        lastOpened = true
        Ranker.learnOpen(profile, a, topicOf(a.sourceId)); saveProfile()
        openArticle = a
    }

    fun onReaderDone(a: Article, millis: Long, finished: Boolean) {
        if (millis > 20_000 || finished) { Ranker.learnEngaged(profile, a, topicOf(a.sourceId), finished); saveProfile() }
    }

    // ------------------------------------------------------------- sources

    private fun saveSources(list: List<Source>) { sources = list; prefs.saveSources(list); sourcesDirty = true }

    fun closeSources() {
        showSources = false
        if (sourcesDirty) { sourcesDirty = false; refresh() }
    }

    fun toggleSource(id: String) { saveSources(sources.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }) }
    fun removeSource(id: String) { saveSources(sources.filterNot { it.id == id }) }
    fun hasUrl(url: String) = sources.any { it.url == url }
    fun addCatalog(c: CatalogSource) { if (!hasUrl(c.url)) saveSources(sources + c.toSource()) }
    fun removeUrl(url: String) { saveSources(sources.filterNot { it.url == url }) }

    /** Resolves a site or feed URL, adds it, and returns an error message (null on success). */
    suspend fun addSource(input: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val d = repo.discover(input)
            if (sources.any { it.url == d.feedUrl }) return@withContext "Already added"
            withContext(Dispatchers.Main) { saveSources(sources + Source(UUID.randomUUID().toString(), d.title, d.feedUrl)) }
            null
        }.getOrElse { it.message ?: "Couldn't add that source" }
    }

    /** Wizard result: replace catalog sources with the chosen ones (custom feeds stay) and tune the profile. */
    fun finishOnboarding(topics: Set<String>, chosen: List<CatalogSource>) {
        val custom = sources.filter { s -> Catalog.topicOfUrl(s.url) == null && s.topic == null }
        val keepDisabled = sources.filter { !it.enabled }.map { it.url }.toSet()
        saveSources(chosen.map { it.toSource().let { s -> if (s.url in keepDisabled) s.copy(enabled = false) else s } } + custom)
        Catalog.topics.forEach { t -> profile.topic[t.id] = if (t.id in topics) maxOf(profile.topic[t.id] ?: 1.0, 1.4) else minOf(profile.topic[t.id] ?: 1.0, 0.8) }
        saveProfile()
        prefs.onboarded = true; onboarded = true; showWizard = false; sourcesDirty = false
        refresh()
    }

    fun interestTopics(): Set<String> =
        sources.mapNotNull { it.topic ?: Catalog.topicOfUrl(it.url) }.toSet()

    // ------------------------------------------------------------ settings

    fun updateTheme(m: ThemeMode) { themeMode = m; prefs.themeMode = m }
    fun updateFontScale(v: Float) { fontScale = v; prefs.fontScale = v }
    fun updateTwoPage(v: Boolean) { twoPage = v; prefs.twoPage = v }

    // ------------------------------------------------------------ helpers

    private val imageMemo = HashMap<String, String>().apply { prefs.loadArticles().forEach { a -> a.image?.let { put(a.id, it) } } }

    /** Many feeds ship no picture: pull each story's og:image in the background. */
    private fun enrichImages(list: List<Article>) {
        val need = list.filter { it.image == null }.take(120)
        if (need.isEmpty()) return
        viewModelScope.launch {
            val gate = Semaphore(6)
            withContext(Dispatchers.IO) {
                need.map { a ->
                    async {
                        gate.withPermit {
                            val img = runCatching { repo.fetchOgImage(a.link) }.getOrNull()
                            if (img != null) synchronized(imageMemo) { imageMemo[a.id] = img }
                        }
                    }
                }.awaitAll()
            }
            // applied once, so pages don't change layout while being read
            val updated = articles.map { a -> if (a.image == null) a.copy(image = imageMemo[a.id]) else a }
            if (updated != articles) { articles = updated; prefs.saveArticles(updated) }
        }
    }

    /** The same story often arrives from several feeds: keep one (prefers the one with a picture). */
    private fun dedupe(list: List<Article>): List<Article> {
        fun canon(url: String) = runCatching {
            val u = java.net.URI(url.trim())
            (u.host ?: "").removePrefix("www.").lowercase() + (u.path ?: "").trimEnd('/').lowercase()
        }.getOrDefault(url.lowercase())
        fun title(t: String) = t.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
        fun best(g: List<Article>) = g.maxWithOrNull(
            compareBy<Article>({ it.image != null }, { it.contentHtml != null }, { it.summary.length })
        )!!
        val byLink = list.groupBy { canon(it.link) }.values.map { best(it) }
        return byLink.groupBy { a -> title(a.title).let { if (it.length < 12) "link:" + canon(a.link) else it } }
            .values.map { best(it) }
            .sortedByDescending { it.published }
    }

    fun cachedBlocks(a: Article): List<Block>? = blockCache[a.id]

    fun nextOf(a: Article): Article? {
        val i = articles.indexOfFirst { it.id == a.id }
        return if (i >= 0) articles.getOrNull(i + 1) else null
    }

    /** Continue straight into the following story (keeps the home flip position in sync). */
    fun advanceTo(next: Article) {
        val i = articles.indexOfFirst { it.id == next.id }
        if (i >= 0) homeIndex = i
        continued = true
        lastShown = i; lastShownAt = System.currentTimeMillis(); lastOpened = true
        Ranker.learnOpen(profile, next, topicOf(next.sourceId)); saveProfile()
        openArticle = next
    }

    /** Full article text, fetched and cleaned for the in-app reader. */
    suspend fun blocksFor(a: Article): List<Block> = withContext(Dispatchers.IO) {
        blockCache[a.id]?.let { return@withContext it }
        var body: List<Block>? = null
        val c = a.contentHtml
        if (!c.isNullOrBlank() && Jsoup.parse(c).text().length >= 1500) body = Extractor.fromFragment(c, a.link)
        if (body == null) body = runCatching { Extractor.fromHtml(repo.get(a.link), a.link) }.getOrNull()
            ?.takeIf { Extractor.textLength(it) >= 400 }
        if (body == null && !c.isNullOrBlank()) body = Extractor.fromFragment(c, a.link).takeIf { Extractor.textLength(it) >= 120 }
        if (body == null) body = listOf(Block.Text(TextKind.Para, a.summary.ifBlank { "This story has no preview text." }))

        val full = buildList {
            add(Block.Text(TextKind.Title, a.title))
            add(Block.Text(TextKind.Byline, bylineOf(a)))
            if (body.firstOrNull() !is Block.Image && a.image != null) add(Block.Image(a.image))
            addAll(body.dropWhile { it is Block.Text && it.text.trim().equals(a.title.trim(), true) })
            if (a.link.startsWith("http")) {
                val label = "Open the original article ↗"
                add(Block.Text(TextKind.Para, label, listOf(SpanInfo(0, label.length, true, false, a.link))))
            }
        }
        blockCache[a.id] = full
        full
    }

    fun bylineOf(a: Article): String {
        val date = if (a.published > 0) java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(a.published)) else ""
        return listOf(a.sourceName, date).filter { it.isNotBlank() }.joinToString("  ·  ")
    }
}
