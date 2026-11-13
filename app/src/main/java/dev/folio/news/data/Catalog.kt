package dev.folio.news.data

import android.content.Context
import org.json.JSONObject

data class Topic(val id: String, val title: String, val blurb: String)

/** kind: "publisher" (a site's own RSS) or "flipboard" (a Flipboard topic feed). */
data class CatalogSource(val id: String, val name: String, val url: String, val topic: String, val kind: String, val featured: Boolean) {
    val isFlipboard get() = kind == "flipboard"
    fun toSource() = Source(id, name, url, true, topic)
}

/** The built-in library: ~800 verified feeds in 28 topics (see tools/ for how it was built). */
object Catalog {
    var topics: List<Topic> = emptyList(); private set
    var sources: List<CatalogSource> = emptyList(); private set
    private var byTopic: Map<String, List<CatalogSource>> = emptyMap()
    private var byUrl: Map<String, CatalogSource> = emptyMap()
    private var hostNames: Map<String, String> = emptyMap()

    private fun hostOf(url: String): String =
        runCatching { java.net.URI(url).host ?: "" }.getOrDefault("").lowercase().removePrefix("www.").removePrefix("feeds.").removePrefix("rss.")

    /** Real publisher name for an article link (used for Flipboard topic feeds, whose items come from many sites). */
    fun publisherName(link: String): String {
        val h = hostOf(link)
        return hostNames[h] ?: h.substringBeforeLast('.').substringAfterLast('.').replaceFirstChar { it.uppercase() }.ifBlank { h }
    }

    fun load(ctx: Context) {
        if (sources.isNotEmpty()) return
        runCatching {
            val o = JSONObject(ctx.assets.open("catalog.json").bufferedReader().use { it.readText() })
            val t = o.getJSONArray("topics")
            topics = (0 until t.length()).map { t.getJSONObject(it).let { x -> Topic(x.getString("id"), x.getString("title"), x.getString("blurb")) } }
            val s = o.getJSONArray("sources")
            sources = (0 until s.length()).map {
                val x = s.getJSONObject(it)
                CatalogSource(x.getString("id"), x.getString("name"), x.getString("url"), x.getString("topic"), x.getString("kind"), x.optBoolean("featured"))
            }
            byTopic = sources.groupBy { it.topic }
            byUrl = sources.associateBy { it.url }
            hostNames = sources.filter { !it.isFlipboard }.groupBy { hostOf(it.url) }.mapValues { (_, v) -> v.first().name.substringBefore(" \u00B7 ") }
        }
    }

    /** Featured first, then Flipboard topic feeds, then publishers alphabetically. */
    fun forTopic(id: String): List<CatalogSource> =
        (byTopic[id] ?: emptyList()).sortedWith(compareByDescending<CatalogSource> { it.featured }.thenBy { it.isFlipboard }.thenBy { it.name.lowercase() })

    fun featured(topicIds: Collection<String>): List<CatalogSource> =
        topicIds.flatMap { id -> (byTopic[id] ?: emptyList()).filter { it.featured } }.distinctBy { it.url }

    fun topicOfUrl(url: String): String? = byUrl[url]?.topic
    fun search(q: String): List<CatalogSource> {
        val n = q.trim().lowercase(); if (n.length < 2) return emptyList()
        return sources.filter { it.name.lowercase().contains(n) || it.url.lowercase().contains(n) }.take(60)
    }
}
