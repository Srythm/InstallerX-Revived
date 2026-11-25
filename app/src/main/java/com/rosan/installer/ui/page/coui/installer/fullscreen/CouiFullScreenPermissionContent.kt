// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import android.content.Context
import android.content.pm.PermissionInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.util.pm.getBestPermissionLabel
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Permissions the install sheet normally shows inline. Split out so the
 * full-screen prepare stage can keep its body short while still surfacing the
 * count and sending the user here for the detail.
 */
val SensitivePermissions: Set<String> = setOf(
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_BACKGROUND_LOCATION",
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.READ_CALL_LOG",
    "android.permission.WRITE_CALL_LOG",
    "android.permission.CALL_PHONE",
    "android.permission.READ_SMS",
    "android.permission.SEND_SMS",
    "android.permission.READ_PHONE_STATE",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.BODY_SENSORS",
    "android.permission.READ_CALENDAR",
    "android.permission.WRITE_CALENDAR",
)

fun List<String>.sensitiveCount(): Int = count { it in SensitivePermissions }

internal fun isSensitivePermission(context: Context, permission: String, inspectionMode: Boolean): Boolean {
    if (inspectionMode) return permission in SensitivePermissions
    return runCatching {
        val info = context.packageManager.getPermissionInfo(permission, 0)
        @Suppress("DEPRECATION")
        (info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS
    }.getOrElse {
        permission in SensitivePermissions
    }
}

@Composable
internal fun rememberSensitivePermissionCount(permissions: List<String>): Int {
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    return remember(permissions, inspectionMode) {
        permissions.count { isSensitivePermission(context, it, inspectionMode) }
    }
}

@Composable
fun CouiFullScreenPermissionContent(
    permissions: List<String>,
    modifier: Modifier = Modifier,
    inline: Boolean = false,
) {
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    val (sensitive, normal) = remember(permissions, inspectionMode) {
        permissions.partition { isSensitivePermission(context, it, inspectionMode) }
    }

    // Inline content joins the prepare page's existing vertical scroll. The standalone mode keeps
    // its own scrolling and chrome insets for previews and any direct full-screen use.
    val contentModifier = if (inline) {
        modifier.fillMaxWidth().padding(bottom = 8.dp)
    } else {
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                top = LocalFullScreenTopBleed.current + 4.dp,
                bottom = bottomInset + 20.dp,
            )
    }
    Column(modifier = contentModifier) {
        if (sensitive.isNotEmpty()) {
            PermissionSectionTitle(
                text = stringResource(R.string.installer_permission_section_sensitive, sensitive.size),
            )
            PermissionCard(permissions = sensitive, isSensitive = true, inline = inline)
        }

        if (normal.isNotEmpty()) {
            PermissionSectionTitle(
                text = stringResource(R.string.installer_permission_section_normal, normal.size),
            )
            PermissionCard(permissions = normal, isSensitive = false, inline = inline)
        }

        if (permissions.isEmpty()) {
            PermissionSectionTitle(text = stringResource(R.string.permission_list))
        }
    }
}

@Composable
private fun PermissionSectionTitle(text: String) {
    Text(
        text = text,
        style = COUITheme.textStyles.bodyS,
        color = COUITheme.colorScheme.labelSecondaryVariant,
        modifier = Modifier.padding(
            start = FullScreenHorizontalPadding,
            end = FullScreenHorizontalPadding,
            top = 6.dp,
            bottom = 6.dp,
        ),
    )
}

@Composable
private fun PermissionCard(
    permissions: List<String>,
    isSensitive: Boolean,
    inline: Boolean,
) {
    if (inline) {
        PermissionRows(permissions = permissions, isSensitive = isSensitive)
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FullScreenHorizontalPadding),
        ) {
            PermissionRows(permissions = permissions, isSensitive = isSensitive)
        }
    }
}

@Composable
private fun PermissionRows(
    permissions: List<String>,
    isSensitive: Boolean,
) {
    val context = LocalContext.current

    // Rows are separated by vertical padding alone: COUI's permission list carries no hairline
    // between entries, and the section titles already delimit the sensitive/normal groups.
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        permissions.forEach { permission ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = context.getBestPermissionLabel(permission),
                        style = COUITheme.textStyles.headlineXS,
                        color = COUITheme.colorScheme.labelPrimary,
                    )
                    Text(
                        text = permission,
                        style = COUITheme.textStyles.bodyS,
                        color = COUITheme.colorScheme.labelSecondaryVariant,
                    )
                }
                if (isSensitive) {
                    Text(
                        text = stringResource(R.string.installer_permission_requires_grant),
                        style = COUITheme.textStyles.bodyS,
                        color = COUITheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
