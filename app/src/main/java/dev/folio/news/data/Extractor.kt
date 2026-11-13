package dev.folio.news.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URL

/** Turns an article page (or an RSS content fragment) into clean reading blocks. */
object Extractor {
    private val WS = Regex("\\s+")
    private val NOISE = "script,style,noscript,nav,footer,aside,form,iframe,svg,button,select,dialog," +
        ".ad,.ads,[class*=advert],[class*=newsletter],[class*=social-share],[class*=share-bar]," +
        "[id*=comments],[class*=comments],[class*=related-],[aria-hidden=true],[hidden]"
    private val BLOCK = setOf(
        "p", "div", "section", "article", "main", "ul", "ol", "li", "blockquote", "figure",
        "h1", "h2", "h3", "h4", "h5", "h6", "pre", "table", "img", "picture", "figcaption",
    )
    private val SKIP = setOf("script", "style", "noscript", "svg", "iframe", "button", "form", "nav", "aside", "footer", "table", "video", "audio", "template")
    private val JUNK = setOf(
        "advertisement", "ad", "read more", "read more:", "share this", "related", "sign up", "subscribe",
        "story continues below advertisement", "continue reading", "watch:", "listen:",
    )

    private val TAIL = Regex("^(most popular|trending|related( stories| articles| posts)?|more from .*|recommended( for you)?|read next|read more|you may also like|more stories|latest from .*|also on .*|comments?)$", RegexOption.IGNORE_CASE)
    private val NOISE_LINE = Regex("^(follow topics and authors|this is the title for the native ad|sign up for .*newsletter|subscribe to .*|advertisement|share this( story| article)?|photograph:?.*|click to expand)", RegexOption.IGNORE_CASE)

    fun fromHtml(html: String, base: String): List<Block> {
        val doc = Jsoup.parse(html, base)
        doc.select(NOISE).remove()
        return finish(pickRoot(doc))
    }

    fun fromFragment(html: String, base: String): List<Block> {
        val doc = Jsoup.parseBodyFragment(html, base)
        return finish(doc.body())
    }

    private fun finish(root: Element): List<Block> {
        val out = ArrayList<Block>()
        walk(root, out, root.baseUri())
        return clean(out)
    }

    private fun pickRoot(doc: Document): Element {
        val sels = listOf(
            "[itemprop=articleBody]", "article [class*=article-body]", "article [class*=entry-content]",
            "[class*=post-content]", "[class*=entry-content]", "[class*=article-body]", "[class*=story-body]",
            "[class*=article__body]", "article", "main",
        )
        for (s in sels) {
            val e = doc.selectFirst(s)
            if (e != null && e.select("p").sumOf { it.text().length } >= 400) return e
        }
        val best = doc.select("div, section, article, main")
            .maxByOrNull { el -> el.children().filter { it.tagName() == "p" }.sumOf { it.text().length } }
        return best ?: doc.body()
    }

    private fun walk(el: Element, out: MutableList<Block>, base: String) {
        for (child in el.childNodes()) {
            when (child) {
                is TextNode -> {
                    val t = child.text().trim()
                    if (t.length > 80 && el.tagName() in setOf("div", "section", "article", "main", "body"))
                        out += Block.Text(TextKind.Para, t)
                }
                is Element -> handle(child, out, base)
                else -> {}
            }
        }
    }

    private fun handle(e: Element, out: MutableList<Block>, base: String) {
        val tag = e.tagName()
        when {
            tag in SKIP -> {}
            tag == "p" -> { addText(TextKind.Para, e, out); e.select("img").forEach { addImage(it, null, out, base) } }
            tag.length == 2 && tag[0] == 'h' && tag[1] in '1'..'6' -> addText(TextKind.Heading, e, out)
            tag == "blockquote" -> addText(TextKind.Quote, e, out)
            tag == "pre" -> addText(TextKind.Para, e, out)
            tag == "ul" || tag == "ol" -> e.children().filter { it.tagName() == "li" }.forEachIndexed { i, li ->
                addText(TextKind.Bullet, li, out, if (tag == "ol") "${i + 1}. " else "• ")
            }
            tag == "figure" -> addImage(e.selectFirst("img"), e.selectFirst("figcaption")?.text(), out, base)
            tag == "picture" -> addImage(e.selectFirst("img"), null, out, base)
            tag == "img" -> addImage(e, null, out, base)
            else -> {
                val hasBlock = e.children().any { it.tagName() in BLOCK }
                if (hasBlock) walk(e, out, base)
                else if (e.text().length > 40) addText(TextKind.Para, e, out)
            }
        }
    }

