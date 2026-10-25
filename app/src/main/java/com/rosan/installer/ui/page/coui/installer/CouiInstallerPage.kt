// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.engine.exception.ModuleInstallException
import com.rosan.installer.domain.engine.model.install.MmzSelectionMode
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.config.InstallMode
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.ui.icons.AppCOUIIcons
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import com.rosan.installer.ui.page.coui.installer.components.rememberAppInfoState
import com.rosan.installer.ui.page.coui.installer.fullscreen.CouiFullScreenAppHeader
import com.rosan.installer.ui.page.coui.installer.fullscreen.CouiFullScreenPermissionContent
import com.rosan.installer.ui.page.coui.installer.fullscreen.CouiFullScreenPrepareContent
import com.rosan.installer.ui.page.coui.installer.fullscreen.CouiFullScreenStatusContent
import com.rosan.installer.ui.page.coui.installer.fullscreen.CouiPositionFullScreen
import com.rosan.installer.ui.page.coui.installer.fullscreen.sensitiveCount
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallChoiceContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallCompletedContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallConfirmContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallExtendedMenuContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallFailedContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallModuleContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallPrepareContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallPreparePermissionContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallPreparingContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallSuccessContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallingContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.LoadingContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.NonInstallFailedContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.PrepareSettingsContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.UnarchiveErrorContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.UnarchivePrepareContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.UninstallFailedContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.UninstallPrepareContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.UninstallSuccessContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.UninstallingContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.unarchiveErrorTitle
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.DropdownItem
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.rememberUnknownSourcePermissionActionVisible
import com.rosan.installer.ui.page.main.widget.util.InstallerEventCollector
import com.rosan.installer.ui.theme.InstallerCOUITheme
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalInstallerColorScheme
import com.rosan.installer.ui.theme.couiSheetColorDark
import com.rosan.installer.ui.theme.couiSheetColorLight
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import com.rosan.installer.ui.util.WindowBlurEffect
import com.rosan.installer.ui.util.getSupportTitle
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.ListPopupDefaults
import io.github.suqi8.coui.kmp.basic.PopupPositionProvider
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.COUITheme.isDynamicColor
import io.github.suqi8.coui.kmp.window.WindowBottomSheet
import io.github.suqi8.coui.kmp.window.WindowListPopup
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun CouiInstallerPage(
    session: InstallerSessionRepository,
    viewModel: InstallerViewModel = koinViewModel { parametersOf(session) },
) {
    val context = LocalContext.current
    val showBottomSheet = remember { mutableStateOf(true) }
    val pendingDismissAction = remember { mutableStateOf<(() -> Unit)?>(null) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage
    val settings = uiState.viewSettings
    val showSettings = uiState.showCouiSheetRightActionSettings
    val showPermissions = uiState.showCouiPermissionList
    val temporarySeedColor = uiState.seedColor
    val currentPackageName = uiState.currentPackageName
    val displayIcons = uiState.displayIcons
    val error = uiState.error

    val globalSeedColor = InstallerTheme.seedColor
    val themeMode = InstallerTheme.themeMode
    val useCouiMonet = InstallerTheme.useCouiMonet
    val useDynamicColor = InstallerTheme.useDynamicColor
    val isDark = InstallerTheme.isDark
    val paletteStyle = InstallerTheme.paletteStyle
    val colorSpec = InstallerTheme.colorSpec
    val globalColorScheme = InstallerTheme.colorScheme

    val activeSeedColor = temporarySeedColor ?: globalSeedColor
    val activeMd3ColorScheme = remember(activeSeedColor, globalColorScheme, isDark, paletteStyle) {
        temporarySeedColor?.let {
            dynamicColorScheme(keyColor = it, isDark = isDark, style = paletteStyle, colorSpec = colorSpec)
        } ?: globalColorScheme
    }

    val analysisResults = uiState.analysisResults
    val sourceType = analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType ?: DataType.NONE
    var selectionMode by remember(session.id, sourceType) { mutableStateOf(MmzSelectionMode.INITIAL_CHOICE) }
    var preparedFromTypeChoice by remember(session.id, sourceType) { mutableStateOf(false) }
    var returnedToTypeChoice by remember(session.id, sourceType) { mutableStateOf(false) }
    val isApkChoice = sourceType == DataType.MIXED_MODULE_ZIP && selectionMode == MmzSelectionMode.APK_CHOICE
    val packageName = currentPackageName ?: analysisResults.firstOrNull()?.packageName ?: ""
    val appInfoState = rememberAppInfoState(
        analysisResults = analysisResults,
        currentPackageName = currentPackageName,
        displayIcons = displayIcons,
    )

    LaunchedEffect(session.id) {
        viewModel.dispatch(InstallerViewAction.CollectSession(session))
    }

    InstallerEventCollector(viewModel)

    val sheetTitle = when (stage) {
        is InstallerStage.Preparing -> stringResource(R.string.installer_preparing)

        is InstallerStage.InstallWaitingUnknownSource -> stringResource(R.string.installer_waiting_unknown_source)

        is InstallerStage.InstallChoice -> sourceType.getSupportTitle()

        is InstallerStage.InstallExtendedMenu -> stringResource(R.string.config_label_install_options)

        is InstallerStage.InstallPrepare -> when {
            showSettings -> stringResource(R.string.installer_settings)

            showPermissions -> stringResource(R.string.permission_list)

            else -> stringResource(
                if (viewModel.isInstallingModule) {
                    R.string.installer_install_module
                } else {
                    R.string.installer_install_app
                },
            )
        }

        is InstallerStage.InstallingModule -> if (stage.isFinished) {
            stringResource(R.string.installer_install_complete)
        } else {
            stringResource(R.string.installer_installing_module)
        }

        is InstallerStage.InstallConfirm -> stringResource(
            when (stage.requestType) {
                ConfirmationRequestType.PRE_APPROVAL -> R.string.installer_install_pre_approval
                ConfirmationRequestType.PERMISSIONS -> R.string.installer_permissions_confirm
                ConfirmationRequestType.INSTALL -> R.string.installer_install_confirm
            },
        )

        is InstallerStage.Installing -> stringResource(R.string.installer_installing)

        is InstallerStage.InstallCompleted -> stringResource(R.string.installer_install_success)

        is InstallerStage.InstallSuccess -> stringResource(R.string.installer_install_success)

        is InstallerStage.InstallFailed -> stringResource(R.string.installer_install_failed)

        is InstallerStage.UninstallReady -> stringResource(
            if (uiState.uiUninstallInfo?.isArchived == true) R.string.uninstall_archive else R.string.installer_uninstall_app,
        )

        is InstallerStage.Uninstalling -> stringResource(R.string.installer_uninstalling)

        is InstallerStage.UninstallSuccess -> stringResource(R.string.uninstall_success_message)

        is InstallerStage.UninstallFailed -> stringResource(R.string.uninstall_failed_message)

        is InstallerStage.UnarchiveReady -> stringResource(R.string.unarchive_restore)

        is InstallerStage.Unarchiving -> stringResource(R.string.unarchive_restoring)

        is InstallerStage.UnarchiveError -> unarchiveErrorTitle(stage.status, stage.installerLabel)

        is InstallerStage.UnarchiveFailed -> stringResource(R.string.unarchive_failed)

        is InstallerStage.AnalyseFailed -> stringResource(R.string.installer_analyse_failed)

        is InstallerStage.ResolveFailed -> stringResource(R.string.installer_resolve_failed)

        is InstallerStage.Resolving -> stringResource(R.string.installer_resolving)

        is InstallerStage.Analysing -> stringResource(R.string.installer_analysing)

        else -> stringResource(R.string.loading)
    }

    fun dismissSheet(afterDismiss: () -> Unit) {
        pendingDismissAction.value = afterDismiss
        showBottomSheet.value = false
    }

    val closeSheet: () -> Unit = {
        dismissSheet {
            viewModel.dispatch(InstallerViewAction.Close)
        }
    }

    val backToTypeChoice: () -> Unit = {
        // Clear the previous branch's selection before offering the other installation type.
        viewModel.dispatch(InstallerViewAction.SetApkSelection(false))
        analysisResults.flatMap { it.appEntities }
            .filter { it.selected && it.app is AppEntity.ModuleEntity }
            .forEach { entity ->
                viewModel.dispatch(InstallerViewAction.ToggleSelection(entity.app.packageName, entity, true))
            }
        selectionMode = MmzSelectionMode.INITIAL_CHOICE
        preparedFromTypeChoice = false
        returnedToTypeChoice = true
        if (stage !is InstallerStage.InstallChoice) {
            viewModel.dispatch(InstallerViewAction.InstallChoice)
        }
    }

    CompositionLocalProvider(
        LocalInstallerColorScheme provides activeMd3ColorScheme,
    ) {
        InstallerCOUITheme(
            seedColor = activeSeedColor,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            darkTheme = isDark,
            themeMode = themeMode,
            useCouiMonet = useCouiMonet,
            useDynamicColor = useDynamicColor && temporarySeedColor == null,
            compatStatusBarColor = false,
        ) {
            // Full-screen install mode. Only the stages migrated so far take this
            // branch; anything else keeps the bottom sheet so no flow loses its UI.
            if (uiState.config.installMode == InstallMode.FullScreen && isFullScreenStage(stage)) {
                CouiFullScreenInstaller(
                    stage = stage,
                    title = sheetTitle,
                    appInfo = appInfoState,
                    entity = appInfoState.primaryEntity as? AppEntity.BaseEntity,
                    versionName = (appInfoState.primaryEntity as? AppEntity.BaseEntity)?.versionName,
                    sourceLabel = uiState.initiatorAppLabel,
                    permissions = (appInfoState.primaryEntity as? AppEntity.BaseEntity)?.permissions.orEmpty(),
                    showPermissions = showPermissions,
                    showSettings = showSettings,
                    viewModel = viewModel,
                    onClose = { viewModel.dispatch(InstallerViewAction.Close) },
                )
                return@InstallerCOUITheme
            }

            WindowBottomSheet(
                show = showBottomSheet.value, // Always true as long as this page is composed.
                backgroundColor = if (isDynamicColor) {
                    COUITheme.colorScheme.surfaceContainerHigh
                } else if (isDark) {
                    couiSheetColorDark
                } else {
                    couiSheetColorLight
                },
                startAction = {
                    when (stage) {
                        is InstallerStage.Preparing,
                        is InstallerStage.InstallWaitingUnknownSource,
                        -> {
                            CouiBackButton(
                                icon = AppCOUIIcons.Close,
                                iconTint = COUITheme.colorScheme.onSurface,
                                onClick = {
                                    dismissSheet {
                                        viewModel.dispatch(InstallerViewAction.Cancel)
                                    }
                                },
                            )
                        }

                        is InstallerStage.InstallChoice -> {
                            if (isApkChoice) {
                                CouiBackButton(
                                    iconTint = COUITheme.colorScheme.onSurface,
                                    onClick = backToTypeChoice,
                                )
                            } else if (uiState.navigatedFromPrepareToChoice && !returnedToTypeChoice) {
                                // Came from Prepare (re-selecting splits) -> Show Back icon, go back to Prepare
                                CouiBackButton(
                                    icon = AppCOUIIcons.Back,
                                    iconTint = COUITheme.colorScheme.onSurface,
                                    onClick = { viewModel.dispatch(InstallerViewAction.InstallPrepare) },
                                )
                            } else {
                                // Initial choice or other origin -> Show Cancel icon, force close
                                CouiBackButton(
                                    icon = AppCOUIIcons.Close,
                                    iconTint = COUITheme.colorScheme.onSurface,
                                    onClick = closeSheet,
                                )
                            }
                        }

                        is InstallerStage.InstallConfirm -> {
                            CouiBackButton(
                                icon = AppCOUIIcons.Close,
                                iconTint = COUITheme.colorScheme.onSurface,
                                onClick = {
                                    if (stage.isSelfSession) {
                                        viewModel.dispatch(InstallerViewAction.ApproveSession(stage.sessionId, false))
                                    } else {
                                        dismissSheet {
                                            viewModel.dispatch(InstallerViewAction.ApproveSession(stage.sessionId, false))
                                        }
                                    }
                                },
                            )
                        }

                        is InstallerStage.InstallCompleted,
                        is InstallerStage.InstallFailed,
                        is InstallerStage.InstallSuccess,
                        is InstallerStage.UninstallReady,
                        is InstallerStage.UninstallSuccess,
                        is InstallerStage.UninstallFailed,
                        is InstallerStage.UnarchiveReady,
                        is InstallerStage.UnarchiveError,
                        is InstallerStage.UnarchiveFailed,
                        is InstallerStage.AnalyseFailed,
                        is InstallerStage.ResolveFailed,
                        -> {
                            CouiBackButton(
                                icon = AppCOUIIcons.Close,
                                iconTint = COUITheme.colorScheme.onSurface,
                                onClick = {
                                    if (uiState.isDismissible) {
                                        dismissSheet {
                                            viewModel.dispatch(InstallerViewAction.Close)
                                        }
                                    }
                                },
                            )
                        }

                        is InstallerStage.InstallPrepare -> {
                            CouiBackButton(
                                icon = if (showSettings || showPermissions || preparedFromTypeChoice) AppCOUIIcons.Back else AppCOUIIcons.Close,
                                iconTint = COUITheme.colorScheme.onSurface,
                                onClick = {
                                    if (showSettings) {
                                        viewModel.dispatch(InstallerViewAction.HideCouiSheetRightActionSettings)
                                    } else if (showPermissions) {
                                        viewModel.dispatch(InstallerViewAction.HideCouiPermissionList)
                                    } else if (preparedFromTypeChoice) {
                                        backToTypeChoice()
                                    } else {
                                        dismissSheet {
                                            viewModel.dispatch(InstallerViewAction.Close)
                                        }
                                    }
                                },
                            )
                        }

                        is InstallerStage.InstallExtendedMenu -> {
                            CouiBackButton(
                                icon = AppCOUIIcons.Back,
                                iconTint = COUITheme.colorScheme.onSurface,
                                onClick = { viewModel.dispatch(InstallerViewAction.InstallPrepare) },
                            )
                        }

                        else -> {}
                    }
                },
                endAction = {
                    when (stage) {
                        is InstallerStage.InstallPrepare -> {
                            if (!showSettings && !showPermissions) {
                                IconButton(onClick = {
                                    viewModel.dispatch(InstallerViewAction.ShowCouiSheetRightActionSettings)
                                }) {
                                    Icon(
                                        imageVector = AppCOUIIcons.Settings,
                                        tint = COUITheme.colorScheme.onSurface,
                                        contentDescription = stringResource(R.string.installer_settings),
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(48.dp))
                            }
                        }

                        is InstallerStage.InstallSuccess -> {
                            IconButton(onClick = {
                                if (packageName.isNotEmpty()) {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.fromParts("package", packageName, null))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                                viewModel.dispatch(InstallerViewAction.Close)
                            }) {
                                Icon(
                                    imageVector = AppCOUIIcons.Info,
                                    tint = COUITheme.colorScheme.onSurface,
                                    contentDescription = stringResource(R.string.installer_settings),
                                )
                            }
                        }

                        is InstallerStage.InstallingModule -> {
                            // Only show the reboot menu when the installation is finished
                            if (stage.isFinished) {
                                RebootListPopup(
                                    rootMode = uiState.rootMode,
                                    onReboot = { reason ->
                                        viewModel.dispatch(InstallerViewAction.Reboot(reason))
                                    },
                                )
                            }
                        }

                        else -> {}
                    }
                },
                title = sheetTitle, // DYNAMIC TITLE
                insideMargin = DpSize(16.dp, 16.dp),
                allowDismiss = uiState.isDismissible,
                onDismissFinished = {
                    val action = pendingDismissAction.value
                    pendingDismissAction.value = null
                    action?.invoke()
                },
                onDismissRequest = {
                    if (uiState.isDismissible) {
                        // If we are in the confirmation stage and the user taps outside or swipes back
                        if (stage is InstallerStage.InstallConfirm) {
                            dismissSheet {
                                viewModel.dispatch(InstallerViewAction.ApproveSession(stage.sessionId, false))
                            }
                            return@WindowBottomSheet
                        }

                        dismissSheet {
                            val disableNotif = settings.disableNotificationOnDismiss
                            val isModule = stage is InstallerStage.InstallingModule
                            val isUninstall =
                                stage is InstallerStage.UninstallReady || stage is InstallerStage.UninstallResolveFailed ||
                                    stage is InstallerStage.UninstallSuccess || stage is InstallerStage.UninstallFailed
                            val isUnarchive =
                                stage is InstallerStage.UnarchiveReady || stage is InstallerStage.UnarchiveError ||
                                    stage is InstallerStage.UnarchiveFailed

                            // 1. If it's a module install, OR uninstall OR the "disable notification" setting is on -> Close
                            // 2. Otherwise (including Preparing, Standard APK install) -> Background
                            val action = if (isModule || disableNotif || isUninstall || isUnarchive) {
                                InstallerViewAction.Close
                            } else {
                                InstallerViewAction.Background
                            }

                            viewModel.dispatch(action)
                        }
                    }
                },

            ) {
                val radius = if (showBottomSheet.value) 30 else 0
                AnimatedContent(
                    targetState = radius,
                    label = "BlurTransition",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 350)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 350))
                    },
                ) { targetState ->
                    WindowBlurEffect(useBlur = settings.useBlur, blurRadius = targetState)
                }

                // Using stage.javaClass instead of viewModel.state::class for stability
                AnimatedContent(
                    targetState = stage.javaClass,
                    label = "CouiSheetContentAnimation",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 150))
                    },
                ) { _ ->
                    when (stage) {
                        is InstallerStage.InstallConfirm -> {
                            InstallConfirmContent(
                                viewModel = viewModel,
                                onCancel = {
                                    if (stage.isSelfSession) {
                                        // INTERNAL INSTALL: Do not close the bottom sheet!
                                        // Dispatch the rejection, backend will wait for the abort exception
                                        // and then switch state to InstallFailed with the correct error message.
                                        viewModel.dispatch(
                                            InstallerViewAction.ApproveSession(stage.sessionId, false),
                                        )
                                    } else {
                                        dismissSheet {
                                            viewModel.dispatch(
                                                InstallerViewAction.ApproveSession(stage.sessionId, false),
                                            )
                                        }
                                    }
                                },
                                onConfirm = {
                                    if (stage.isSelfSession) {
                                        // INTERNAL INSTALL: Do not close the bottom sheet!
                                        viewModel.dispatch(
                                            InstallerViewAction.ApproveSession(stage.sessionId, true),
                                        )
                                    } else {
                                        dismissSheet {
                                            viewModel.dispatch(
                                                InstallerViewAction.ApproveSession(stage.sessionId, true),
                                            )
                                        }
                                    }
                                },
                            )
                        }

                        is InstallerStage.InstallChoice -> {
                            InstallChoiceContent(
                                viewModel = viewModel,
                                selectionMode = selectionMode,
                                onSelectionModeChange = { selectionMode = it },
                                onSelectMixedModuleType = { installAsModule ->
                                    preparedFromTypeChoice = true
                                    returnedToTypeChoice = false
                                    viewModel.dispatch(InstallerViewAction.SelectMixedModuleType(installAsModule))
                                },
                                onBackToTypeChoice = backToTypeChoice,
                                onCancel = closeSheet,
                            )
                        }

                        is InstallerStage.InstallExtendedMenu -> {
                            InstallExtendedMenuContent(
                                viewModel = viewModel,
                            )
                        }

                        is InstallerStage.Preparing -> {
                            InstallPreparingContent(
                                viewModel = viewModel,
                                onBackground = {
                                    dismissSheet {
                                        viewModel.dispatch(InstallerViewAction.Background)
                                    }
                                },
                            )
                        }

                        is InstallerStage.InstallWaitingUnknownSource -> {
                            val sourceAppLabel = uiState.unknownSourcePermissionAppLabel
                                ?: uiState.initiatorAppLabel
                                ?: stringResource(R.string.installer_label_unknown)
                            val showPermissionAction = rememberUnknownSourcePermissionActionVisible(
                                isWaitingUnknownSource = true,
                            )
                            InstallPreparingContent(
                                viewModel = viewModel,
                                onBackground = {
                                    viewModel.dispatch(InstallerViewAction.RequestUnknownSourcePermission)
                                },
                                descriptionText = stringResource(R.string.installer_waiting_unknown_source_desc, sourceAppLabel),
                                buttonTextRes = R.string.suggestion_allow_unknown_source,
                                showButton = showPermissionAction,
                            )
                        }

                        is InstallerStage.InstallPrepare -> {
                            val prepareSubState = when {
                                showSettings -> "settings"
                                showPermissions -> "permissions"
                                else -> "prepare"
                            }

                            AnimatedContent(
                                targetState = prepareSubState,
                                label = "PrepareContentVsSettingsVsPermissions",
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                                        fadeOut(animationSpec = tween(durationMillis = 150))
                                },
                            ) { subState ->
                                when (subState) {
                                    "settings" -> {
                                        PrepareSettingsContent(
                                            viewModel = viewModel,
                                        )
                                    }

                                    "permissions" -> {
                                        InstallPreparePermissionContent(
                                            viewModel = viewModel,
                                            onBack = { viewModel.dispatch(InstallerViewAction.HideCouiPermissionList) },
                                        )
                                    }

                                    else -> { // "prepare"
                                        InstallPrepareContent(
                                            viewModel = viewModel,
                                            appInfo = appInfoState,
                                            onCancel = closeSheet,
                                            onInstall = {
                                                viewModel.dispatch(InstallerViewAction.Install(true))
                                                if (settings.autoSilentInstall && !viewModel.isInstallingModule) {
                                                    dismissSheet {
                                                        viewModel.dispatch(InstallerViewAction.Background)
                                                    }
                                                }
                                            },
                                            onLongInstall = {
                                                if (!settings.longClickBackgroundInstall) return@InstallPrepareContent
                                                // Trigger install directly
                                                viewModel.dispatch(InstallerViewAction.Install(true))
                                                // Force background auto silent install regardless of settings
                                                if (!viewModel.isInstallingModule) {
                                                    dismissSheet {
                                                        viewModel.dispatch(InstallerViewAction.Background)
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        is InstallerStage.Installing -> {
                            InstallingContent(
                                stage = stage,
                                appInfo = appInfoState,
                                onButtonClick = {
                                    dismissSheet {
                                        viewModel.dispatch(InstallerViewAction.Background)
                                    }
                                },
                            )
                        }

                        is InstallerStage.InstallSuccess -> {
                            InstallSuccessContent(
                                appInfo = appInfoState,
                                viewModel = viewModel,
                                closeSessionCountDown = settings.closeSessionCountDown,
                                onClose = closeSheet,
                            )
                        }

                        is InstallerStage.InstallCompleted -> {
                            InstallCompletedContent(
                                results = stage.results,
                                onClose = closeSheet,
                            )
                        }

                        is InstallerStage.InstallFailed -> {
                            if (error is ModuleInstallException) {
                                NonInstallFailedContent(
                                    error = error,
                                    onClose = closeSheet,
                                )
                            } else {
                                InstallFailedContent(
                                    appInfo = appInfoState,
                                    viewModel = viewModel,
                                    onClose = closeSheet,
                                )
                            }
                        }

                        is InstallerStage.InstallingModule -> {
                            InstallModuleContent(
                                outputLines = stage.output,
                                isFinished = stage.isFinished,
                                onClose = closeSheet,
                            )
                        }

                        is InstallerStage.UninstallReady -> {
                            UninstallPrepareContent(
                                viewModel = viewModel,
                                onCancel = closeSheet,
                                onUninstall = { viewModel.dispatch(InstallerViewAction.Uninstall) },
                            )
                        }

                        is InstallerStage.Uninstalling -> {
                            UninstallingContent(viewModel = viewModel)
                        }

                        is InstallerStage.UninstallSuccess -> {
                            UninstallSuccessContent(viewModel = viewModel, onClose = closeSheet)
                        }

                        is InstallerStage.UninstallFailed -> {
                            UninstallFailedContent(
                                viewModel = viewModel,
                                onClose = closeSheet,
                            )
                        }

                        is InstallerStage.UnarchiveReady -> {
                            UnarchivePrepareContent(
                                appLabel = stage.appLabel,
                                installerLabel = stage.installerLabel,
                                onCancel = closeSheet,
                                onRestore = { viewModel.dispatch(InstallerViewAction.StartUnarchive) },
                            )
                        }

                        is InstallerStage.Unarchiving -> {
                            LoadingContent(statusText = stringResource(R.string.unarchive_restoring))
                        }

                        is InstallerStage.UnarchiveError -> {
                            UnarchiveErrorContent(
                                status = stage.status,
                                requiredBytes = stage.requiredBytes,
                                installerLabel = stage.installerLabel,
                                onClose = closeSheet,
                                onPrimaryAction = { viewModel.dispatch(InstallerViewAction.OpenUnarchiveErrorAction) },
                            )
                        }

                        is InstallerStage.UnarchiveFailed -> {
                            NonInstallFailedContent(
                                error = error,
                                onClose = closeSheet,
                            )
                        }

                        is InstallerStage.AnalyseFailed, is InstallerStage.ResolveFailed -> {
                            NonInstallFailedContent(
                                error = error,
                                onClose = closeSheet,
                            )
                        }

                        is InstallerStage.Resolving, is InstallerStage.Analysing -> {
                            LoadingContent(
                                statusText = if (stage is InstallerStage.Resolving) {
                                    stringResource(R.string.installer_resolving)
                                } else {
                                    stringResource(R.string.installer_analysing)
                                },
                            )
                        }

                        else -> {
                            LoadingContent(statusText = stringResource(R.string.loading))
                        }
                    }
                }
            }
        }
    }
}

private fun isFullScreenStage(stage: InstallerStage): Boolean = when (stage) {
    is InstallerStage.InstallPrepare,
    is InstallerStage.Installing,
    is InstallerStage.InstallCompleted,
    is InstallerStage.InstallSuccess,
    is InstallerStage.InstallFailed,
    -> true

    else -> false
}

/**
 * Full-screen install flow.
 *
 * Mirrors the sheet's state machine for the migrated stages but keeps its own
 * information hierarchy: the app identity stays pinned, the body carries the
 * stage, and the container owns the action row so the status marks line up.
 * [sheetcontent] is untouched by design, so the sheet presentation keeps
 * behaving exactly as before.
 */
@Composable
private fun CouiFullScreenInstaller(
    stage: InstallerStage,
    title: String,
    appInfo: AppInfoState,
    entity: AppEntity.BaseEntity?,
    versionName: String?,
    sourceLabel: String?,
    permissions: List<String>,
    showPermissions: Boolean,
    showSettings: Boolean,
    viewModel: InstallerViewModel,
    onClose: () -> Unit,
) {
    val subPage = showPermissions || showSettings

    val headerContent: (@Composable () -> Unit)? = if (subPage) {
        null
    } else {
        { CouiFullScreenAppHeader(appInfo = appInfo, versionName = versionName) }
    }
    val bottomBarContent: (@Composable () -> Unit)? = if (subPage) {
        null
    } else {
        { CouiFullScreenActionRow(stage = stage, viewModel = viewModel, onClose = onClose) }
    }

    val pageTitle = when {
        showPermissions -> stringResource(R.string.permission_list)
        showSettings -> stringResource(R.string.installer_settings)
        else -> title
    }

    // Keyed by page rather than by stage instance so a progress tick does not
    // re-run the transition, while any real page change still cross-fades.
    val pageKey: Any = when {
        stage is InstallerStage.InstallPrepare && showSettings -> "settings"
        stage is InstallerStage.InstallPrepare && showPermissions -> "permissions"
        else -> stage.javaClass
    }

    CouiPositionFullScreen(
        title = pageTitle,
        onBack = {
            when {
                showPermissions -> viewModel.dispatch(InstallerViewAction.HideCouiPermissionList)
                showSettings -> viewModel.dispatch(InstallerViewAction.HideCouiSheetRightActionSettings)
                else -> onClose()
            }
        },
        onMore = if (!subPage && stage is InstallerStage.InstallPrepare) {
            { viewModel.dispatch(InstallerViewAction.ShowCouiSheetRightActionSettings) }
        } else {
            null
        },
        header = headerContent,
        bottomBar = bottomBarContent,
    ) {
        AnimatedContent(
            targetState = pageKey,
            modifier = Modifier.fillMaxSize(),
            label = "CouiFullScreenPage",
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 180)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 120))
            },
        ) { _ ->
            when {
                stage is InstallerStage.InstallPrepare && showSettings -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                    ) {
                        PrepareSettingsContent(viewModel = viewModel)
                    }
                }

                stage is InstallerStage.InstallPrepare && showPermissions -> {
                    CouiFullScreenPermissionContent(permissions = permissions)
                }

                stage is InstallerStage.InstallPrepare -> {
                    CouiFullScreenPrepareContent(
                        entity = entity,
                        sourceLabel = sourceLabel,
                        permissionCount = permissions.size,
                        sensitivePermissionCount = permissions.sensitiveCount(),
                        onOpenPermissions = { viewModel.dispatch(InstallerViewAction.ShowCouiPermissionList) },
                    )
                }

                stage is InstallerStage.Installing -> {
                    CouiFullScreenStatusContent(
                        title = stringResource(R.string.installer_installing),
                        subtitle = "${(stage.progress.coerceIn(0f, 1f) * 100).toInt()}%",
                        progress = stage.progress,
                    )
                }

                stage is InstallerStage.InstallCompleted || stage is InstallerStage.InstallSuccess -> {
                    CouiFullScreenStatusContent(
                        title = stringResource(R.string.installer_install_success),
                        icon = AppCOUIIcons.Ok,
                        iconTint = FullScreenSuccessColor,
                        haloColor = FullScreenSuccessHaloColor,
                    )
                }

                stage is InstallerStage.InstallFailed -> {
                    CouiFullScreenStatusContent(
                        title = stringResource(R.string.installer_install_failed),
                        icon = AppCOUIIcons.Close,
                        iconTint = COUITheme.colorScheme.error,
                        haloColor = COUITheme.colorScheme.errorContainer,
                    )
                }
            }
        }
    }
}

