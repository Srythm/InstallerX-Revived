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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode
import com.rosan.installer.ui.theme.material.animateAsState
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.Colors
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

    val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(id = android.R.color.system_accent1_500)
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

    // 2. Wrap it with smooth transitions
    val animatedColorScheme = baseColorScheme.animateAsState()

    CompositionLocalProvider(
        LocalIsDark provides isDark,
        LocalPaletteStyle provides paletteStyle,
        LocalSeedColor provides seedColor,
        LocalInstallerColorScheme provides animatedColorScheme,
        LocalThemeMode provides themeMode,
        LocalUseCouiMonet provides useCouiMonet,
        LocalUseDynamicColor provides useDynamicColor,
        LocalThemeColorSpec provides colorSpec,
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

    /*
     * COUI 1.1.0 derives its palette through material-color-utilities 5.x, whose
     * DynamicScheme no longer exists in the 6.x line this module depends on.
     * Calling ThemeController with a key color would therefore fail at runtime,
     * so the palette is produced by our own material-kolor pipeline and handed
     * to COUI as a finished Colors instance.
     */
    val colors = if (useCouiMonet) {
        val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            colorResource(id = android.R.color.system_accent1_500)
        } else {
            seedColor
        }
        remember(keyColor, isDark, paletteStyle, colorSpec) {
            dynamicColorScheme(keyColor, isDark, paletteStyle, 0.0, colorSpec).toCouiColors()
        }
    } else if (isDark) {
        couiDarkColorScheme()
    } else {
        couiLightColorScheme()
    }

    COUITheme(
        colors = colors,
        content = content,
    )
}

/**
 * Maps a Material 3 [ColorScheme] onto COUI's role palette.
 *
 * The two designs share most of the Material 3 role model, so the mapping is
 * direct where a role exists in both and falls back to the closest M3 role
 * (container roles for the "variant" slots, onSurface alpha steps for the
 * disabled slots) where COUI has no counterpart.
 */
private fun ColorScheme.toCouiColors(): Colors {
    val disabledContainer = onSurface.copy(alpha = 0.12f)
    val disabledContent = onSurface.copy(alpha = 0.38f)

    return Colors(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primaryContainer,
        onPrimaryVariant = onPrimaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        disabledPrimary = disabledContainer,
        disabledOnPrimary = disabledContent,
        disabledPrimaryButton = disabledContainer,
        disabledOnPrimaryButton = disabledContent,
        disabledPrimarySlider = disabledContainer,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryVariant = secondaryContainer,
        onSecondaryVariant = onSecondaryContainer,
        disabledSecondary = disabledContainer,
        disabledOnSecondary = disabledContent,
        disabledSecondaryVariant = disabledContainer,
        disabledOnSecondaryVariant = disabledContent,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        secondaryContainerVariant = secondaryContainer,
        onSecondaryContainerVariant = onSecondaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        tertiaryContainerVariant = tertiaryContainer,
        background = background,
        onBackground = onBackground,
        onBackgroundVariant = onSurfaceVariant,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceSecondary = onSurfaceVariant,
        onSurfaceVariantSummary = onSurfaceVariant,
        onSurfaceVariantActions = primary,
        disabledOnSurface = disabledContent,
        surfaceContainer = surfaceContainer,
        onSurfaceContainer = onSurface,
        onSurfaceContainerVariant = onSurfaceVariant,
        surfaceContainerHigh = surfaceContainerHigh,
        onSurfaceContainerHigh = onSurface,
        surfaceContainerHighest = surfaceContainerHighest,
        onSurfaceContainerHighest = onSurface,
        outline = outline,
        dividerLine = outlineVariant,
        windowDimming = scrim,
        sliderKeyPoint = primary,
        sliderKeyPointForeground = onPrimary,
        sliderBackground = surfaceVariant,
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
