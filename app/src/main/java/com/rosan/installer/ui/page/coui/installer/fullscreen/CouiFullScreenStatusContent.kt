// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.rosan.installer.ui.theme.colorOsSpring
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

/**
 * Progress and result state, built as one continuous surface.
 *
 * The mark slot never changes size or position: a ring is swapped for a check mark inside it, so
 * "installing" to "installed" reads as the ring resolving rather than as a new screen appearing.
 * The labels cross-fade for the same reason, and the text block animates its own height so a
 * longer explanation does not shove the mark around.
 */
@Composable
fun CouiFullScreenStatusContent(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progressText: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = COUITheme.colorScheme.success,
    haloColor: Color = COUITheme.colorScheme.success.copy(alpha = 0.12f),
    progress: Float? = null,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val value = fullScreenProgress(progress)
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = FullScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        StatusMark(
            progress = value,
            icon = icon,
            iconTint = iconTint,
            haloColor = haloColor,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    fadeIn(tween(durationMillis = 180, delayMillis = 50)) togetherWith
                        fadeOut(tween(durationMillis = 110))
                },
                label = "StatusTitle",
            ) { label ->
                Text(
                    text = label,
                    style = COUITheme.textStyles.headlineS,
                    color = COUITheme.colorScheme.labelPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            AnimatedContent(
                targetState = subtitle.orEmpty(),
                transitionSpec = {
                    fadeIn(tween(durationMillis = 180, delayMillis = 50)) togetherWith
                        fadeOut(tween(durationMillis = 110))
                },
                label = "StatusSubtitle",
            ) { line ->
                if (line.isNotBlank()) {
                    Text(
                        text = line,
                        style = COUITheme.textStyles.bodyL,
                        color = COUITheme.colorScheme.labelSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
            val lastProgressText = remember { mutableStateOf(progressText) }
            if (!progressText.isNullOrBlank()) {
                lastProgressText.value = progressText
            }
            AnimatedVisibility(
                visible = !progressText.isNullOrBlank(),
                enter = fadeIn(tween(durationMillis = 150)),
                exit = fadeOut(tween(durationMillis = 150)),
            ) {
                lastProgressText.value?.takeIf { it.isNotBlank() }?.let { pct ->
                    Text(
                        text = pct,
                        style = COUITheme.textStyles.bodyL,
                        color = COUITheme.colorScheme.labelSecondaryVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
            if (extraContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    extraContent()
                }
            } else {
                Spacer(Modifier.size(16.dp))
            }
        }
    }
}

/**
 * The one mark slot. Both variants are 92dp boxes centred on the same point, and each fades in as
 * the other fades out, so the transition has no jump in size, position or baseline.
 */
@Composable
private fun StatusMark(
    progress: Float?,
    icon: ImageVector?,
    iconTint: Color,
    haloColor: Color,
) {
    Box(Modifier.size(MarkSize), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = icon == null,
            enter = fadeIn(tween(durationMillis = 200)) + scaleIn(tween(durationMillis = 220), initialScale = 0.9f),
            exit = fadeOut(tween(durationMillis = 140)) + scaleOut(tween(durationMillis = 160), targetScale = 0.9f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CircularProgressIndicator(
                progress = progress,
                colors = ProgressIndicatorDefaults.loadingViewColors(),
                // The determinate variant subtracts 8dp from its visible ring diameter.
                size = if (progress == null) RingDiameter else RingDiameter + 8.dp,
                strokeWidth = RingStroke,
            )
        }
        AnimatedVisibility(
            visible = icon != null,
            enter = fadeIn(tween(durationMillis = 240)) + scaleIn(colorOsSpring(responseSeconds = 0.36f), initialScale = 0.7f),
            exit = fadeOut(tween(durationMillis = 120)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            if (icon != null) {
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
    }
}
