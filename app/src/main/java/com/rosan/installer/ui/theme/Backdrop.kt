// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.effect.LocalMaterialBackdrop
import io.github.suqi8.coui.kmp.effect.MaterialBackdropProvider
import io.github.suqi8.coui.kmp.effect.MaterialBlurProvider
import io.github.suqi8.coui.kmp.theme.COUITheme
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.blendColors
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
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
    if (!isRuntimeShaderSupported()) return null
    val surfaceColor = COUITheme.colorScheme.backgroundWithCard
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * A recording without the opaque fill [rememberCouiBlurBackdrop] paints.
 *
 * Use it for a surface that already paints its own background across the whole recorded node, such
 * as a bottom sheet: the fill would cover the node's padding as well and leave a coloured margin
 * around the panel.
 *
 * @param enableBlur Whether the blur effect is globally enabled.
 * @return A LayerBackdrop instance if supported and enabled, null otherwise.
 */
@Composable
fun rememberCouiSurfaceBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    if (!isRuntimeShaderSupported()) return null
    return rememberLayerBackdrop()
}

/**
 * The COUI 17 popup material (menus, dropdowns, context menus), backed by the miuix layer this
 * project already records for the bars.
 *
 * COUI leaves every list popup on its opaque fill unless the app supplies one of these through
 * [LocalPopupMaterialBlur]; the popup then paints a blurred copy of the page behind it instead.
 * The spec arrives in raw pixels (COUI hands the platform 160px), while miuix takes dp, so the
 * radius is divided by the current density.
 *
 * @param backdrop The recording of the page the menus open over. Must not contain the popups
 *   themselves, which is why it is provided above the page content and not inside it.
 * @return A provider, or null when blur is off or the runtime has no shaders — the popups then
 *   keep their opaque surface, which is COUI's own fallback.
 */
@Composable
fun rememberCouiPopupBlurProvider(backdrop: LayerBackdrop?): MaterialBlurProvider? {
    if (backdrop == null || !isRuntimeShaderSupported()) return null
    val density = LocalDensity.current.density
    return remember(backdrop, density) {
        MaterialBlurProvider { spec, shape ->
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = shape,
                blurRadius = spec.blurRadiusPx / density,
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = spec.mixColor),
                        BlendColorEntry(color = spec.blendColor),
                    ),
                ),
            )
        }
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

/**
 * The COUI 2 "condensed light" (凝光) band for a main tab's `TopAppBar(glow = ...)`.
 *
 * Ported from COUI's own `TopAppBarGlow` recipe, because the library's version takes a COUI
 * `Backdrop` and cannot consume the Miuix layer this project records:
 *
 * 1. A progressive backdrop blur whose radius falls off as `1 - (y / H)^3`, i.e. full strength at
 *    the status bar and pixel-sharp at the bottom of the band.
 * 2. A gradient sheet of the page colour over it: opaque at the top, ~70% at two thirds, clear at
 *    the bottom edge (dark theme: 40% at the top). This is what makes the band readable — a
 *    blur alone over a flat page background has nothing to show.
 * 3. An optional scroll-driven alpha, `min(1, scrollOffsetPx / 30dp)`. Pass `null` to keep the
 *    band fully on: COUI ties it to the scroll offset, but a band that is invisible at rest reads
 *    as "this page has no blur", so every call site in this app keeps it on.
 *
 * The glow slot already spans the status bar plus the library's 24dp tail, so no extra layout
 * padding, spacer or inset compensation is needed, and `TopAppBar` suppresses its divider on its
 * own while a glow is set.
 *
 * Call sites only pass this when a backdrop exists; without one the bar keeps its opaque colour,
 * which is why they pass `glow = null` in that case.
 *
 * @param scrollOffsetPx The bar's resolved scroll distance in pixels, or `null` to keep the band
 *   fully visible instead of fading it in with the scroll.
 */
@Composable
fun BoxScope.CouiProgressiveTopBarGlow(
    backdrop: LayerBackdrop,
    scrollOffsetPx: Float? = null,
    baseColor: Color = COUITheme.colorScheme.backgroundWithCard,
    isDark: Boolean = COUITheme.isDark,
) {
    if (!isRuntimeShaderSupported()) {
        // COUI's no-blur fallback: paint the opaque page colour over the bar band and leave the
        // 24dp tail empty, so unblurred content never shows through.
        Box(Modifier.matchParentSize().padding(bottom = TopBarGlowExtraHeight).background(baseColor))
        return
    }

    // COUI 17 AppBarBlurOverlayBehavior.computeOverlayAlpha: alpha = min(1, scrollPx / 30dp).
    val overlayAlpha = if (scrollOffsetPx == null) {
        1f
    } else {
        val thresholdPx = with(LocalDensity.current) { TopBarGlowAlphaScrollThreshold.toPx() }
        (scrollOffsetPx / thresholdPx).coerceIn(0f, 1f)
    }
    if (overlayAlpha <= 0f) return

    // COUI passes 30f straight through as raw pixels (createGradientBlurEffect), so this converts
    // the token back from dp: miuix multiplies the dp it is given by the density.
    val blurRadiusDp = with(LocalDensity.current) { TopBarGlowBlurRadiusPx / density }

    // GradientBackgroundDrawable builds LinearGradient(0, height, 0, 0), whose position 0 is the
    // bottom edge; the stops are mirrored here onto Compose's top-to-bottom axis.
    val sheet = if (isDark) {
        // Dark theme uses two stops: 40% at the top edge, clear at the bottom.
        Brush.verticalGradient(
            0f to baseColor.copy(alpha = 0.4f),
            1f to baseColor.copy(alpha = 0f),
        )
    } else {
        Brush.verticalGradient(
            0f to baseColor.copy(alpha = 1f),
            0.6f to baseColor.copy(alpha = 0.7f),
            1f to baseColor.copy(alpha = 0f),
        )
    }

    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer { alpha = overlayAlpha }
            .progressiveTextureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = blurRadiusDp,
                // curve > 1 keeps the radius high through most of the band and drops it late,
                // which is the 1 - (y / H)^3 falloff of the COUI SkSL ramp.
                gradient = ProgressiveBlur(angle = 90f, startFraction = 0f, endFraction = 1f, curve = 3f),
            )
            .background(sheet),
    )
}

