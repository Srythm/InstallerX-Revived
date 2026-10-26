// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.icons.AppCOUIIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Height of the bottom action area.
 *
 * The status stages (installing / success / failed) centre their mark inside the
 * weighted content area, so this height must stay constant even when a stage
 * only renders a single button. Otherwise the mark would drift vertically
 * between stages.
 */
val FullScreenBottomBarHeight = 70.dp

/** Single horizontal rhythm shared by the top bar, header, cards and actions. */
val FullScreenHorizontalPadding = 16.dp

private val TopBarHeight = 60.dp
private val ActionButtonSize = 44.dp

/**
 * Full-screen installer shell.
 *
 * Renders only the frame: top bar, optional application header, the stage
 * content, and an optional fixed bottom action area. The bottom sheet
 * counterpart in `sheetcontent/` is untouched; this shell exists so the
 * full-screen install mode has its own information hierarchy.
 *
 * Nothing here draws glass. An earlier revision composited the chrome with
 * `drawBackdrop` + runtime-shader highlights and crashed on launch: inside a
 * full-screen container there is no sibling content to refract, and the shader
 * path was the only thing that differed from a working build. The chrome is
 * therefore painted with plain palette colours, and any material treatment is
 * applied at the *window* level by the caller (see `WindowBlurEffect`), which
 * blurs the real app behind the translucent installer window.
 *
 * Enter / exit:
 * The whole layer is a single alpha-animated surface. It fades in on first
 * composition. The exit is driven by the external [isClosing] signal, which is
 * [InstallerViewModel.isClosingFullscreen] — the ViewModel flips it, waits
 * [InstallerViewModel.FullscreenCloseFadeOutMs] on its own scope, and only then
 * tears the session down. Doing that work here (e.g. `rememberCoroutineScope()
 * .launch { delay(); onClose() }`) is a trap: the teardown resets the stage to
 * `Ready`, this composable leaves the tree, and the composition-bound coroutine
 * is cancelled mid-fade — the exit animation never plays.
 *
 * @param title text shown in the top bar
 * @param isClosing external "begin exit" signal; see the KDoc above
 * @param onBack back action for the top-left button *and* the system back
 *   gesture; `null` disables both (used while a package is being written)
 * @param contentKey changes when the visible page changes, so backing out of a
 *   sub-page can fade the layer back in without restarting the whole session
 * @param header optional slot placed under the top bar, usually the app identity
 * @param bottomBar optional fixed-height action area; omit it for pages that
 *   navigate back through the top bar only
 */
@Composable
fun CouiPositionFullScreen(
    title: String,
    isClosing: Boolean,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentKey: Any? = null,
    onMore: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val fadeMs = InstallerViewModel.FullscreenCloseFadeOutMs.toInt()
    val uiAlpha = remember { Animatable(0f) }
    // Re-entrancy guard: a second back press during the exit fade must not
    // retrigger the close path.
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        uiAlpha.animateTo(1f, animationSpec = tween(durationMillis = fadeMs))
    }

    LaunchedEffect(isClosing) {
        if (isClosing && !isDismissing) {
            isDismissing = true
            uiAlpha.animateTo(0f, animationSpec = tween(durationMillis = fadeMs))
        }
    }

    // Re-enter after an in-flow back navigation (e.g. the permission or settings
    // sub-page). Only valid while no real close is in flight: a stage change
    // inside the close window must not fade the layer back in.
    LaunchedEffect(contentKey, isClosing) {
        if (isDismissing && !isClosing) {
            isDismissing = false
            uiAlpha.animateTo(1f, animationSpec = tween(durationMillis = fadeMs))
        }
    }

    BackHandler(enabled = true) {
        if (!isDismissing) onBack?.invoke()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // `graphicsLayer` reads the animated value in the draw phase, so the
            // fade does not recompose this subtree 60 times a second.
            .graphicsLayer { alpha = uiAlpha.value },
    ) {
        // The installer window is translucent (Theme.Installer.Translucent), so
        // the full-screen surface must paint its own opaque background or the
        // launching app stays visible behind it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(COUITheme.colorScheme.surface),
        ) {
            val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            // One status-bar height of breathing room *above* the safe area, so
            // the top bar never crowds the system status bar.
            val contentEdgePadding = statusBarInset

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarInset + contentEdgePadding),
            ) {
                CouiFullScreenTopBar(
                    title = title,
                    onBack = onBack,
                    onMore = onMore,
                    modifier = Modifier.padding(horizontal = FullScreenHorizontalPadding),
                )

                header?.invoke()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    content = content,
                )

                if (bottomBar != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = navInset),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(FullScreenBottomBarHeight),
                        ) {
                            bottomBar()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouiFullScreenTopBar(
    title: String,
    onBack: (() -> Unit)?,
    onMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TopBarHeight),
    ) {
        RoundIconAction(
            icon = AppCOUIIcons.Back,
            contentDescription = null,
            enabled = onBack != null,
            onClick = { onBack?.invoke() },
            modifier = Modifier.align(Alignment.CenterStart),
        )

        Text(
            text = title,
            style = COUITheme.textStyles.title3,
            color = COUITheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = ActionButtonSize + 12.dp),
        )

        if (onMore != null) {
            RoundIconAction(
                icon = AppCOUIIcons.More,
                contentDescription = null,
                enabled = true,
                onClick = onMore,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/**
 * Circular chrome button.
 *
 * The COUI palette defines `secondaryContainer` as an 8% on-surface tint, which
 * reads as a grey blob on a light page. Chrome buttons use the solid
 * `surfaceVariant` fill plus a hairline `outline` edge so they stay readable on
 * both page variants.
 */
@Composable
private fun RoundIconAction(
    icon: ImageVector,
    contentDescription: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = ActionButtonSize,
) {
    val shape = CircleShape

    Box(
        modifier = modifier
            .size(size)
            .background(COUITheme.colorScheme.surfaceVariant, shape)
            .border(0.5.dp, COUITheme.colorScheme.outline, shape)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) {
                COUITheme.colorScheme.onSurface
            } else {
                COUITheme.colorScheme.disabledOnSurface
            },
            modifier = Modifier.size(size * 0.46f),
        )
    }
}
