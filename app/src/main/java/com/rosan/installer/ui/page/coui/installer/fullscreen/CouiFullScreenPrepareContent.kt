// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.ui.icons.AppCOUIIcons
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Body of the full-screen prepare stage.
 *
 * Deliberately minimal: install options live in the top-bar overflow menu and
 * the permission list has its own page, so this only answers "what am I about
 * to install" and offers the one secondary destination.
 */
@Composable
fun CouiFullScreenPrepareContent(
    entity: AppEntity.BaseEntity?,
    sourceLabel: String?,
    permissionCount: Int,
    sensitivePermissionCount: Int,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
    ) {
        SectionTitle(text = stringResource(R.string.installer_detail_title))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                entity?.size?.takeIf { it > 0 }?.let {
                    DetailRow(
                        label = stringResource(R.string.installer_detail_size),
                        value = Formatter.formatFileSize(context, it),
                    )
                    HorizontalDivider()
                }

                entity?.targetSdk?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(
                        label = stringResource(R.string.installer_detail_target_sdk),
                        value = it,
                    )
                    HorizontalDivider()
                }

                entity?.minSdk?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(
                        label = stringResource(R.string.installer_detail_min_sdk),
                        value = it,
                    )
                    HorizontalDivider()
                }

                entity?.arch?.let {
                    DetailRow(
                        label = stringResource(R.string.installer_detail_arch),
                        value = it.toString(),
                    )
                    HorizontalDivider()
                }

                sourceLabel?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(
                        label = stringResource(R.string.installer_detail_source),
                        value = it,
                    )
                    HorizontalDivider()
                }

                entity?.signatureHash?.let {
                    DetailRow(
                        label = stringResource(R.string.installer_detail_signature),
                        value = it.toShortFingerprint(),
                        compact = true,
                    )
                }
            }
        }

        if (permissionCount > 0) {
            Spacer(modifier = Modifier.size(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPermissions)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AppCOUIIcons.Info,
                        contentDescription = null,
                        tint = COUITheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.permission_list),
                        style = COUITheme.textStyles.body1,
                        color = COUITheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (sensitivePermissionCount > 0) {
                            stringResource(
                                R.string.installer_permission_entry_summary_sensitive,
                                permissionCount,
                                sensitivePermissionCount,
                            )
                        } else {
                            stringResource(
                                R.string.installer_permission_entry_summary,
                                permissionCount,
                            )
                        },
                        style = COUITheme.textStyles.footnote1,
                        color = COUITheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = AppCOUIIcons.Back,
                        contentDescription = null,
                        tint = COUITheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier
                            .size(12.dp)
                            .rotate(180f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = COUITheme.textStyles.footnote1,
        color = COUITheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 6.dp),
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = COUITheme.textStyles.body1,
            color = COUITheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = COUITheme.textStyles.body1,
            fontSize = if (compact) 12.sp else COUITheme.textStyles.body1.fontSize,
            color = COUITheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

private fun String.toShortFingerprint(): String =
    if (length <= 14) this else "${take(6)}…${takeLast(4)}"
