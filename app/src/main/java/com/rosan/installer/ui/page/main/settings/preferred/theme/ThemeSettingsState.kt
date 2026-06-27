// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.theme

import androidx.compose.ui.graphics.Color
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackExitDirection
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.ui.theme.material.PresetColors
import com.rosan.installer.ui.theme.material.RawColor
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode

data class ThemeSettingsState(
    // Blur is off by default. When enabled, every settings subpage that
    // passes useBlur=true gets a `LayerBackdrop` re-rendering the
    // surface every frame (`textureBlur` + `installerMaterial3BlurEffect`),
    // and `AnimatedFluidBackground` (shown on the home status card while
    // the app is the active installer) runs 4 color animations plus
    // a `withFrameNanos` loop that recomposes the Canvas every frame
    // to redraw ~13 radial gradients. The combined cost was enough to
    // cause a sustained >40% GPU load on a mid-range Android 13+ device
    // when just *opening* the settings page. Users who want the glass
    // effect can opt in from 主题 → 模糊. The flag itself is a
    // user-controlled preference; this only changes the *initial* value
    // before the user has made an explicit choice.
    val useBlur: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = true,
    val useAppleFloatingBar: Boolean = false,
    val seedColor: Color = PresetColors[0].color,
    val availableColors: List<RawColor> = PresetColors,
    val useDynColorFollowPkgIcon: Boolean = false,
    val useDynColorFollowPkgIconForLiveActivity: Boolean = false,
    val preferSystemIcon: Boolean = false,
    val showLiveActivity: Boolean = false,
    val predictiveBackAnimation: PredictiveBackAnimation = PredictiveBackAnimation.AOSP,
    val predictiveBackExitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.FOLLOW_GESTURE
)
