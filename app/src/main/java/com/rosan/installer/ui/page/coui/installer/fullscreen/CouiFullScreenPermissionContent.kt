// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.util.pm.getBestPermissionLabel
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
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

@Composable
fun CouiFullScreenPermissionContent(
    permissions: List<String>,
    modifier: Modifier = Modifier,
) {
    val sensitive = permissions.filter { it in SensitivePermissions }
    val normal = permissions.filter { it !in SensitivePermissions }

    // The shell leaves the bottom edge alone so the list can run under the navigation bar while
    // scrolling; the tail padding keeps the last row readable once it settles.
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp, bottom = bottomInset + 20.dp),
    ) {
        if (sensitive.isNotEmpty()) {
            PermissionSectionTitle(
                text = stringResource(R.string.installer_permission_section_sensitive, sensitive.size),
            )
            PermissionCard(permissions = sensitive, isSensitive = true)
        }

        if (normal.isNotEmpty()) {
            PermissionSectionTitle(
                text = stringResource(R.string.installer_permission_section_normal, normal.size),
            )
            PermissionCard(permissions = normal, isSensitive = false)
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
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FullScreenHorizontalPadding),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            permissions.forEachIndexed { index, permission ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = context.getBestPermissionLabel(permission),
                        style = COUITheme.textStyles.bodyXL,
                        color = COUITheme.colorScheme.labelPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSensitive) {
                        Text(
                            text = stringResource(R.string.installer_permission_requires_grant),
                            style = COUITheme.textStyles.bodyS,
                            color = COUITheme.colorScheme.error,
                        )
                    }
                }
                if (index != permissions.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}
