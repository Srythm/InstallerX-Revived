// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppCOUIIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.theme.ProvideCouiMaterialBackdrop
import com.rosan.installer.ui.theme.colorOsSpring
import com.rosan.installer.ui.theme.rememberCouiBlurBackdrop
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.effect.LocalCOUIMaterialEffectEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import top.yukonga.miuix.kmp.blur.layerBackdrop

/** Reserved even when installing has no actions, so the status mark never moves. */
val FullScreenBottomBarHeight = 70.dp

/** Single horizontal rhythm shared by the header, cards and actions. */
val FullScreenHorizontalPadding = 16.dp

internal val LocalFullScreenActionsEnabled = staticCompositionLocalOf { true }

// The ViewModel owns teardown; this opaque shell only follows its closing signal.
@Composable
fun CouiPositionFullScreen(
    title: String,
    isClosing: Boolean,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    useBlur: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inspectionMode = LocalInspectionMode.current
    val pageColor = COUITheme.colorScheme.backgroundWithCard.copy(alpha = 1f)
    // The only backdrop source on this page. Null when blur is disabled by the user or the
    // runtime cannot support it, which is what every material surface below degrades on.
    val backdrop = if (inspectionMode) null else rememberCouiBlurBackdrop(useBlur)
    // The shell is opaque from its first frame. The previous version started at alpha 0 and faded
    // in, which left the activity visible for the first frames — and the bottom sheet that the
    // full-screen mode replaces was often still on screen then, so it flashed. Only the exit fades.
    val uiAlpha = remember { Animatable(1f) }
    LaunchedEffect(isClosing) {
        if (isClosing) {
            uiAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(InstallerViewModel.FullscreenCloseFadeOutMs.toInt()),
            )
        }
    }

    // Entry motion lives on the content, not on the shell: it can rise while the background is
    // already opaque, so the transition reads as content arriving instead of a page popping in.
    val enter = remember { Animatable(if (inspectionMode) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!inspectionMode) enter.animateTo(1f, colorOsSpring(responseSeconds = 0.34f))
    }

    // Consume system back even when navigation is disabled during installation.
    BackHandler {
        if (!isClosing) onBack?.invoke()
    }

    CompositionLocalProvider(
        LocalFullScreenActionsEnabled provides !isClosing,
        // Material (edge light, inner shadow, touch spotlight) only when an internal source
        // exists. COUI keeps its opaque fill on its own when the local switch is off. Kept on
        // during the close fade so the buttons do not pop back to a solid fill mid-fade.
        LocalCOUIMaterialEffectEnabled provides (backdrop != null),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { alpha = uiAlpha.value }
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
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .graphicsLayer {
                            alpha = enter.value
                            translationY = (1f - enter.value) * 28.dp.toPx()
                        },
                ) {
                    SmallTopAppBar(
                        title = title,
                        centerTitle = true,
                        color = if (backdrop != null) Color.Transparent else pageColor,
                        defaultWindowInsetsPadding = false,
                        showDivider = false,
                        navigationIcon = {
                            IconButton(
                                onClick = { if (!isClosing) onBack?.invoke() },
                                enabled = !isClosing && onBack != null,
                            ) {
                                Icon(
                                    imageVector = AppCOUIIcons.Back,
                                    contentDescription = stringResource(R.string.back),
                                    tint = if (!isClosing && onBack != null) {
                                        COUITheme.colorScheme.labelPrimary
                                    } else {
                                        COUITheme.colorScheme.disabledNeutral
                                    },
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        },
                        actions = {
                            if (onMore != null) {
                                IconButton(
                                    onClick = { if (!isClosing) onMore() },
                                    enabled = !isClosing,
                                ) {
                                    Icon(
                                        imageVector = AppCOUIIcons.More,
                                        contentDescription = stringResource(R.string.installer_settings),
                                        tint = COUITheme.colorScheme.labelPrimary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        },
                    )
                    header?.invoke()
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        content = content,
                    )
                    if (bottomBar != null) {
                        Box(Modifier.fillMaxWidth().height(FullScreenBottomBarHeight)) {
                            bottomBar()
                        }
                    }
                }
            }
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
                        colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.05f),
                        radius = size.minDimension * 0.9f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(assist.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.95f),
                        radius = size.minDimension * 0.8f,
                    ),
                )
            },
    )
}
