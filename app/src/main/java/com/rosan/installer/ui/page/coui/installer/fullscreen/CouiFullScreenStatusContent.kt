// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.ProgressIndicatorDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

private val MarkSize = 92.dp
private val RingDiameter = 76.dp
private val RingStroke = 6.dp

/** Negative/non-finite values mean unknown; zero is a genuine determinate value. */
internal fun fullScreenProgress(progress: Float?): Float? = progress?.takeIf { it.isFinite() && it >= 0f }?.coerceAtMost(1f)

// Text scrolls independently so status changes never move the mark.
@Composable
fun CouiFullScreenStatusContent(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = COUITheme.colorScheme.success,
    haloColor: Color = COUITheme.colorScheme.success.copy(alpha = 0.12f),
    progress: Float? = null,
) {
    val value = fullScreenProgress(progress)
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = FullScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(MarkSize), contentAlignment = Alignment.Center) {
            if (icon == null) {
                CircularProgressIndicator(
                    progress = value,
                    colors = ProgressIndicatorDefaults.loadingViewColors(),
                    // The determinate variant subtracts 8dp from its visible ring diameter.
                    size = if (value == null) RingDiameter else RingDiameter + 8.dp,
                    strokeWidth = RingStroke,
                )
            } else {
                Box(
                    // Soft halo instead of a solid disc: a radial ramp fades out on its own, so
                    // the mark reads as light rather than as a second circle.
                    Modifier
                        .size(MarkSize)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    haloColor,
                                    haloColor.copy(alpha = haloColor.alpha * 0.55f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null, // The adjacent title names the result.
                        tint = iconTint,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = COUITheme.textStyles.headlineS,
                color = COUITheme.colorScheme.labelPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = COUITheme.textStyles.bodyL,
                    color = COUITheme.colorScheme.labelSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp),
                )
            }
        }
    }
}
