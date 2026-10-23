// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.config.edit

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.settings.model.config.BiometricAuthMode
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiUnsavedChangesDialog
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewEvent
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.theme.getCouiAppBarColor
import com.rosan.installer.ui.theme.installerCouiBlurEffect
import com.rosan.installer.ui.theme.rememberCouiBlurBackdrop
import com.rosan.installer.ui.util.isNoneActive
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.SnackbarHost
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Close
import io.github.suqi8.coui.kmp.icon.extended.Ok
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic
import io.github.suqi8.coui.kmp.window.WindowDialog
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
fun CouiEditPage(id: Long? = null, viewModel: EditViewModel = koinViewModel { parametersOf(id) }, useBlur: Boolean) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dispatch = viewModel::dispatch

    val snackBarHostState = remember { SnackbarHostState() }
    val scrollBehavior = COUIScrollBehavior()
    val showUnsavedDialogState = remember { mutableStateOf(false) }
    val showAutoApproveSessionDialogState = remember { mutableStateOf(false) }

    CouiUnsavedChangesDialog(
        showState = showUnsavedDialogState,
        onDismiss = {
            showUnsavedDialogState.value = false
        },
        onConfirm = {
            showUnsavedDialogState.value = false
            navigator.pop()
        },
        errorMessages = state.activeErrorResIds.map { stringResource(it) },
    )

    WindowDialog(
        show = showAutoApproveSessionDialogState.value,
        onDismissRequest = {
            showAutoApproveSessionDialogState.value = false
        },
        title = stringResource(R.string.warning),
        content = {
            Column {
                Text(
                    text = stringResource(R.string.config_auto_approve_session_warning),
                    color = COUITheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showAutoApproveSessionDialogState.value = false
                        },
                        text = stringResource(R.string.cancel),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showAutoApproveSessionDialogState.value = false
                            dispatch(EditViewAction.ChangeDataAutoApproveSession(true))
                        },
                        text = stringResource(R.string.confirm),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        },
    )

    val shouldInterceptBackPress = state.hasUnsavedChanges || state.hasErrors

    BackHandler(enabled = shouldInterceptBackPress) {
        showUnsavedDialogState.value = true
    }

    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer

    val unknownErrorString = stringResource(R.string.installer_unknown_error)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditViewEvent.SnackBar -> {
                    // Resolve priority:
                    // 1. Specific Resource ID
                    // 2. Explicit String message
                    // 3. Localized generic fallback
                    val snackBarText =
                        event.messageResId?.let {
                            @SuppressLint("LocalContextGetResourceValueCall")
                            context.getString(it)
                        }
                            ?: event.message
                            ?: unknownErrorString

                    snackBarHostState.showSnackbar(
                        message = snackBarText,
                        withDismissAction = true,
                    )
                }

                is EditViewEvent.Saved -> {
                    navigator.pop()
                }
            }
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val topBarBackdrop = rememberCouiBlurBackdrop(useBlur)

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                modifier = Modifier.installerCouiBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getCouiAppBarColor(),
                scrollBehavior = scrollBehavior,
                title = stringResource(id?.let { R.string.update } ?: R.string.add),
                navigationIcon = {
                    CouiBackButton(
                        icon = COUIIcons.Regular.Close,
                        onClick = { navigator.pop() },
                    )
                },
                actions = {
                    IconButton(
                        onClick = { dispatch(EditViewAction.SaveData) },
                    ) {
                        Icon(
                            imageVector = COUIIcons.Regular.Ok,
                            contentDescription = stringResource(R.string.save),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(state = snackBarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
            ),
            overscrollEffect = null,
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { CouiDataNameWidget(state = state, dispatch = dispatch) }
            item { CouiDataDescriptionWidget(state = state, dispatch = dispatch) }
            item { SmallTitle(stringResource(R.string.config)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    CouiDataAuthorizerWidget(state = state, dispatch = dispatch)
                    CouiDataCustomizeAuthorizerWidget(state = state, dispatch = dispatch)
                    CouiDataInstallModeWidget(state = state, dispatch = dispatch)
                    CouiDataAutoApproveSessionWidget(
                        state = state,
                        dispatch = dispatch,
                        onEnableRequest = {
                            showAutoApproveSessionDialogState.value = true
                        },
                    )
                    if (state.globalInstallerBiometricAuthMode == BiometricAuthMode.FollowConfig) {
                        CouiDataRequireBiometricAuthWidget(state = state, dispatch = dispatch)
                    }
                    CouiToastModeWidget(state = state, dispatch = dispatch)
                }
            }
            if (isNoneActive(stateAuthorizer, globalAuthorizer)) {
                item { CouiSettingsTipCard(stringResource(R.string.config_authorizer_none_tips)) }
            }
            item { SmallTitle(stringResource(R.string.config_label_installer_settings)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    CouiDataUserWidget(state = state, dispatch = dispatch)
                    CouiInstallReasonWidget(state = state, dispatch = dispatch)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        CouiDataPackageSourceWidget(state = state, dispatch = dispatch)
                    }
                    CouiDataInstallRequesterWidget(state = state, dispatch = dispatch)
                    CouiDataDeclareInstallerWidget(state = state, dispatch = dispatch)
                    CouiDataManualDexoptWidget(state = state, dispatch = dispatch)
                    CouiDataAutoDeleteWidget(state = state, dispatch = dispatch)
                    CouiDisplaySdkWidget(state = state, dispatch = dispatch)
                    CouiDisplaySizeWidget(state = state, dispatch = dispatch)
                }
            }
            item { SmallTitle(stringResource(R.string.config_label_install_options)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp),
                ) {
                    CouiDataForAllUserWidget(state = state, dispatch = dispatch)
                    CouiDataAllowTestOnlyWidget(state = state, dispatch = dispatch)
                    CouiDataAllowDowngradeWidget(state = state, dispatch = dispatch)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        CouiDataBypassLowTargetSdkWidget(state = state, dispatch = dispatch)
                    }
                    CouiDataAllowAllRequestedPermissionsWidget(state = state, dispatch = dispatch)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        CouiRequestUpdateOwnershipWidget(state = state, dispatch = dispatch)
                    }
                }
            }
            item { SmallTitle(stringResource(R.string.config_label_preferences)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp),
                ) {
                    CouiDataSplitChooseAllWidget(state = state, dispatch = dispatch)
                    CouiDataApkChooseAllWidget(state = state, dispatch = dispatch)
                    if (state.checkAppSignature) {
                        CouiDataAllowSigMismatchWidget(state = state, dispatch = dispatch)
                        CouiDataAllowSigUnknownWidget(state = state, dispatch = dispatch)
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
