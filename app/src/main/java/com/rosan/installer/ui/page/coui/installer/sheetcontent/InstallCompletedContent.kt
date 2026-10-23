// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.session.model.InstallResult
import com.rosan.installer.ui.icons.AppCOUIIcons
import com.rosan.installer.ui.theme.couiSheetCardColors
import com.rosan.installer.ui.util.isGestureNavigation
import com.rosan.installer.util.help
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic

@Composable
fun InstallCompletedContent(results: List<InstallResult>, onClose: () -> Unit) {
    val filteredResults = remember(results) {
        results
            // 1. Group by packageName
            .groupBy { it.entity.app.packageName }
            .flatMap { (_, groupResults) ->
                // Find BaseEntity
                val baseResult = groupResults.find { it.entity.app is AppEntity.BaseEntity }

                if (baseResult != null) {
                    listOf(baseResult)
                } else {
                    listOfNotNull(groupResults.firstOrNull())
                }
            }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
            LazyColumn(
                modifier = Modifier
                    .wrapContentSize()
                    .scrollEndHaptic()
                    .overScrollVertical(),
                overscrollEffect = null,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(filteredResults, key = { it.entity.app.packageName + it.entity.app.name }) { result ->
                    CouiResultItemCard(
                        result = result,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
        ) {
            TextButton(
                onClick = onClose,
                text = stringResource(R.string.finish),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CouiResultItemCard(result: InstallResult) {
    val app = result.entity.app
    val appLabel = (app as? AppEntity.BaseEntity)?.label ?: app.packageName

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = couiSheetCardColors(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = appLabel,
                    style = COUITheme.textStyles.headline1,
                )
                Text(
                    text = app.packageName,
                    style = COUITheme.textStyles.subtitle,
                    color = COUITheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            if (result.success) {
                CouiSuccessRow()
            } else if (result.error != null) {
                CouiCompletedErrorCardContent(error = result.error)
            }
        }
    }
}

@Composable
private fun CouiSuccessRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = AppCOUIIcons.Ok,
            contentDescription = "Success",
            tint = COUITheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.installer_install_success),
            style = COUITheme.textStyles.headline1,
            color = COUITheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CouiCompletedErrorCardContent(error: Throwable, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val contentColor = if (isDark) COUITheme.colorScheme.onSurface else Color(0xFF601A15)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = AppCOUIIcons.Close,
                contentDescription = stringResource(R.string.installer_install_failed),
                tint = contentColor,
            )
            Text(
                text = error.help(),
                fontWeight = FontWeight.Bold,
                style = COUITheme.textStyles.body1,
                color = contentColor,
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp),
            color = MaterialTheme.colorScheme.outline,
        )

        Text(
            text = (error.message ?: "An unknown error occurred.").trim(),
            style = COUITheme.textStyles.subtitle,
            color = COUITheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
        )
    }
}
