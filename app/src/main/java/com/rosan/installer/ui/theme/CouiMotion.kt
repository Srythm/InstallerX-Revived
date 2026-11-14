// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.PI
import kotlin.math.pow

/**
 * Builds a Compose spring from a ColorOS spring description.
 *
 * ColorOS describes springs by response time and damping fraction, while Compose takes
 * stiffness and damping ratio. The two relate as `stiffness = (2π / response)²` and
 * `dampingRatio = 1 - bounce`. Writing motion in response time keeps the feel aligned with the
 * system and makes the intent readable, instead of leaving hand-tuned stiffness values behind.
 *
 * @param responseSeconds time the animation takes to reach its target, in seconds.
 * @param dampingRatio 1.0 settles without overshoot; lower values bounce before settling.
 */
fun <T> colorOsSpring(responseSeconds: Float, dampingRatio: Float = 1f): SpringSpec<T> {
    require(responseSeconds > 0f) { "responseSeconds must be positive, was $responseSeconds" }
    val stiffness = ((2.0 * PI / responseSeconds).pow(2.0)).toFloat()
    return spring(dampingRatio = dampingRatio, stiffness = stiffness)
}
