// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.domain.engine.model.install.UninstallFlags
import com.rosan.installer.ui.page.coui.installer.components.AppInfoSlot
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import com.rosan.installer.ui.page.coui.widgets.CouiCheckboxWidget
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.theme.couiSheetCardColors
import com.rosan.installer.ui.util.isGestureNavigation
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.COUITheme.isDynamicColor

@Composable
fun UninstallPrepareContent(
    viewModel: InstallerViewModel,
    onCancel: () -> Unit,
    onUninstall: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val info = uiState.uiUninstallInfo ?: return
    val uninstallFlags = uiState.config.uninstallFlags
    val isArchived = info.isArchived

    val deleteKeepData = uninstallFlags.hasFlag(UninstallFlags.DELETE_KEEP_DATA)
    val deleteAllUsers = uninstallFlags.hasFlag(UninstallFlags.DELETE_ALL_USERS)
    val deleteSystemApp = uninstallFlags.hasFlag(UninstallFlags.DELETE_SYSTEM_APP)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppInfoSlot(
            appInfo = AppInfoState(
                icon = uiState.displayIcons[info.packageName],
                label = info.appLabel ?: "Unknown App",
                packageName = info.packageName,
            ),
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isArchived) {
            Text(
                text = stringResource(R.string.uninstall_archive_message),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = COUITheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                style = COUITheme.textStyles.body1,
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = couiSheetCardColors(),
            ) {
                CouiCheckboxWidget(
                    title = stringResource(id = R.string.uninstall_keep_data),
                    description = stringResource(id = R.string.uninstall_keep_data_desc),
                    checked = deleteKeepData,
                    onCheckedChange = { isChecked ->
                        viewModel.dispatch(
                            InstallerViewAction.ToggleUninstallFlag(
                                flag = UninstallFlags.DELETE_KEEP_DATA,
                                enable = isChecked,
                            ),
                        )
                    },
                )

                CouiCheckboxWidget(
                    title = stringResource(id = R.string.uninstall_all_users),
                    description = stringResource(id = R.string.uninstall_all_users_desc),
                    checked = deleteAllUsers,
                    onCheckedChange = { isChecked ->
                        viewModel.dispatch(
                            InstallerViewAction.ToggleUninstallFlag(
                                flag = UninstallFlags.DELETE_ALL_USERS,
                                enable = isChecked,
                            ),
                        )
                    },
                )

                CouiCheckboxWidget(
                    title = stringResource(id = R.string.uninstall_delete_system_app),
                    description = stringResource(id = R.string.uninstall_delete_system_app_desc),
                    checked = deleteSystemApp,
                    onCheckedChange = { isChecked ->
                        viewModel.dispatch(
                            InstallerViewAction.ToggleUninstallFlag(
                                flag = UninstallFlags.DELETE_SYSTEM_APP,
                                enable = isChecked,
                            ),
                        )
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                text = stringResource(R.string.cancel),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    color = if (isDynamicColor) COUITheme.colorScheme.secondaryContainer else COUITheme.colorScheme.secondaryVariant,
                    textColor = if (isDynamicColor) COUITheme.colorScheme.onSecondaryContainer else COUITheme.colorScheme.onSecondaryVariant,
                ),
                onClick = onCancel,
            )
            TextButton(
                text = stringResource(if (isArchived) R.string.uninstall_archive else R.string.uninstall),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    color = if (isDynamicColor) COUITheme.colorScheme.secondaryContainer else COUITheme.colorScheme.secondaryVariant,
                    textColor = MaterialTheme.colorScheme.error,
                ),
                onClick = onUninstall,
            )
        }
    }
}
