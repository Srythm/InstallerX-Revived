// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.rosan.installer.ui.theme.toCouiColors
import io.github.suqi8.coui.kmp.theme.lightColorScheme as couiLightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CouiThemeColorsTest {
    @Test
    fun customAccentReachesControlsAndPopupText() {
        val primary = Color(0xFF795548)
        val scheme = lightColorScheme(primary = primary)
        val colors = scheme.toCouiColors(isDark = false, useAmoled = false)

        assertEquals(primary, colors.primary)
        assertEquals(primary, colors.containerTheme)
        assertEquals(primary, colors.primaryText)
        assertEquals(primary, colors.primaryTextOnPopup)
        assertEquals(primary, colors.labelTheme)
        assertEquals(primary, colors.link)
        assertEquals(primary, colors.tips)
        assertEquals(primary.copy(alpha = couiLightColorScheme().secondary.alpha), colors.secondary)
        assertEquals(couiLightColorScheme().success, colors.success)
        assertEquals(scheme.onPrimary, colors.labelOnColor)
    }

    @Test
    fun darkAmoledRetainsElevatedSurfaces() {
        val scheme = darkColorScheme()
        val colors = scheme.toCouiColors(isDark = true, useAmoled = true)

        assertEquals(Color.Black, colors.background)
        assertEquals(Color.Black, colors.backgroundWithCard)
        assertEquals(scheme.surfaceContainerLow, colors.surface)
        assertEquals(scheme.surfaceContainer, colors.cardBackground)
        assertEquals(scheme.surfaceContainer, colors.bar)
        assertNotEquals(Color.Black, colors.cardBackground)
    }

    @Test
    fun amoledDoesNotDarkenLightTheme() {
        val scheme = lightColorScheme()
        val colors = scheme.toCouiColors(isDark = false, useAmoled = true)

        assertEquals(scheme.background, colors.background)
        assertEquals(scheme.surface, colors.backgroundWithCard)
    }

    @Test
    fun pageCardAndPanelUseDistinctMaterialRoles() {
        val scheme = lightColorScheme()
        val colors = scheme.toCouiColors(isDark = false, useAmoled = false)

        assertEquals(scheme.surface, colors.backgroundWithCard)
        assertEquals(scheme.surfaceContainerLow, colors.surface)
        assertEquals(scheme.surfaceContainer, colors.cardBackground)
        assertEquals(scheme.surfaceContainerHigh, colors.cardPressed)
        assertEquals(scheme.onSurface, colors.labelPrimary)
        assertEquals(scheme.outlineVariant, colors.divider)
    }
}
