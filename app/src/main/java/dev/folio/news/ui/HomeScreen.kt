package dev.folio.news.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.folio.news.AppViewModel
import dev.folio.news.data.Article

private val Serif = FontFamily.Serif

@Composable
fun HomeScreen(vm: AppViewModel) {
    val bg = MaterialTheme.colorScheme.background
    var panelOpen by remember { mutableStateOf(false) }
    var acc by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(panelOpen) { if (panelOpen) { delay(6000); panelOpen = false } }
    // A horizontal swipe anywhere on the home screen shows/hides the panel; vertical swipes still flip pages.
    // The gesture sits on the PARENT so the flip pager underneath keeps receiving its own touches.
    Box(
        Modifier.fillMaxSize().background(bg).draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { acc += it },
            onDragStarted = { acc = 0f },
            onDragStopped = { if (acc > 70f) panelOpen = true else if (acc < -70f) panelOpen = false },
        )
    ) {
        val list = vm.feed
        if (list.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (vm.loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("Fetching your news…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Nothing to read yet", fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(vm.error ?: "Add a source to get started.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    FilledTonalButton(onClick = { vm.showSources = true }) { Text("Manage sources") }
                }
            }
        } else {
            // page 0 is the daily briefing cover, story i lives on page i + 1
            val brief = vm.briefing()
            FlipPager(
                count = list.size + 1,
                initialPage = vm.homeIndex + 1,
                onSettled = { p -> vm.homeIndex = p - 1; if (p > 0) vm.onHomePage(p - 1) },
                onTap = { p, y ->
                    if (p == 0) {
                        val k = (y * BRIEF_WEIGHT - BRIEF_HEAD).toInt()
                        brief.getOrNull(k)?.let { a -> vm.open(a) }
                    } else list.getOrNull(p - 1)?.let { a -> vm.open(a) }
                },
                modifier = Modifier.fillMaxSize(),
            ) { p -> if (p == 0) BriefingPage(brief, vm.section?.let { id -> dev.folio.news.data.Catalog.topics.firstOrNull { it.id == id }?.title }) else StoryPage(list[p - 1], p - 1) }
        }

        // legibility scrim so system icons stay readable over any photo
        Box(
            Modifier.fillMaxWidth().height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 36.dp)
                .background(Brush.verticalGradient(listOf(bg.copy(alpha = 0.85f), Color.Transparent)))
        )
        SidePanel(vm, panelOpen) { panelOpen = it }
    }
}

/** Logo + actions live in a left panel that stays hidden until you swipe right. */
@Composable
private fun SidePanel(vm: AppViewModel, open: Boolean, setOpen: (Boolean) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        if (vm.loading) LinearProgressIndicator(
            Modifier.fillMaxWidth().statusBarsPadding().height(2.dp),
            color = MaterialTheme.colorScheme.primary, trackColor = Color.Transparent,
        )
        AnimatedVisibility(open, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
            ) { setOpen(false) })
        }
        AnimatedVisibility(
            open,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
        ) {
            Column(
                Modifier.fillMaxHeight().width(240.dp).background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Folio", fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
                Spacer(Modifier.height(28.dp))
                PanelItem(Icons.Default.Refresh, if (vm.loading) "Refreshing\u2026" else "Refresh") { vm.refresh(); setOpen(false) }
                PanelItem(Icons.Default.Menu, "Sections") { setOpen(false); vm.showSections = true }
                PanelItem(Icons.Default.Favorite, "Magazines") { setOpen(false); vm.showMagazines = true }
                PanelItem(Icons.AutoMirrored.Filled.List, "Sources") { setOpen(false); vm.showSources = true }
                PanelItem(Icons.Default.Star, "Interests") { setOpen(false); vm.showWizard = true }
                PanelItem(Icons.Default.Settings, "Settings") { setOpen(false); vm.showSettings = true }
            }
        }
    }
}

@Composable
private fun PanelItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

fun relTime(ms: Long): String {
    if (ms <= 0) return ""
    val d = System.currentTimeMillis() - ms
    val m = d / 60000
    return when {
        m < 1 -> "now"
        m < 60 -> "${m}m ago"
        m < 60 * 24 -> "${m / 60}h ago"
        m < 60 * 24 * 7 -> "${m / 1440}d ago"
        else -> ""
    }
}

@Composable
fun StoryPage(a: Article, index: Int) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= 700.dp
        val tpl = if (a.image == null) 3 else index % 3
        when (tpl) {
            0 -> HeroStory(a, wide)
            1 -> SplitStory(a, wide, imageFirst = true)
            2 -> SplitStory(a, wide, imageFirst = false)
            else -> TextStory(a, wide)
        }
    }
}

