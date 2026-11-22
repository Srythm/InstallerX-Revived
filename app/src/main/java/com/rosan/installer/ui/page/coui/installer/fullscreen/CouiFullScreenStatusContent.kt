// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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

private class SlotHolder<T>(var value: T)

private data class StatusIconSnapshot(
    val icon: ImageVector,
    val tint: Color,
    val haloColor: Color,
)

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
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    fadeIn(tween(durationMillis = 200)) togetherWith
                        fadeOut(tween(durationMillis = 160))
                },
                label = "StatusTitle",
            ) { label ->
                Text(
                    text = label,
                    style = COUITheme.textStyles.headlineS,
                    color = COUITheme.colorScheme.labelPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            AnimatedContent(
                targetState = subtitle.orEmpty(),
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    fadeIn(tween(durationMillis = 200)) togetherWith
                        fadeOut(tween(durationMillis = 160))
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
            val lastProgressText = remember { SlotHolder(progressText) }
            if (!progressText.isNullOrBlank()) {
                lastProgressText.value = progressText
            }
            AnimatedVisibility(
                visible = !progressText.isNullOrBlank(),
                enter = fadeIn(tween(durationMillis = 150)) + expandVertically(tween(durationMillis = 150)),
                exit = fadeOut(tween(durationMillis = 150)) + shrinkVertically(tween(durationMillis = 150)),
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
            val lastExtraContent = remember { SlotHolder(extraContent) }
            if (extraContent != null) {
                lastExtraContent.value = extraContent
            }
            AnimatedVisibility(
                visible = extraContent != null,
                enter = fadeIn(tween(durationMillis = 200)) + expandVertically(tween(durationMillis = 200)),
                exit = fadeOut(tween(durationMillis = 140)) + shrinkVertically(tween(durationMillis = 140)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    lastExtraContent.value?.invoke()
                }
            }
            Spacer(Modifier.size(16.dp))
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
    val lastRingProgress = remember { SlotHolder(progress) }
    if (icon == null) {
        lastRingProgress.value = progress
    }
    val effectiveProgress = if (icon == null) progress else lastRingProgress.value

    val lastIcon = remember {
        SlotHolder(icon?.let { StatusIconSnapshot(it, iconTint, haloColor) })
    }
    if (icon != null) {
        lastIcon.value = StatusIconSnapshot(icon, iconTint, haloColor)
    }

    Box(Modifier.size(MarkSize), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = icon == null,
            enter = fadeIn(tween(durationMillis = 200)) + scaleIn(tween(durationMillis = 220), initialScale = 0.9f),
            exit = fadeOut(tween(durationMillis = 140)) + scaleOut(tween(durationMillis = 160), targetScale = 0.9f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Crossfade(
                targetState = effectiveProgress == null,
                modifier = Modifier.fillMaxSize(),
                animationSpec = tween(durationMillis = 180),
                label = "StatusRingMode",
            ) { indeterminate ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = if (indeterminate) null else effectiveProgress,
                        colors = ProgressIndicatorDefaults.loadingViewColors(),
                        // The determinate variant subtracts 8dp from its visible ring diameter.
                        size = if (indeterminate) RingDiameter else RingDiameter + 8.dp,
                        strokeWidth = RingStroke,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = icon != null,
            enter = fadeIn(tween(durationMillis = 220)) +
                scaleIn(colorOsSpring(responseSeconds = 0.36f), initialScale = 0.7f),
            exit = fadeOut(tween(durationMillis = 120)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            lastIcon.value?.let { snap ->
                Box(
                    // Soft halo instead of a solid disc: a radial ramp fades out on its own, so
                    // the mark reads as light rather than as a second circle.
                    Modifier
                        .size(MarkSize)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    snap.haloColor,
                                    snap.haloColor.copy(alpha = snap.haloColor.alpha * 0.55f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = snap.icon,
                        contentDescription = null, // The adjacent title names the result.
                        tint = snap.tint,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}
