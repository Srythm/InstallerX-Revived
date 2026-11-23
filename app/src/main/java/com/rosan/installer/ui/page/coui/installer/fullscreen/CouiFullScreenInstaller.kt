// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.domain.archive.model.UnarchiveErrorAction
import com.rosan.installer.domain.archive.model.UnarchiveStatus
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.ModuleInstallException
import com.rosan.installer.domain.engine.model.install.InstallPhase
import com.rosan.installer.domain.engine.model.install.MmzSelectionMode
import com.rosan.installer.domain.engine.model.install.UninstallFlags
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.engine.model.packageinfo.InstalledModuleInfo
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase.Companion.PRIVILEGED_START_TIMEOUT_MS
import com.rosan.installer.domain.privileged.usecase.OpenLSPosedUseCase
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.domain.session.model.InstallResult
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.UninstallInfo
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.isPrivileged
import com.rosan.installer.ui.icons.AppCOUIIcons
import com.rosan.installer.ui.page.coui.installer.RebootListPopup
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import com.rosan.installer.ui.page.coui.installer.components.rememberCouiPreparedPackageState
import com.rosan.installer.ui.page.coui.installer.sheetcontent.CouiErrorSuggestions
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallChoiceContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.InstallExtendedMenuContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.PrepareSettingsContent
import com.rosan.installer.ui.page.coui.installer.sheetcontent.unarchiveErrorTitle
import com.rosan.installer.ui.page.coui.widgets.CouiCheckboxWidget
import com.rosan.installer.ui.page.coui.widgets.CouiErrorTextBlock
import com.rosan.installer.ui.page.coui.widgets.CouiInstallerTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsCard
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.rememberUnknownSourcePermissionActionVisible
import com.rosan.installer.ui.page.main.widget.chip.NoticeModel
import com.rosan.installer.ui.theme.InstallerTheme.useCouiMonet
import com.rosan.installer.ui.theme.colorOsSpring
import com.rosan.installer.ui.util.KeyEventBlocker
import com.rosan.installer.util.help
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.theme.COUITheme
import java.text.NumberFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

internal class FullScreenSlotRef<T>(var value: T)

@Composable
private fun TransitionSlotGuard(
    isActive: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isActive) {
                    Modifier
                        .clearAndSetSemantics { }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                }
                            }
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

internal enum class FullScreenSubPage { Main, Permissions, Settings, ExtendedMenu }

internal data class FullScreenPage(
    val stage: InstallerStage,
    val title: String,
    val subPage: FullScreenSubPage = FullScreenSubPage.Main,
    val progress: Float? = null,
    val error: Throwable? = null,
) {
    // Never key by progress, log contents or the module's finished flag.
    val key: Any get() = if (subPage == FullScreenSubPage.Main) stage.javaClass else subPage
}

private data class FullScreenLiveSnapshot(
    val page: FullScreenPage,
    val appInfo: AppInfoState,
    val entity: AppEntity?,
    val versionLine: String?,
    val sourceLabel: String?,
    val permissions: List<String>,
    val notices: List<NoticeModel>,
    val installedAppInfo: InstalledAppInfo?,
    val installedModuleInfo: InstalledModuleInfo?,
    val totalSelectedSize: Long,
    val selectedEntityCount: Int,
    val isSplitUpdateMode: Boolean,
    val isInvalidSplitInstall: Boolean,
    val canInstall: Boolean,
    @param:StringRes val primaryButtonRes: Int,
    val canOpenInstallOptions: Boolean,
    val canOpenSplitChoice: Boolean,
    val uninstallInfo: UninstallInfo?,
    val uninstallIcon: ImageBitmap?,
)

private data class StageBodySnapshot(
    val page: FullScreenPage,
    val appInfo: AppInfoState,
    val entity: AppEntity?,
    val sourceLabel: String?,
    val permissions: List<String>,
    val sensitivePermissionCount: Int,
    val notices: List<NoticeModel>,
    val installedAppInfo: InstalledAppInfo?,
    val installedModuleInfo: InstalledModuleInfo?,
    val totalSelectedSize: Long,
    val selectedEntityCount: Int,
    val displaySdk: Boolean,
    val displaySize: Boolean,
    val hideIdenticalComparisons: Boolean,
    val showOppoSpecial: Boolean,
    val labShowFilePath: Boolean,
    val labShowInstallInitiator: Boolean,
    val isSplitUpdateMode: Boolean,
    val isInvalidSplitInstall: Boolean,
    val canOpenInstallOptions: Boolean,
    val canOpenSplitChoice: Boolean,
    val showOpenLsposed: Boolean,
    val unknownSourceAppLabel: String,
    val uninstallInfo: UninstallInfo?,
    val uninstallFlags: Int,
)