@Composable
private fun Meta(a: Article, color: Color) {
    val t = relTime(a.published)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(a.sourceName.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.4.sp)
        if (t.isNotEmpty()) Text("   $t", color = color, fontSize = 12.sp)
    }
}

@Composable
private fun HeroStory(a: Article, wide: Boolean) {
    Box(Modifier.fillMaxSize()) {
        AsyncImage(a.image, null, Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f))))
        Column(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding()
                .padding(start = if (wide) 64.dp else 24.dp, end = if (wide) 220.dp else 24.dp, bottom = 40.dp),
        ) {
            Text(a.sourceName.uppercase(), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(10.dp))
            Text(a.title, color = Color.White, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = if (wide) 44.sp else 33.sp, lineHeight = if (wide) 50.sp else 38.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
            if (a.summary.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(a.summary, color = Color.White.copy(alpha = 0.82f), fontSize = if (wide) 18.sp else 15.sp, lineHeight = if (wide) 26.sp else 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            val t = relTime(a.published)
            if (t.isNotEmpty()) { Spacer(Modifier.height(10.dp)); Text(t, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) }
        }
    }
}

@Composable
private fun SplitStory(a: Article, wide: Boolean, imageFirst: Boolean) {
    val bg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    val image: @Composable (Modifier) -> Unit = { m ->
        AsyncImage(a.image, null, m.background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
    }
    val text: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.Center) {
            Meta(a, sub)
            Spacer(Modifier.height(12.dp))
            Text(a.title, color = onBg, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = if (wide) 40.sp else 30.sp, lineHeight = if (wide) 46.sp else 36.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
            if (a.summary.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(a.summary, color = sub, fontSize = if (wide) 18.sp else 16.sp, lineHeight = if (wide) 27.sp else 24.sp, maxLines = if (wide) 8 else 5, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    if (wide) {
        Row(Modifier.fillMaxSize().background(bg)) {
            if (imageFirst) {
                image(Modifier.weight(1.1f).fillMaxHeight())
                text(Modifier.weight(1f).fillMaxHeight().statusBarsPadding().navigationBarsPadding().padding(horizontal = 48.dp, vertical = 56.dp))
            } else {
                text(Modifier.weight(1f).fillMaxHeight().statusBarsPadding().navigationBarsPadding().padding(horizontal = 48.dp, vertical = 56.dp))
                image(Modifier.weight(1.1f).fillMaxHeight())
            }
        }
    } else {
        Column(Modifier.fillMaxSize().background(bg)) {
            if (imageFirst) {
                image(Modifier.weight(1.15f).fillMaxWidth())
                text(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp))
            } else {
                text(Modifier.weight(1f).fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 20.dp))
                image(Modifier.weight(1.15f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TextStory(a: Article, wide: Boolean) {
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = if (wide) 120.dp else 28.dp, vertical = 56.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Meta(a, sub)
        Spacer(Modifier.height(16.dp))
        Text(a.title, color = MaterialTheme.colorScheme.onBackground, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = if (wide) 52.sp else 38.sp, lineHeight = if (wide) 58.sp else 44.sp, maxLines = 7, overflow = TextOverflow.Ellipsis)
        if (a.summary.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Text(a.summary, color = sub, fontSize = if (wide) 20.sp else 17.sp, lineHeight = if (wide) 30.sp else 26.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
        }
    }
}

private const val BRIEF_HEAD = 1.1f
private const val BRIEF_WEIGHT = 6.1f   // header weight + five rows

/** Flipboard-style "daily edition" cover: date, greeting and the top stories from different publishers. */
@Composable
fun BriefingPage(items: List<Article>, section: String? = null) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val now = remember { java.util.Calendar.getInstance() }
    val hello = when (now.get(java.util.Calendar.HOUR_OF_DAY)) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    val date = remember { java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(java.util.Date()) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp)) {
        Column(Modifier.weight(BRIEF_HEAD).fillMaxWidth(), verticalArrangement = Arrangement.Bottom) {
            Text(date.uppercase(), color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(6.dp))
            Text(hello, color = onBg, fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 40.sp)
            Text(if (section == null) "Your briefing" else "Your briefing \u00b7 $section", color = sub, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
        }
        for (i in 0 until 5) {
            val a = items.getOrNull(i)
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", color = accent, fontFamily = Serif, fontWeight = FontWeight.Black, fontSize = 28.sp, modifier = Modifier.width(34.dp))
                if (a != null) Column(Modifier.weight(1f)) {
                    Text(a.sourceName.uppercase(), color = sub, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp, maxLines = 1)
                    Text(a.title, color = onBg, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            if (i < 4) HorizontalDivider(color = sub.copy(alpha = 0.18f))
        }
        Text("\u2191 Swipe up to flip", color = sub.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp))
    }
}
