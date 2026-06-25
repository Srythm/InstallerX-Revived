// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogButton
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.dialogButtons

/**
 * Displays a confirmation dialog after a successful uninstallation.
 * It shows the information of the app that was just uninstalled and provides an action to close the dialog.
 *
 * This implementation is modeled after InstallSuccessDialog.
 */
@Composable
fun uninstallSuccessDialog(
    viewModel: InstallerViewModel
): DialogParams {
    // Use the shared uninstallInfoDialog to get the base layout with the app's icon, title, and subtitle.
    // The "magic wand" extra-button is intentionally hidden: the app is
    // already gone at this point, so opening its settings page is not
    // possible (and the previous empty onClick was a dead affordance).
    val baseParams = uninstallInfoDialog(
        viewModel = viewModel,
        onTitleExtraClick = {
            // Intentionally blank — kept only because [uninstallInfoDialog]
            // requires a callback. The button is hidden via
            // [showTitleExtra] = false so the click path is unreachable.
        },
        showTitleExtra = false
    )

    // Override the text and buttons sections to provide a success message and a finish button.
    return baseParams.copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerUninstallSuccess.id,
        ) {
            // Display a clear success message to the user. The fullscreen
            // InstallMode renders this through [PositionFullScreen]'s body
            // slot, which lays it out as a single Text inside a horizontally
            // centered Box. Without [Modifier.fillMaxWidth] the Text wraps
            // to its own intrinsic width, and [TextAlign.Center] has no
            // extra room to align within — the text appears left-justified
            // (or however the parent aligns it) instead of centered. The
            // dialog mode already centers the text via its own Surface
            // container's default alignment, so this is effectively a
            // no-op there.
            Text(
                text = stringResource(R.string.uninstall_success_message),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        buttons = dialogButtons(
            DialogParamsType.InstallerUninstallSuccess.id
        ) {
            // The button list contains only a "Finish" button to close the dialog.
            listOf(
                DialogButton(stringResource(R.string.finish)) {
                    viewModel.dispatch(InstallerViewAction.Close)
                }
            )
        }
    )
}