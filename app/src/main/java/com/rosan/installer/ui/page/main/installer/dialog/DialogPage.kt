// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.config.InstallMode
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.components.PositionDialog
import com.rosan.installer.ui.page.main.installer.components.PositionFullScreen
import com.rosan.installer.ui.page.main.installer.components.workingIcon
import com.rosan.installer.ui.page.main.installer.dialog.inner.ModuleInstallSheetContent
import com.rosan.installer.ui.page.main.installer.dialog.inner.installInfoDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.uninstallInfoDialog
import com.rosan.installer.ui.page.main.widget.util.InstallerEventCollector
import com.rosan.installer.ui.theme.InstallerMaterialExpressiveTheme
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalInstallerColorScheme
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import com.rosan.installer.ui.util.WindowBlurEffect
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogPage(
    session: InstallerSessionRepository,
    viewModel: InstallerViewModel = koinViewModel { parametersOf(session) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage
    val viewSettings = uiState.viewSettings
    val installMode = uiState.config.installMode
    val temporarySeedColor = uiState.seedColor
    val useBlur = viewSettings.useBlur
    // External signal that drives the fullscreen layer's exit fade. Flipped
    // to `true` by [InstallerViewModel.requestFullscreenClose] (which is
    // what [performBack] / [performFullscreenExit] call from the fullscreen
    // branch below). The actual session teardown happens ~220ms later on
    // `viewModelScope` so the fade-out has time to play.
    val isClosingFullscreen by viewModel.isClosingFullscreen.collectAsStateWithLifecycle()

    val globalColorScheme = InstallerTheme.colorScheme
    val isDark = InstallerTheme.isDark
    val paletteStyle = InstallerTheme.paletteStyle
    val colorSpec = InstallerTheme.colorSpec

    val activeColorScheme = remember(temporarySeedColor, globalColorScheme, isDark, paletteStyle) {
        temporarySeedColor?.let {
            dynamicColorScheme(
                keyColor = it,
                isDark = isDark,
                style = paletteStyle,
                colorSpec = colorSpec
            )
        } ?: globalColorScheme
    }

    LaunchedEffect(session.id) {
        viewModel.dispatch(InstallerViewAction.CollectSession(session))
    }

    InstallerEventCollector(viewModel)

    CompositionLocalProvider(
        LocalInstallerColorScheme provides activeColorScheme
    ) {
        InstallerMaterialExpressiveTheme(
            colorScheme = activeColorScheme,
            darkTheme = isDark,
            compatStatusBarColor = false
        ) {
            val colorScheme = InstallerTheme.colorScheme
            // Handle InstallingModule state: Show ModalBottomSheet
            if (stage is InstallerStage.InstallingModule) {
                // Do NOT create a local variable for isDismissible here.
                // Capturing a changing local variable causes the lambda below to change,
                // which forces rememberModalBottomSheetState to recreate the state, resetting the sheet.

                val sheetState = rememberBottomSheetState(
                    initialValue = SheetValue.Hidden,
                    enabledValues = setOf(
                        SheetValue.Hidden,
                        SheetValue.Expanded
                    ),
                    confirmValueChange = { sheetValue ->
                        if (sheetValue == SheetValue.Hidden) {
                            viewModel.uiState.value.isDismissible
                        } else {
                            true
                        }
                    }
                )

                ModalBottomSheet(
                    onDismissRequest = {
                        if (uiState.isDismissible) {
                            viewModel.dispatch(InstallerViewAction.Close)
                        }
                    },
                    sheetState = sheetState,
                    containerColor = colorScheme.surfaceContainer,
                    contentColor = colorScheme.onSurface
                ) {
                    val blurRadius = if (sheetState.targetValue == SheetValue.Expanded) 30 else 0
                    AnimatedContent(targetState = blurRadius) { targetState ->
                        WindowBlurEffect(useBlur = useBlur, blurRadius = targetState)
                    }

                    ModuleInstallSheetContent(
                        rootMode = uiState.rootMode,
                        outputLines = stage.output,
                        isFinished = stage.isFinished,
                        onReboot = { viewModel.dispatch(InstallerViewAction.Reboot("")) },
                        onSoftReboot = { viewModel.dispatch(InstallerViewAction.Reboot("ksud_soft_reboot")) },
                        onClose = { viewModel.dispatch(InstallerViewAction.Close) },
                        colorScheme = colorScheme
                    )
                }
            }
            // Handle other non-Ready states: Show standard PositionDialog, or full-screen UI when FullScreen mode
            else if (stage !is InstallerStage.Ready) {
                val params = dialogGenerateParams(viewModel)

                fun performDismiss() {
                    val currentUiState = viewModel.uiState.value
                    val currentStage = currentUiState.stage

                    if (currentUiState.isDismissible) {
                        // If we are in the confirmation stage and the user taps outside (scrim) or swipes back
                        if (currentStage is InstallerStage.InstallConfirm) {
                            viewModel.dispatch(InstallerViewAction.ApproveSession(currentStage.sessionId, false))
                            return
                        }

                        // Normal dismissal logic for other stages
                        if (currentUiState.viewSettings.disableNotificationOnDismiss) {
                            viewModel.dispatch(InstallerViewAction.Close)
                        } else {
                            viewModel.dispatch(InstallerViewAction.Background)
                        }
                    }
                }

                // Fullscreen-only exit path. Back press / scrim tap in the
                // fullscreen install flow always closes the activity — never
                // backgrounds it with a "Install X?" notification. The
                // fullscreen experience should feel like a real fullscreen
                // app: the user backs out, the activity disappears, that's
                // it. The InstallConfirm case is intentionally NOT handled
                // here (it's routed through performBack) because the
                // system install session needs an explicit approve/deny
                // answer rather than a simple app close.
                //
                // Note: this does NOT synchronously close the session. It
                // delegates to [InstallerViewModel.requestFullscreenClose],
                // which (a) flips the [InstallerViewModel.isClosingFullscreen]
                // state to drive the fade-out animation in
                // [PositionFullScreen], and (b) waits 220ms on
                // `viewModelScope` (whose lifetime is decoupled from the
                // composition) before performing the actual `close()`.
                // This sequencing is what makes the alpha fade-out visible:
                // if `close()` ran synchronously, the composable would
                // leave composition at t=0 and the fade would be cancelled
                // before it played.
                fun performFullscreenExit() {
                    if (viewModel.uiState.value.isDismissible) {
                        viewModel.requestFullscreenClose()
                    }
                }

                // Fullscreen-specific back navigation. Walks the install flow
                // back to its parent stage instead of always closing the
                // activity, so that submenus and intermediate screens can be
                // exited with the system back gesture / scrim tap without
                // aborting the whole install. When the user reaches the
                // root of the flow, the activity is closed (via
                // performFullscreenExit) so no "install?" notification
                // appears.
                fun performBack() {
                    val currentStage = viewModel.uiState.value.stage
                    val currentState = viewModel.uiState.value
                    when (currentStage) {
                        is InstallerStage.InstallExtendedSubMenu -> {
                            viewModel.dispatch(InstallerViewAction.InstallExtendedMenu)
                        }

                        is InstallerStage.InstallExtendedMenu -> {
                            viewModel.dispatch(InstallerViewAction.InstallPrepare)
                        }

                        is InstallerStage.InstallChoice -> {
                            if (currentState.navigatedFromPrepareToChoice) {
                                viewModel.dispatch(InstallerViewAction.InstallPrepare)
                            } else {
                                performFullscreenExit()
                            }
                        }

                        is InstallerStage.InstallPrepare -> {
                            // Prepare is the root of the install flow in fullscreen
                            // mode; pressing back here closes the activity without
                            // leaving a "install?" notification behind.
                            performFullscreenExit()
                        }

                        is InstallerStage.InstallConfirm -> {
                            viewModel.dispatch(
                                InstallerViewAction.ApproveSession(currentStage.sessionId, false)
                            )
                        }

                        else -> {
                            if (currentState.isDismissible) {
                                performFullscreenExit()
                            }
                        }
                    }
                }

                if (installMode == InstallMode.FullScreen) {
                    // Fullscreen keeps the package header (icon, title, subtitle)
                    // constant across the whole install flow, while the body
                    // content and the bottom buttons animate when the stage
                    // changes.
                    //
                    // The header source depends on the flow:
                    //   - For install / pre-install stages, the header is built
                    //     from the package being installed (installInfoDialog).
                    //   - For uninstall stages, the header is built from the
                    //     package being uninstalled (uninstallInfoDialog), so
                    //     the user still sees icon / label / package name / version
                    //     during UninstallReady / Uninstalling / UninstallSuccess /
                    //     UninstallFailed.
                    //   - For unarchive-only stages, neither info dialog has data,
                    //     so the header is empty (the body still shows everything
                    //     the user needs to know).
                    val headerParams = when (stage) {
                        is InstallerStage.UninstallReady,
                        is InstallerStage.UninstallResolveFailed,
                        is InstallerStage.Uninstalling,
                        is InstallerStage.UninstallSuccess,
                        is InstallerStage.UninstallFailed ->
                            uninstallInfoDialog(viewModel, showTitleExtra = false)

                        else -> installInfoDialog(viewModel)
                    }
                    // The "Analysing" stage has no body text in its stage
                    // dialog (the working icon sits in the header), but in
                    // fullscreen mode the header is fixed to the package
                    // info, so we synthesize a matching body here (working
                    // spinner + "Analysing..." text) so the user gets the
                    // same feedback as in dialog mode.
                    val isAnalysing = stage is InstallerStage.Analysing
                    val bodyText: (@Composable () -> Unit)? =
                        if (isAnalysing) {
                            {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    workingIcon()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.installer_analysing),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            dialogInnerWidget(params.text)
                        }
                    val bodyContent: (@Composable () -> Unit)? =
                        if (isAnalysing) null else dialogInnerWidget(params.content)
                    PositionFullScreen(
                        onBackRequest = { performBack() },
                        isClosing = isClosingFullscreen,
                        contentKey = stage,
                        centerIcon = dialogInnerWidget(headerParams.icon),
                        centerTitle = dialogInnerWidget(headerParams.title),
                        centerSubtitle = dialogInnerWidget(headerParams.subtitle),
                        centerContent = bodyContent,
                        centerText = bodyText,
                        centerButton = dialogInnerWidget(params.buttons)
                    )
                } else {
                    PositionDialog(
                        useBlur = useBlur,
                        onDismissRequest = { performDismiss() },
                        centerIcon = dialogInnerWidget(params.icon),
                        centerTitle = dialogInnerWidget(params.title),
                        centerSubtitle = dialogInnerWidget(params.subtitle),
                        centerText = dialogInnerWidget(params.text),
                        centerContent = dialogInnerWidget(params.content),
                        centerButton = dialogInnerWidget(params.buttons)
                    )
                }
            }
        }
    }
}
