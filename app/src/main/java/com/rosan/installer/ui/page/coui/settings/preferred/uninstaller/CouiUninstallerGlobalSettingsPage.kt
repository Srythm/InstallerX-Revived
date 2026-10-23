// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.uninstaller

import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.domain.engine.model.install.UninstallFlags
import com.rosan.installer.ui.activity.UninstallerActivity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsEvent
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsViewModel
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiNavigationItemWidget
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchWidget
import com.rosan.installer.ui.page.coui.widgets.CouiUninstallPackageDialog
import com.rosan.installer.ui.theme.getCouiAppBarColor
import com.rosan.installer.ui.theme.installerCouiBlurEffect
import com.rosan.installer.ui.theme.rememberCouiBlurBackdrop
import com.rosan.installer.util.toast
import org.koin.androidx.compose.koinViewModel
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic

@Composable
fun CouiUninstallerGlobalSettingsPage(useBlur: Boolean, viewModel: UninstallerSettingsViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = COUIScrollBehavior()
    var showUninstallInputDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UninstallerSettingsEvent.ShowMessage -> context.toast(event.resId)
            }
        }
    }

    CouiUninstallPackageDialog(
        showState = showUninstallInputDialog,
        onDismiss = { showUninstallInputDialog.value = false },
        onConfirm = { packageName ->
            showUninstallInputDialog.value = false
            val intent = Intent(context, UninstallerActivity::class.java).apply {
                putExtra("package_name", packageName)
            }
            context.startActivity(intent)
        },
    )

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val topBarBackdrop = rememberCouiBlurBackdrop(useBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerCouiBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getCouiAppBarColor(),
                title = stringResource(R.string.uninstaller_settings),
                navigationIcon = {
                    CouiBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior,
            )
        },
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
            item { CouiSettingsTipCard(stringResource(R.string.uninstall_authorizer_tip)) }
            item { SmallTitle(stringResource(R.string.global)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp),
                ) {
                    CouiSwitchWidget(
                        title = stringResource(id = R.string.uninstall_keep_data),
                        description = stringResource(id = R.string.uninstall_keep_data_desc),
                        checked = uiState.uninstallFlags.hasFlag(UninstallFlags.DELETE_KEEP_DATA),
                        onCheckedChange = {
                            viewModel.dispatch(
                                UninstallerSettingsAction.ToggleGlobalUninstallFlag(UninstallFlags.DELETE_KEEP_DATA, it),
                            )
                        },
                    )
                    CouiSwitchWidget(
                        title = stringResource(id = R.string.uninstall_all_users),
                        description = stringResource(id = R.string.uninstall_all_users_desc),
                        checked = uiState.uninstallFlags.hasFlag(UninstallFlags.DELETE_ALL_USERS),
                        onCheckedChange = {
                            viewModel.dispatch(
                                UninstallerSettingsAction.ToggleGlobalUninstallFlag(UninstallFlags.DELETE_ALL_USERS, it),
                            )
                        },
                    )
                    CouiSwitchWidget(
                        title = stringResource(id = R.string.uninstall_delete_system_app),
                        description = stringResource(id = R.string.uninstall_delete_system_app_desc),
                        checked = uiState.uninstallFlags.hasFlag(UninstallFlags.DELETE_SYSTEM_APP),
                        onCheckedChange = {
                            viewModel.dispatch(
                                UninstallerSettingsAction.ToggleGlobalUninstallFlag(UninstallFlags.DELETE_SYSTEM_APP, it),
                            )
                        },
                    )
                    if (BiometricManager
                            .from(LocalContext.current)
                            .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                        BiometricManager.BIOMETRIC_SUCCESS
                    ) {
                        CouiSwitchWidget(
                            icon = AppIcons.BiometricAuth,
                            title = stringResource(R.string.uninstaller_settings_require_biometric_auth),
                            description = stringResource(R.string.uninstaller_settings_require_biometric_auth_desc),
                            checked = uiState.uninstallerRequireBiometricAuth,
                            onCheckedChange = {
                                viewModel.dispatch(UninstallerSettingsAction.ChangeBiometricAuth(it))
                            },
                        )
                    }
                }
            }
            item { SmallTitle(stringResource(R.string.uninstall_call_uninstaller)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp),
                ) {
                    CouiNavigationItemWidget(
                        title = stringResource(R.string.uninstall_call_uninstaller_manually),
                        description = stringResource(R.string.uninstall_call_uninstaller_manually_desc),
                    ) {
                        showUninstallInputDialog.value = true
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
