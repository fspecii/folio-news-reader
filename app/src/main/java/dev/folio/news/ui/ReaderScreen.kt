package dev.folio.news.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDecoration
import coil.compose.AsyncImage
import dev.folio.news.AppViewModel
import dev.folio.news.data.Article
import dev.folio.news.data.Block
import dev.folio.news.data.TextKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private sealed interface Item {
    val before: Float
    data class T(val text: AnnotatedString, val style: TextStyle, override val before: Float, val indent: Float, val bar: Boolean) : Item
    data class I(val url: String, val h: Float, override val before: Float) : Item
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(vm: AppViewModel, article: Article, next: Article?, onNext: (Article) -> Unit, onClose: () -> Unit) {
    val dark = isDark(vm.themeMode)
    val paper = if (dark) Color(0xFF111113) else Color(0xFFFBF8F1)
    val ink = if (dark) Color(0xFFDEDBD3) else Color(0xFF1B1A18)
    val sub = if (dark) Color(0xFF8F8B83) else Color(0xFF7A756B)
    val accent = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()

    val blocks by produceState<List<Block>?>(vm.cachedBlocks(article), article.id) {
        value = try { vm.blocksFor(article) } catch (e: Exception) {
            listOf(Block.Text(TextKind.Title, article.title), Block.Text(TextKind.Para, article.summary))
        }
    }
    // warm up the following story so continuing into it is instant
    LaunchedEffect(blocks, next?.id) { if (blocks != null && next != null) runCatching { vm.blocksFor(next) } }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val startedAt = remember { System.currentTimeMillis() }
    var finished by remember { mutableStateOf(false) }
    val finishedNow by rememberUpdatedState(finished)
    DisposableEffect(article.id) { onDispose { vm.onReaderDone(article, System.currentTimeMillis() - startedAt, finishedNow) } }

    Box(
        Modifier.fillMaxSize().background(paper)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        val b = blocks
        if (b == null) {
            Column(
                Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = accent, strokeWidth = 2.dp)
                Spacer(Modifier.height(20.dp))
                Text(article.title, color = sub, fontFamily = FontFamily.Serif, fontSize = 16.sp, maxLines = 3)
            }
            IconButton(onClick = onClose, Modifier.statusBarsPadding().padding(6.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ink)
            }
            return@Box
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val fs = vm.fontScale

            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()
            val twoUp = vm.twoPage && maxWidth >= 780.dp
            val margin = with(density) { (if (maxWidth >= 600.dp) 56.dp else 26.dp).toPx() }
            val gutter = with(density) { 72.dp.toPx() }
            val pageWpx = if (twoUp) ((wPx - 2 * margin - gutter) / 2f).toInt()
            else min(wPx - 2 * margin, with(density) { 640.dp.toPx() }).toInt()
            val topPad = WindowInsets.statusBars.getTop(density) + with(density) { 22.dp.toPx() }
            val botArea = WindowInsets.navigationBars.getBottom(density) + with(density) { 46.dp.toPx() }
            val contentHpx = (hPx - topPad - botArea).toInt()

            val pages = remember(b, pageWpx, contentHpx, fs, dark) {
                paginate(b, measurer, density, pageWpx, contentHpx, fs, ink, sub, accent)
            }
            val spreads = remember(pages, twoUp) { if (twoUp) pages.chunked(2) else pages.map { listOf(it) } }

            val endPage: List<Any>? = remember(next?.id, pageWpx, contentHpx, fs, dark) {
                next?.let { n ->
                    val cover = buildList {
                        add(Block.Text(TextKind.Title, n.title))
                        add(Block.Text(TextKind.Byline, vm.bylineOf(n)))
                        if (n.image != null) add(Block.Image(n.image))
                    }
                    paginate(cover, measurer, density, pageWpx, contentHpx, fs, ink, sub, accent).firstOrNull()
                }
            }
            val hasEnd = endPage != null
            val total = spreads.size + if (hasEnd) 1 else 0

            val curl = rememberCurlState(0)
            var frac by remember { mutableFloatStateOf(0f) }
            val nSpreads by rememberUpdatedState(spreads.size)
            val currentOnNext by rememberUpdatedState(onNext)
            val currentNext by rememberUpdatedState(next)
            LaunchedEffect(curl) {
                snapshotFlow { curl.settled }.collect {
                    frac = it.toFloat() / max(1, nSpreads)
                    if (nSpreads > 1 && it >= nSpreads - 1) finished = true
                    // swiped past the last page: carry on straight into the next story
                    if (hasEnd && it == nSpreads && nSpreads > 0) currentNext?.let { n -> currentOnNext(n) }
                }
            }
            LaunchedEffect(spreads.size) {
                val target = (frac * spreads.size).toInt().coerceIn(0, max(0, spreads.size - 1))
                if (target != curl.settled) curl.jump(target)
            }

            var chrome by remember { mutableStateOf(!vm.continued) }
            LaunchedEffect(Unit) { if (chrome) { delay(2200); chrome = false } }
            LaunchedEffect(Unit) { vm.continued = false }

            val pageWdp = with(density) { pageWpx.toDp() }
            val contentHdp = with(density) { contentHpx.toDp() }
            val topDp = with(density) { topPad.toDp() }

            CurlPager(
                count = total, state = curl, paper = paper, contentKey = Triple(fs, dark, pageWpx to contentHpx),
                modifier = Modifier.fillMaxSize(),
                onTap = { x, w ->
                    when {
                        x < w * 0.33f -> scope.launch { curl.curlY = 0.84f; if (curl.settled > 0 || curl.progress.value > 0f) curl.goTo((curl.progress.value.toInt() - if (curl.progress.value % 1f == 0f) 1 else 0).coerceAtLeast(0)) }
                        x > w * 0.67f -> scope.launch { curl.curlY = 0.84f; curl.goTo(curl.progress.value.toInt() + 1) }
                        else -> chrome = !chrome
                    }
                },
            ) { sp ->
                Box(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                        if (sp < spreads.size) {
                            val group = spreads[sp]
                            group.forEachIndexed { k, items ->
                                if (k == 1) Spacer(Modifier.width(with(density) { gutter.toDp() }))
                                val pageNo = if (twoUp) sp * 2 + k + 1 else sp + 1
                                PageColumn(items, article.sourceName, "$pageNo of ${pages.size}", pageWdp, contentHdp, topDp, sub)
                            }
                            if (twoUp && group.size == 1) {
                                Spacer(Modifier.width(with(density) { gutter.toDp() }))
                                Spacer(Modifier.width(pageWdp))
                            }
                        } else if (endPage != null) {
                            PageColumn(endPage, next?.sourceName ?: "", "Next article", pageWdp, contentHdp, topDp, sub)
                            if (twoUp) {
                                Spacer(Modifier.width(with(density) { gutter.toDp() }))
                                Spacer(Modifier.width(pageWdp))
                            }
                        }
                    }
                    if (twoUp && sp < spreads.size) {
                        // faint spine between the two pages
                        Box(
                            Modifier.align(Alignment.Center).width(1.dp).fillMaxHeight(0.7f)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, sub.copy(alpha = 0.35f), Color.Transparent)))
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = chrome, enter = fadeIn(tween(160)), exit = fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    Modifier.fillMaxWidth().background(paper.copy(alpha = 0.96f)).statusBarsPadding().padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ink) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { runCatching { uriHandler.openUri(article.link) } }) { Text("Open \u2197", color = accent, fontSize = 15.sp) }
                    TextButton(onClick = { vm.updateFontScale((vm.fontScale - 0.1f).coerceAtLeast(0.8f)) }) { Text("A−", color = ink, fontSize = 15.sp) }
                    TextButton(onClick = { vm.updateFontScale((vm.fontScale + 0.1f).coerceAtMost(1.6f)) }) { Text("A+", color = ink, fontSize = 20.sp) }
                    TextButton(onClick = { vm.showSettings = true }) { Text("Aa", color = ink, fontFamily = FontFamily.Serif, fontSize = 18.sp) }
                }
            }
        }
    }
}

