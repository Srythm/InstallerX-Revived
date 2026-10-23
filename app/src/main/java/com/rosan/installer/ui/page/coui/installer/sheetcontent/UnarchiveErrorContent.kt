// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.domain.archive.model.UnarchiveErrorAction
import com.rosan.installer.domain.archive.model.UnarchiveStatus
import com.rosan.installer.ui.util.isGestureNavigation
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.COUITheme.isDynamicColor

@Composable
fun UnarchiveErrorContent(
    status: UnarchiveStatus,
    requiredBytes: Long,
    installerLabel: CharSequence?,
    onClose: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = unarchiveErrorTitle(status, installerLabel),
            style = COUITheme.textStyles.title3,
            color = COUITheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        Text(
            text = unarchiveErrorMessage(status, requiredBytes, installerLabel),
            style = COUITheme.textStyles.body1,
            color = COUITheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onClose,
                text = stringResource(R.string.close),
                colors = ButtonDefaults.textButtonColors(
                    color = if (isDynamicColor) COUITheme.colorScheme.secondaryContainer else COUITheme.colorScheme.secondaryVariant,
                    textColor = if (isDynamicColor) COUITheme.colorScheme.onSecondaryContainer else COUITheme.colorScheme.onSecondaryVariant,
                ),
                modifier = Modifier.weight(1f),
            )
            if (status.primaryAction != UnarchiveErrorAction.CLOSE) {
                TextButton(
                    onClick = onPrimaryAction,
                    text = stringResource(status.actionLabelResId),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun unarchiveErrorTitle(
    status: UnarchiveStatus,
    installerLabel: CharSequence?,
): String = when (status) {
    UnarchiveStatus.InstallerDisabled,
    UnarchiveStatus.InstallerUninstalled,
    -> stringResource(
        status.titleResId,
        installerLabel ?: stringResource(R.string.installer_label_unknown),
    )

    else -> stringResource(status.titleResId)
}

@Composable
private fun unarchiveErrorMessage(
    status: UnarchiveStatus,
    requiredBytes: Long,
    installerLabel: CharSequence?,
): String {
    val context = LocalContext.current
    return when (status) {
        UnarchiveStatus.InsufficientStorage -> stringResource(
            status.messageResId,
            Formatter.formatShortFileSize(context, requiredBytes),
        )

        UnarchiveStatus.InstallerDisabled,
        UnarchiveStatus.InstallerUninstalled,
        -> stringResource(
            status.messageResId,
            installerLabel ?: stringResource(R.string.installer_label_unknown),
        )

        else -> stringResource(status.messageResId)
    }
}
