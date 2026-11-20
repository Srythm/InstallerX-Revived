// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.ui.icons.AppCOUIIcons
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsCard
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/** App details and the permission destination; install options remain in the top bar. */
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
    val actionsEnabled = LocalFullScreenActionsEnabled.current
    val details = listOfNotNull(
        entity?.size?.takeIf { it > 0 }?.let {
            stringResource(R.string.installer_detail_size) to Formatter.formatFileSize(context, it)
        },
        entity?.targetSdk?.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.installer_detail_target_sdk) to it
        },
        entity?.minSdk?.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.installer_detail_min_sdk) to it
        },
        entity?.arch?.toString()?.takeIf { it.isNotBlank() && it != "NONE" }?.let {
            stringResource(R.string.installer_detail_arch) to it
        },
        sourceLabel?.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.installer_detail_source) to it
        },
        entity?.signatureHash?.let {
            stringResource(R.string.installer_detail_signature) to it.toShortFingerprint()
        },
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        if (details.isNotEmpty()) {
            SectionTitle(stringResource(R.string.installer_detail_title))
            CouiSettingsCard(Modifier.fillMaxWidth().padding(horizontal = FullScreenHorizontalPadding)) {
                details.forEachIndexed { index, (label, value) ->
                    DetailRow(label, value)
                    if (index != details.lastIndex) {
                        // Indented like COUI's own grouped rows: the rule starts where the text
                        // does, not at the card edge.
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
        if (permissionCount > 0) {
            Spacer(Modifier.size(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FullScreenHorizontalPadding),
                onClick = if (actionsEnabled) onOpenPermissions else null,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AppCOUIIcons.Info,
                        contentDescription = null,
                        tint = COUITheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permission_list),
                            style = COUITheme.textStyles.bodyXL,
                            color = COUITheme.colorScheme.labelPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (sensitivePermissionCount > 0) {
                                stringResource(
                                    R.string.installer_permission_entry_summary_sensitive,
                                    permissionCount,
                                    sensitivePermissionCount,
                                )
                            } else {
                                stringResource(R.string.installer_permission_entry_summary, permissionCount)
                            },
                            style = COUITheme.textStyles.bodyM,
                            color = COUITheme.colorScheme.labelSecondaryVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Icon(
                        imageVector = AppCOUIIcons.Back,
                        contentDescription = null,
                        tint = COUITheme.colorScheme.labelTertiary,
                        modifier = Modifier.size(16.dp).rotate(180f),
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
        style = COUITheme.textStyles.bodyM,
        color = COUITheme.colorScheme.labelSecondaryVariant,
        modifier = Modifier.padding(
            start = FullScreenHorizontalPadding,
            end = FullScreenHorizontalPadding,
            top = 4.dp,
            bottom = 8.dp,
        ),
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            style = COUITheme.textStyles.bodyXL,
            color = COUITheme.colorScheme.labelPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        // The value column gets the larger share: a source such as "Shell (com.android.shell)"
        // used to wrap onto a second line and make that one row taller than the rest.
        Text(
            text = value,
            style = COUITheme.textStyles.bodyM,
            color = COUITheme.colorScheme.labelSecondaryVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.6f),
        )
    }
}

private fun String.toShortFingerprint(): String = if (length <= 14) this else "${take(6)}…${takeLast(4)}"
