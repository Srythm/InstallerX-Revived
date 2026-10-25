// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.icons.AppCOUIIcons
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

private val TopBarHeight = 44.dp
private val ActionButtonSize = 34.dp

/**
 * Full-screen installer shell.
 *
 * Renders only the frame: top bar, optional application header, the stage
 * content, and an optional bottom action area. The bottom sheet counterpart in
 * `sheetcontent/` is untouched; this shell exists so the full-screen install
 * mode has its own information hierarchy.
 *
 * @param title text shown in the top bar
 * @param onBack return action for the top-left button; `null` disables it
 * @param onMore optional overflow action rendered as a top-right button
 * @param header optional slot placed under the top bar, usually the app identity
 * @param bottomBar optional fixed-height action area; omit it for pages that
 *   navigate back through the top bar only
 */
@Composable
fun CouiPositionFullScreen(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(COUITheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        CouiFullScreenTopBar(
            title = title,
            onBack = onBack,
            onMore = onMore,
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
                    .height(FullScreenBottomBarHeight),
            ) {
                bottomBar()
            }
        }
    }
}

@Composable
private fun CouiFullScreenTopBar(
    title: String,
    onBack: (() -> Unit)?,
    onMore: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TopBarHeight),
    ) {
        RoundIconAction(
            icon = AppCOUIIcons.Back,
            contentDescription = null,
            enabled = onBack != null,
            onClick = { onBack?.invoke() },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp),
            size = ActionButtonSize,
        )

        Text(
            text = title,
            style = COUITheme.textStyles.title4,
            color = COUITheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center),
        )

        if (onMore != null) {
            RoundIconAction(
                icon = AppCOUIIcons.More,
                contentDescription = null,
                enabled = true,
                onClick = onMore,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                size = ActionButtonSize,
            )
        }
    }
}

@Composable
private fun RoundIconAction(
    icon: ImageVector,
    contentDescription: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = ActionButtonSize,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(COUITheme.colorScheme.secondaryContainer)
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
            modifier = Modifier.size(size * 0.44f),
        )
    }
}