/**
 * A progressive band for a fixed region that page content scrolls behind.
 *
 * Same recipe as [CouiProgressiveTopBarGlow] — 30px radius, cubic falloff, theme sheet — but the
 * caller owns placement and size, and the ramp can point either way. The sheet is mirrored with
 * the ramp so its opaque end always sits against the fixed region and the clear end against the
 * content. Unlike the top bar glow there is no scroll-driven alpha: the caller fades the band with
 * the fixed region it belongs to.
 *
 * @param fromTop True when the fixed region is above the band's clear end (top chrome), false when
 *   it is below (action row).
 * @param tinted Whether to paint the theme sheet over the blur. Leave false when the recorded
 *   content already fades out at the band's strong end: a flat sheet there would read as a colour
 *   band against the page wash.
 */
@Composable
fun CouiProgressiveEdgeBand(
    backdrop: LayerBackdrop,
    fromTop: Boolean,
    modifier: Modifier = Modifier,
    baseColor: Color = COUITheme.colorScheme.backgroundWithCard,
    isDark: Boolean = COUITheme.isDark,
    tinted: Boolean = true,
) {
    val blurRadiusDp = with(LocalDensity.current) { TopBarGlowBlurRadiusPx / density }

    val sheet = if (fromTop) {
        if (isDark) {
            Brush.verticalGradient(
                0f to baseColor.copy(alpha = 0.4f),
                1f to baseColor.copy(alpha = 0f),
            )
        } else {
            Brush.verticalGradient(
                0f to baseColor.copy(alpha = 1f),
                0.6f to baseColor.copy(alpha = 0.7f),
                1f to baseColor.copy(alpha = 0f),
            )
        }
    } else {
        if (isDark) {
            Brush.verticalGradient(
                0f to baseColor.copy(alpha = 0f),
                1f to baseColor.copy(alpha = 0.4f),
            )
        } else {
            Brush.verticalGradient(
                0f to baseColor.copy(alpha = 0f),
                0.4f to baseColor.copy(alpha = 0.7f),
                1f to baseColor.copy(alpha = 1f),
            )
        }
    }

    Box(
        modifier = modifier
            .progressiveTextureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = blurRadiusDp,
                gradient = ProgressiveBlur(
                    angle = if (fromTop) 90f else 270f,
                    startFraction = 0f,
                    endFraction = 1f,
                    curve = 3f,
                ),
            )
            .then(if (tinted) Modifier.background(sheet) else Modifier),
    )
}

/** `AppBarBlurOverlayView.computeExtraHeightPx`: the glow runs 24dp past the bar's bottom edge. */
private val TopBarGlowExtraHeight = 24.dp

/** COUI `AppBarBlurOverlayBehavior` alpha threshold: fully frosted after a 30dp scroll. */
private val TopBarGlowAlphaScrollThreshold = 30.dp

/** COUI `createGradientBlurEffect(30f, ...)` — raw pixels, not dp. */
private const val TopBarGlowBlurRadiusPx = 30f

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
    BlendMode.Hardlight -> BlurBlendMode.HardLight
    BlendMode.Softlight -> BlurBlendMode.SoftLight
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

/**
 * Material 3 counterpart of [CouiProgressiveTopBarGlow]: the same vertical ramp, applied directly
 * to the app bar instead of a dedicated glow slot, because the Material 3 top app bar has none.
 *
 * The bar keeps its own layout and touch bounds; only the painted band ramps, and no extra
 * padding or inset is involved.
 *
 * @param backdrop The LayerBackdrop providing the visual source.
 * @param enabled Whether the effect is locally enabled for this component.
 * @param blurRadius The radius of the Gaussian blur at full strength, in dp.
 * @param gradient Where the ramp is full strength and where it reaches clear.
 */
@Composable
fun Modifier.installerMaterial3ProgressiveBlurEffect(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    gradient: ProgressiveBlur = ProgressiveBlur.Top,
    shape: Shape = RectangleShape,
): Modifier {
    // Return early if disabled or backdrop is unavailable
    if (!enabled || backdrop == null) return this

    // Same 80% surfaceContainer tint as the uniform version; inside the progressive pipeline the
    // blend is folded into the ramp pass, so it fades out with the blur.
    val blendColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)

    return this.then(
        Modifier.progressiveTextureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            gradient = gradient,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = blendColor),
                ),
            ),
        ),
    )
}
