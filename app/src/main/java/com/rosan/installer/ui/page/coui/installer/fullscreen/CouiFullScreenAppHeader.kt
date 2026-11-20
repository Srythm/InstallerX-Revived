// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.theme.COUITheme

private val AppIconSize = 76.dp
private val AppIconCorner = 20.dp

/**
 * Application identity block for the full-screen installer.
 *
 * Centred, like the system install confirmation: one large icon, the label under it, then the
 * package line. It sits outside the stage body, so the install moves *below* a fixed identity
 * instead of the whole page being rebuilt around it.
 */
@Composable
fun CouiFullScreenAppHeader(
    appInfo: AppInfoState,
    versionName: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconShape = SquircleShape(AppIconCorner)
        val bitmap = appInfo.icon

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(AppIconSize)
                    .clip(iconShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(AppIconSize)
                    .clip(iconShape)
                    .background(COUITheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = appInfo.label.take(1).uppercase(),
                    style = COUITheme.textStyles.headlineM,
                    color = COUITheme.colorScheme.labelOnColor,
                )
            }
        }

        Text(
            text = appInfo.label,
            style = COUITheme.textStyles.headlineM,
            color = COUITheme.colorScheme.labelPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 14.dp, start = 32.dp, end = 32.dp),
        )
        Text(
            text = if (versionName.isNullOrBlank()) {
                appInfo.packageName
            } else {
                "${appInfo.packageName} · $versionName"
            },
            style = COUITheme.textStyles.bodyS,
            color = COUITheme.colorScheme.labelSecondaryVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp),
        )
    }
}
