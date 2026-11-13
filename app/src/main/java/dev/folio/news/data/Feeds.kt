package dev.folio.news.data

import android.util.Xml
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URL
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

const val UA = "Mozilla/5.0 (Linux; Android 14; SM-F946B) AppleWebKit/537.36 (KHTML, like Gecko) " +
    "Chrome/126.0 Mobile Safari/537.36"

data class Discovered(val feedUrl: String, val title: String)

class FeedRepository(private val client: OkHttpClient) {

    fun get(url: String): String {
        val req = Request.Builder().url(url).header("User-Agent", UA)
            .header("Accept", "text/html,application/xhtml+xml,application/xml,application/rss+xml,application/atom+xml,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9").build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            return r.body?.string() ?: ""
        }
    }

    /** Reads og:image / twitter:image from the start of an article page. */
    fun fetchOgImage(url: String): String? {
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val src = r.body?.source() ?: return null
            src.request(250_000)
            val html = src.buffer.clone().readUtf8()
            val doc = Jsoup.parse(html, url)
            val u = doc.selectFirst("meta[property=og:image], meta[name=og:image], meta[name=twitter:image], meta[property=twitter:image]")
                ?.absUrl("content")
            return u?.takeIf { it.startsWith("http") }
        }
    }

    fun fetchSource(s: Source): List<Article> = parseFeed(get(s.url), s)

    /** Accepts a feed URL or any website URL and finds the feed. */
    fun discover(input: String): Discovered {
        var url = input.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        val body = get(url)
        if (looksLikeFeed(body)) return Discovered(url, feedTitle(body) ?: URL(url).host)

        val doc = Jsoup.parse(body, url)
        val link = doc.select("link[rel~=(?i)alternate][type~=(?i)(rss|atom)\\+xml], link[type~=(?i)application/(rss|atom)\\+xml]")
            .firstOrNull()?.absUrl("href")
        val candidates = buildList {
            if (!link.isNullOrBlank()) add(link)
            val root = URL(url).let { "${it.protocol}://${it.host}" }
            listOf("/feed", "/rss", "/feed.xml", "/rss.xml", "/atom.xml", "/index.xml").forEach { add(root + it) }
        }
        for (c in candidates) {
            val txt = runCatching { get(c) }.getOrNull() ?: continue
            if (looksLikeFeed(txt)) {
                val t = feedTitle(txt) ?: doc.title().ifBlank { URL(url).host }
                return Discovered(c, t)
            }
        }
        error("No feed found at that address")
    }

    private fun looksLikeFeed(body: String): Boolean {
        val head = body.take(3000).lowercase()
        return "<rss" in head || "<feed" in head || "<rdf:rdf" in head
    }

    private fun feedTitle(xml: String): String? = runCatching {
        val p = newParser(xml)
        var inItem = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                if (p.name == "item" || p.name == "entry") inItem = true
                else if (p.name == "title" && !inItem) return@runCatching p.nextText().trim().ifBlank { null }
            }
            ev = p.next()
        }
        null
    }.getOrNull()

    private fun newParser(xml: String): XmlPullParser {
        val p = Xml.newPullParser()
        runCatching { p.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true) }
        p.setInput(StringReader(xml.trimStart('﻿', ' ', '\n', '\r', '\t')))
        return p
    }

    fun parseFeed(xml: String, s: Source): List<Article> {
        val p = newParser(xml)
        val out = ArrayList<Article>()
        var rank = 0
        var inItem = false
        var title = ""; var link = ""; var desc = ""; var content = ""; var date = ""; var img: String? = null

        fun text(): String = runCatching { p.nextText() }.getOrDefault("")

        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> {
                    val n = p.name
                    if (n == "item" || n == "entry") {
                        inItem = true; title = ""; link = ""; desc = ""; content = ""; date = ""; img = null
                    } else if (inItem) when (n) {
                        "title" -> title = text()
                        "link" -> {
                            val href = p.getAttributeValue(null, "href")
                            if (href != null) {
                                val rel = p.getAttributeValue(null, "rel")
                                if (rel == null || rel == "alternate") link = href
                            } else link = text()
                        }
                        "description", "summary" -> desc = text()
                        "content:encoded", "content" -> content = text()
                        "pubDate", "published", "updated", "dc:date" -> { val t = text(); if (date.isEmpty()) date = t }
                        "media:content", "media:thumbnail" -> {
                            val u = p.getAttributeValue(null, "url")
                            val type = p.getAttributeValue(null, "type") ?: ""
                            val medium = p.getAttributeValue(null, "medium") ?: ""
                            if (u != null && img == null && (n == "media:thumbnail" || medium == "image" || type.startsWith("image") || (type.isEmpty() && medium.isEmpty()))) img = u
                        }
                        "enclosure" -> {
                            val u = p.getAttributeValue(null, "url"); val type = p.getAttributeValue(null, "type") ?: ""
                            if (u != null && type.startsWith("image") && img == null) img = u
                        }
                    }
                }
                XmlPullParser.END_TAG -> if ((p.name == "item" || p.name == "entry") && inItem) {
                    inItem = false
                    val html = content.ifBlank { desc }
                    val sum = Jsoup.parse(desc.ifBlank { content }).text().trim().take(320)
                    val image = img ?: Jsoup.parse(html).selectFirst("img[src]")?.attr("src")
                        ?.takeIf { it.startsWith("http") && "1x1" !in it && "pixel" !in it }
                    val cleanTitle = Jsoup.parse(title).text().trim()
                    if (cleanTitle.isNotEmpty() && link.isNotBlank()) {
                        out += Article(
                            id = (s.id + link).hashCode().toUInt().toString(16),
                            sourceId = s.id, sourceName = s.name, title = cleanTitle, summary = sum,
                            image = image, link = cleanLink(link.trim()), published = parseDate(date),
                            contentHtml = content.ifBlank { null }, rank = rank++,
                        )
                    }
                }
            }
            ev = p.next()
        }
        return out
    }

    /** Drops utm_* style tracking parameters so the same story from two feeds looks identical. */
    private fun cleanLink(u: String): String {
        val q = u.indexOf('?')
        if (q < 0) return u
        val kept = u.substring(q + 1).split('&').filter { p ->
            val k = p.substringBefore('=').lowercase()
            !(k.startsWith("utm_") || k in setOf("fbclid", "gclid", "ref", "cmpid", "ncid", "source", "mc_cid", "mc_eid"))
        }
        return u.substring(0, q) + if (kept.isEmpty()) "" else "?" + kept.joinToString("&")
    }

    private fun parseDate(s: String): Long {
        val t = s.trim()
        if (t.isEmpty()) return 0
        for (f in listOf(DateTimeFormatter.RFC_1123_DATE_TIME, DateTimeFormatter.ISO_OFFSET_DATE_TIME, DateTimeFormatter.ISO_ZONED_DATE_TIME)) {
            runCatching { return ZonedDateTime.parse(t, f).toInstant().toEpochMilli() }
        }
        for (pat in listOf("EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, d MMM yyyy HH:mm:ss zzz", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd HH:mm:ss")) {
            runCatching { return SimpleDateFormat(pat, Locale.US).parse(t)!!.time }
        }
        return 0
    }
}
