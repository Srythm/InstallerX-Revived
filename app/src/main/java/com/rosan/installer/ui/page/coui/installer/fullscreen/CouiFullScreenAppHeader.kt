// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import com.rosan.installer.util.toast
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

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
    versionLine: String?,
    modifier: Modifier = Modifier,
    onIconClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconShape = SquircleShape(AppIconCorner)
        val bitmap = appInfo.icon
        val iconModifier = Modifier
            .size(AppIconSize)
            .clip(iconShape)
            .then(if (onIconClick != null) Modifier.clickable { onIconClick() } else Modifier)

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = iconModifier,
            )
        } else {
            Box(
                modifier = iconModifier.background(COUITheme.colorScheme.primary),
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
            modifier = Modifier
                .padding(top = 14.dp, start = 32.dp, end = 32.dp)
                .pointerInput(appInfo.label) {
                    detectTapGestures(
                        onLongPress = {
                            scope.launch {
                                val clipData = ClipData.newPlainText("App Name", appInfo.label)
                                clipboard.setClipEntry(clipData.toClipEntry())
                                context.toast(R.string.copied_format, appInfo.label)
                            }
                        },
                    )
                },
        )
        Text(
            text = if (versionLine.isNullOrBlank()) {
                appInfo.packageName
            } else {
                "${appInfo.packageName} · $versionLine"
            },
            style = COUITheme.textStyles.bodyS,
            color = COUITheme.colorScheme.labelSecondaryVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 4.dp, start = 32.dp, end = 32.dp)
                .pointerInput(appInfo.packageName) {
                    detectTapGestures(
                        onLongPress = {
                            scope.launch {
                                val clipData = ClipData.newPlainText("Package Name", appInfo.packageName)
                                clipboard.setClipEntry(clipData.toClipEntry())
                                context.toast(R.string.copied_format, appInfo.packageName)
                            }
                        },
                    )
                },
        )
    }
}
