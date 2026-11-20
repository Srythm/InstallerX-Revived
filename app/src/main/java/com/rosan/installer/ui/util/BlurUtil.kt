// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.util

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import java.util.function.Consumer
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collect

/**
 * Applies a window-level blur-behind effect (Android 12+).
 * Correctly handles Activity and Dialog windows, and respects system settings.
 *
 * The [blurRadius] can change continuously (e.g. driven by an [androidx.compose.animation.core.Animatable]):
 * each new value is applied to the window's attributes via [Window.setAttributes], so callers
 * can fade the blur in and out smoothly. The previous version used [blurRadius] as a
 * [DisposableEffect] key, which restarted the effect (and called [Window.clearBlur]) on every
 * change — that produced a flicker when the radius was animated frame-by-frame. Here the
 * radius is tracked via [snapshotFlow] and applied without tearing the effect down.
 */
@Composable
fun WindowBlurEffect(useBlur: Boolean, blurRadius: Int = 30) {
    // Window-level blur is only supported on Android 12 (API 31) and above.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val window = findCurrentWindow() ?: return
    val blurEnabledBySystem = isCrossWindowBlurEnabled()
    val currentBlurRadius = rememberUpdatedState(blurRadius)

    // Drive the window attributes from useBlur + blurRadius. The key only restarts the
    // coroutine when the underlying window / useBlur / system-toggle changes, so a
    // continuously-animated blurRadius does NOT restart the effect each frame.
    LaunchedEffect(window, useBlur, blurEnabledBySystem) {
        if (useBlur && blurEnabledBySystem) {
            // When the animated radius drops to 0 we proactively call clearBlur
            // (which removes FLAG_BLUR_BEHIND) during the exit animation, not
            // at dispose time. Otherwise the teardown path produces a one-frame
            // visual: window still has FLAG_BLUR_BEHIND but the install UI is
            // already gone, so the user briefly sees the background blur with
            // no UI in front of it. Removing the flag while the user is still
            // watching the fade-out (visually identical to "flag set, radius 0")
            // keeps the dispose step a no-op.
            snapshotFlow { currentBlurRadius.value }.collect { radius ->
                if (radius > 0) {
                    window.applyBlur(radius)
                } else {
                    window.clearBlur()
                }
            }
        } else {
            window.clearBlur()
        }
    }

    // Always clear the blur when the composable leaves the tree. This is independent
    // of the LaunchedEffect above so it fires even if the useBlur path is active when
    // the composable is disposed.
    DisposableEffect(window) {
        onDispose {
            window.clearBlur()
        }
    }
}

/**
 * A single, stable window-blur driver for surfaces that animate in and out.
 *
 * Instead of letting an [androidx.compose.animation.AnimatedContent] cross-fade between two
 * radii — which spawns two [WindowBlurEffect] instances writing the same window while both are
 * composed, and can clear the flag from the outgoing instance — this keeps one effect for the
 * whole lifetime of the surface and feeds it a continuously animated radius.
 *
 * The progress starts at 0, so the first composition of a dialog that is already "visible"
 * still gets a real 0 -> [maxRadius] ramp instead of a radius that is applied straight away.
 * A radius of 0 clears `FLAG_BLUR_BEHIND` proactively (see [WindowBlurEffect]), so the exit
 * leaves no residual blur behind if the surface is disposed mid-animation.
 *
 * @param visible Whether the surface is currently shown. Drives the ramp direction.
 * @param useBlur Whether window blur is enabled at all. `false` clears the blur immediately.
 * @param maxRadius Blur radius in px at full strength.
 */
@Composable
fun WindowBlurTransition(
    visible: Boolean,
    useBlur: Boolean,
    maxRadius: Int = 30,
    enterDurationMillis: Int = 220,
    exitDurationMillis: Int = 200,
) {
    // Window-level blur is only supported on Android 12 (API 31) and above.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible, useBlur) {
        val target = if (visible && useBlur) 1f else 0f
        if (progress.value == target) return@LaunchedEffect
        progress.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = if (target > 0f) enterDurationMillis else exitDurationMillis,
                easing = LinearOutSlowInEasing,
            ),
        )
    }

    WindowBlurEffect(
        useBlur = useBlur,
        blurRadius = (progress.value * maxRadius).roundToInt(),
    )
}

/**
 * Finds the [Window] instance for the current composition.
 */
@Composable
private fun findCurrentWindow(): Window? {
    val view = LocalView.current
    // 1. Check if we are in a Dialog (Compose Dialogs use DialogWindowProvider)
    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
    if (dialogWindow != null) return dialogWindow

    // 2. Otherwise, find the Activity window
    return LocalActivity.current?.window
}

/**
 * Reactive check for system-wide blur support.
 * Blur might be disabled by the user in Accessibility or automatically in Battery Saver mode.
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun isCrossWindowBlurEnabled(): Boolean {
    val context = LocalContext.current
    val wm = remember(context) { context.getSystemService(WindowManager::class.java) }

    // Initialize with the current real-time state instead of 'false'.
    var isEnabled by remember { mutableStateOf(wm.isCrossWindowBlurEnabled) }

    DisposableEffect(wm) {
        val listener = Consumer<Boolean> { enabled ->
            isEnabled = enabled
        }

        wm.addCrossWindowBlurEnabledListener(listener)

        onDispose {
            wm.removeCrossWindowBlurEnabledListener(listener)
        }
    }

    return isEnabled
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Window.applyBlur(radius: Int) {
    // FLAG_BLUR_BEHIND is required for window blur to work.
    addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    attributes = attributes.apply {
        blurBehindRadius = radius.coerceIn(0, 150) // System usually clamps to 150.
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Window.clearBlur() {
    clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    attributes = attributes.apply {
        blurBehindRadius = 0
    }
}
