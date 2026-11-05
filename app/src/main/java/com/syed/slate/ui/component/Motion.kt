package com.syed.slate.ui.component

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * One set of timings for every transition in the app: quick enough that nothing waits on an animation (the user asked
 * for smooth, not slow), long enough that a change reads as movement rather than a jump.
 */
object Motion {
    const val FAST = 160    // colour / state changes
    const val MEDIUM = 220  // expand / collapse, page changes

    fun <T> fast() = tween<T>(FAST, easing = FastOutSlowInEasing)
    fun <T> medium() = tween<T>(MEDIUM, easing = FastOutSlowInEasing)

    /** A block opening in place (explanation, answer, extended section). */
    val expand: EnterTransition = expandVertically(medium(), expandFrom = androidx.compose.ui.Alignment.Top) + fadeIn(tween(MEDIUM, delayMillis = 40))
    val collapse: ExitTransition = shrinkVertically(medium(), shrinkTowards = androidx.compose.ui.Alignment.Top) + fadeOut(fast())

    // Page changes: the new page fades in while sliding a short way, the old one fades out. Back runs the other way.
    // The default NavHost transition is a 700 ms crossfade, which felt sluggish.
    private const val SHIFT = 12 // 1/12 of the width: enough to show direction, small enough to stay calm
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(MEDIUM, easing = LinearOutSlowInEasing)) { it / SHIFT } + fadeIn(tween(MEDIUM))
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(MEDIUM, easing = FastOutSlowInEasing)) { -it / (SHIFT * 2) } + fadeOut(tween(FAST))
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(MEDIUM, easing = LinearOutSlowInEasing)) { -it / SHIFT } + fadeIn(tween(MEDIUM))
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(MEDIUM, easing = FastOutSlowInEasing)) { it / (SHIFT * 2) } + fadeOut(tween(FAST))
    }
}
