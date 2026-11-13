package dev.folio.news.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A vertical "flip" pager: the lower half of the current page folds up around the centre
 * hinge and lands on the upper half, revealing the next page underneath.
 * Progress is a single float (0 .. count-1), so swiping back simply plays it in reverse.
 */
@Composable
fun FlipPager(
    count: Int,
    initialPage: Int,
    onSettled: (Int) -> Unit,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val maxIdx = (count - 1).coerceAtLeast(0)
    val maxP = maxIdx.toFloat()
    val progress = remember { Animatable(initialPage.coerceIn(0, maxIdx).toFloat()) }
    var height by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(count, initialPage) {
        // external reset (e.g. after refresh)
        if (progress.value > maxP) progress.snapTo(maxP)
        if (!progress.isRunning && initialPage != progress.value.roundToInt()) progress.snapTo(initialPage.coerceIn(0, maxIdx).toFloat())
    }

    val dragState = rememberDraggableState { dy ->
        scope.launch { progress.snapTo((progress.value - dy / height * 1.15f).coerceIn(0f, maxP)) }
    }

    val base by remember { derivedStateOf { floor(progress.value).toInt().coerceIn(0, maxIdx) } }
    val flipping by remember { derivedStateOf { progress.value - floor(progress.value) > 0.002f && progress.value < maxP } }
    val firstHalf by remember { derivedStateOf { progress.value - floor(progress.value) < 0.5f } }
    fun frac() = (progress.value - floor(progress.value)).coerceIn(0f, 1f)

    Box(
        modifier
            .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { v ->
                    scope.launch {
                        val cur = progress.value
                        val target = when {
                            v < -600f -> ceil(cur)
                            v > 600f -> floor(cur)
                            else -> cur.roundToInt().toFloat()
                        }.coerceIn(0f, maxP)
                        val dist = abs(target - cur)
                        progress.animateTo(target, tween((140 + 360 * dist).toInt(), easing = FastOutSlowInEasing))
                        onSettled(target.roundToInt())
                    }
                },
            )
            .pointerInput(count) {
                detectTapGestures { if (!progress.isRunning) onTap(progress.value.roundToInt().coerceIn(0, maxIdx)) }
            }
    ) {
        if (count == 0) return@Box
        if (!flipping) {
            Box(Modifier.fillMaxSize()) { page(progress.value.roundToInt().coerceIn(0, maxIdx)) }
        } else {
            val i = base
            val j = (base + 1).coerceAtMost(maxIdx)

            // revealed lower half: the NEXT page
            HalfBox(top = false, Modifier.align(Alignment.BottomCenter)) { page(j) }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.5f)
                    .drawBehind { drawRect(Color.Black.copy(alpha = 0.45f * (1f - frac()))) }
            )
            // upper half stays as the CURRENT page until the flap lands on it
            HalfBox(top = true, Modifier.align(Alignment.TopCenter)) { page(i) }
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().fillMaxHeight(0.5f)
                    .drawBehind { drawRect(Color.Black.copy(alpha = 0.40f * frac())) }
            )

            if (firstHalf) {
                // lower half of the current page swings up toward the viewer
                HalfBox(
                    top = false,
                    Modifier.align(Alignment.BottomCenter).graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        rotationX = 180f * frac()
                        cameraDistance = 14f * density * 6f
                    }
                ) { page(i) }
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.5f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            rotationX = 180f * frac()
                            cameraDistance = 14f * density * 6f
                        }
                        .drawBehind { drawRect(Color.Black.copy(alpha = 0.35f * frac() * 2f)) }
                )
            } else {
                // back face of the flap: the top of the next page, settling onto the upper half
                HalfBox(
                    top = true,
                    Modifier.align(Alignment.TopCenter).graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        rotationX = -180f * (1f - frac())
                        cameraDistance = 14f * density * 6f
                    }
                ) { page(j) }
                Box(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth().fillMaxHeight(0.5f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            rotationX = -180f * (1f - frac())
                            cameraDistance = 14f * density * 6f
                        }
                        .drawBehind { drawRect(Color.Black.copy(alpha = 0.35f * (1f - (frac() - 0.5f) * 2f))) }
                )
            }
        }
    }
}

/** Shows only the top or bottom half of a full-size page. */
@Composable
private fun HalfBox(top: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier.fillMaxWidth().clipToBounds()) { measurables, c ->
        val w = c.maxWidth
        val full = c.maxHeight
        val half = full / 2
        val p = measurables.first().measure(Constraints.fixed(w, full))
        val h = if (top) half else full - half
        layout(w, h) { p.place(0, if (top) 0 else -half) }
    }
}
