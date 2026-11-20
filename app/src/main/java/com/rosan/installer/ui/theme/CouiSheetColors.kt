// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.suqi8.coui.kmp.basic.CardColors
import io.github.suqi8.coui.kmp.theme.COUITheme

@Composable
fun couiSheetCardColor(): Color = COUITheme.colorScheme.cardBackground

@Composable
fun couiSheetCardColors(contentColor: Color = COUITheme.colorScheme.labelPrimary): CardColors = CardColors(
    color = couiSheetCardColor(),
    contentColor = contentColor,
    pressedColor = COUITheme.colorScheme.cardPressed,
)
