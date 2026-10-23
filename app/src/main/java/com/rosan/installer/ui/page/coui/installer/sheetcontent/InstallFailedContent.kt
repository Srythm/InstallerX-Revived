package com.rosan.installer.ui.page.coui.installer.sheetcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.page.coui.installer.components.AppInfoSlot
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import com.rosan.installer.ui.page.coui.widgets.CouiErrorTextBlock
import com.rosan.installer.ui.page.coui.widgets.CouiNavigationItemWidget
import com.rosan.installer.ui.page.coui.widgets.CouiUninstallConfirmationDialog
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.theme.couiSheetCardColors
import com.rosan.installer.ui.util.isGestureNavigation
import com.rosan.installer.ui.util.rememberErrorSuggestions
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.COUITheme.isDynamicColor

@Composable
fun InstallFailedContent(
    appInfo: AppInfoState,
    viewModel: InstallerViewModel,
    onClose: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentError = uiState.error

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppInfoSlot(appInfo = appInfo)
        Spacer(modifier = Modifier.height(32.dp))
        CouiErrorTextBlock(
            error = currentError,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.height(16.dp))
        CouiErrorSuggestions(
            error = currentError,
            viewModel = viewModel,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
        ) {
            TextButton(
                onClick = onClose,
                text = stringResource(R.string.close),
                colors = ButtonDefaults.textButtonColors(
                    color = if (isDynamicColor) COUITheme.colorScheme.secondaryContainer else COUITheme.colorScheme.secondaryVariant,
                    textColor = if (isDynamicColor) COUITheme.colorScheme.onSecondaryContainer else COUITheme.colorScheme.onSecondaryVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CouiErrorSuggestions(
    error: Throwable,
    viewModel: InstallerViewModel,
) {
    val showUninstallConfirmDialogState = remember { mutableStateOf(false) }
    var confirmKeepData by remember { mutableStateOf(false) }
    var pendingConflictingPackage by remember { mutableStateOf<String?>(null) }

    val visibleSuggestions = rememberErrorSuggestions(
        error = error,
        viewModel = viewModel,
        onShowUninstallConfirm = { keepData, conflictingPkg ->
            confirmKeepData = keepData
            pendingConflictingPackage = conflictingPkg
            showUninstallConfirmDialogState.value = true
        },
    )

    if (visibleSuggestions.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SmallTitle(
                text = stringResource(R.string.smart_suggestions),
                insideMargin = PaddingValues(12.dp, 8.dp),
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = couiSheetCardColors(),
            ) {
                visibleSuggestions.forEach { suggestion ->
                    CouiNavigationItemWidget(
                        title = stringResource(id = suggestion.labelRes),
                        description = suggestion.descriptionRes?.let { stringResource(id = it) } ?: "",
                        onClick = suggestion.onClick,
                        insideMargin = PaddingValues(12.dp),
                    )
                }
            }
        }
    }

    CouiUninstallConfirmationDialog(
        showState = showUninstallConfirmDialogState,
        onDismiss = { showUninstallConfirmDialogState.value = false },
        onConfirm = {
            viewModel.dispatch(
                InstallerViewAction.UninstallAndRetryInstall(
                    keepData = confirmKeepData,
                    conflictingPackage = pendingConflictingPackage,
                ),
            )
        },
        keepData = confirmKeepData,
    )
}
