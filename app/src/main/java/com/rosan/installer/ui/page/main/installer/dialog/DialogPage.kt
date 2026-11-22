// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.domain.engine.model.install.MmzSelectionMode
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.config.InstallMode
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.components.PositionDialog
import com.rosan.installer.ui.page.main.installer.components.PositionFullScreen
import com.rosan.installer.ui.page.main.installer.dialog.inner.ModuleInstallFullScreenContent
import com.rosan.installer.ui.page.main.installer.dialog.inner.ModuleInstallSheetContent
import com.rosan.installer.ui.page.main.installer.dialog.inner.installInfoDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.uninstallInfoDialog
import com.rosan.installer.ui.page.main.widget.util.InstallerEventCollector
import com.rosan.installer.ui.theme.InstallerMaterialExpressiveTheme
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalInstallerColorScheme
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import com.rosan.installer.ui.util.WindowBlurTransition
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.nav.gesture.WindowNavigationEventBridge

private data class FullScreenContentKey(
    val stageType: Any,
    val textId: String,
    val contentId: String,
    val buttonsId: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogPage(
    session: InstallerSessionRepository,
    viewModel: InstallerViewModel = koinViewModel { parametersOf(session) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage
    val viewSettings = uiState.viewSettings
    val installMode = uiState.config.installMode
    val temporarySeedColor = uiState.seedColor
    val useBlur = viewSettings.useBlur
    // Combined atomically with `stage` in `uiState` so tearing down the
    // fullscreen layer never pairs `isClosing = false` with an outdated stage.
    val isClosingFullscreen = uiState.isClosingFullscreen

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
                colorSpec = colorSpec,
            )
        } ?: globalColorScheme
    }

    LaunchedEffect(session.id) {
        viewModel.dispatch(InstallerViewAction.CollectSession(session))
    }

    InstallerEventCollector(viewModel)

    CompositionLocalProvider(
        LocalInstallerColorScheme provides activeColorScheme,
    ) {
        InstallerMaterialExpressiveTheme(
            colorScheme = activeColorScheme,
            darkTheme = isDark,
            compatStatusBarColor = false,
        ) {
            val colorScheme = InstallerTheme.colorScheme
            // Keep module flashing inside the selected presentation mode. Full-screen installs
            // remain edge-to-edge; dialog/notification flows retain the module bottom sheet.
            //
            // In fullscreen mode the module-progress stage is rendered by the SAME
            // [PositionFullScreen] instance as the rest of the install flow (see the
            // `useFullScreen` branch below). Routing it through a second, sibling call site
            // here would make Compose treat the two as unrelated composables: the pre-install
            // instance would leave composition and the module instance would enter it, re-running
            // its alpha enter fade from 0 — which is exactly the unwanted "fade in/out between
            // pages" the user sees. Module installs therefore only take this branch for the
            // non-fullscreen (dialog / notification) presentations.
            if (stage is InstallerStage.InstallingModule && installMode != InstallMode.FullScreen) {
                // Do NOT create a local variable for isDismissible here.
                // Capturing a changing local variable causes the lambda below to change,
                // which forces rememberModalBottomSheetState to recreate the state, resetting the sheet.

                val sheetState = rememberBottomSheetState(
                    initialValue = SheetValue.Hidden,
                    enabledValues = setOf(
                        SheetValue.Hidden,
                        SheetValue.Expanded,
                    ),
                    confirmValueChange = { sheetValue ->
                        if (sheetValue == SheetValue.Hidden) {
                            viewModel.uiState.value.isDismissible
                        } else {
                            true
                        }
                    },
                )

                ModalBottomSheet(
                    onDismissRequest = {
                        if (uiState.isDismissible) {
                            viewModel.dispatch(InstallerViewAction.Close)
                        }
                    },
                    sheetState = sheetState,
                    containerColor = colorScheme.surfaceContainer,
                    contentColor = colorScheme.onSurface,
                ) {
                    WindowNavigationEventBridge()
                    // Same single-instance rule as the COUI sheet: one stable effect fed by an
                    // animated radius, so a sheet that is already expanded on first composition
                    // still ramps 0 -> 30 and the exit leaves no residual blur flag.
                    WindowBlurTransition(
                        visible = sheetState.targetValue == SheetValue.Expanded,
                        useBlur = useBlur,
                    )

                    ModuleInstallSheetContent(
                        rootMode = uiState.rootMode,
                        outputLines = stage.output,
                        isFinished = stage.isFinished,
                        onReboot = { viewModel.dispatch(InstallerViewAction.Reboot("")) },
                        onSoftReboot = { viewModel.dispatch(InstallerViewAction.Reboot("ksud_soft_reboot")) },
                        onClose = { viewModel.dispatch(InstallerViewAction.Close) },
                        colorScheme = colorScheme,
                    )
                }
            }
            // Handle other non-Ready states: Show standard PositionDialog, or full-screen UI when FullScreen mode
            else if (stage !is InstallerStage.Ready) {
                val isInstallChoice = stage is InstallerStage.InstallChoice
                val useFullScreen = installMode == InstallMode.FullScreen ||
                    (isInstallChoice && installMode.isNotification)
                DisposableEffect(viewModel, useFullScreen) {
                    viewModel.setFullscreenPresentation(useFullScreen)
                    onDispose {
                        viewModel.setFullscreenPresentation(false)
                    }
                }

                val params = dialogGenerateParams(viewModel, isFullScreen = useFullScreen)

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

                fun performFullscreenExit() {
                    if (viewModel.uiState.value.isDismissible) {
                        viewModel.requestFullscreenClose()
                    }
                }

                fun performBack() {
                    val currentState = viewModel.uiState.value
                    when (val currentStage = currentState.stage) {
                        is InstallerStage.InstallExtendedSubMenu -> {
                            viewModel.dispatch(InstallerViewAction.InstallExtendedMenu)
                        }

                        is InstallerStage.InstallExtendedMenu -> {
                            viewModel.dispatch(InstallerViewAction.InstallPrepare)
                        }

                        is InstallerStage.InstallChoice -> {
                            if (currentState.mmzSelectionMode == MmzSelectionMode.APK_CHOICE) {
                                viewModel.dispatch(InstallerViewAction.SetApkSelection(false))
                                viewModel.dispatch(InstallerViewAction.SetMmzSelectionMode(MmzSelectionMode.INITIAL_CHOICE))
                            } else if (currentState.navigatedFromPrepareToChoice) {
                                viewModel.dispatch(InstallerViewAction.InstallPrepare)
                            } else {
                                performFullscreenExit()
                            }
                        }

                        is InstallerStage.Preparing,
                        is InstallerStage.InstallWaitingUnknownSource,
                        -> {
                            viewModel.dispatch(InstallerViewAction.Cancel)
                        }

                        is InstallerStage.InstallPrepare -> {
                            performFullscreenExit()
                        }

                        is InstallerStage.InstallConfirm -> {
                            viewModel.dispatch(
                                InstallerViewAction.ApproveSession(currentStage.sessionId, false),
                            )
                        }

                        else -> {
                            if (currentState.isDismissible) {
                                performFullscreenExit()
                            }
                        }
                    }
                }

                if (useFullScreen) {
                    val isSinglePackageInstallStage = stage is InstallerStage.InstallPrepare ||
                        stage is InstallerStage.InstallExtendedMenu ||
                        stage is InstallerStage.InstallExtendedSubMenu ||
                        stage is InstallerStage.Installing ||
                        stage is InstallerStage.InstallRetryDowngradeUsingUninstall ||
                        stage is InstallerStage.InstallSuccess ||
                        stage is InstallerStage.InstallFailed ||
                        stage is InstallerStage.InstallingModule
                    val isUninstallPackageStage = stage is InstallerStage.UninstallReady ||
                        stage is InstallerStage.Uninstalling ||
                        stage is InstallerStage.UninstallSuccess ||
                        stage is InstallerStage.UninstallFailed

                    val headerParams = when {
                        isUninstallPackageStage -> uninstallInfoDialog(viewModel, showTitleExtra = false)
                            .takeIf { it.title.content != null || it.icon.content != null }
                            ?: params

                        isSinglePackageInstallStage -> installInfoDialog(viewModel, showTitleExtra = false)
                            .takeIf { it.title.content != null || it.icon.content != null }
                            ?: params

                        else -> params
                    }
                    val isModuleInstalling = stage is InstallerStage.InstallingModule
                    val bodyText: (@Composable () -> Unit)? = if (isModuleInstalling) {
                        null
                    } else {
                        dialogInnerWidget(params.text)
                    }
                    val bodyContent: (@Composable () -> Unit)? = when (stage) {
                        is InstallerStage.InstallingModule -> {
                            {
                                ModuleInstallFullScreenContent(
                                    outputLines = stage.output,
                                    isFinished = stage.isFinished,
                                    colorScheme = colorScheme,
                                    onReboot = { viewModel.dispatch(InstallerViewAction.Reboot("")) },
                                    onClose = { viewModel.dispatch(InstallerViewAction.Close) },
                                )
                            }
                        }

                        else -> dialogInnerWidget(params.content)
                    }
                    val footerButton = if (isModuleInstalling) null else dialogInnerWidget(params.buttons)
                    PositionFullScreen(
                        onBackRequest = { performBack() },
                        isClosing = isClosingFullscreen,
                        onCloseAnimationFinished = { viewModel.completeFullscreenClose() },
                        contentKey = FullScreenContentKey(
                            stageType = stage::class,
                            textId = if (isModuleInstalling) "module" else params.text.id,
                            contentId = if (isModuleInstalling) "module_log" else params.content.id,
                            buttonsId = if (isModuleInstalling) "module" else params.buttons.id,
                        ),
                        centerIcon = dialogInnerWidget(headerParams.icon),
                        centerTitle = dialogInnerWidget(headerParams.title),
                        centerSubtitle = dialogInnerWidget(headerParams.subtitle),
                        centerContent = bodyContent,
                        centerText = bodyText,
                        centerButton = footerButton,
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
                        centerButton = dialogInnerWidget(params.buttons),
                    )
                }
            }
        }
    }
}
