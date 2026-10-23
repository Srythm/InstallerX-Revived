// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.suqi8.coui.kmp.basic.CardColors
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.COUITheme.isDynamicColor

@Composable
fun couiSheetCardColor(): Color = if (isDynamicColor) {
    COUITheme.colorScheme.surfaceContainer
} else if (InstallerTheme.isDark) {
    couiSheetCardColorDark
} else {
    Color.White
}

@Composable
fun couiSheetCardColors(contentColor: Color = COUITheme.colorScheme.onSurface): CardColors = CardColors(
    color = couiSheetCardColor(),
    contentColor = contentColor,
    pressedColor = couiSheetCardColor(),
)
