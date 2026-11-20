// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode
import com.rosan.installer.ui.theme.material.animateAsState
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import io.github.suqi8.coui.kmp.effect.LocalCOUIMaterialEffectEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.Colors
import io.github.suqi8.coui.kmp.theme.LocalContentColor as LocalCouiContentColor
import io.github.suqi8.coui.kmp.theme.darkColorScheme as couiDarkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme as couiLightColorScheme

private val LocalIsDark = staticCompositionLocalOf { false }
private val LocalPaletteStyle = staticCompositionLocalOf { PaletteStyle.Expressive }
private val LocalThemeColorSpec = staticCompositionLocalOf { ThemeColorSpec.SPEC_2025 }
private val LocalSeedColor = staticCompositionLocalOf { Color.Unspecified }
private val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
private val LocalUseCouiMonet = staticCompositionLocalOf { false }
private val LocalUseDynamicColor = staticCompositionLocalOf { false }

val LocalInstallerColorScheme = staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }

object InstallerTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalInstallerColorScheme.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsDark.current

    val seedColor: Color
        @Composable @ReadOnlyComposable
        get() = LocalSeedColor.current

    val paletteStyle: PaletteStyle
        @Composable @ReadOnlyComposable
        get() = LocalPaletteStyle.current

    val colorSpec: ThemeColorSpec
        @Composable @ReadOnlyComposable
        get() = LocalThemeColorSpec.current

    val themeMode: ThemeMode
        @Composable @ReadOnlyComposable
        get() = LocalThemeMode.current

    val useCouiMonet: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalUseCouiMonet.current

    val useDynamicColor: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalUseDynamicColor.current
}

@Composable
fun InstallerTheme(
    useCoui: Boolean,
    themeMode: ThemeMode,
    paletteStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    useDynamicColor: Boolean,
    useCouiMonet: Boolean,
    seedColor: Color,
    useAmoled: Boolean = false,
    useBlur: Boolean = false,
    content: @Composable () -> Unit,
) {
    val preservedContent = remember {
        movableContentOf<@Composable () -> Unit> { targetContent ->
            targetContent()
        }
    }

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // ColorOS and other OPlus ROMs expose the exact palette seed the user picked in the system
    // settings, which is more accurate than the quantized android.R.color.system_accent1_500
    // bucket. Roms without the setting fall back to that bucket.
    val systemSeedColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberSystemSeedColor()
    } else {
        null
    }
    val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        systemSeedColor ?: colorResource(id = android.R.color.system_accent1_500)
    } else {
        seedColor
    }

    // 1. Generate the base scheme with spec support
    val baseColorScheme = remember(keyColor, isDark, paletteStyle, colorSpec) {
        dynamicColorScheme(
            keyColor = keyColor,
            isDark = isDark,
            style = paletteStyle,
            colorSpec = colorSpec,
        )
    }

    // Only callers that opt in use AMOLED backgrounds, and only in dark mode.
    val colorScheme = if (useAmoled && isDark) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color.Black,
        )
    } else {
        baseColorScheme
    }

    // 2. Wrap it with smooth transitions
    val animatedColorScheme = colorScheme.animateAsState()

    CompositionLocalProvider(
        LocalIsDark provides isDark,
        LocalPaletteStyle provides paletteStyle,
        LocalSeedColor provides seedColor,
        LocalInstallerColorScheme provides animatedColorScheme,
        LocalThemeMode provides themeMode,
        LocalUseCouiMonet provides useCouiMonet,
        LocalUseDynamicColor provides useDynamicColor,
        LocalThemeColorSpec provides colorSpec,
        LocalCOUIMaterialEffectEnabled provides (useBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU),
    ) {
        // Disable navigation bar contrast enforced for Android 10 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NavigationBarContrastHandler()
        }

        if (useCoui) {
            InstallerCOUITheme(
                darkTheme = isDark,
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
                useCouiMonet = useCouiMonet,
                seedColor = seedColor,
                paletteStyle = paletteStyle,
                colorSpec = colorSpec,
                useAmoled = useAmoled,
                systemSeedColor = systemSeedColor,
            ) {
                preservedContent(content)
            }
        } else {
            InstallerMaterialExpressiveTheme(
                darkTheme = isDark,
                colorScheme = animatedColorScheme,
            ) {
                preservedContent(content)
            }
        }
    }
}

@Composable
fun InstallerMaterialExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme,
    compatStatusBarColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    // Applies Material 3 Expressive defaults including the expressive MotionScheme
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content,
    )
}

@Composable
fun InstallerCOUITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: ThemeMode,
    useCouiMonet: Boolean,
    useDynamicColor: Boolean = false,
    compatStatusBarColor: Boolean = true,
    seedColor: Color,
    paletteStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    useAmoled: Boolean = false,
    systemSeedColor: Color? = if (useCouiMonet && useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberSystemSeedColor()
    } else {
        null
    },
    content: @Composable () -> Unit,
) {
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> darkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // COUI still calls the removed DynamicScheme API; retain the MaterialKolor 6 pipeline.
    val colors = if (useCouiMonet) {
        val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            systemSeedColor ?: colorResource(id = android.R.color.system_accent1_500)
        } else {
            seedColor
        }
        remember(keyColor, isDark, paletteStyle, colorSpec, useAmoled) {
            dynamicColorScheme(keyColor, isDark, paletteStyle, 0.0, colorSpec)
                .toCouiColors(isDark = isDark, useAmoled = useAmoled)
        }
    } else {
        remember(isDark) { if (isDark) couiDarkColorScheme() else couiLightColorScheme() }
    }

    // COUI's explicit-colors overload omits its dynamic flag and LocalContentColor.
    CompositionLocalProvider(
        LocalUseCouiMonet provides useCouiMonet,
        LocalCouiContentColor provides colors.labelPrimary,
    ) {
        COUITheme(
            colors = colors,
            content = content,
        )
    }
}

