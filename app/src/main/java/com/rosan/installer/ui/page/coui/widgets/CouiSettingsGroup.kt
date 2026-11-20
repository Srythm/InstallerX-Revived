// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardColors
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.LocalCardListPosition
import io.github.suqi8.coui.kmp.basic.cardListPositionOf
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.LocalContentColor

@Composable
fun CouiSettingsCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.defaultColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        cornerRadius = CardDefaults.CornerRadius,
        colors = colors,
        content = content,
    )
}

// Lazy rows need individual outlines and group positions without an eager CardListGroup.
@Composable
fun CouiSettingsGroupItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.defaultColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val radius = CardDefaults.CornerRadius
    val isHead = index == 0
    val isTail = index == count - 1
    val top = if (isHead) radius else 0.dp
    val bottom = if (isTail) radius else 0.dp

    CompositionLocalProvider(
        LocalContentColor provides colors.contentColor,
        LocalCardListPosition provides cardListPositionOf(index, count),
    ) {
        // Mirrors COUI's own card container so joined rows keep the same accessibility grouping and
        // minimum-constraint behaviour as a regular card.
        Box(
            modifier = modifier
                .semantics(mergeDescendants = false) {
                    isTraversalGroup = true
                }
                .squircleSurface(
                    color = colors.color,
                    topStart = top,
                    topEnd = top,
                    bottomEnd = bottom,
                    bottomStart = bottom,
                ),
            propagateMinConstraints = true,
        ) {
            Column(content = content)
        }
    }
}
