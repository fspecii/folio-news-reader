package dev.folio.news.data

import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The "For You" algorithm. A tiny on-device model that learns from what you open, skip and finish:
 *   score = recency x interest(topic) x trust(source) x quality x (1 + keywords) x feed-rank,
 * then a diversity re-rank so you never get five stories from one site in a row, and a little
 * exploration so new interests can surface. Nothing leaves the phone.
 */
class Profile {
    val topic = HashMap<String, Double>()          // learned interest per topic, ~0..2.5
    val opens = HashMap<String, Int>()             // per source
    val skips = HashMap<String, Int>()
    val keywords = HashMap<String, Double>()       // title words you engage with
    val read = LinkedHashSet<String>()             // article ids already opened

    fun toJson(): String {
        val o = JSONObject()
        o.put("t", JSONObject(topic as Map<*, *>)); o.put("o", JSONObject(opens as Map<*, *>)); o.put("s", JSONObject(skips as Map<*, *>))
        val kw = keywords.entries.sortedByDescending { it.value }.take(300).associate { it.key to it.value }
        o.put("k", JSONObject(kw as Map<*, *>)); o.put("r", org.json.JSONArray(read.toList().takeLast(800)))
        return o.toString()
    }

    companion object {
        fun fromJson(j: String?): Profile {
            val p = Profile(); if (j == null) return p
            runCatching {
                val o = JSONObject(j)
                o.getJSONObject("t").let { x -> x.keys().forEach { k -> p.topic[k] = x.getDouble(k) } }
                o.getJSONObject("o").let { x -> x.keys().forEach { k -> p.opens[k] = x.getInt(k) } }
                o.getJSONObject("s").let { x -> x.keys().forEach { k -> p.skips[k] = x.getInt(k) } }
                o.getJSONObject("k").let { x -> x.keys().forEach { k -> p.keywords[k] = x.getDouble(k) } }
                o.getJSONArray("r").let { a -> for (i in 0 until a.length()) p.read.add(a.getString(i)) }
            }
            return p
        }
    }
}

object Ranker {
    private val STOP = ("the a an and or of to in on for with at by from is are was were be been it its this that as after before over under into about " +
        "new says say will can could how why what who when you your we our they their his her he she not no more most than up out one two").split(" ").toSet()

    fun tokens(title: String): List<String> =
        title.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length > 3 && it !in STOP }.distinct()

    fun learnOpen(p: Profile, a: Article, topicId: String?) {
        p.opens[a.sourceId] = (p.opens[a.sourceId] ?: 0) + 1
        topicId?.let { p.topic[it] = min(2.5, (p.topic[it] ?: 1.0) + 0.22) }
        tokens(a.title).forEach { p.keywords[it] = min(3.0, (p.keywords[it] ?: 0.0) + 0.45) }
        p.read.add(a.id)
    }

    fun learnSkip(p: Profile, a: Article, topicId: String?) {
        p.skips[a.sourceId] = (p.skips[a.sourceId] ?: 0) + 1
        topicId?.let { p.topic[it] = max(0.15, (p.topic[it] ?: 1.0) - 0.035) }
    }

    fun learnEngaged(p: Profile, a: Article, topicId: String?, finished: Boolean) {
        topicId?.let { p.topic[it] = min(2.5, (p.topic[it] ?: 1.0) + if (finished) 0.3 else 0.15) }
        tokens(a.title).forEach { p.keywords[it] = min(3.0, (p.keywords[it] ?: 0.0) + if (finished) 0.5 else 0.2) }
    }

    /** Called on each refresh so old habits fade. */
    fun decay(p: Profile) {
        p.keywords.replaceAll { _, v -> v * 0.96 }; p.keywords.entries.removeAll { it.value < 0.08 }
    }

    fun score(a: Article, p: Profile, topicOf: (String) -> String?, now: Long): Double {
        val ageH = if (a.published > 0) max(0.0, (now - a.published) / 3_600_000.0) else 24.0
        val recency = 0.5.pow(min(ageH, 96.0) / 10.0)
        val topicId = topicOf(a.sourceId)
        val interest = 0.55 + 0.7 * (p.topic[topicId] ?: 1.0).coerceIn(0.0, 2.5)
        val o = (p.opens[a.sourceId] ?: 0).toDouble(); val s = (p.skips[a.sourceId] ?: 0).toDouble()
        val trust = 0.75 + 0.75 * (o + 1.0) / (o + 0.4 * s + 3.0)
        val quality = 1.0 + (if (a.image != null) 0.14 else 0.0) + (if (a.summary.length > 80) 0.05 else 0.0)
        val kw = min(1.0, tokens(a.title).sumOf { p.keywords[it] ?: 0.0 } / 5.0)
        val feedRank = 1.0 + 0.25 * exp(-a.rank / 6.0)         // the feed's own editorial order
        var sc = recency * interest * trust * quality * (1.0 + 0.7 * kw) * feedRank
        if (a.id in p.read) sc *= 0.12
        return sc
    }

    fun rank(list: List<Article>, p: Profile, topicOf: (String) -> String?): List<Article> {
        val now = System.currentTimeMillis()
        val pool = list.map { it to score(it, p, topicOf, now) }.sortedByDescending { it.second }.toMutableList()
        val out = ArrayList<Article>(pool.size)
        val recentSrc = ArrayDeque<String>(); val recentTopic = ArrayDeque<String?>()
        var slot = 0
        while (pool.isNotEmpty()) {
            slot++
            var pick = -1
            // exploration: every 9th slot, take the best story from a topic you rarely see
            if (slot % 9 == 0) {
                val seenTopics = out.takeLast(30).map { topicOf(it.sourceId) }.toSet()
                pick = pool.indexOfFirst { topicOf(it.first.sourceId) !in seenTopics && it.first.id !in p.read }
            }
            if (pick < 0) {
                var best = -1.0
                for (i in 0 until min(pool.size, 40)) {
                    val (a, sc) = pool[i]
                    val sameSrc = recentSrc.count { it == a.sourceId }
                    val sameTopic = recentTopic.count { it == topicOf(a.sourceId) }
                    val adj = sc * 0.62.pow(sameSrc) * 0.86.pow(sameTopic)
                    if (adj > best) { best = adj; pick = i }
                }
            }
            val a = pool.removeAt(pick).first
            out += a
            recentSrc.addLast(a.sourceId); if (recentSrc.size > 4) recentSrc.removeFirst()
            recentTopic.addLast(topicOf(a.sourceId)); if (recentTopic.size > 3) recentTopic.removeFirst()
        }
        return out
    }
}
