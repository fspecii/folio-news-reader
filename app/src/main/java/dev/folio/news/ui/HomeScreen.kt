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
        val list = vm.articles
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
            FlipPager(
                count = list.size,
                initialPage = vm.homeIndex,
                onSettled = { vm.homeIndex = it; vm.onHomePage(it) },
                onTap = { list.getOrNull(it)?.let { a -> vm.open(a) } },
                modifier = Modifier.fillMaxSize(),
            ) { i -> StoryPage(list[i], i) }
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