    private class Inline { val sb = StringBuilder(); val spans = ArrayList<SpanInfo>() }

    private fun addText(kind: TextKind, e: Element, out: MutableList<Block>, prefix: String = "") {
        val ctx = Inline()
        ctx.sb.append(prefix)
        e.childNodes().forEach { inline(it, ctx, false, false, null) }
        val text = ctx.sb.toString().trimEnd()
        if (text.length < 2 || text.trim().length <= prefix.trim().length) return
        val spans = ctx.spans.mapNotNull {
            val en = minOf(it.end, text.length)
            if (en > it.start) it.copy(end = en) else null
        }
        out += Block.Text(kind, text, spans)
    }

    private fun inline(n: Node, ctx: Inline, bold: Boolean, italic: Boolean, url: String?) {
        when (n) {
            is TextNode -> {
                var t = WS.replace(n.wholeText, " ")
                if (ctx.sb.isEmpty() || ctx.sb.last() == ' ' || ctx.sb.last() == '\n') t = t.trimStart()
                if (t.isNotEmpty()) {
                    val s = ctx.sb.length
                    ctx.sb.append(t)
                    if (bold || italic || url != null) ctx.spans += SpanInfo(s, ctx.sb.length, bold, italic, url)
                }
            }
            is Element -> when (n.tagName()) {
                "br" -> ctx.sb.append('\n')
                in SKIP, "img" -> {}
                "b", "strong" -> n.childNodes().forEach { inline(it, ctx, true, italic, url) }
                "i", "em", "cite" -> n.childNodes().forEach { inline(it, ctx, bold, true, url) }
                "a" -> {
                    val href = n.absUrl("href").takeIf { it.startsWith("http") }
                    n.childNodes().forEach { inline(it, ctx, bold, italic, href ?: url) }
                }
                else -> n.childNodes().forEach { inline(it, ctx, bold, italic, url) }
            }
            else -> {}
        }
    }

    private fun addImage(img: Element?, caption: String?, out: MutableList<Block>, base: String) {
        if (img == null) return
        val url = imageUrl(img, base) ?: return
        val w = img.attr("width").toIntOrNull(); val h = img.attr("height").toIntOrNull()
        if ((w != null && w < 120) || (h != null && h < 80)) return
        val l = url.lowercase()
        if (l.endsWith(".svg") || "1x1" in l || "pixel" in l || "spacer" in l || "/logo" in l || "avatar" in l) return
        out += Block.Image(url, caption?.takeIf { it.isNotBlank() })
    }

    private fun imageUrl(img: Element, base: String): String? {
        var u = listOf("data-src", "data-lazy-src", "data-original").map { img.absUrl(it) }.firstOrNull { it.isNotBlank() } ?: ""
        if (u.isBlank()) u = img.absUrl("src")
        if (u.isBlank() || u.startsWith("data:")) {
            val ss = img.attr("srcset").ifBlank { img.attr("data-srcset") }
            val last = ss.split(",").map { it.trim().substringBefore(" ") }.lastOrNull { it.isNotBlank() }
            if (last != null) u = runCatching { URL(URL(base), last).toString() }.getOrDefault("")
        }
        return u.takeIf { it.startsWith("http") }
    }

    private fun clean(list: List<Block>): List<Block> {
        val out = ArrayList<Block>()
        val seenImg = HashSet<String>()
        for (b in list) {
            when (b) {
                is Block.Text -> {
                    if (b.text.trim().lowercase() in JUNK) continue
                    val prev = out.lastOrNull()
                    if (prev is Block.Text && prev.text == b.text) continue
                    out += b
                }
                is Block.Image -> if (seenImg.add(b.url.substringBefore('?'))) out += b
            }
        }
        // everything from a "Most popular / Related / More from…" block onwards is page furniture, not the story
        val cut = out.indexOfFirst { b ->
            b is Block.Text && (b.kind == TextKind.Heading || b.text.length < 40) && TAIL.containsMatchIn(b.text.trim())
        }
        if (cut > 3) while (out.size > cut) out.removeAt(out.size - 1)
        out.removeAll { b -> b is Block.Text && NOISE_LINE.containsMatchIn(b.text.trim()) }
        // a trailing heading with nothing after it is noise
        while (out.lastOrNull().let { it is Block.Text && it.kind == TextKind.Heading }) out.removeAt(out.size - 1)
        return out
    }

    fun textLength(blocks: List<Block>) = blocks.sumOf { (it as? Block.Text)?.text?.length ?: 0 }
}
