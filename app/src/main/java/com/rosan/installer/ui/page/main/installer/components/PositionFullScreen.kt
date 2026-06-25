// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A full-screen equivalent of [PositionDialog].
 *
 * Unlike [PositionDialog], it is **not** hosted in a `Dialog` window, so the install UI
 * fills the entire activity area. This is used when the configured
 * [com.rosan.installer.domain.settings.model.config.InstallMode] is `FullScreen`, so the
 * install confirmation, progress, and result screens render edge-to-edge (behind the
 * status / navigation bars) instead of as a centered card.
 *
 * Layout structure (top-to-bottom):
 * 1. **Header** (icon, title, subtitle) — rendered once, stays constant while the
 *    install flow progresses. Padded by `2 × systemBars.top` so the title sits a
 *    full status-bar height below the system bar.
 * 2. **Body** (text / content) — fills the remaining area. Its own height is
 *    independent of the footer, so changing the buttons row never causes the
 *    body to reflow (no flicker). Wrapped in [AnimatedContent] on [contentKey]
 *    change for a same-position cross-fade.
 * 3. **Footer** (buttons) — overlaid on top of the body's bottom edge (just
 *    like the dialog's button row), so its size change doesn't push the body
 *    around. Its own height is animated with `animateDpAsState` so the body's
 *    bottom padding transitions smoothly.
 *
 * Enter / exit transition:
 * The fullscreen layer is a single alpha-animated surface. On first composition
 * it fades in (`alpha 0 → 1` over [EnterExitDurationMs]).
 *
 * The exit transition is driven by an **external** `isClosing` signal (typically
 * a [Boolean] state owned by the [com.rosan.installer.ui.page.main.installer.InstallerViewModel]).
 * This is intentional: the exit *teardown* (closing the session, resetting
 * [com.rosan.installer.ui.page.main.installer.InstallerStage.Ready]) must NOT
 * happen until the fade has had time to play. If it were driven by a
 * composition-bound coroutine (e.g. `rememberCoroutineScope().launch { animateTo;
 * onBackRequest }`), the synchronous session teardown would cause the
 * composable to leave composition at t=0 and cancel the fade-out — the user
 * would see the install UI "snap" away. By pushing the teardown into a
 * `viewModelScope` (whose lifetime is decoupled from the composition), the
 * fade gets a guaranteed 220ms to play, and the session is only torn down
 * after that window.
 *
 * The background [Surface] uses [RectangleShape] by default so the rounded
 * corners (or any window blur the system applies) are not exposed in the
 * status / navigation bar areas.
 */