@Composable
private fun PageColumn(
    items: List<Any>, footerLeft: String, footerRight: String,
    pageW: Dp, contentH: Dp, topPad: Dp, sub: Color,
) {
    @Suppress("UNCHECKED_CAST") val list = items as List<Item>
    val density = LocalDensity.current
    val barColor = MaterialTheme.colorScheme.primary
    Column(Modifier.width(pageW).fillMaxHeight().padding(top = topPad)) {
        Column(Modifier.width(pageW).height(contentH)) {
            for (it in list) {
                val before = with(density) { it.before.toDp() }
                when (it) {
                    is Item.T -> {
                        val tm = Modifier.padding(top = before).let { m ->
                            if (it.bar) m.drawWithContent {
                                drawContent()
                                drawRect(barColor, Offset.Zero, Size(3.dp.toPx(), size.height))
                            } else m
                        }
                        Box(tm) {
                            Text(
                                it.text, style = it.style,
                                modifier = Modifier.padding(start = with(density) { it.indent.toDp() }),
                            )
                        }
                    }
                    is Item.I -> AsyncImage(
                        it.url, null,
                        Modifier.padding(top = before).fillMaxWidth().height(with(density) { it.h.toDp() })
                            .clip(RoundedCornerShape(4.dp)).background(sub.copy(alpha = 0.12f)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        Row(Modifier.width(pageW).padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(footerLeft, color = sub, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
            Text(footerRight, color = sub, fontSize = 11.sp)
        }
    }
}

private fun paginate(
    blocks: List<Block>, measurer: TextMeasurer, density: androidx.compose.ui.unit.Density,
    pageW: Int, pageH: Int, fs: Float, ink: Color, sub: Color, accent: Color,
): List<List<Any>> {
    val dp = { v: Float -> with(density) { v.dp.toPx() } }
    val bodyLine = with(density) { (29f * fs).sp.toPx() }
    val lhs = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    val serif = FontFamily.Serif
    val body = TextStyle(fontFamily = serif, fontSize = (18f * fs).sp, lineHeight = (29f * fs).sp, color = ink, lineHeightStyle = lhs)
    val styles = mapOf(
        TextKind.Title to TextStyle(fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = (31f * fs).sp, lineHeight = (38f * fs).sp, color = ink, lineHeightStyle = lhs),
        TextKind.Byline to TextStyle(fontSize = (12f * fs).sp, lineHeight = (18f * fs).sp, color = sub, letterSpacing = 0.8.sp, lineHeightStyle = lhs),
        TextKind.Heading to TextStyle(fontFamily = serif, fontWeight = FontWeight.Bold, fontSize = (22f * fs).sp, lineHeight = (30f * fs).sp, color = ink, lineHeightStyle = lhs),
        TextKind.Para to body,
        TextKind.Bullet to body,
        TextKind.Quote to body.copy(fontStyle = FontStyle.Italic, color = ink.copy(alpha = 0.86f)),
    )
    val before = mapOf(
        TextKind.Title to 0f, TextKind.Byline to dp(10f), TextKind.Heading to dp(24f),
        TextKind.Para to dp(12f), TextKind.Bullet to dp(6f), TextKind.Quote to dp(16f),
    )

    val pages = ArrayList<List<Any>>()
    var cur = ArrayList<Item>()
    var used = 0f
    fun flush() { if (cur.isNotEmpty()) { pages += cur; cur = ArrayList(); used = 0f } }

    for (b in blocks) {
        when (b) {
            is Block.Image -> {
                val h = min(pageW * 9f / 16f, pageH * 0.42f)
                var sb = if (cur.isEmpty()) 0f else dp(18f)
                if (cur.isNotEmpty() && used + sb + h > pageH) { flush(); sb = 0f }
                cur += Item.I(b.url, h, sb); used += sb + h
            }
            is Block.Text -> {
                val style = styles.getValue(b.kind)
                val indent = if (b.kind == TextKind.Quote) dp(18f) else 0f
                val at = buildAnnotatedString {
                    append(b.text)
                    b.spans.forEach { s ->
                        if (s.end > b.text.length || s.start >= s.end) return@forEach
                        val st = SpanStyle(
                            fontWeight = if (s.bold) FontWeight.Bold else null,
                            fontStyle = if (s.italic) FontStyle.Italic else null,
                        )
                        addStyle(st, s.start, s.end)
                        if (s.url != null) addLink(
                            LinkAnnotation.Url(
                                s.url,
                                TextLinkStyles(SpanStyle(color = accent, textDecoration = TextDecoration.Underline)),
                            ),
                            s.start, s.end,
                        )
                    }
                }
                val layout = measurer.measure(at, style, constraints = Constraints(maxWidth = (pageW - indent).toInt().coerceAtLeast(50)))
                val total = layout.lineCount
                val firstH = layout.getLineBottom(0) - layout.getLineTop(0)
                var sb = if (cur.isEmpty()) 0f else before.getValue(b.kind)
                // keep a heading together with the first lines of what follows it
                if (b.kind == TextKind.Heading && cur.isNotEmpty() && used + sb + firstH + 2 * bodyLine > pageH) {
                    flush(); sb = 0f
                }
                var line = 0
                while (line < total) {
                    val avail = pageH - used - sb
                    val topY = layout.getLineTop(line)
                    var end = line
                    while (end < total && layout.getLineBottom(end) - topY <= avail) end++
                    val fit = end - line
                    if (fit == 0) {
                        if (cur.isNotEmpty()) { flush(); sb = 0f; continue } else end = line + 1
                    } else if (end < total) {
                        if (fit < 2 && cur.isNotEmpty()) { flush(); sb = 0f; continue }
                        if (total - end == 1 && fit >= 3) end -= 1
                    }
                    val h = layout.getLineBottom(end - 1) - topY
                    val sub = at.subSequence(layout.getLineStart(line), layout.getLineEnd(end - 1))
                    cur += Item.T(sub, style, sb, indent, b.kind == TextKind.Quote)
                    used += sb + h
                    line = end; sb = 0f
                    if (line < total) flush()
                }
            }
        }
    }
    flush()
    if (pages.isEmpty()) pages += emptyList<Item>()
    return pages
}
