package dev.folio.news.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Prefs(private val ctx: Context) {
    private val sp = ctx.getSharedPreferences("folio", Context.MODE_PRIVATE)

    fun loadSources(): List<Source> {
        val raw = sp.getString("sources", null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Source(o.getString("id"), o.getString("name"), o.getString("url"), o.optBoolean("enabled", true), if (o.has("topic")) o.getString("topic") else null)
            }
        }.getOrDefault(emptyList())
    }

    fun saveSources(list: List<Source>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().put("id", it.id).put("name", it.name).put("url", it.url).put("enabled", it.enabled).apply { it.topic?.let { t -> put("topic", t) } })
        }
        sp.edit().putString("sources", a.toString()).apply()
    }

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(sp.getString("theme", "System")!!) }.getOrDefault(ThemeMode.System)
        set(v) = sp.edit().putString("theme", v.name).apply()

    var fontScale: Float
        get() = sp.getFloat("fontScale", 1f)
        set(v) = sp.edit().putFloat("fontScale", v).apply()

    var twoPage: Boolean
        get() = sp.getBoolean("twoPage", true)
        set(v) = sp.edit().putBoolean("twoPage", v).apply()

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    var smartFeed: Boolean
        get() = sp.getBoolean("smartFeed", true)
        set(v) = sp.edit().putBoolean("smartFeed", v).apply()

    var profileJson: String?
        get() = sp.getString("profile", null)
        set(v) = sp.edit().putString("profile", v).apply()

    // ---- muted publishers ----
    var muted: Set<String>
        get() = sp.getStringSet("muted", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("muted", v).apply()

    var lastUpdateCheck: Long
        get() = sp.getLong("lastUpdateCheck", 0L)
        set(v) = sp.edit().putLong("lastUpdateCheck", v).apply()

    var autoUpdate: Boolean
        get() = sp.getBoolean("autoUpdate", true)
        set(v) = sp.edit().putBoolean("autoUpdate", v).apply()

    // ---- offline cache of the last fetched stories ----
    private val cacheFile get() = File(ctx.filesDir, "articles.json")
    private val magFile get() = File(ctx.filesDir, "magazines.json")

    private fun toJson(it: Article) = JSONObject().put("id", it.id).put("sid", it.sourceId).put("sn", it.sourceName)
        .put("t", it.title).put("s", it.summary).put("i", it.image ?: JSONObject.NULL)
        .put("l", it.link).put("p", it.published).put("c", it.contentHtml ?: JSONObject.NULL).put("r", it.rank)

    private fun fromJson(o: JSONObject) = Article(
        o.getString("id"), o.getString("sid"), o.getString("sn"), o.getString("t"), o.getString("s"),
        if (o.isNull("i")) null else o.getString("i"), o.getString("l"), o.getLong("p"),
        if (o.isNull("c")) null else o.getString("c"), o.optInt("r", 0),
    )

    fun saveArticles(list: List<Article>) {
        runCatching { cacheFile.writeText(JSONArray().also { a -> list.forEach { a.put(toJson(it)) } }.toString()) }
    }

    fun loadArticles(): List<Article> = runCatching {
        val a = JSONArray(cacheFile.readText())
        (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    // ---- magazines (collections of saved stories) ----
    fun saveMagazines(list: List<Magazine>) {
        runCatching {
            val a = JSONArray()
            list.forEach { m ->
                a.put(JSONObject().put("id", m.id).put("name", m.name).put("items", JSONArray().also { ia ->
                    // saved stories keep their text so they read offline; the HTML body is kept only for these
                    m.articles.forEach { ia.put(toJson(it)) }
                }))
            }
            magFile.writeText(a.toString())
        }
    }

    fun loadMagazines(): List<Magazine> = runCatching {
        val a = JSONArray(magFile.readText())
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            val ia = o.getJSONArray("items")
            Magazine(o.getString("id"), o.getString("name"), (0 until ia.length()).map { j -> fromJson(ia.getJSONObject(j)) })
        }
    }.getOrDefault(emptyList())
}
