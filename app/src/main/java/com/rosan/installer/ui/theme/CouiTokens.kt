// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.dp

/**
 * Layout geometry and motion constants for the ColorOS-styled (COUI) UI family.
 *
 * The values describe the intended ColorOS 17 appearance. Devices below API 33 draw plain
 * rounded corners instead of squircles, because COUI only enables its corner shader when
 * runtime shaders are available, so none of these values need a manual version guard.
 */
object CouiTokens {

    /**
     * Visual corner radius of a settings card or group.
     *
     * COUI's own `CardDefaults.CornerRadius` is 12.dp, which maps to `coui_round_corner_m`.
     * ColorOS 17 cards read noticeably larger, so the COUI family overrides it here. COUI
     * expands the value by its 1.2819 squircle extension when the shader path is active, and
     * that expansion is what produces the continuous-corner outline.
     */
    val CardCornerRadius = 20.dp

    /** Compact radius for nested surfaces, chips and popups. */
    val CompactCornerRadius = 16.dp

    /** Horizontal inset of a settings group from the screen edge. */
    val ScreenPadding = 12.dp

    /** Vertical gap between two settings groups. */
    val GroupSpacing = 12.dp

    /** Minimum height of a single settings row. */
    val RowMinHeight = 56.dp

    /** Preferred horizontal padding inside a settings row. */
    val RowHorizontalPadding = 16.dp

    /** Motion constants shared by COUI surfaces. */
    object Motion {

        /** ColorOS standard easing: quick departure with a long settling tail. */
        val Standard: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

        /** Thumb travel duration of a ColorOS switch. */
        const val SwitchThumbMillis = 383

        /** Color cross-fade duration shared by switch tracks, icons and labels. */
        const val ColorTransitionMillis = 450
    }
}
