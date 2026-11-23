// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppCOUIIcons
import com.rosan.installer.ui.page.coui.widgets.CouiSheetPopupHost
import com.rosan.installer.ui.page.coui.widgets.CouiSheetPopupProvider
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.theme.CouiProgressiveEdgeBand
import com.rosan.installer.ui.theme.ProvideCouiMaterialBackdrop
import com.rosan.installer.ui.theme.colorOsSpring
import com.rosan.installer.ui.theme.rememberCouiBlurBackdrop
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.effect.LocalCOUIMaterialEffectEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import top.yukonga.miuix.kmp.blur.layerBackdrop

/** Reserved even when installing has no actions, so the status mark never moves. */
val FullScreenBottomBarHeight = 70.dp

/** Single horizontal rhythm shared by the header, cards and actions. */
val FullScreenHorizontalPadding = 16.dp

/** COUI's `SmallTopAppBar` height. */
val FullScreenTopBarHeight = 56.dp

internal val LocalFullScreenActionsEnabled = staticCompositionLocalOf { true }

/**
 * Top space a page must reserve for the fixed chrome, which is also the height of the progressive
 * band above it. Scrolling pages put it in their content padding so the list can run behind the
 * chrome; fixed pages put it in their outer padding. Zero when there is no content blur to sample.
 */
internal val LocalFullScreenTopBleed = staticCompositionLocalOf { 0.dp }

/** Bottom counterpart of [LocalFullScreenTopBleed], sized by the reserved action row. */
internal val LocalFullScreenBottomBleed = staticCompositionLocalOf { 0.dp }

/**
 * The full-screen installer surface.
 *
 * The bar and the action row are drawn exactly once, here, and never take part in a page change:
 * letting each page draw its own bar put two sets of icon buttons on the same pixels during a
 * push, which reads as the buttons flashing grey. Only [content] moves between pages, and it is
 * the only thing recorded into the content blur source.
 *
 * The ViewModel owns teardown; this shell notifies [onCloseAnimationFinished] when the exit fade
 * finishes (with a ViewModel fallback timeout if the callback never arrives).
 */