// InstallerStage folds unknown progress into zero; the session retains the nullable byte progress.
@Composable
fun CouiFullScreenInstaller(
    stage: InstallerStage,
    title: String,
    appInfo: AppInfoState,
    selectionMode: MmzSelectionMode,
    onSelectionModeChange: (MmzSelectionMode) -> Unit,
    onSelectMixedModuleType: (Boolean) -> Unit,
    onBackToTypeChoice: () -> Unit,
    isApkChoice: Boolean,
    preparedFromTypeChoice: Boolean,
    returnedToTypeChoice: Boolean,
    isClosing: Boolean,
    onCloseAnimationFinished: () -> Unit = {},
    viewModel: InstallerViewModel,
    session: InstallerSessionRepository,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val capabilityProvider: DeviceCapabilityProvider = koinInject()
    val openAppUseCase: OpenAppUseCase = koinInject()
    val openLSPosedUseCase: OpenLSPosedUseCase = koinInject()

    val sessionProgress by session.progress.collectAsStateWithLifecycle(initialValue = ProgressEntity.Ready)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.viewSettings
    val config = uiState.config
    val showSettings = uiState.showCouiSheetRightActionSettings
    val showPermissions = uiState.showCouiPermissionList

    val subPage = when {
        stage is InstallerStage.InstallExtendedMenu -> FullScreenSubPage.ExtendedMenu
        stage is InstallerStage.InstallPrepare && showSettings -> FullScreenSubPage.Settings
        stage is InstallerStage.InstallPrepare && showPermissions -> FullScreenSubPage.Permissions
        else -> FullScreenSubPage.Main
    }
    val rawInstall = (sessionProgress as? ProgressEntity.Installing)?.takeIf {
        stage is InstallerStage.Installing && it.current == stage.current &&
            it.total == stage.total && it.phase == stage.phase
    }
    val progress = when (stage) {
        is InstallerStage.Preparing -> fullScreenProgress(stage.progress)

        is InstallerStage.Installing -> if (stage.phase == InstallPhase.WRITING) {
            fullScreenProgress(rawInstall?.overallProgress() ?: stage.progress)
        } else {
            null
        }

        else -> null
    }
    val currentPage = FullScreenPage(stage, title, subPage, progress, uiState.error)

    val prepared = if (stage is InstallerStage.InstallPrepare) rememberCouiPreparedPackageState(uiState) else null
    val primaryEntity = prepared?.primaryEntity ?: appInfo.primaryEntity
    val versionLine = when (primaryEntity) {
        is AppEntity.BaseEntity -> "${primaryEntity.versionName} (${primaryEntity.versionCode})"
        is AppEntity.ModuleEntity -> "${primaryEntity.version} (${primaryEntity.versionCode})"
        is AppEntity.SplitEntity -> primaryEntity.splitName
        else -> null
    }
    val permissions = (prepared?.rawBaseEntity ?: primaryEntity as? AppEntity.BaseEntity)?.permissions.orEmpty()
    val canOpenInstallOptions = prepared?.rawBaseEntity != null &&
        config.authorizer != Authorizer.Dhizuku &&
        config.authorizer != Authorizer.None
    val canOpenSplitChoice = prepared?.rawBaseEntity != null &&
        prepared.currentPackage.appEntities.size > 1

    val liveSnapshot = FullScreenLiveSnapshot(
        page = currentPage,
        appInfo = appInfo,
        entity = primaryEntity,
        versionLine = versionLine,
        sourceLabel = uiState.initiatorAppLabel,
        permissions = permissions,
        notices = prepared?.installStateResult?.notices.orEmpty(),
        installedAppInfo = prepared?.currentPackage?.installedAppInfo,
        installedModuleInfo = prepared?.currentPackage?.installedModuleInfo,
        totalSelectedSize = prepared?.totalSelectedSize ?: (primaryEntity?.size ?: 0L),
        selectedEntityCount = prepared?.selectedEntities?.size ?: 1,
        isSplitUpdateMode = prepared?.isSplitUpdateMode ?: false,
        isInvalidSplitInstall = prepared?.isInvalidSplitInstall ?: false,
        canInstall = prepared?.canInstall ?: false,
        primaryButtonRes = prepared?.installStateResult?.buttonTextId ?: R.string.install,
        canOpenInstallOptions = canOpenInstallOptions,
        canOpenSplitChoice = canOpenSplitChoice,
        uninstallInfo = uiState.uiUninstallInfo,
        uninstallIcon = uiState.uiUninstallInfo?.let { uiState.displayIcons[it.packageName] },
    )

    // Keep the visible page and metadata through teardown, rather than snapping to Ready/Unknown mid-fade.
    val lastOpenSnapshot = remember { mutableStateOf(liveSnapshot) }
    SideEffect { if (!isClosing) lastOpenSnapshot.value = liveSnapshot }
    val activeSnapshot = if (isClosing) lastOpenSnapshot.value else liveSnapshot

    val activePackageName = uiState.currentPackageName
        ?: uiState.analysisResults.firstOrNull()?.packageName
        ?: activeSnapshot.appInfo.packageName
    val prepareScrollState = remember(session.id, activePackageName) { ScrollState(0) }
    val launchIntent = remember(activePackageName) {
        if (activePackageName.isNotEmpty()) {
            context.packageManager.getLaunchIntentForPackage(activePackageName)
        } else {
            null
        }
    }
    val hasPrivilege = config.isPrivileged(capabilityProvider)
    val isXposedModule = settings.detectXposedModule &&
        (activeSnapshot.appInfo.primaryEntity as? AppEntity.BaseEntity)?.isXposedModule == true

    val showUnknownSourceAction = rememberUnknownSourcePermissionActionVisible(
        isWaitingUnknownSource = activeSnapshot.page.stage is InstallerStage.InstallWaitingUnknownSource,
    )
    val unknownSourceAppLabel = uiState.unknownSourcePermissionAppLabel
        ?: uiState.initiatorAppLabel
        ?: stringResource(R.string.installer_label_unknown)

    val safeClose: () -> Unit = { if (!viewModel.isClosingFullscreen.value) onClose() }
    val safeDispatch: (InstallerViewAction) -> Unit = {
        if (!viewModel.isClosingFullscreen.value) viewModel.dispatch(it)
    }

    val topEndActionKey: Any? = when {
        isClosing || activeSnapshot.page.subPage != FullScreenSubPage.Main -> null

        activeSnapshot.page.stage is InstallerStage.InstallPrepare -> "prepare_settings"

        activeSnapshot.page.stage is InstallerStage.InstallSuccess && activePackageName.isNotEmpty() ->
            "install_success_info"

        activeSnapshot.page.stage is InstallerStage.InstallingModule &&
            activeSnapshot.page.stage.isFinished -> "module_reboot"

        else -> null
    }

    val topEndAction: (@Composable () -> Unit)? = when {
        isClosing || activeSnapshot.page.subPage != FullScreenSubPage.Main -> null

        activeSnapshot.page.stage is InstallerStage.InstallPrepare -> {
            {
                IconButton(onClick = { safeDispatch(InstallerViewAction.ShowCouiSheetRightActionSettings) }) {
                    Icon(
                        imageVector = AppCOUIIcons.More,
                        contentDescription = stringResource(R.string.installer_settings),
                        tint = COUITheme.colorScheme.labelPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        activeSnapshot.page.stage is InstallerStage.InstallSuccess && activePackageName.isNotEmpty() -> {
            {
                IconButton(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", activePackageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                    safeClose()
                }) {
                    Icon(
                        imageVector = AppCOUIIcons.Info,
                        contentDescription = stringResource(R.string.installer_settings),
                        tint = COUITheme.colorScheme.labelPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        activeSnapshot.page.stage is InstallerStage.InstallingModule &&
            activeSnapshot.page.stage.isFinished -> {
            {
                RebootListPopup(
                    rootMode = uiState.rootMode,
                    onReboot = { reason -> safeDispatch(InstallerViewAction.Reboot(reason)) },
                )
            }
        }

        else -> null
    }

    CouiFullScreenInstallerContent(
        page = activeSnapshot.page,
        appInfo = activeSnapshot.appInfo,
        prepareScrollState = prepareScrollState,
        useBlur = settings.useBlur,
        isModule = viewModel.isInstallingModule,
        entity = activeSnapshot.entity,
        versionLine = activeSnapshot.versionLine,
        sourceLabel = activeSnapshot.sourceLabel,
        permissions = activeSnapshot.permissions,
        notices = activeSnapshot.notices,
        installedAppInfo = activeSnapshot.installedAppInfo,
        installedModuleInfo = activeSnapshot.installedModuleInfo,
        totalSelectedSize = activeSnapshot.totalSelectedSize,
        selectedEntityCount = activeSnapshot.selectedEntityCount,
        displaySdk = config.displaySdk,
        displaySize = config.displaySize,
        hideIdenticalComparisons = settings.hideIdenticalComparisons,
        showOppoSpecial = settings.showOPPOSpecial,
        labShowFilePath = settings.labShowFilePath,
        labShowInstallInitiator = settings.labShowInstallInitiator,
        isSplitUpdateMode = activeSnapshot.isSplitUpdateMode,
        isInvalidSplitInstall = activeSnapshot.isInvalidSplitInstall,
        canInstall = activeSnapshot.canInstall,
        primaryButtonRes = activeSnapshot.primaryButtonRes,
        hasLaunchIntent = launchIntent != null,
        showOpenLsposed = isXposedModule && settings.quickOpenLSPosed && hasPrivilege,
        showUnknownSourceAction = showUnknownSourceAction,
        unknownSourceAppLabel = unknownSourceAppLabel,
        uninstallInfo = activeSnapshot.uninstallInfo,
        uninstallIcon = activeSnapshot.uninstallIcon,
        uninstallFlags = config.uninstallFlags,
        isConfirmationSubmitting = uiState.isConfirmationSubmitting,
        isApkChoice = isApkChoice,
        navigatedFromPrepareToChoice = uiState.navigatedFromPrepareToChoice,
        preparedFromTypeChoice = preparedFromTypeChoice,
        returnedToTypeChoice = returnedToTypeChoice,
        canOpenInstallOptions = activeSnapshot.canOpenInstallOptions,
        canOpenSplitChoice = activeSnapshot.canOpenSplitChoice,
        isClosing = isClosing,
        onCloseAnimationFinished = onCloseAnimationFinished,
        onClose = safeClose,
        onCancel = { safeDispatch(InstallerViewAction.Cancel) },
        onBackground = { safeDispatch(InstallerViewAction.Background) },
        onBackToTypeChoice = { if (!viewModel.isClosingFullscreen.value) onBackToTypeChoice() },
        onInstall = {
            if (!viewModel.isClosingFullscreen.value) {
                viewModel.dispatch(InstallerViewAction.Install(true))
                if (settings.autoSilentInstall && !viewModel.isInstallingModule) {
                    viewModel.dispatch(InstallerViewAction.Background)
                }
            }
        },
        onOpenApp = {
            if (launchIntent != null && !viewModel.isClosingFullscreen.value) {
                viewModel.dispatch(InstallerViewAction.PrepareClose)
                coroutineScope.launch(Dispatchers.IO) {
                    val result = openAppUseCase(config = config, launchIntent = launchIntent)
                    when (result) {
                        is OpenAppUseCase.Result.SuccessPrivileged -> {
                            withContext(Dispatchers.Main) {
                                viewModel.dispatch(InstallerViewAction.Close)
                            }
                        }

                        is OpenAppUseCase.Result.FallbackRequired -> {
                            withContext(Dispatchers.Main) {
                                context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                delay(if (!hasPrivilege) settings.closeSessionCountDown * 1000L else PRIVILEGED_START_TIMEOUT_MS)
                                viewModel.dispatch(InstallerViewAction.Close)
                            }
                        }
                    }
                }
            }
        },
        onOpenLsposed = {
            if (!viewModel.isClosingFullscreen.value) {
                viewModel.dispatch(InstallerViewAction.PrepareClose)
                coroutineScope.launch(Dispatchers.IO) {
                    val success = openLSPosedUseCase(config)
                    if (success) {
                        withContext(Dispatchers.Main) {
                            viewModel.dispatch(InstallerViewAction.Close)
                        }
                    }
                }
            }
        },
        onOpenInstallOptions = { safeDispatch(InstallerViewAction.InstallExtendedMenu) },
        onOpenSplitChoice = { safeDispatch(InstallerViewAction.InstallChoice) },
        onRequestUnknownSourcePermission = { safeDispatch(InstallerViewAction.RequestUnknownSourcePermission) },
        onApproveSession = { id, granted -> safeDispatch(InstallerViewAction.ApproveSession(id, granted)) },
        onUninstall = { safeDispatch(InstallerViewAction.Uninstall) },
        onToggleUninstallFlag = { flag, enable -> safeDispatch(InstallerViewAction.ToggleUninstallFlag(flag, enable)) },
        onStartUnarchive = { safeDispatch(InstallerViewAction.StartUnarchive) },
        onOpenUnarchiveErrorAction = { safeDispatch(InstallerViewAction.OpenUnarchiveErrorAction) },
        onIconClick = if (settings.labTapIconToShare && activeSnapshot.entity != null) {
            { safeDispatch(InstallerViewAction.ShareApp(activeSnapshot.entity)) }
        } else {
            null
        },
        onAction = safeDispatch,
        topEndAction = topEndAction,
        topEndActionKey = topEndActionKey,
        choiceContent = {
            InstallChoiceContent(
                viewModel = viewModel,
                selectionMode = selectionMode,
                onSelectionModeChange = onSelectionModeChange,
                onSelectMixedModuleType = onSelectMixedModuleType,
                onBackToTypeChoice = onBackToTypeChoice,
                onCancel = safeClose,
                contentTopPadding = LocalFullScreenTopBleed.current,
            )
        },
        extendedMenuContent = {
            InstallExtendedMenuContent(
                viewModel = viewModel,
                backHandlerEnabled = false,
                showBackButton = false,
                contentTopPadding = LocalFullScreenTopBleed.current,
            )
        },
        settingsContent = {
            PrepareSettingsContent(
                viewModel = viewModel,
                backHandlerEnabled = false,
                contentTopPadding = LocalFullScreenTopBleed.current,
            )
        },
        errorSuggestionsContent = { err ->
            if (settings.showSmartSuggestion) {
                CouiErrorSuggestions(error = err, viewModel = viewModel)
            }
        },
    )
}

@Composable
internal fun CouiFullScreenInstallerContent(
    page: FullScreenPage,
    appInfo: AppInfoState,
    prepareScrollState: ScrollState = rememberScrollState(),
    useBlur: Boolean = false,
    isModule: Boolean = false,
    entity: AppEntity? = null,
    versionLine: String? = null,
    sourceLabel: String? = null,
    permissions: List<String> = emptyList(),
    notices: List<NoticeModel> = emptyList(),
    installedAppInfo: InstalledAppInfo? = null,
    installedModuleInfo: InstalledModuleInfo? = null,
    totalSelectedSize: Long = entity?.size ?: 0L,
    selectedEntityCount: Int = 1,
    displaySdk: Boolean = true,
    displaySize: Boolean = true,
    hideIdenticalComparisons: Boolean = true,
    showOppoSpecial: Boolean = false,
    labShowFilePath: Boolean = false,
    labShowInstallInitiator: Boolean = true,
    isSplitUpdateMode: Boolean = false,
    isInvalidSplitInstall: Boolean = false,
    canInstall: Boolean = true,
    @StringRes primaryButtonRes: Int = R.string.install,
    hasLaunchIntent: Boolean = false,
    showOpenLsposed: Boolean = false,
    showUnknownSourceAction: Boolean = false,
    unknownSourceAppLabel: String = "",
    uninstallInfo: UninstallInfo? = null,
    uninstallIcon: ImageBitmap? = null,
    uninstallFlags: Int = 0,
    isConfirmationSubmitting: Boolean = false,
    isApkChoice: Boolean = false,
    navigatedFromPrepareToChoice: Boolean = false,
    preparedFromTypeChoice: Boolean = false,
    returnedToTypeChoice: Boolean = false,
    canOpenInstallOptions: Boolean = false,
    canOpenSplitChoice: Boolean = false,
    isClosing: Boolean = false,
    onCloseAnimationFinished: () -> Unit = {},
    onClose: () -> Unit = {},
    onCancel: () -> Unit = onClose,
    onBackground: () -> Unit = {},
    onBackToTypeChoice: () -> Unit = {},
    onInstall: () -> Unit = {},
    onOpenApp: () -> Unit = {},
    onOpenLsposed: () -> Unit = {},
    onOpenInstallOptions: () -> Unit = {},
    onOpenSplitChoice: () -> Unit = {},
    onRequestUnknownSourcePermission: () -> Unit = {},
    onApproveSession: (Int, Boolean) -> Unit = { _, _ -> },
    onUninstall: () -> Unit = {},
    onToggleUninstallFlag: (Int, Boolean) -> Unit = { _, _ -> },
    onStartUnarchive: () -> Unit = {},
    onOpenUnarchiveErrorAction: () -> Unit = {},
    onIconClick: (() -> Unit)? = null,
    onAction: (InstallerViewAction) -> Unit = {},
    topEndAction: (@Composable () -> Unit)? = null,
    topEndActionKey: Any? = topEndAction != null,
    choiceContent: @Composable () -> Unit = {},
    extendedMenuContent: @Composable () -> Unit = {},
    settingsContent: @Composable () -> Unit = {},
    errorSuggestionsContent: @Composable (Throwable) -> Unit = {},
) {
    val stage = page.stage
    val isMain = page.subPage == FullScreenSubPage.Main

    val title = when (page.subPage) {
        FullScreenSubPage.Permissions -> stringResource(R.string.permission_list)

        FullScreenSubPage.Settings -> stringResource(R.string.installer_settings)

        FullScreenSubPage.ExtendedMenu -> stringResource(R.string.config_label_install_options)

        FullScreenSubPage.Main -> when (stage) {
            is InstallerStage.InstallChoice -> page.title

            is InstallerStage.InstallExtendedMenu -> stringResource(R.string.config_label_install_options)

            is InstallerStage.InstallConfirm -> stringResource(
                when (stage.requestType) {
                    ConfirmationRequestType.PRE_APPROVAL -> R.string.installer_install_pre_approval
                    ConfirmationRequestType.PERMISSIONS -> R.string.installer_permissions_confirm
                    ConfirmationRequestType.INSTALL -> R.string.installer_install_confirm
                },
            )

            is InstallerStage.UninstallReady,
            is InstallerStage.Uninstalling,
            is InstallerStage.UninstallSuccess,
            is InstallerStage.UninstallFailed,
            is InstallerStage.UninstallResolveFailed,
            -> stringResource(
                if (uninstallInfo?.isArchived == true) R.string.uninstall_archive else R.string.installer_uninstall_app,
            )

            is InstallerStage.UnarchiveReady,
            is InstallerStage.Unarchiving,
            is InstallerStage.UnarchiveError,
            is InstallerStage.UnarchiveFailed,
            -> stringResource(R.string.unarchive_restore)

            else -> stringResource(
                if (isModule) R.string.installer_install_module else R.string.installer_install_app,
            )
        }
    }

    val backEnabled = when {
        isClosing -> false

        page.subPage != FullScreenSubPage.Main -> true

        stage is InstallerStage.Resolving ||
            stage is InstallerStage.Analysing ||
            stage is InstallerStage.Installing ||
            stage is InstallerStage.InstallRetryDowngradeUsingUninstall ||
            stage is InstallerStage.Uninstalling ||
            stage is InstallerStage.Unarchiving -> false

        stage is InstallerStage.InstallingModule -> stage.isFinished

        stage is InstallerStage.InstallConfirm -> !isConfirmationSubmitting

        else -> true
    }

    val backAction: () -> Unit = when {
        !backEnabled -> ({})

        page.subPage == FullScreenSubPage.Permissions -> ({ onAction(InstallerViewAction.HideCouiPermissionList) })

        page.subPage == FullScreenSubPage.Settings -> ({ onAction(InstallerViewAction.HideCouiSheetRightActionSettings) })

        page.subPage == FullScreenSubPage.ExtendedMenu -> ({ onAction(InstallerViewAction.InstallPrepare) })

        stage is InstallerStage.Preparing ||
            stage is InstallerStage.InstallWaitingUnknownSource -> onCancel

        stage is InstallerStage.InstallChoice -> {
            if (isApkChoice) {
                onBackToTypeChoice
            } else if (navigatedFromPrepareToChoice && !returnedToTypeChoice) {
                ({ onAction(InstallerViewAction.InstallPrepare) })
            } else {
                onClose
            }
        }

        stage is InstallerStage.InstallExtendedMenu -> ({ onAction(InstallerViewAction.InstallPrepare) })

        stage is InstallerStage.InstallPrepare && preparedFromTypeChoice -> onBackToTypeChoice

        stage is InstallerStage.InstallConfirm -> ({ onApproveSession(stage.sessionId, false) })

        else -> onClose
    }

    val usesShellBottomBar = isMain && stage !is InstallerStage.InstallChoice

    val sensitivePermissionCount = rememberSensitivePermissionCount(permissions)

    val headerData = rememberHeaderDisplay(
        stage = stage,
        appInfo = appInfo,
        versionLine = versionLine,
        onIconClick = onIconClick,
        uninstallInfo = uninstallInfo,
        uninstallIcon = uninstallIcon,
    )
    val liveBodySnapshot = StageBodySnapshot(
        page = page,
        appInfo = appInfo,
        entity = entity,
        sourceLabel = sourceLabel,
        permissions = permissions,
        sensitivePermissionCount = sensitivePermissionCount,
        notices = notices,
        installedAppInfo = installedAppInfo,
        installedModuleInfo = installedModuleInfo,
        totalSelectedSize = totalSelectedSize,
        selectedEntityCount = selectedEntityCount,
        displaySdk = displaySdk,
        displaySize = displaySize,
        hideIdenticalComparisons = hideIdenticalComparisons,
        showOppoSpecial = showOppoSpecial,
        labShowFilePath = labShowFilePath,
        labShowInstallInitiator = labShowInstallInitiator,
        isSplitUpdateMode = isSplitUpdateMode,
        isInvalidSplitInstall = isInvalidSplitInstall,
        canOpenInstallOptions = canOpenInstallOptions,
        canOpenSplitChoice = canOpenSplitChoice,
        showOpenLsposed = showOpenLsposed,
        unknownSourceAppLabel = unknownSourceAppLabel,
        uninstallInfo = uninstallInfo,
        uninstallFlags = uninstallFlags,
    )

    CouiPositionFullScreen(
        title = title,
        isClosing = isClosing,
        onBack = backAction,
        backEnabled = backEnabled,
        endAction = topEndAction,
        endActionKey = topEndActionKey,
        useBlur = useBlur,
        fixedHeader = {
            val showFixedHeader = page.subPage == FullScreenSubPage.Main && headerData != null
            AnimatedContent(
                targetState = showFixedHeader,
                contentKey = { it },
                transitionSpec = {
                    fadeIn(tween(durationMillis = 220)) togetherWith
                        fadeOut(tween(durationMillis = 180))
                },
                label = "FullScreenHeaderMode",
            ) { slotPresent ->
                val frozenHeader = remember { FullScreenSlotRef(headerData) }
                if (slotPresent == showFixedHeader) {
                    frozenHeader.value = headerData
                }
                val header = frozenHeader.value
                if (slotPresent && header != null) {
                    CouiFullScreenAppHeader(
                        appInfo = header.appInfo,
                        versionLine = header.versionLine,
                        onIconClick = header.onIconClick,
                    )
                }
            }
        },
        bottomBar = if (usesShellBottomBar) {
            {
                FullScreenActionRow(
                    stage = stage,
                    canInstall = canInstall,
                    primaryButtonRes = primaryButtonRes,
                    hasLaunchIntent = hasLaunchIntent,
                    showUnknownSourceAction = showUnknownSourceAction,
                    preparedFromTypeChoice = preparedFromTypeChoice,
                    isConfirmationSubmitting = isConfirmationSubmitting,
                    isArchivedUninstall = uninstallInfo?.isArchived == true,
                    onClose = onClose,
                    onCancel = onCancel,
                    onBackground = onBackground,
                    onBackToTypeChoice = onBackToTypeChoice,
                    onInstall = onInstall,
                    onOpenApp = onOpenApp,
                    onRequestUnknownSourcePermission = onRequestUnknownSourcePermission,
                    onApproveSession = onApproveSession,
                    onUninstall = onUninstall,
                    onStartUnarchive = onStartUnarchive,
                    onOpenUnarchiveErrorAction = onOpenUnarchiveErrorAction,
                )
            }
        } else {
            null
        },
        onCloseAnimationFinished = onCloseAnimationFinished,
    ) {
        AnimatedContent(
            targetState = page.subPage,
            modifier = Modifier.fillMaxSize().clipToBounds(),
            contentKey = { it },
            transitionSpec = { subPageTransition() },
            label = "FullScreenSubPage",
        ) { subPage ->
            val frozenSubPage = remember {
                FullScreenSlotRef(Pair(liveBodySnapshot, permissions))
            }
            if (subPage == page.subPage) {
                frozenSubPage.value = Pair(liveBodySnapshot, permissions)
            }
            val (slotBodySnapshot, slotPermissions) = frozenSubPage.value
            TransitionSlotGuard(isActive = subPage == page.subPage) {
                when (subPage) {
                    FullScreenSubPage.Main -> FullScreenStageBody(
                        snapshot = slotBodySnapshot,
                        prepareScrollState = prepareScrollState,
                        onOpenPermissions = { onAction(InstallerViewAction.ShowCouiPermissionList) },
                        onOpenInstallOptions = onOpenInstallOptions,
                        onOpenSplitChoice = onOpenSplitChoice,
                        onOpenLsposed = onOpenLsposed,
                        onToggleUninstallFlag = onToggleUninstallFlag,
                        choiceContent = choiceContent,
                        errorSuggestionsContent = errorSuggestionsContent,
                    )

                    FullScreenSubPage.Permissions -> CouiFullScreenPermissionContent(slotPermissions)

                    FullScreenSubPage.Settings -> Box(
                        Modifier.fillMaxSize().padding(horizontal = FullScreenHorizontalPadding),
                    ) {
                        settingsContent()
                    }

                    FullScreenSubPage.ExtendedMenu -> Box(
                        Modifier.fillMaxSize().padding(horizontal = FullScreenHorizontalPadding),
                    ) {
                        extendedMenuContent()
                    }
                }
            }
        }
    }
}

private data class HeaderDisplayData(
    val appInfo: AppInfoState,
    val versionLine: String?,
    val onIconClick: (() -> Unit)?,
)

private fun rememberHeaderDisplay(
    stage: InstallerStage,
    appInfo: AppInfoState,
    versionLine: String?,
    onIconClick: (() -> Unit)?,
    uninstallInfo: UninstallInfo?,
    uninstallIcon: ImageBitmap?,
): HeaderDisplayData? = when {
    (
        stage is InstallerStage.Preparing ||
            stage is InstallerStage.InstallPrepare ||
            stage is InstallerStage.InstallWaitingUnknownSource ||
            stage is InstallerStage.Installing ||
            stage is InstallerStage.InstallRetryDowngradeUsingUninstall ||
            stage is InstallerStage.InstallSuccess ||
            stage is InstallerStage.InstallFailed ||
            stage is InstallerStage.InstallingModule
        ) && appInfo.primaryEntity != null -> HeaderDisplayData(
        appInfo = appInfo,
        versionLine = versionLine,
        onIconClick = if (stage is InstallerStage.InstallPrepare) onIconClick else null,
    )

    (
        stage is InstallerStage.UninstallReady ||
            stage is InstallerStage.Uninstalling ||
            stage is InstallerStage.UninstallSuccess ||
            stage is InstallerStage.UninstallFailed
        ) && uninstallInfo != null -> {
        val uninstallVersion = when {
            !uninstallInfo.versionName.isNullOrBlank() && uninstallInfo.versionCode != null ->
                "${uninstallInfo.versionName} (${uninstallInfo.versionCode})"

            !uninstallInfo.versionName.isNullOrBlank() -> uninstallInfo.versionName

            uninstallInfo.versionCode != null -> uninstallInfo.versionCode.toString()

            else -> null
        }
        HeaderDisplayData(
            appInfo = AppInfoState(
                icon = uninstallIcon,
                label = uninstallInfo.appLabel ?: uninstallInfo.packageName,
                packageName = uninstallInfo.packageName,
            ),
            versionLine = uninstallVersion,
            onIconClick = null,
        )
    }

    else -> null
}

private fun AnimatedContentTransitionScope<FullScreenSubPage>.subPageTransition(): ContentTransform = if (targetState != FullScreenSubPage.Main) {
    (
        fadeIn(tween(durationMillis = 220)) +
            slideInHorizontally(colorOsSpring(responseSeconds = 0.32f)) { it / 4 }
        ) togetherWith (
        fadeOut(tween(durationMillis = 160)) +
            slideOutHorizontally(tween(durationMillis = 160)) { -it / 6 }
        )
} else {
    (
        fadeIn(tween(durationMillis = 220)) +
            slideInHorizontally(colorOsSpring(responseSeconds = 0.32f)) { -it / 6 }
        ) togetherWith (
        fadeOut(tween(durationMillis = 160)) +
            slideOutHorizontally(tween(durationMillis = 160)) { it / 4 }
        )
}

private enum class StageBucket {
    Prepare,
    Choice,
    Confirm,
    Module,
    Completed,
    Error,
    UninstallPrepare,
    UnarchiveReady,
    UnarchiveError,
    StatusRing,
}

private fun stageBucketOf(stage: InstallerStage): StageBucket = when (stage) {
    is InstallerStage.InstallPrepare -> StageBucket.Prepare

    is InstallerStage.InstallChoice -> StageBucket.Choice

    is InstallerStage.InstallConfirm -> StageBucket.Confirm

    is InstallerStage.InstallingModule -> StageBucket.Module

    is InstallerStage.InstallCompleted -> StageBucket.Completed

    is InstallerStage.InstallFailed,
    is InstallerStage.ResolveFailed,
    is InstallerStage.AnalyseFailed,
    is InstallerStage.UninstallFailed,
    is InstallerStage.UninstallResolveFailed,
    is InstallerStage.UnarchiveFailed,
    -> StageBucket.Error

    is InstallerStage.UninstallReady -> StageBucket.UninstallPrepare

    is InstallerStage.UnarchiveReady -> StageBucket.UnarchiveReady

    is InstallerStage.UnarchiveError -> StageBucket.UnarchiveError

    else -> StageBucket.StatusRing
}

@Composable
private fun FullScreenStageBody(
    snapshot: StageBodySnapshot,
    prepareScrollState: ScrollState,
    onOpenPermissions: () -> Unit,
    onOpenInstallOptions: () -> Unit,
    onOpenSplitChoice: () -> Unit,
    onOpenLsposed: () -> Unit,
    onToggleUninstallFlag: (Int, Boolean) -> Unit,
    choiceContent: @Composable () -> Unit,
    errorSuggestionsContent: @Composable (Throwable) -> Unit,
) {
    val activeBucket = stageBucketOf(snapshot.page.stage)

    AnimatedContent(
        targetState = activeBucket,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        contentKey = { it },
        transitionSpec = {
            (
                fadeIn(tween(durationMillis = 220)) +
                    slideInVertically(tween(durationMillis = 240)) { it / 48 }
                ) togetherWith fadeOut(tween(durationMillis = 180))
        },
        label = "FullScreenStageBody",
    ) { slotBucket ->
        val frozenSnapshot = remember { FullScreenSlotRef(snapshot) }
        if (slotBucket == activeBucket) {
            frozenSnapshot.value = snapshot
        }
        TransitionSlotGuard(isActive = slotBucket == activeBucket) {
            StageBodyContent(
                snapshot = frozenSnapshot.value,
                prepareScrollState = prepareScrollState,
                onOpenPermissions = onOpenPermissions,
                onOpenInstallOptions = onOpenInstallOptions,
                onOpenSplitChoice = onOpenSplitChoice,
                onOpenLsposed = onOpenLsposed,
                onToggleUninstallFlag = onToggleUninstallFlag,
                choiceContent = choiceContent,
                errorSuggestionsContent = errorSuggestionsContent,
            )
        }
    }
}

@Composable
private fun StageBodyContent(
    snapshot: StageBodySnapshot,
    prepareScrollState: ScrollState,
    onOpenPermissions: () -> Unit,
    onOpenInstallOptions: () -> Unit,
    onOpenSplitChoice: () -> Unit,
    onOpenLsposed: () -> Unit,
    onToggleUninstallFlag: (Int, Boolean) -> Unit,
    choiceContent: @Composable () -> Unit,
    errorSuggestionsContent: @Composable (Throwable) -> Unit,
) {
    val page = snapshot.page
    val topBleed = LocalFullScreenTopBleed.current
    val bottomBleed = LocalFullScreenBottomBleed.current
    // Prepare and batch results scroll, so their safe distance goes into the scroll content and the
    // viewport can run behind the fixed chrome. Every other body keeps its layout reserved, which
    // is what keeps the centred status marks from moving.
    val scrollsUnderChrome = page.stage is InstallerStage.InstallPrepare ||
        page.stage is InstallerStage.InstallCompleted
    val bodyModifier = when {
        page.stage is InstallerStage.InstallChoice -> Modifier.fillMaxSize()
        scrollsUnderChrome -> Modifier.fillMaxSize()
        else -> Modifier.fillMaxSize().padding(top = topBleed, bottom = bottomBleed)
    }
    Box(modifier = bodyModifier) {
        when (val stage = page.stage) {
            is InstallerStage.InstallPrepare -> CouiFullScreenPrepareContent(
                entity = snapshot.entity,
                sourceLabel = snapshot.sourceLabel,
                permissionCount = snapshot.permissions.size,
                sensitivePermissionCount = snapshot.sensitivePermissionCount,
                onOpenPermissions = onOpenPermissions,
                scrollState = prepareScrollState,
                notices = snapshot.notices,
                installedAppInfo = snapshot.installedAppInfo,
                installedModuleInfo = snapshot.installedModuleInfo,
                totalSelectedSize = snapshot.totalSelectedSize,
                selectedEntityCount = snapshot.selectedEntityCount,
                displaySdk = snapshot.displaySdk,
                displaySize = snapshot.displaySize,
                hideIdenticalComparisons = snapshot.hideIdenticalComparisons,
                showOppoSpecial = snapshot.showOppoSpecial,
                labShowFilePath = snapshot.labShowFilePath,
                labShowInstallInitiator = snapshot.labShowInstallInitiator,
                isSplitUpdateMode = snapshot.isSplitUpdateMode,
                isInvalidSplitInstall = snapshot.isInvalidSplitInstall,
                onOpenInstallOptions = if (snapshot.canOpenInstallOptions) onOpenInstallOptions else null,
                onOpenSplitChoice = if (snapshot.canOpenSplitChoice) onOpenSplitChoice else null,
                contentTopPadding = topBleed,
                contentBottomPadding = bottomBleed + 20.dp,
            )

            is InstallerStage.InstallChoice -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = FullScreenHorizontalPadding),
                contentAlignment = Alignment.Center,
            ) {
                choiceContent()
            }

            is InstallerStage.InstallConfirm -> FullScreenConfirmBody(stage = stage)

            is InstallerStage.InstallingModule -> FullScreenModuleContent(stage)

            is InstallerStage.InstallCompleted -> FullScreenResultsContent(
                results = stage.results,
                contentTopPadding = topBleed,
                contentBottomPadding = bottomBleed,
            )

            is InstallerStage.InstallFailed -> {
                val errorTitle = stringResource(R.string.installer_install_failed)
                FullScreenErrorBody(
                    title = errorTitle,
                    error = page.error ?: Throwable(),
                    suggestions = {
                        if (page.error != null && page.error !is ModuleInstallException) {
                            errorSuggestionsContent(page.error)
                        }
                    },
                )
            }

            is InstallerStage.ResolveFailed -> FullScreenErrorBody(
                title = stringResource(R.string.installer_resolve_failed),
                error = page.error ?: Throwable(),
            )

            is InstallerStage.AnalyseFailed -> FullScreenErrorBody(
                title = stringResource(R.string.installer_analyse_failed),
                error = page.error ?: Throwable(),
            )

            is InstallerStage.UninstallFailed,
            is InstallerStage.UninstallResolveFailed,
            -> FullScreenErrorBody(
                title = stringResource(R.string.uninstall_failed_message),
                error = page.error ?: Throwable(),
            )

            is InstallerStage.UnarchiveFailed -> FullScreenErrorBody(
                title = stringResource(R.string.unarchive_failed),
                error = page.error ?: Throwable(),
            )

            is InstallerStage.UninstallReady -> FullScreenUninstallPrepareBody(
                isArchived = snapshot.uninstallInfo?.isArchived == true,
                uninstallFlags = snapshot.uninstallFlags,
                onToggleUninstallFlag = onToggleUninstallFlag,
            )

            is InstallerStage.UnarchiveReady -> FullScreenUnarchiveReadyBody(
                appLabel = stage.appLabel,
                installerLabel = stage.installerLabel,
            )

            is InstallerStage.UnarchiveError -> FullScreenUnarchiveErrorBody(
                status = stage.status,
                requiredBytes = stage.requiredBytes,
                installerLabel = stage.installerLabel,
            )

            else -> FullScreenStatusBody(
                page = page,
                appInfo = snapshot.appInfo,
                unknownSourceAppLabel = snapshot.unknownSourceAppLabel,
                showOpenLsposed = snapshot.showOpenLsposed,
                onOpenLsposed = onOpenLsposed,
            )
        }
    }
}

@Composable
private fun FullScreenStatusBody(
    page: FullScreenPage,
    appInfo: AppInfoState,
    unknownSourceAppLabel: String,
    showOpenLsposed: Boolean,
    onOpenLsposed: () -> Unit,
) {
    val isEarlyLoading = page.stage is InstallerStage.Resolving || page.stage is InstallerStage.Analysing
    var showEarlyLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isEarlyLoading) {
        if (isEarlyLoading) {
            delay(120)
            showEarlyLoading = true
        } else {
            showEarlyLoading = false
        }
    }
    val spec = statusSpecOf(page, appInfo, unknownSourceAppLabel)
    val actionsEnabled = LocalFullScreenActionsEnabled.current
    val extraContent: (@Composable () -> Unit)? = if (page.stage is InstallerStage.InstallSuccess && showOpenLsposed) {
        {
            TextButton(
                text = stringResource(R.string.open_lsposed),
                onClick = { if (actionsEnabled) onOpenLsposed() },
                enabled = actionsEnabled,
                colors = ButtonDefaults.textButtonColors(),
            )
        }
    } else {
        null
    }
    AnimatedVisibility(
        visible = !isEarlyLoading || showEarlyLoading,
        enter = fadeIn(tween(durationMillis = 180)),
        exit = ExitTransition.None,
        modifier = Modifier.fillMaxSize(),
    ) {
        CouiFullScreenStatusContent(
            title = spec.title,
            subtitle = spec.subtitle,
            progressText = spec.progressText,
            progress = spec.progress,
            icon = spec.icon,
            iconTint = spec.iconTint,
            haloColor = spec.halo,
            extraContent = extraContent,
        )
    }
}

private data class StatusSpec(
    val title: String,
    val subtitle: String? = null,
    val progressText: String? = null,
    val progress: Float? = null,
    val icon: ImageVector? = null,
    val iconTint: Color = Color.Unspecified,
    val halo: Color = Color.Unspecified,
)

@Composable
private fun statusSpecOf(
    page: FullScreenPage,
    appInfo: AppInfoState,
    unknownSourceAppLabel: String,
): StatusSpec {
    val successTint = COUITheme.colorScheme.success
    return when (val stage = page.stage) {
        is InstallerStage.Preparing -> StatusSpec(
            title = stringResource(R.string.installer_preparing),
            subtitle = stringResource(R.string.installer_preparing_desc),
            progressText = page.progress?.asPercent(),
            progress = page.progress,
        )

        is InstallerStage.Resolving -> StatusSpec(title = stringResource(R.string.installer_resolving))

        is InstallerStage.Analysing -> StatusSpec(title = stringResource(R.string.installer_analysing))

        is InstallerStage.InstallWaitingUnknownSource -> StatusSpec(
            title = stringResource(R.string.installer_waiting_unknown_source),
            subtitle = stringResource(R.string.installer_waiting_unknown_source_desc, unknownSourceAppLabel),
        )

        is InstallerStage.Installing -> StatusSpec(
            title = stringResource(
                if (stage.phase == InstallPhase.WRITING) R.string.installer_writing else R.string.installer_installing,
            ),
            subtitle = if (stage.total > 1) {
                stringResource(
                    R.string.installing_progress_text,
                    stage.appLabel ?: appInfo.label,
                    stage.current,
                    stage.total,
                )
            } else {
                null
            },
            progressText = page.progress?.asPercent(),
            progress = page.progress,
        )

        is InstallerStage.InstallRetryDowngradeUsingUninstall -> StatusSpec(
            title = stringResource(R.string.installer_installing),
        )

        is InstallerStage.InstallSuccess -> StatusSpec(
            title = stringResource(R.string.installer_install_success),
            icon = AppCOUIIcons.Ok,
            iconTint = successTint,
            halo = successTint.copy(alpha = 0.12f),
        )

        is InstallerStage.Uninstalling -> StatusSpec(
            title = stringResource(R.string.installer_uninstalling),
        )

        is InstallerStage.UninstallSuccess -> StatusSpec(
            title = stringResource(R.string.uninstall_success_message),
            icon = AppCOUIIcons.Ok,
            iconTint = successTint,
            halo = successTint.copy(alpha = 0.12f),
        )

        is InstallerStage.Unarchiving -> StatusSpec(
            title = stringResource(R.string.unarchive_restoring),
        )

        else -> StatusSpec(title = stringResource(R.string.loading))
    }
}

private fun Float.asPercent(): String = NumberFormat.getPercentInstance().format(toDouble())

private data class ActionButtonSpec(
    val text: String,
    val enabled: Boolean,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit,
)

private data class ActionBarSpec(
    val secondary: ActionButtonSpec? = null,
    val primary: ActionButtonSpec? = null,
)

@Composable
private fun FullScreenActionRow(
    stage: InstallerStage,
    canInstall: Boolean,
    @StringRes primaryButtonRes: Int,
    hasLaunchIntent: Boolean,
    showUnknownSourceAction: Boolean,
    preparedFromTypeChoice: Boolean,
    isConfirmationSubmitting: Boolean,
    isArchivedUninstall: Boolean,
    onClose: () -> Unit,
    onCancel: () -> Unit,
    onBackground: () -> Unit,
    onBackToTypeChoice: () -> Unit,
    onInstall: () -> Unit,
    onOpenApp: () -> Unit,
    onRequestUnknownSourcePermission: () -> Unit,
    onApproveSession: (Int, Boolean) -> Unit,
    onUninstall: () -> Unit,
    onStartUnarchive: () -> Unit,
    onOpenUnarchiveErrorAction: () -> Unit,
) {
    val enabled = LocalFullScreenActionsEnabled.current
    val spec = when (stage) {
        is InstallerStage.InstallPrepare -> ActionBarSpec(
            secondary = ActionButtonSpec(
                text = stringResource(if (preparedFromTypeChoice) R.string.back else R.string.cancel),
                enabled = enabled,
                onClick = if (preparedFromTypeChoice) onBackToTypeChoice else onClose,
            ),
            primary = ActionButtonSpec(
                text = stringResource(primaryButtonRes),
                enabled = enabled && canInstall,
                onClick = onInstall,
            ),
        )

        is InstallerStage.Preparing -> ActionBarSpec(
            secondary = ActionButtonSpec(stringResource(R.string.cancel), enabled, onClick = onCancel),
            primary = ActionButtonSpec(stringResource(R.string.installer_move_to_background), enabled, onClick = onBackground),
        )

        is InstallerStage.Installing -> ActionBarSpec(
            secondary = ActionButtonSpec(stringResource(R.string.installer_move_to_background), enabled, onClick = onBackground),
        )

        is InstallerStage.InstallWaitingUnknownSource -> ActionBarSpec(
            secondary = ActionButtonSpec(stringResource(R.string.cancel), enabled, onClick = onCancel),
            primary = if (showUnknownSourceAction) {
                ActionButtonSpec(stringResource(R.string.suggestion_allow_unknown_source), enabled, onClick = onRequestUnknownSourcePermission)
            } else {
                null
            },
        )

        is InstallerStage.InstallConfirm -> {
            val confirmText = when {
                stage.isOwnershipConflict -> stringResource(R.string.install_anyway)
                stage.requestType == ConfirmationRequestType.PRE_APPROVAL -> stringResource(R.string.pre_approve)
                else -> stringResource(R.string.confirm)
            }
            val actionEnabled = enabled && !isConfirmationSubmitting
            ActionBarSpec(
                secondary = ActionButtonSpec(
                    text = stringResource(R.string.cancel),
                    enabled = actionEnabled,
                    onClick = { onApproveSession(stage.sessionId, false) },
                ),
                primary = ActionButtonSpec(
                    text = confirmText,
                    enabled = actionEnabled,
                    onClick = { onApproveSession(stage.sessionId, true) },
                ),
            )
        }

        is InstallerStage.InstallSuccess -> if (hasLaunchIntent) {
            ActionBarSpec(
                secondary = ActionButtonSpec(stringResource(R.string.finish), enabled, onClick = onClose),
                primary = ActionButtonSpec(stringResource(R.string.open), enabled, onClick = onOpenApp),
            )
        } else {
            ActionBarSpec(
                primary = ActionButtonSpec(stringResource(R.string.finish), enabled, onClick = onClose),
            )
        }

        is InstallerStage.UninstallReady -> ActionBarSpec(
            secondary = ActionButtonSpec(stringResource(R.string.cancel), enabled, onClick = onClose),
            primary = ActionButtonSpec(
                text = stringResource(if (isArchivedUninstall) R.string.uninstall_archive else R.string.uninstall),
                enabled = enabled,
                isDestructive = true,
                onClick = onUninstall,
            ),
        )

        is InstallerStage.UnarchiveReady -> ActionBarSpec(
            secondary = ActionButtonSpec(stringResource(R.string.cancel), enabled, onClick = onClose),
            primary = ActionButtonSpec(stringResource(R.string.unarchive_restore), enabled, onClick = onStartUnarchive),
        )

        is InstallerStage.UnarchiveError -> {
            val hasPrimary = stage.status.primaryAction != UnarchiveErrorAction.CLOSE
            if (hasPrimary) {
                ActionBarSpec(
                    secondary = ActionButtonSpec(stringResource(R.string.close), enabled, onClick = onClose),
                    primary = ActionButtonSpec(
                        text = stringResource(stage.status.actionLabelResId),
                        enabled = enabled,
                        onClick = onOpenUnarchiveErrorAction,
                    ),
                )
            } else {
                ActionBarSpec(
                    primary = ActionButtonSpec(stringResource(R.string.close), enabled, onClick = onClose),
                )
            }
        }

        is InstallerStage.InstallCompleted,
        is InstallerStage.UninstallSuccess,
        -> ActionBarSpec(
            primary = ActionButtonSpec(stringResource(R.string.finish), enabled, onClick = onClose),
        )

        is InstallerStage.InstallingModule -> if (stage.isFinished) {
            ActionBarSpec(
                primary = ActionButtonSpec(stringResource(R.string.close), enabled, onClick = onClose),
            )
        } else {
            ActionBarSpec()
        }

        is InstallerStage.InstallFailed,
        is InstallerStage.ResolveFailed,
        is InstallerStage.AnalyseFailed,
        is InstallerStage.UninstallFailed,
        is InstallerStage.UninstallResolveFailed,
        is InstallerStage.UnarchiveFailed,
        -> ActionBarSpec(
            primary = ActionButtonSpec(stringResource(R.string.close), enabled, onClick = onClose),
        )

        else -> ActionBarSpec()
    }

    val layoutKey = Triple(
        spec.secondary != null,
        spec.primary != null,
        spec.primary?.isDestructive == true,
    )

    AnimatedContent(
        targetState = layoutKey,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        contentKey = { it },
        transitionSpec = {
            fadeIn(tween(durationMillis = 200)) togetherWith
                fadeOut(tween(durationMillis = 160))
        },
        label = "FullScreenActions",
    ) { slotLayout ->
        val frozenSpec = remember { FullScreenSlotRef(spec) }
        if (slotLayout == layoutKey) {
            frozenSpec.value = spec
        }
        val slotSpec = frozenSpec.value
        TransitionSlotGuard(isActive = slotLayout == layoutKey) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = FullScreenHorizontalPadding, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                slotSpec.secondary?.let { btn ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (btn.isDestructive) {
                            DestructiveAction(btn.text, btn.onClick, btn.enabled)
                        } else {
                            SecondaryAction(btn.text, btn.onClick, btn.enabled)
                        }
                    }
                }
                slotSpec.primary?.let { btn ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (btn.isDestructive) {
                            DestructiveAction(btn.text, btn.onClick, btn.enabled)
                        } else {
                            PrimaryAction(btn.text, btn.onClick, btn.enabled)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Button(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColorsPrimary(),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Crossfade(
            targetState = text,
            animationSpec = tween(durationMillis = 160),
            label = "PrimaryActionLabel",
        ) { label ->
            Text(
                text = label,
                style = COUITheme.textStyles.buttonL,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SecondaryAction(text: String, onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Crossfade(
            targetState = text,
            animationSpec = tween(durationMillis = 160),
            label = "SecondaryActionLabel",
        ) { label ->
            Text(
                text = label,
                style = COUITheme.textStyles.buttonL,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DestructiveAction(text: String, onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            color = if (useCouiMonet) COUITheme.colorScheme.secondaryContainer else COUITheme.colorScheme.secondaryVariant,
        ),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Crossfade(
            targetState = text,
            animationSpec = tween(durationMillis = 160),
            label = "DestructiveActionLabel",
        ) { label ->
            Text(
                text = label,
                style = COUITheme.textStyles.buttonL,
                color = COUITheme.colorScheme.error,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FullScreenConfirmBody(stage: InstallerStage.InstallConfirm) {
    val tipMessage = when {
        stage.requestType == ConfirmationRequestType.PRE_APPROVAL -> {
            val initiator = stage.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
            stringResource(R.string.install_confirm_pre_approval_tip, initiator)
        }

        stage.requestType == ConfirmationRequestType.PERMISSIONS -> {
            val initiator = stage.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
            stringResource(R.string.install_confirm_permissions_tip, initiator)
        }

        stage.isOwnershipConflict -> {
            val owner = stage.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
            stringResource(R.string.install_confirm_question_update_owner_reminder, owner)
        }

        !stage.isSelfSession -> {
            val initiator = stage.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
            stringResource(R.string.install_confirm_external_request_tip, initiator)
        }

        else -> stringResource(R.string.installer_prepare_type_unknown_confirm)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FullScreenHorizontalPadding, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (stage.appIcon != null) {
            Image(
                bitmap = stage.appIcon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = stage.appLabel.toString(),
            style = COUITheme.textStyles.headlineM,
            color = COUITheme.colorScheme.labelPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        CouiInstallerTipCard(tipMessage)
    }
}

@Composable
private fun FullScreenErrorBody(
    title: String,
    error: Throwable,
    suggestions: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FullScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(COUITheme.colorScheme.error.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppCOUIIcons.Close,
                    contentDescription = null,
                    tint = COUITheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = title,
                style = COUITheme.textStyles.headlineXS,
                color = COUITheme.colorScheme.labelPrimary,
            )
        }
        Spacer(Modifier.size(8.dp))
        // Bounded height via weight ensures CouiErrorTextBlock's inner verticalScroll never sees infinity.
        CouiErrorTextBlock(
            error = error,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
        )
        Spacer(Modifier.size(12.dp))
        suggestions()
    }
}

@Composable
private fun FullScreenUninstallPrepareBody(
    isArchived: Boolean,
    uninstallFlags: Int,
    onToggleUninstallFlag: (Int, Boolean) -> Unit,
) {
    val deleteKeepData = uninstallFlags.hasFlag(UninstallFlags.DELETE_KEEP_DATA)
    val deleteAllUsers = uninstallFlags.hasFlag(UninstallFlags.DELETE_ALL_USERS)
    val deleteSystemApp = uninstallFlags.hasFlag(UninstallFlags.DELETE_SYSTEM_APP)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FullScreenHorizontalPadding, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isArchived) {
            CouiInstallerTipCard(stringResource(R.string.uninstall_archive_message))
        } else {
            CouiSettingsCard(Modifier.fillMaxWidth()) {
                CouiCheckboxWidget(
                    title = stringResource(id = R.string.uninstall_keep_data),
                    description = stringResource(id = R.string.uninstall_keep_data_desc),
                    checked = deleteKeepData,
                    onCheckedChange = { onToggleUninstallFlag(UninstallFlags.DELETE_KEEP_DATA, it) },
                )
                CouiCheckboxWidget(
                    title = stringResource(id = R.string.uninstall_all_users),
                    description = stringResource(id = R.string.uninstall_all_users_desc),
                    checked = deleteAllUsers,
                    onCheckedChange = { onToggleUninstallFlag(UninstallFlags.DELETE_ALL_USERS, it) },
                )
                CouiCheckboxWidget(
                    title = stringResource(id = R.string.uninstall_delete_system_app),
                    description = stringResource(id = R.string.uninstall_delete_system_app_desc),
                    checked = deleteSystemApp,
                    onCheckedChange = { onToggleUninstallFlag(UninstallFlags.DELETE_SYSTEM_APP, it) },
                )
            }
        }
    }
}

@Composable
private fun FullScreenUnarchiveReadyBody(
    appLabel: CharSequence,
    installerLabel: CharSequence,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FullScreenHorizontalPadding, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.unarchive_title, appLabel),
            style = COUITheme.textStyles.headlineM,
            color = COUITheme.colorScheme.labelPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text(
            text = stringResource(R.string.unarchive_message, installerLabel),
            style = COUITheme.textStyles.bodyXL,
            color = COUITheme.colorScheme.labelSecondaryVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun FullScreenUnarchiveErrorBody(
    status: UnarchiveStatus,
    requiredBytes: Long,
    installerLabel: CharSequence?,
) {
    val context = LocalContext.current
    val message = when (status) {
        UnarchiveStatus.InsufficientStorage -> stringResource(
            status.messageResId,
            android.text.format.Formatter.formatShortFileSize(context, requiredBytes),
        )

        UnarchiveStatus.InstallerDisabled,
        UnarchiveStatus.InstallerUninstalled,
        -> stringResource(
            status.messageResId,
            installerLabel ?: stringResource(R.string.installer_label_unknown),
        )

        else -> stringResource(status.messageResId)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FullScreenHorizontalPadding, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = unarchiveErrorTitle(status, installerLabel),
            style = COUITheme.textStyles.headlineM,
            color = COUITheme.colorScheme.labelPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text(
            text = message,
            style = COUITheme.textStyles.bodyXL,
            color = COUITheme.colorScheme.labelSecondaryVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun FullScreenModuleContent(stage: InstallerStage.InstallingModule) {
    KeyEventBlocker { it.key == Key.VolumeDown || it.key == Key.VolumeUp }
    val listState = rememberLazyListState()
    LaunchedEffect(stage.output.size, stage.isFinished) {
        if (stage.output.isNotEmpty()) {
            if (stage.isFinished) {
                listState.animateScrollToItem(stage.output.lastIndex)
            } else {
                listState.scrollToItem(stage.output.lastIndex)
            }
        }
    }
    val statusTitle = stringResource(
        when {
            !stage.isFinished -> R.string.installer_installing_module
            stage.failed -> R.string.installer_install_failed
            else -> R.string.installer_install_success
        },
    )
    Column(
        Modifier.fillMaxSize().padding(horizontal = FullScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = stage.isFinished,
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    (
                        fadeIn(tween(durationMillis = 150, delayMillis = 80)) +
                            scaleIn(tween(durationMillis = 180, delayMillis = 80), initialScale = 0.85f)
                        ) togetherWith fadeOut(tween(durationMillis = 80))
                },
                label = "ModuleStatusIcon",
            ) { finished ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (finished) {
                        Icon(
                            imageVector = if (stage.failed) AppCOUIIcons.Close else AppCOUIIcons.Ok,
                            contentDescription = null,
                            tint = if (stage.failed) COUITheme.colorScheme.error else COUITheme.colorScheme.success,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        CircularProgressIndicator(size = 24.dp, strokeWidth = 3.dp)
                    }
                }
            }
            AnimatedContent(
                targetState = statusTitle,
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    fadeIn(tween(durationMillis = 150, delayMillis = 80)) togetherWith
                        fadeOut(tween(durationMillis = 80))
                },
                label = "ModuleStatusTitle",
            ) { label ->
                Text(
                    text = label,
                    style = COUITheme.textStyles.headlineXS,
                    color = COUITheme.colorScheme.labelPrimary,
                )
            }
        }
        Card(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(stage.output) { line ->
                    Text(
                        text = line,
                        style = COUITheme.textStyles.bodyS,
                        fontFamily = FontFamily.Monospace,
                        color = if (line.startsWith("ERROR:")) COUITheme.colorScheme.error else COUITheme.colorScheme.labelPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenResultsContent(
    results: List<InstallResult>,
    contentTopPadding: Dp = 0.dp,
    contentBottomPadding: Dp = 0.dp,
) {
    val groupedResults = remember(results) {
        results
            .groupBy { it.entity.app.packageName }
            .flatMap { (_, groupResults) ->
                val failedResult = groupResults.firstOrNull { !it.success }
                val baseResult = groupResults.find { it.entity.app is AppEntity.BaseEntity }
                listOfNotNull(failedResult ?: baseResult ?: groupResults.firstOrNull())
            }
    }
    val successes = groupedResults.count { it.success }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = FullScreenHorizontalPadding,
            end = FullScreenHorizontalPadding,
            top = contentTopPadding + 8.dp,
            bottom = contentBottomPadding + 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.installer_completed_subtitle, successes, groupedResults.size - successes),
                style = COUITheme.textStyles.headlineS,
                color = COUITheme.colorScheme.labelPrimary,
            )
        }
        // Mixed batches must retain individual failures rather than show a generic success.
        items(groupedResults, key = { it.entity.app.packageName + it.entity.app.name }) { result ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val app = result.entity.app
                    Text(
                        text = (app as? AppEntity.BaseEntity)?.label ?: app.name,
                        style = COUITheme.textStyles.headlineXS,
                        color = COUITheme.colorScheme.labelPrimary,
                    )
                    Text(
                        text = app.packageName,
                        style = COUITheme.textStyles.bodyS,
                        color = COUITheme.colorScheme.labelSecondaryVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val resultColor = if (result.success) COUITheme.colorScheme.success else COUITheme.colorScheme.error
                        Icon(
                            imageVector = if (result.success) AppCOUIIcons.Ok else AppCOUIIcons.Close,
                            contentDescription = null,
                            tint = resultColor,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(if (result.success) R.string.installer_install_success else R.string.installer_install_failed),
                            style = COUITheme.textStyles.bodyL,
                            color = resultColor,
                        )
                    }
                    if (!result.success) {
                        result.error?.let { error ->
                            Text(error.help(), style = COUITheme.textStyles.bodyM, color = COUITheme.colorScheme.labelSecondary)
                            error.message?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = COUITheme.textStyles.bodyS, color = COUITheme.colorScheme.labelSecondaryVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
