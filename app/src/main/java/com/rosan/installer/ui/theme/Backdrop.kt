// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import io.github.suqi8.coui.kmp.effect.LocalMaterialBackdrop
import io.github.suqi8.coui.kmp.effect.MaterialBackdropProvider
import io.github.suqi8.coui.kmp.theme.COUITheme
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blendColors
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported

/**
 * Remember a LayerBackdrop with a solid background to prevent alpha-blending artifacts.
 * @param enableBlur Whether the blur effect is globally enabled.
 * @return A LayerBackdrop instance if supported and enabled, null otherwise.
 */
@Composable
fun rememberCouiBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val surfaceColor = COUITheme.colorScheme.backgroundWithCard
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Determine the app bar background color based on the Backdrop availability.
 * @return Transparent if Backdrop is active, otherwise the default surface color.
 */
@Composable
fun LayerBackdrop?.getCouiAppBarColor(): Color = this?.let { Color.Transparent } ?: COUITheme.colorScheme.bar

/**
 * Apply a standard glassmorphism blur effect using Coui Backdrop.
 * @param backdrop The LayerBackdrop providing the visual source.
 * @param enabled Whether the effect is locally enabled for this component.
 * @param blurRadius The radius of the Gaussian blur.
 * @param shape The clipping shape for the blurred area.
 */
@Composable
fun Modifier.installerCouiBlurEffect(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape,
): Modifier {
    // Return early if disabled or backdrop is unavailable
    if (!enabled || backdrop == null) return this

    // Grab the current theme surface color for blending
    val blendColor = COUITheme.colorScheme.bar.copy(alpha = 0.8f)

    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = blendColor),
                ),
            ),
        ),
    )
}

@Composable
fun ProvideCouiMaterialBackdrop(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    val provider = remember(backdrop) {
        backdrop?.let { source ->
            MaterialBackdropProvider { spec, shape ->
                val colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(spec.layer1, spec.mode1.toBlurBlendMode()),
                        BlendColorEntry(spec.layer2, spec.mode2.toBlurBlendMode()),
                    ),
                )
                Modifier.drawBackdrop(
                    backdrop = source,
                    shape = { shape },
                    effects = {
                        // COUI supplies raw HWUI pixels; miuix uses a 0.45 radius-to-sigma factor.
                        if (spec.blurRadiusPx > 0f) blur((0.57735f * spec.blurRadiusPx + 0.5f) / 0.45f)
                        blendColors(colors)
                    },
                )
            }
        }
    }
    CompositionLocalProvider(LocalMaterialBackdrop provides provider, content = content)
}

private fun BlendMode.toBlurBlendMode(): BlurBlendMode = when (this) {
    BlendMode.Clear -> BlurBlendMode.Clear
    BlendMode.Src -> BlurBlendMode.Src
    BlendMode.Dst -> BlurBlendMode.Dst
    BlendMode.SrcOver -> BlurBlendMode.SrcOver
    BlendMode.DstOver -> BlurBlendMode.DstOver
    BlendMode.SrcIn -> BlurBlendMode.SrcIn
    BlendMode.DstIn -> BlurBlendMode.DstIn
    BlendMode.SrcOut -> BlurBlendMode.SrcOut
    BlendMode.DstOut -> BlurBlendMode.DstOut
    BlendMode.SrcAtop -> BlurBlendMode.SrcAtop
    BlendMode.DstAtop -> BlurBlendMode.DstAtop
    BlendMode.Xor -> BlurBlendMode.Xor
    BlendMode.Plus -> BlurBlendMode.Plus
    BlendMode.Modulate -> BlurBlendMode.Modulate
    BlendMode.Screen -> BlurBlendMode.Screen
    BlendMode.Overlay -> BlurBlendMode.Overlay
    BlendMode.Darken -> BlurBlendMode.Darken
    BlendMode.Lighten -> BlurBlendMode.Lighten
    BlendMode.ColorDodge -> BlurBlendMode.ColorDodge
    BlendMode.ColorBurn -> BlurBlendMode.ColorBurn
    BlendMode.HardLight -> BlurBlendMode.HardLight
    BlendMode.SoftLight -> BlurBlendMode.SoftLight
    BlendMode.Difference -> BlurBlendMode.Difference
    BlendMode.Exclusion -> BlurBlendMode.Exclusion
    BlendMode.Multiply -> BlurBlendMode.Multiply
    BlendMode.Hue -> BlurBlendMode.Hue
    BlendMode.Saturation -> BlurBlendMode.Saturation
    BlendMode.Color -> BlurBlendMode.Color
    BlendMode.Luminosity -> BlurBlendMode.Luminosity
    else -> error("Unsupported blend mode: $this")
}

/**
 * Remember a LayerBackdrop for Material 3 with a surfaceContainer background
 * to prevent alpha-blending artifacts.
 *
 * @param enableBlur Whether the blur effect is globally enabled.
 * @return A LayerBackdrop instance if supported and enabled, null otherwise.
 */
@Composable
fun rememberMaterial3BlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRuntimeShaderSupported()) return null
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Determine the app bar background color for Material 3 based on the Backdrop availability.
 *
 * @return Transparent if Backdrop is active, otherwise the surfaceContainer color.
 */
@Composable
fun LayerBackdrop?.getMaterial3AppBarColor(): Color = this?.let { Color.Transparent } ?: MaterialTheme.colorScheme.surfaceContainer

/**
 * Apply a standard glassmorphism blur effect using Material 3 color schemes.
 *
 * @param backdrop The LayerBackdrop providing the visual source.
 * @param enabled Whether the effect is locally enabled for this component.
 * @param blurRadius The radius of the Gaussian blur.
 * @param shape The clipping shape for the blurred area.
 */
@Composable
fun Modifier.installerMaterial3BlurEffect(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape,
): Modifier {
    // Return early if disabled or backdrop is unavailable
    if (!enabled || backdrop == null) return this

    // Grab the current M3 surfaceContainer color for blending with 80% opacity
    val blendColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)

    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = blendColor),
                ),
            ),
        ),
    )
}