@Composable
fun CouiPositionFullScreen(
    title: String,
    isClosing: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    endAction: (@Composable () -> Unit)? = null,
    endActionKey: Any? = endAction != null,
    useBlur: Boolean = false,
    bottomBar: (@Composable () -> Unit)? = null,
    onCloseAnimationFinished: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val inspectionMode = LocalInspectionMode.current
    val pageColor = COUITheme.colorScheme.backgroundWithCard.copy(alpha = 1f)
    // Two sources with separate jobs. The static one feeds card/button material and popups; the
    // content one records the laid-out page so the fixed chrome can blur what scrolls behind it.
    // Both are null when blur is off or unsupported, which is what every consumer degrades on.
    val backdrop = if (inspectionMode) null else rememberCouiBlurBackdrop(useBlur)
    val contentBackdrop = if (inspectionMode) null else rememberCouiBlurBackdrop(useBlur)
    val currentOnCloseAnimationFinished by rememberUpdatedState(onCloseAnimationFinished)
    // Start transparent at runtime for symmetric enter fade; previews start opaque.
    val uiAlpha = remember { Animatable(if (inspectionMode) 1f else 0f) }
    LaunchedEffect(isClosing) {
        uiAlpha.animateTo(
            targetValue = if (isClosing) 0f else 1f,
            animationSpec = tween(InstallerViewModel.FullscreenCloseFadeOutMs.toInt()),
        )
        if (isClosing) {
            currentOnCloseAnimationFinished()
        }
    }

    // Always consume system back while fullscreen is mounted; invoke onBack only when allowed.
    BackHandler {
        if (!isClosing && backEnabled) onBack()
    }

    CompositionLocalProvider(
        // Material (edge light, inner shadow, touch spotlight) only when an internal source
        // exists. COUI keeps its opaque fill on its own when the local switch is off.
        LocalCOUIMaterialEffectEnabled provides (backdrop != null),
    ) {
        CouiSheetPopupProvider(backdrop) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = uiAlpha.value
                        compositingStrategy = if (uiAlpha.value < 1f) {
                            CompositingStrategy.Offscreen
                        } else {
                            CompositingStrategy.Auto
                        }
                    }
                    .onPreviewKeyEvent { isClosing }
                    .then(
                        if (isClosing) {
                            Modifier
                                .clearAndSetSemantics { }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                        }
                                    }
                                }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                // Static material source: the opaque page colour plus a static colour wash. Kept
                // apart from the content recording so material surfaces never sample themselves,
                // and so progress or log updates do not re-record it.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
                ) {
                    FullScreenBackgroundWash(base = pageColor)
                }

                ProvideCouiMaterialBackdrop(backdrop) {
                    Box(Modifier.fillMaxSize()) {
                        val density = LocalDensity.current
                        var fixedTopHeightPx by remember { mutableIntStateOf(0) }
                        val fixedTopHeight = with(density) { fixedTopHeightPx.toDp() }
                        val bottomBarReservedPadding =
                            FullScreenBottomBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        // The band and the viewport both run to the screen edge: the ramp's opaque,
                        // fully blurred end then sits off-screen instead of drawing a rectangle edge
                        // across the page wash. Content at rest starts where the ramp reaches clear.
                        // Without a content source the viewport stops at the chrome as it did before.
                        val topBleed = if (contentBackdrop != null) fixedTopHeight else 0.dp
                        val bottomBleed = if (contentBackdrop != null) bottomBarReservedPadding else 0.dp
                        val viewportTopInset = (fixedTopHeight - topBleed).coerceAtLeast(0.dp)

                        // Recorded content. The wash is repeated here so the sampled layer carries
                        // the same base colour as the page instead of transparent margins.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(contentBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
                        ) {
                            FullScreenBackgroundWash(base = pageColor)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(
                                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                                    )
                                    .padding(
                                        top = viewportTopInset,
                                        bottom = (bottomBarReservedPadding - bottomBleed).coerceAtLeast(0.dp),
                                    ),
                            ) {
                                CompositionLocalProvider(
                                    LocalFullScreenTopBleed provides topBleed,
                                    LocalFullScreenBottomBleed provides bottomBleed,
                                ) {
                                    content()
                                }
                            }
                        }

                        // Progressive band over the whole fixed top chrome, clear at its bottom edge.
                        if (contentBackdrop != null) {
                            CouiProgressiveEdgeBand(
                                backdrop = contentBackdrop,
                                fromTop = true,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(topBleed),
                            )
                        }

                        // Fixed chrome: one bar, never part of a page change. Its height is stable,
                        // so sub-page pushes keep the released horizontal motion with no vertical
                        // shift of the content viewport.
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .onSizeChanged { fixedTopHeightPx = it.height }
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                                ),
                        ) {
                            FullScreenTopBar(
                                title = title,
                                onBack = onBack,
                                backEnabled = backEnabled && !isClosing,
                                endAction = endAction,
                                endActionKey = endActionKey,
                            )
                        }

                        val lastBottomBar = remember { FullScreenSlotRef(bottomBar) }
                        if (bottomBar != null) {
                            lastBottomBar.value = bottomBar
                        }
                        val bottomBarAlpha by animateFloatAsState(
                            targetValue = if (bottomBar != null) 1f else 0f,
                            animationSpec = tween(durationMillis = if (bottomBar != null) 180 else 120),
                            label = "FullScreenBottomBarAlpha",
                        )
                        if (bottomBarAlpha > 0.01f) {
                            // Only pages that actually have an action row get a bottom band; a page
                            // without buttons must not keep a mask while the old row fades out.
                            if (contentBackdrop != null && bottomBar != null) {
                                CouiProgressiveEdgeBand(
                                    backdrop = contentBackdrop,
                                    fromTop = false,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(bottomBarReservedPadding)
                                        .graphicsLayer { alpha = bottomBarAlpha },
                                )
                            }
                            val bottomBarActive = bottomBar != null && !isClosing
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = bottomBarAlpha
                                        translationY = (1f - bottomBarAlpha) * 16.dp.toPx()
                                        compositingStrategy = if (bottomBarAlpha < 1f) {
                                            CompositingStrategy.Offscreen
                                        } else {
                                            CompositingStrategy.Auto
                                        }
                                    }
                                    .then(
                                        if (!bottomBarActive) {
                                            Modifier
                                                .clearAndSetSemantics { }
                                                .pointerInput(Unit) {
                                                    awaitPointerEventScope {
                                                        while (true) {
                                                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                                        }
                                                    }
                                                }
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .windowInsetsPadding(
                                        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                                    )
                                    .height(FullScreenBottomBarHeight),
                            ) {
                                lastBottomBar.value?.invoke()
                            }
                        }
                    }
                }

                if (!inspectionMode) {
                    CouiSheetPopupHost()
                }
            }
        }
    }
}