private val FullScreenSuccessColor = Color(0xFF16A34A)
private val FullScreenSuccessHaloColor = Color(0x1F16A34A)

@Composable
private fun CouiFullScreenActionRow(
    stage: InstallerStage,
    viewModel: InstallerViewModel,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (stage) {
            is InstallerStage.InstallPrepare -> {
                FullScreenAction(
                    text = stringResource(R.string.cancel),
                    filled = false,
                    weight = 1f,
                    onClick = onClose,
                )
                FullScreenAction(
                    text = stringResource(R.string.install),
                    filled = true,
                    weight = 1.6f,
                    onClick = { viewModel.dispatch(InstallerViewAction.Install(true)) },
                )
            }

            is InstallerStage.Installing -> {
                FullScreenAction(
                    text = stringResource(R.string.cancel),
                    filled = false,
                    weight = 1f,
                    onClick = { viewModel.dispatch(InstallerViewAction.Cancel) },
                )
            }

            else -> {
                FullScreenAction(
                    text = stringResource(R.string.finish),
                    filled = true,
                    weight = 1f,
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun RowScope.FullScreenAction(
    text: String,
    filled: Boolean,
    weight: Float,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (filled) {
                    COUITheme.colorScheme.primary
                } else {
                    COUITheme.colorScheme.secondaryContainer
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = COUITheme.textStyles.button,
            color = if (filled) {
                COUITheme.colorScheme.onPrimary
            } else {
                COUITheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun RebootListPopup(
    modifier: Modifier = Modifier,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.TopEnd,
    rootMode: RootMode,
    onReboot: (String) -> Unit,
) {
    val showTopPopup = remember { mutableStateOf(false) }

    IconButton(
        modifier = modifier,
        onClick = { showTopPopup.value = true },
        holdDownState = showTopPopup.value,
    ) {
        Icon(
            imageVector = AppCOUIIcons.Refresh,
            contentDescription = stringResource(id = R.string.reboot),
            tint = COUITheme.colorScheme.onBackground,
        )
    }

    WindowListPopup(
        show = showTopPopup.value,
        popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
        alignment = alignment,
        onDismissRequest = { showTopPopup.value = false },
    ) {
        val context = LocalContext.current
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?

        @Suppress("DEPRECATION")
        val isRebootingUserspaceSupported =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true

        ListPopupColumn {
            val rebootOptions = remember {
                val options = mutableListOf(
                    Pair(R.string.reboot, ""),
                    Pair(R.string.reboot_recovery, "recovery"),
                    Pair(R.string.reboot_bootloader, "bootloader"),
                    Pair(R.string.reboot_download, "download"),
                    Pair(R.string.reboot_edl, "edl"),
                )
                if (isRebootingUserspaceSupported) {
                    options.add(1, Pair(R.string.reboot_userspace, "userspace"))
                }
                if (rootMode == RootMode.KernelSU) {
                    options.add(1, Pair(R.string.reboot_soft_reboot, "ksud_soft_reboot"))
                }
                options
            }

            rebootOptions.forEachIndexed { idx, (id, reason) ->
                RebootDropdownItem(
                    id = id,
                    reason = reason,
                    showTopPopup = showTopPopup,
                    optionSize = rebootOptions.size,
                    index = idx,
                    onReboot = onReboot,
                )
            }
        }
    }
}

@Composable
private fun RebootDropdownItem(
    @StringRes id: Int,
    reason: String = "",
    showTopPopup: MutableState<Boolean>,
    optionSize: Int,
    index: Int,
    onReboot: (String) -> Unit,
) {
    DropdownItem(
        text = stringResource(id),
        optionSize = optionSize,
        index = index,
        onSelectedIndexChange = {
            onReboot(reason)
            showTopPopup.value = false
        },
    )
}
