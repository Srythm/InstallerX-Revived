// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.rosan.installer.ui.theme.ProvideCouiMaterialBackdrop
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

internal val LocalFullScreenBottomBarPadding = staticCompositionLocalOf { 0.dp }

/**
 * The full-screen installer surface.
 *
 * The bar and the action row are drawn exactly once, here, and never take part in a page change:
 * letting each page draw its own bar put two sets of icon buttons on the same pixels during a
 * push, which reads as the buttons flashing grey. Only [content] moves between pages.
 *
 * The ViewModel owns teardown; this shell only follows its closing signal.
 */
@Composable
fun CouiPositionFullScreen(
    title: String,
    isClosing: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    endAction: (@Composable () -> Unit)? = null,
    useBlur: Boolean = false,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val inspectionMode = LocalInspectionMode.current
    val pageColor = COUITheme.colorScheme.backgroundWithCard.copy(alpha = 1f)
    // The only backdrop source on this page. Null when blur is disabled by the user or the
    // runtime cannot support it, which is what every material surface below degrades on.
    val backdrop = if (inspectionMode) null else rememberCouiBlurBackdrop(useBlur)
    // Start transparent at runtime for symmetric enter fade; previews start opaque.
    val uiAlpha = remember { Animatable(if (inspectionMode) 1f else 0f) }
    LaunchedEffect(isClosing) {
        uiAlpha.animateTo(
            targetValue = if (isClosing) 0f else 1f,
            animationSpec = tween(InstallerViewModel.FullscreenCloseFadeOutMs.toInt()),
        )
    }

    // Always consume system back while fullscreen is mounted; invoke onBack only when allowed.
    BackHandler {
        if (!isClosing && backEnabled) onBack()
    }

    CompositionLocalProvider(
        LocalFullScreenActionsEnabled provides !isClosing,
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
                // Backing sibling: the opaque page colour plus a static colour wash, and the only
                // thing recorded into the backdrop. The foreground never samples itself, and the
                // module log or progress text can update without re-recording the source.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
                ) {
                    FullScreenBackgroundWash(base = pageColor)
                }

                ProvideCouiMaterialBackdrop(backdrop) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            // Only the top and the sides are inset here. The bottom is left to the
                            // page: scrolling content must be able to run under the navigation bar
                            // (that is what makes it feel immersive) and only the action row needs to
                            // be lifted above it.
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                            ),
                    ) {
                        FullScreenTopBar(
                            title = title,
                            onBack = onBack,
                            backEnabled = backEnabled && !isClosing,
                            endAction = endAction,
                        )
                        val bottomBarReservedPadding =
                            FullScreenBottomBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        val lastBottomBar = remember { mutableStateOf(bottomBar) }
                        if (bottomBar != null) {
                            lastBottomBar.value = bottomBar
                        }
                        val bottomBarAlpha by animateFloatAsState(
                            targetValue = if (bottomBar != null) 1f else 0f,
                            animationSpec = tween(durationMillis = if (bottomBar != null) 180 else 120),
                            label = "FullScreenBottomBarAlpha",
                        )
                        Box(Modifier.fillMaxWidth().weight(1f)) {
                            CompositionLocalProvider(
                                LocalFullScreenBottomBarPadding provides bottomBarReservedPadding,
                            ) {
                                content()
                            }
                            // Overlaid rather than stacked in the Column so toggling sub-pages never
                            // changes content height or shifts the page vertically mid-transition.
                            if (bottomBarAlpha > 0.01f) {
                                CompositionLocalProvider(
                                    LocalFullScreenActionsEnabled provides (bottomBar != null && !isClosing),
                                ) {
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
) {
    Box(Modifier.fillMaxWidth().height(FullScreenTopBarHeight)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FullScreenHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { if (backEnabled) onBack() },
                enabled = backEnabled,
            ) {
                Icon(
                    imageVector = AppCOUIIcons.Back,
                    contentDescription = stringResource(R.string.back),
                    tint = if (backEnabled) {
                        COUITheme.colorScheme.labelPrimary
                    } else {
                        COUITheme.colorScheme.disabledNeutral
                    },
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            val lastEndAction = remember { mutableStateOf(endAction) }
            if (endAction != null) {
                lastEndAction.value = endAction
            }
            val endActionAlpha by animateFloatAsState(
                targetValue = if (endAction != null) 1f else 0f,
                animationSpec = tween(durationMillis = if (endAction != null) 240 else 200),
                label = "FullScreenEndActionAlpha",
            )
            if (endActionAlpha > 0.01f) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = endActionAlpha
                        scaleX = 0.78f + 0.22f * endActionAlpha
                        scaleY = 0.78f + 0.22f * endActionAlpha
                        compositingStrategy = if (endActionAlpha < 1f) {
                            CompositingStrategy.Offscreen
                        } else {
                            CompositingStrategy.Auto
                        }
                    },
                ) {
                    lastEndAction.value?.invoke()
                }
            }
        }
        AnimatedContent(
            targetState = title,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                (
                    fadeIn(tween(durationMillis = 220, delayMillis = 60)) +
                        slideInVertically(tween(durationMillis = 240, delayMillis = 40)) { it / 3 }
                    ) togetherWith (
                    fadeOut(tween(durationMillis = 120)) +
                        slideOutVertically(tween(durationMillis = 160)) { -it / 3 }
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
