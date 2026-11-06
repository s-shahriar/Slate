package com.syed.slate.ui.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

/**
 * One set of timings for every transition in the app: quick enough that nothing waits on an animation (the user asked
 * for smooth, not slow), long enough that a change reads as movement rather than a jump.
 */
object Motion {
    const val FAST = 160    // colour / state changes
    const val MEDIUM = 220  // expand / collapse (page changes are instant, see SlateNav)

    fun <T> fast() = tween<T>(FAST, easing = FastOutSlowInEasing)
    fun <T> medium() = tween<T>(MEDIUM, easing = FastOutSlowInEasing)

    /** A block opening in place (explanation, answer, extended section). */
    val expand: EnterTransition = expandVertically(medium(), expandFrom = androidx.compose.ui.Alignment.Top) + fadeIn(tween(MEDIUM, delayMillis = 40))
    val collapse: ExitTransition = shrinkVertically(medium(), shrinkTowards = androidx.compose.ui.Alignment.Top) + fadeOut(fast())
}
