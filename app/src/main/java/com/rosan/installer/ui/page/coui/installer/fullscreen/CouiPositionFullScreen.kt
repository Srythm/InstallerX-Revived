// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.theme.ProvideCouiMaterialBackdrop
import com.rosan.installer.ui.theme.rememberCouiBlurBackdrop
import io.github.suqi8.coui.kmp.effect.LocalCOUIMaterialEffectEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import top.yukonga.miuix.kmp.blur.layerBackdrop

/** Reserved even when installing has no actions, so the status mark never moves. */
val FullScreenBottomBarHeight = 70.dp

/** Single horizontal rhythm shared by the header, cards and actions. */
val FullScreenHorizontalPadding = 16.dp

internal val LocalFullScreenActionsEnabled = staticCompositionLocalOf { true }

/**
 * The full-screen installer surface.
 *
 * This is deliberately only a surface: an opaque page colour, a static colour wash, the blur
 * backdrop, the close fade and the safe-area inset. The bar, the app header, the stage body and
 * the action row all live inside [content], so a page-to-page transition can move the whole page
 * as one unit — a fixed bar over a sliding body reads as two unrelated animations.
 *
 * The ViewModel owns teardown; this shell only follows its closing signal.
 */
@Composable
fun CouiPositionFullScreen(
    isClosing: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    useBlur: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val inspectionMode = LocalInspectionMode.current
    val pageColor = COUITheme.colorScheme.backgroundWithCard.copy(alpha = 1f)
    // The only backdrop source on this page. Null when blur is disabled by the user or the
    // runtime cannot support it, which is what every material surface below degrades on.
    val backdrop = if (inspectionMode) null else rememberCouiBlurBackdrop(useBlur)
    // Opaque from the first frame: the activity behind us may still be showing the sheet that the
    // full-screen mode replaces, and a transparent first frame would flash it.
    val uiAlpha = remember { Animatable(1f) }
    LaunchedEffect(isClosing) {
        if (isClosing) {
            uiAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(InstallerViewModel.FullscreenCloseFadeOutMs.toInt()),
            )
        }
    }

    // Consume system back even when navigation is disabled during installation.
    BackHandler {
        if (!isClosing) onBack()
    }

    CompositionLocalProvider(
        LocalFullScreenActionsEnabled provides !isClosing,
        // Material (edge light, inner shadow, touch spotlight) only when an internal source
        // exists. COUI keeps its opaque fill on its own when the local switch is off.
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    content = content,
                )
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
