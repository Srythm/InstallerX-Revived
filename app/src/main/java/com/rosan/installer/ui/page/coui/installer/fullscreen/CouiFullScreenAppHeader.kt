// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

private val AppIconSize = 48.dp

/**
 * Application identity block for the full-screen installer.
 *
 * Kept outside the stage content so the icon, name and package line stay put
 * while the installation moves from prepare to installing to the result. The
 * stage body below it is what changes.
 */
@Composable
fun CouiFullScreenAppHeader(
    appInfo: AppInfoState,
    versionName: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconShape = RoundedCornerShape(14.dp)
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
                    style = COUITheme.textStyles.title4,
                    color = COUITheme.colorScheme.onPrimary,
                )
            }
        }

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = appInfo.label,
                style = COUITheme.textStyles.title4,
                color = COUITheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (versionName.isNullOrBlank()) {
                    appInfo.packageName
                } else {
                    "${appInfo.packageName} · $versionName"
                },
                style = COUITheme.textStyles.footnote1,
                color = COUITheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
