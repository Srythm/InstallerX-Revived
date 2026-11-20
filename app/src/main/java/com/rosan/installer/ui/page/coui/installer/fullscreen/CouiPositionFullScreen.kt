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
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.effect.LocalCOUIMaterialEffectEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme

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
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inspectionMode = LocalInspectionMode.current
    val pageColor = COUITheme.colorScheme.backgroundWithCard.copy(alpha = 1f)
    val uiAlpha = remember { Animatable(if (inspectionMode) 1f else 0f) }
    LaunchedEffect(isClosing) {
        uiAlpha.animateTo(
            targetValue = if (isClosing) 0f else 1f,
            animationSpec = tween(InstallerViewModel.FullscreenCloseFadeOutMs.toInt()),
        )
    }

    // Consume system back even when navigation is disabled during installation.
    BackHandler {
        if (!isClosing) onBack?.invoke()
    }

    CompositionLocalProvider(
        LocalFullScreenActionsEnabled provides !isClosing,
        // No sibling backdrop exists here. Keep library press feedback, not glass.
        LocalCOUIMaterialEffectEnabled provides false,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { alpha = uiAlpha.value }
                .background(pageColor)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                SmallTopAppBar(
                    title = title,
                    centerTitle = true,
                    color = pageColor,
                    defaultWindowInsetsPadding = false,
                    showDivider = false,
                    navigationIcon = {
                        IconButton(
                            onClick = { if (!isClosing) onBack?.invoke() },
                            enabled = !isClosing && onBack != null,
                            material = false,
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
                                material = false,
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
