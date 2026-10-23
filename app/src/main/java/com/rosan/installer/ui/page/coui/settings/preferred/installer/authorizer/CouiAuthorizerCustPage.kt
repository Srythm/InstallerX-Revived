// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.installer.authorizer

import android.os.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustViewModel
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiIntNumberPickerWidget
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchWidget
import com.rosan.installer.ui.theme.getCouiAppBarColor
import com.rosan.installer.ui.theme.installerCouiBlurEffect
import com.rosan.installer.ui.theme.rememberCouiBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic

@Composable
fun CouiAuthorizerCustPage(
    useBlur: Boolean,
    viewModel: AuthorizerCustViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val scrollBehavior = COUIScrollBehavior()

    val isSystemApp = capabilityProvider.isSystemApp

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val topBarBackdrop = rememberCouiBlurBackdrop(useBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerCouiBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getCouiAppBarColor(),
                title = stringResource(R.string.authorizer_customization),
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

            item {
                SmallTitle(
                    text = if (isSystemApp) {
                        stringResource(R.string.working_status_system_installer)
                    } else {
                        stringResource(R.string.config_authorizer_none)
                    },
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    if (isSystemApp) {
                        CouiSwitchWidget(
                            title = stringResource(R.string.config_always_use_root_in_system),
                            description = stringResource(R.string.config_always_use_root_in_system_desc),
                            checked = uiState.alwaysUseRootInSystem,
                            onCheckedChange = { viewModel.dispatch(AuthorizerCustAction.ChangeAlwaysUseRootInSystem(it)) },
                        )
                    }
                    if (!isSystemApp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        CouiSwitchWidget(
                            title = stringResource(R.string.lab_install_without_user_action),
                            description = stringResource(R.string.lab_install_without_user_action_desc),
                            checked = uiState.allowInstallWithoutUserAction,
                            onCheckedChange = { viewModel.dispatch(AuthorizerCustAction.ChangeAllowInstallWithoutUserAction(it)) },
                        )
                    }
                }
            }

            if (!isSystemApp) {
                item { SmallTitle(stringResource(R.string.extras)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        CouiIntNumberPickerWidget(
                            title = stringResource(R.string.close_session_countdown),
                            description = stringResource(R.string.close_session_countdown_desc),
                            value = uiState.closeSessionCountDown,
                            startInt = 1,
                            endInt = 10,
                            onValueChange = {
                                viewModel.dispatch(AuthorizerCustAction.ChangeCloseSessionCountDown(it))
                            },
                        )
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