// Preserve COUI's semantic colors and neutral alpha steps while replacing the theme palette.
internal fun ColorScheme.toCouiColors(isDark: Boolean, useAmoled: Boolean): Colors {
    val base = if (isDark) couiDarkColorScheme() else couiLightColorScheme()
    val pageBackground = if (isDark && useAmoled) Color.Black else background
    val pageWithCard = if (isDark && useAmoled) Color.Black else surface
    val disabledSecondaryFill = surfaceContainerHigh.copy(alpha = 0.6f).compositeOver(surfaceContainerLow)

    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryText = primary,
        primaryTextOnPopup = primary,
        labelTheme = primary,
        link = primary,
        // COUI containers carry the accent itself, not M3's primaryContainer tone.
        containerTheme = primary,
        containerThemeHalftone = primary.copy(alpha = base.containerThemeHalftone.alpha),
        secondary = primary.copy(alpha = base.secondary.alpha),
        textColorHighlight = primary.copy(alpha = base.textColorHighlight.alpha),
        disable = primary.copy(alpha = base.disable.alpha),
        focus = primary.copy(alpha = base.focus.alpha),
        focusOutline = primary.copy(alpha = base.focusOutline.alpha),
        tips = primary,
        additional = tertiary,
        assist = secondary,
        error = error,
        onError = onError,
        errorDisabled = error.copy(alpha = base.errorDisabled.alpha),
        errorTextBg = error.copy(alpha = base.errorTextBg.alpha),
        // AMOLED affects pages only: panels, floating bars and cards stay elevated.
        background = pageBackground,
        backgroundWithCard = pageWithCard,
        backgroundElevated = surfaceContainerLow,
        backgroundElevatedWithCard = surfaceContainerLow,
        surface = surfaceContainerLow,
        surfaceWithCard = surfaceContainerLow,
        surfaceTop = surfaceContainerHighest,
        backgroundTop = surfaceContainerHighest,
        bar = surfaceContainer,
        bottomBar = surfaceContainer,
        card = surfaceContainer,
        cardBackground = surfaceContainer,
        cardPressed = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        labelPrimary = onSurface,
        labelPrimaryVariant = onSurface.copy(alpha = base.labelPrimaryVariant.alpha),
        labelSecondary = onSurface.copy(alpha = base.labelSecondary.alpha),
        labelSecondaryVariant = onSurface.copy(alpha = base.labelSecondaryVariant.alpha),
        labelTertiary = onSurface.copy(alpha = base.labelTertiary.alpha),
        labelQuaternary = onSurface.copy(alpha = base.labelQuaternary.alpha),
        labelOnColor = onPrimary,
        disabledNeutral = onSurface.copy(alpha = base.disabledNeutral.alpha),
        hintNeutral = onSurface.copy(alpha = base.hintNeutral.alpha),
        quarternaryNeutral = onSurface.copy(alpha = base.quarternaryNeutral.alpha),
        container4 = onSurface.copy(alpha = base.container4.alpha),
        container8 = onSurface.copy(alpha = base.container8.alpha),
        container12 = onSurface.copy(alpha = base.container12.alpha),
        container16 = onSurface.copy(alpha = base.container16.alpha),
        containerSolid = surfaceContainerHigh,
        fillSolid = surfaceContainerHigh,
        fillThin = onSurface.copy(alpha = base.fillThin.alpha),
        controls = onSurface.copy(alpha = base.controls.alpha),
        outline = outline,
        divider = outlineVariant,
        press = onSurface.copy(alpha = base.press.alpha),
        hover = onSurface.copy(alpha = base.hover.alpha),
        pressBackground = onSurface.copy(alpha = base.pressBackground.alpha),
        secondaryVariant = surfaceContainerHigh,
        disabledSecondaryVariant = disabledSecondaryFill,
        secondaryContainer = secondaryContainer,
        dialogButton = surfaceContainerHigh,
        dragHandle = onSurface.copy(alpha = base.dragHandle.alpha),
        secondaryButton = surfaceContainerHigh,
        disabledSecondaryButton = disabledSecondaryFill,
        disabledOnPrimaryButton = onPrimary.copy(alpha = base.disabledOnPrimaryButton.alpha),
        disabledOnSecondaryButton = onSurface.copy(alpha = base.disabledOnSecondaryButton.alpha),
        disabledProgress = surfaceContainerHigh,
        disabledProgressBackground = onSurface.copy(alpha = base.disabledProgressBackground.alpha),
        sliderBackground = primary.copy(alpha = 0.2f).compositeOver(surfaceContainer),
        disabledSliderThumb = onPrimary.copy(alpha = base.disabledSliderThumb.alpha),
        disabledSliderKeyPoint = onSurface.copy(alpha = base.disabledSliderKeyPoint.alpha),
        disabledTextFieldBorder = onSurface.copy(alpha = base.disabledTextFieldBorder.alpha),
        disabledChipText = onSurface.copy(alpha = base.disabledChipText.alpha),
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun NavigationBarContrastHandler() {
    val configuration = LocalConfiguration.current
    val activity = LocalActivity.current

    DisposableEffect(configuration) {
        val window = activity?.window
        window?.isNavigationBarContrastEnforced = false

        onDispose { /** Keep empty as we want this behavior to persist **/ }
    }
}