@Composable
fun PositionFullScreen(
    modifier: Modifier = Modifier,
    onBackRequest: () -> Unit,
    shape: Shape = RectangleShape,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    iconContentColor: Color = MaterialTheme.colorScheme.secondary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation: Dp = 0.dp,
    contentKey: Any? = null,
    /**
     * External "begin exit" signal. When this flips from `false` to `true`
     * the fullscreen layer fades out (alpha 1 → 0) over [EnterExitDurationMs],
     * and the caller is expected to perform the actual teardown
     * (`session.close()` etc.) on its own — typically from a `viewModelScope`
     * coroutine that delays for at least [EnterExitDurationMs] before
     * touching the session. See the KDoc on this function for why the
     * teardown must not run inside a composition-bound coroutine.
     */
    isClosing: Boolean = false,
    leftIcon: @Composable (() -> Unit)? = null,
    centerIcon: @Composable (() -> Unit)? = null,
    rightIcon: @Composable (() -> Unit)? = null,
    leftTitle: @Composable (() -> Unit)? = null,
    centerTitle: @Composable (() -> Unit)? = null,
    rightTitle: @Composable (() -> Unit)? = null,
    leftSubtitle: @Composable (() -> Unit)? = null,
    centerSubtitle: @Composable (() -> Unit)? = null,
    rightSubtitle: @Composable (() -> Unit)? = null,
    leftText: @Composable (() -> Unit)? = null,
    centerText: @Composable (() -> Unit)? = null,
    rightText: @Composable (() -> Unit)? = null,
    leftContent: @Composable (() -> Unit)? = null,
    centerContent: @Composable (() -> Unit)? = null,
    rightContent: @Composable (() -> Unit)? = null,
    leftButton: @Composable (() -> Unit)? = null,
    centerButton: @Composable (() -> Unit)? = null,
    rightButton: @Composable (() -> Unit)? = null
) {
    // Layer alpha — animates the whole fullscreen layer in and out.
    // enter: 0 → 1 (EnterExitDurationMs)
    // exit:  1 → 0 (EnterExitDurationMs) — same spec as enter so the exit
    // feels symmetric. The exit is started by [LaunchedEffect] reacting to
    // the [isClosing] signal (NOT by an inline `coroutineScope.launch`),
    // because the teardown that follows the exit must outlive the
    // composition.
    val uiAlpha = remember { Animatable(0f) }
    // Re-entrancy guard for the back gesture. A second back press during
    // the exit fade must not retrigger the close path.
    var isDismissing by remember { mutableStateOf(false) }

    // Enter: fade the install UI in on the very first composition.
    LaunchedEffect(Unit) {
        uiAlpha.animateTo(1f, animationSpec = tween(durationMillis = EnterExitDurationMs))
    }

    // Exit: driven by the external [isClosing] signal. When the caller
    // (typically the ViewModel's `requestFullscreenClose`) flips this to
    // true, we fade the layer out and *do nothing else* — the caller is
    // responsible for tearing down the session ~220ms later on its own
    // scope. Doing the teardown here would let the synchronous close()
    // path (e.g. `close()` setting `stage = InstallerStage.Ready`)
    // immediately remove this composable from the tree, cancelling
    // `animateTo` mid-flight and producing the "instant disappear" bug.
    LaunchedEffect(isClosing) {
        if (isClosing && !isDismissing) {
            isDismissing = true
            uiAlpha.animateTo(0f, animationSpec = tween(durationMillis = EnterExitDurationMs))
        }
    }

    // Re-enter after an in-flow back navigation. When the user backs out
    // of a submenu (e.g. InstallExtendedSubMenu -> InstallExtendedMenu),
    // the caller routes to the parent stage without disposing
    // [PositionFullScreen]. The exit fade has already taken alpha to 0,
    // so we re-run the enter fade on the new [contentKey] to bring the
    // layer back in. The `isDismissing` guard makes sure we don't re-run
    // this on the very first composition (where the first LaunchedEffect
    // already handled the enter fade).
    LaunchedEffect(contentKey) {
        if (isDismissing) {
            isDismissing = false
            uiAlpha.animateTo(1f, animationSpec = tween(durationMillis = EnterExitDurationMs))
        }
    }

    // Back press (predictive-back and legacy). Just forwards to the
    // caller — the caller is expected to flip [isClosing] on its own
    // scope (e.g. `viewModel.requestFullscreenClose()`). We deliberately
    // do NOT add a scrim-tap handler on the outer Box: a real fullscreen
    // app does not dismiss itself on arbitrary background taps — the
    // user should use explicit buttons or the system back gesture.
    val triggerBack: () -> Unit = {
        if (!isDismissing) onBackRequest()
    }
    BackHandler(enabled = true) {
        triggerBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(uiAlpha.value)
    ) {
        // Surface (and therefore the container color) extends edge-to-edge.
        // The shape defaults to RectangleShape so the blur is not exposed in
        // the rounded corners under the status / navigation bar areas.
        Surface(
            modifier = modifier.fillMaxSize(),
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation
        ) {
            val topInset = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
            val navInset = WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding()
            // Reserve a full status-bar height above the safe area for the
            // header so the title sits visibly below the system status bar.
            val headerTopPadding = topInset * 2

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerTopPadding)
            ) {
                // ---- Stable header (icon, title, subtitle) ----
                // Rendered once and not animated; the caller passes composables
                // that are stable across the install flow (e.g. based on the
                // current package, not the current stage).
                PositionChildWidget(
                    leftIcon, centerIcon, rightIcon
                ) { icon ->
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(
                            modifier = Modifier
                                .padding(IconPadding)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            icon?.invoke()
                        }
                    }
                }
                PositionChildWidget(
                    leftTitle, centerTitle, rightTitle
                ) { title ->
                    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                            Box(
                                modifier = Modifier
                                    .padding(TitlePadding)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                title?.invoke()
                            }
                        }
                    }
                }
                PositionChildWidget(
                    leftSubtitle, centerSubtitle, rightSubtitle
                ) { subtitle ->
                    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            Box(
                                modifier = Modifier
                                    .padding(SubtitlePadding)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                subtitle?.invoke()
                            }
                        }
                    }
                }

                // ---- Body + Footer area ----
                // Body and footer are independent children of this Box so
                // resizing the buttons row doesn't reflow the body. The body's
                // bottom padding tracks the (animated) footer height, so when
                // the buttons change, the body's content area shrinks/grows
                // smoothly — no flicker.
                val contentMode =
                    leftContent != null || centerContent != null || rightContent != null

                var footerHeightPx by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current
                val footerHeight: Dp =
                    (footerHeightPx / density.density).dp
                val animatedFooterHeight by animateDpAsState(
                    targetValue = footerHeight,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "fs_footer_height"
                )

                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .fillMaxWidth()
                ) {
                    // Body fills the entire body+footer area minus the
                    // (animated) footer height. The bottom padding change
                    // is what "slides" the body content out of the way of
                    // a resizing buttons row.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = animatedFooterHeight.coerceAtLeast(0.dp))
                    ) {
                        val bodyTransitionSpec:
                                AnimatedContentTransitionScope<Any?>.() -> androidx.compose.animation.ContentTransform = {
                            // Same-position cross-fade. SizeTransform is
                            // included so any size delta between the two
                            // body snapshots is animated too, instead of
                            // producing a one-frame visual jump.
                            fadeIn(animationSpec = tween(durationMillis = 220)) togetherWith
                                    fadeOut(animationSpec = tween(durationMillis = 180)) using
                                    androidx.compose.animation.SizeTransform(
                                        clip = false
                                    ) { _, _ -> tween(durationMillis = 220) }
                        }
                        if (contentKey != null) {
                            AnimatedContent(
                                targetState = contentKey,
                                transitionSpec = bodyTransitionSpec,
                                label = "FullScreenBodyTransition"
                            ) { _ ->
                                PositionChildWidget(
                                    if (contentMode) leftContent else leftText,
                                    if (contentMode) centerContent else centerText,
                                    if (contentMode) rightContent else rightText
                                ) { text ->
                                    CompositionLocalProvider(LocalContentColor provides textContentColor) {
                                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(if (contentMode) ContentPadding else TextPadding)
                                                    .fillMaxWidth()
                                            ) {
                                                text?.invoke()
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            PositionChildWidget(
                                if (contentMode) leftContent else leftText,
                                if (contentMode) centerContent else centerText,
                                if (contentMode) rightContent else rightText
                            ) { text ->
                                CompositionLocalProvider(LocalContentColor provides textContentColor) {
                                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                                        Box(
                                            modifier = Modifier
                                                .padding(if (contentMode) ContentPadding else TextPadding)
                                                .fillMaxWidth()
                                        ) {
                                            text?.invoke()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Footer — overlaid at the bottom edge, identical in
                    // shape to how the dialog's button row sits. Its height
                    // is reported back via onSizeChanged so the body can
                    // reserve room for it.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = navInset)
                            .onSizeChanged { footerHeightPx = it.height }
                    ) {
                        val footerTransitionSpec:
                                AnimatedContentTransitionScope<Any?>.() -> androidx.compose.animation.ContentTransform = {
                            // Buttons row is small enough that a plain
                            // cross-fade + matching SizeTransform is enough
                            // — it never needs to "slide in" from anywhere.
                            fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                                    fadeOut(animationSpec = tween(durationMillis = 160)) using
                                    androidx.compose.animation.SizeTransform(
                                        clip = false
                                    ) { _, _ -> tween(durationMillis = 200) }
                        }
                        if (contentKey != null) {
                            AnimatedContent(
                                targetState = contentKey,
                                transitionSpec = footerTransitionSpec,
                                label = "FullScreenFooterTransition"
                            ) { _ ->
                                PositionChildWidget(
                                    leftButton, centerButton, rightButton
                                ) { button ->
                                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                                        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                                            Box(modifier = Modifier.padding(ButtonPadding)) {
                                                button?.invoke()
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            PositionChildWidget(
                                leftButton, centerButton, rightButton
                            ) { button ->
                                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                                    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                                        Box(modifier = Modifier.padding(ButtonPadding)) {
                                            button?.invoke()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val EnterExitDurationMs = 220

private val DialogSinglePadding = 16.dp
private val IconPadding =
    PaddingValues.Absolute(left = DialogSinglePadding, right = DialogSinglePadding, bottom = 12.dp)
private val TitlePadding =
    PaddingValues.Absolute(left = DialogSinglePadding, right = DialogSinglePadding, bottom = 4.dp)
private val SubtitlePadding =
    PaddingValues.Absolute(left = DialogSinglePadding, right = DialogSinglePadding, bottom = 12.dp)
private val TextPadding =
    PaddingValues.Absolute(left = DialogSinglePadding, right = DialogSinglePadding, bottom = 12.dp)
private val ContentPadding = PaddingValues.Absolute(bottom = 8.dp)
private val ButtonPadding = PaddingValues(
    start = DialogSinglePadding,
    end = DialogSinglePadding,
    bottom = 0.dp
)
