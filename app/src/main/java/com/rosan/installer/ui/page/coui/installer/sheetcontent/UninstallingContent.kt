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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.page.coui.installer.components.AppInfoSlot
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.util.isGestureNavigation
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.InfiniteProgressIndicator
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.COUITheme.isDynamicColor

@Composable
fun UninstallingContent(
    viewModel: InstallerViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val info = uiState.uiUninstallInfo ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppInfoSlot(
            appInfo = AppInfoState(
                icon = uiState.displayIcons[info.packageName],
                label = info.appLabel ?: "Unknown App",
                packageName = info.packageName,
            ),
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            enabled = false,
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                disabledColor = if (isDynamicColor) COUITheme.colorScheme.secondaryContainer else COUITheme.colorScheme.secondaryVariant,
                disabledContentColor = if (isDynamicColor) COUITheme.colorScheme.onSecondaryContainer else COUITheme.colorScheme.onSecondaryVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfiniteProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.installer_uninstalling),
                )
            }
        }
    }
}
