package com.devscion.lingualink.ui.theme

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Two CompositionLocals that screens can opt into to share elements between
 * navigation destinations. Both are nullable so screens stay usable in
 * isolation (previews, isolated tests) — when the locals are null, the
 * shared-modifier helpers fall through and return the receiver unchanged.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Stable keys for elements shared across destinations. */
object SharedKeys {
    const val HERO_ORB = "hero-orb"
    const val LANG_PILL = "lang-pill"
    const val BRAND_MARK = "brand-mark"
}

/**
 * Tag a composable as the anchor for a cross-screen shared transition. When
 * the shared-transition scopes are present in the composition, the matching
 * keys on source + destination morph between their bounds. When absent (e.g.
 * inside a Preview), this is a no-op.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedAcrossScreens(key: String): Modifier {
    val sts = LocalSharedTransitionScope.current ?: return this
    val avs = LocalAnimatedVisibilityScope.current ?: return this
    return with(sts) {
        this@sharedAcrossScreens.sharedElement(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = avs,
        )
    }
}

/**
 * Same as [sharedAcrossScreens] but for elements where source/destination
 * differ in shape or content — uses [SharedTransitionScope.sharedBounds] so
 * the bounds animate while each side renders its own content.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsAcrossScreens(key: String): Modifier {
    val sts = LocalSharedTransitionScope.current ?: return this
    val avs = LocalAnimatedVisibilityScope.current ?: return this
    return with(sts) {
        this@sharedBoundsAcrossScreens.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = avs,
        )
    }
}
