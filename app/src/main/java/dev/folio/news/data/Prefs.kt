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

    // ---- offline cache of the last fetched stories ----
    private val cacheFile get() = File(ctx.filesDir, "articles.json")

    fun saveArticles(list: List<Article>) {
        runCatching {
            val a = JSONArray()
            list.forEach {
                a.put(
                    JSONObject().put("id", it.id).put("sid", it.sourceId).put("sn", it.sourceName)
                        .put("t", it.title).put("s", it.summary).put("i", it.image ?: JSONObject.NULL)
                        .put("l", it.link).put("p", it.published).put("c", it.contentHtml ?: JSONObject.NULL).put("r", it.rank)
                )
            }
            cacheFile.writeText(a.toString())
        }
    }

    fun loadArticles(): List<Article> = runCatching {
        val a = JSONArray(cacheFile.readText())
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Article(
                o.getString("id"), o.getString("sid"), o.getString("sn"), o.getString("t"), o.getString("s"),
                if (o.isNull("i")) null else o.getString("i"), o.getString("l"), o.getLong("p"),
                if (o.isNull("c")) null else o.getString("c"), o.optInt("r", 0),
            )
        }
    }.getOrDefault(emptyList())
}
