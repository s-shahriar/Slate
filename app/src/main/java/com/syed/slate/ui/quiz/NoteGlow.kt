package com.syed.slate.ui.quiz

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * The web apps' saved-note glare (`.has-note` in index.css): a chip that holds a note keeps catching the eye so an
 * annotated question stands out on the card. Everything animated is drawn OUTSIDE the chip — the fill, border and
 * glyph colour are untouched, so the icon stays as readable as a plain chip. One 2.4 s loop drives both layers:
 *  - a thin ring that starts on the chip's edge, spreads 7 dp out and fades (the web's expanding box-shadow "ping"),
 *  - a soft band hugging the edge that breathes in and out (the web's drop-shadow on the icon).
 * Put [noteGlow] BEFORE `clip()` in the modifier chain, or the chip's clip would cut the rings off at its bounds; the
 * chip's own area is masked out here anyway so a translucent "on" background never shows the glow through it.
 */
fun Modifier.noteGlow(active: Boolean, color: Color, shape: Shape): Modifier = if (!active) this else composed {
    val t = glowPhase("noteGlow")
    drawBehind {
        val chip = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }
        clipPath(chip, ClipOp.Difference) {
            val ping = 1f - (1f - t) * (1f - t) // ease-out, like the web keyframe
            val stroke = 1.5.dp.toPx()
            ringAround(shape, 7.dp.toPx() * ping + stroke / 2, stroke, color.copy(alpha = .6f * (1f - ping)))
            val band = 5.dp.toPx()
            ringAround(shape, band / 2, band, color.copy(alpha = .22f * sin(t * PI).toFloat()))
        }
    }
}

/** The glyph's share of the glare: a gentle breathe in size, in step with [noteGlow]. Keeps the icon fully opaque. */
fun Modifier.noteGlowIcon(active: Boolean): Modifier = if (!active) this else composed {
    val t = glowPhase("noteGlowIcon")
    graphicsLayer {
        val s = 1f + .08f * sin(t * PI).toFloat()
        scaleX = s
        scaleY = s
    }
}

private const val PERIOD_MS = 2400

@Composable
private fun glowPhase(label: String): Float {
    val t by rememberInfiniteTransition(label = label).animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PERIOD_MS, easing = LinearEasing), RepeatMode.Restart), label = "$label.t",
    )
    return t
}

/** A stroke of [shape] inflated by [offset] on every side, centred [offset] outside the chip's edge. */
private fun DrawScope.ringAround(shape: Shape, offset: Float, strokeWidth: Float, color: Color) {
    val outline = shape.createOutline(Size(size.width + offset * 2, size.height + offset * 2), layoutDirection, this)
    translate(-offset, -offset) { drawOutline(outline, color, style = Stroke(strokeWidth)) }
}
