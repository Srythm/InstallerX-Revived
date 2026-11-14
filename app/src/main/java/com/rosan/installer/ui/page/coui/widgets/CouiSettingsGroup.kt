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
import com.rosan.installer.ui.theme.CouiTokens
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardColors
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.LocalContentColor

/**
 * A settings card using the ColorOS 17 corner radius.
 *
 * COUI's [Card] already paints its background through COUI's squircle surface, so the only
 * difference to a plain `Card { }` is the radius: COUI defaults to
 * `CardDefaults.CornerRadius` (12.dp, `coui_round_corner_m`), while ColorOS 17 cards read
 * noticeably larger. Delegating keeps every other behaviour of the original card, including its
 * traversal-group semantics and minimum-constraint propagation.
 *
 * @param modifier applied to the card container, typically the screen padding.
 * @param colors container and content colors, defaulting to COUI's card colors.
 * @param content stacked row content, normally a list of setting widgets.
 */
@Composable
fun CouiSettingsCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.defaultColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        cornerRadius = CouiTokens.CardCornerRadius,
        colors = colors,
        content = content,
    )
}

/**
 * One row of a visually joined settings group.
 *
 * Use this when the rows of one group live in separate lazy list items but must read as a single
 * surface: the first row rounds its top corners, the last rounds its bottom corners, and rows in
 * between stay square. This mirrors COUI's `COUICardListHelper.getPositionInGroup` model
 * (`HEAD` / `MIDDLE` / `TAIL` / `FULL`), kept local so the component does not depend on additional
 * framework symbols.
 *
 * @param index zero-based position of this row inside the group.
 * @param count total number of rows in the group.
 * @param modifier applied to the row container, typically the screen padding on the last row.
 * @param colors container and content colors, defaulting to COUI's card colors.
 * @param content stacked row content.
 */
@Composable
fun CouiSettingsGroupItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.defaultColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val radius = CouiTokens.CardCornerRadius
    val isHead = index == 0
    val isTail = index == count - 1
    val top = if (isHead) radius else 0.dp
    val bottom = if (isTail) radius else 0.dp

    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        // Mirrors COUI's own card container so joined rows keep the same accessibility grouping and
        // minimum-constraint behaviour as a regular card.
        Box(
            modifier = modifier
                .semantics(mergeDescendants = false) {
                    isTraversalGroup = true
                }
                .squircleSurface(colors.color, top, top, bottom, bottom),
            propagateMinConstraints = true,
        ) {
            Column(content = content)
        }
    }
}
