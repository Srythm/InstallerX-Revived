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
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.ThemeColorSpec as CouiColorSpec
import io.github.suqi8.coui.kmp.theme.ThemeController
import io.github.suqi8.coui.kmp.theme.ThemePaletteStyle as CouiPaletteStyle

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

    val controller = if (useCouiMonet) {
        // --- Monet Engine Path ---
        val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            colorResource(id = android.R.color.system_accent1_500)
        } else {
            seedColor
        }

        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
            ThemeMode.DARK -> ColorSchemeMode.MonetDark
        }

        val style = when (paletteStyle) {
            PaletteStyle.TonalSpot -> CouiPaletteStyle.TonalSpot
            PaletteStyle.Neutral -> CouiPaletteStyle.Neutral
            PaletteStyle.Vibrant -> CouiPaletteStyle.Vibrant
            PaletteStyle.Expressive -> CouiPaletteStyle.Expressive
            PaletteStyle.Rainbow -> CouiPaletteStyle.Rainbow
            PaletteStyle.FruitSalad -> CouiPaletteStyle.FruitSalad
            PaletteStyle.Monochrome -> CouiPaletteStyle.Monochrome
            PaletteStyle.Fidelity -> CouiPaletteStyle.Fidelity
            PaletteStyle.Content -> CouiPaletteStyle.Content
            // COUI does not ship the CMF style; fall back to the closest tonal match.
            PaletteStyle.Cmf -> CouiPaletteStyle.TonalSpot
        }

        val colorSpecVersion = when (paletteStyle.effectiveSpec(colorSpec)) {
            // COUI stops at the 2025 spec; newer specs degrade to 2025.
            ThemeColorSpec.SPEC_2026 -> CouiColorSpec.Spec2025
            ThemeColorSpec.SPEC_2025 -> CouiColorSpec.Spec2025
            ThemeColorSpec.SPEC_2021 -> CouiColorSpec.Spec2021
        }

        remember(colorSchemeMode, keyColor, paletteStyle, colorSpecVersion, darkTheme) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                keyColor = keyColor,
                paletteStyle = style,
                colorSpec = colorSpecVersion,
                isDark = darkTheme,
            )
        }
    } else {
        // --- Default Coui Theme Path ---
        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.System
            ThemeMode.LIGHT -> ColorSchemeMode.Light
            ThemeMode.DARK -> ColorSchemeMode.Dark
        }

        remember(colorSchemeMode, darkTheme) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                isDark = darkTheme,
            )
        }
    }

    COUITheme(
        controller = controller,
        content = content,
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