/**
 * The bar shared by every page of the full-screen installer.
 *
 * Drawn as an overlay rather than a `Row` so the title stays optically centred regardless of how
 * many action buttons are present, and the label cross-fades in place when a sub page renames it.
 */
@Composable
private fun FullScreenTopBar(
    title: String,
    onBack: () -> Unit,
    backEnabled: Boolean,
    endAction: (@Composable () -> Unit)?,
    endActionKey: Any?,
) {
    Box(Modifier.fillMaxWidth().height(FullScreenTopBarHeight)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = FullScreenHorizontalPadding,
                    end = FullScreenHorizontalPadding - 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backTint by animateColorAsState(
                targetValue = if (backEnabled) {
                    COUITheme.colorScheme.labelPrimary
                } else {
                    COUITheme.colorScheme.disabledNeutral
                },
                animationSpec = tween(durationMillis = 180),
                label = "FullScreenBackTint",
            )
            IconButton(
                onClick = { if (backEnabled) onBack() },
                enabled = backEnabled,
            ) {
                Icon(
                    imageVector = AppCOUIIcons.Back,
                    contentDescription = stringResource(R.string.back),
                    tint = backTint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = endActionKey,
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    (
                        scaleIn(
                            animationSpec = colorOsSpring(responseSeconds = 0.28f),
                            initialScale = 0f,
                        ) togetherWith scaleOut(
                            animationSpec = tween(durationMillis = 160),
                            targetScale = 0f,
                        )
                        ).using(SizeTransform(clip = false))
                },
                label = "FullScreenEndAction",
            ) { slotKey ->
                val isSlotActive = slotKey == endActionKey
                val frozenAction = remember { FullScreenSlotRef(endAction) }
                if (isSlotActive) {
                    frozenAction.value = endAction
                }
                val actionContent = frozenAction.value
                if (actionContent != null) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .then(
                                if (!isSlotActive) {
                                    Modifier
                                        .clearAndSetSemantics { }
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    awaitPointerEvent(PointerEventPass.Initial)
                                                        .changes
                                                        .forEach { it.consume() }
                                                }
                                            }
                                        }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        actionContent()
                    }
                } else {
                    Spacer(Modifier.size(56.dp))
                }
            }
        }
        AnimatedContent(
            targetState = title,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                (
                    fadeIn(tween(durationMillis = 200)) +
                        slideInVertically(tween(durationMillis = 220)) { it / 3 }
                    ) togetherWith (
                    fadeOut(tween(durationMillis = 140)) +
                        slideOutVertically(tween(durationMillis = 140)) { -it / 3 }
                    )
            },
            modifier = Modifier.align(Alignment.Center).clipToBounds(),
            label = "FullScreenTitle",
        ) { label ->
            Text(
                text = label,
                style = COUITheme.textStyles.headlineS,
                color = COUITheme.colorScheme.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 72.dp),
            )
        }
    }
}

/**
 * The static colour wash of the full-screen surface.
 *
 * A flat colour would stay a flat colour no matter the blur radius, so the backing layer carries
 * a little large-scale variation: two soft radial tints over the page colour. It is deliberately
 * static — no animated shader and no per-frame texture, so the recorded source stays cheap and is
 * never re-recorded by progress or log updates.
 */
@Composable
private fun FullScreenBackgroundWash(base: Color, modifier: Modifier = Modifier) {
    val colorScheme = COUITheme.colorScheme
    val accent = colorScheme.containerTheme
    val assist = colorScheme.assist

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.05f),
                        radius = size.minDimension * 0.9f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(assist.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.95f),
                        radius = size.minDimension * 0.8f,
                    ),
                )
            },
    )
}
