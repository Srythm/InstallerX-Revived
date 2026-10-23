// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.home.priv

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiCustomAuthorizerDialog
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsTipCard
import com.rosan.installer.ui.theme.getCouiAppBarColor
import com.rosan.installer.ui.theme.installerCouiBlurEffect
import com.rosan.installer.ui.theme.rememberCouiBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.coui.kmp.preference.CheckboxLocation
import top.yukonga.coui.kmp.preference.CheckboxPreference
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic

@Composable
fun CouiPrivPage(useBlur: Boolean, viewModel: HomePageViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = COUIScrollBehavior()
    val showCustomizeAuthorizerDialog = remember { mutableStateOf(false) }
    var selectCustomizeOnConfirm by remember { mutableStateOf(false) }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    OnLifecycleEvent(event = Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(HomePageViewAction.RefreshActivateStatus)
    }

    CouiCustomAuthorizerDialog(
        showState = showCustomizeAuthorizerDialog,
        initialAuthorizer = uiState.customizeAuthorizer,
        onDismiss = {
            showCustomizeAuthorizerDialog.value = false
            selectCustomizeOnConfirm = false
        },
        onConfirm = { authorizer ->
            val customizeAuthorizer = authorizer.trim()
            if (customizeAuthorizer.isBlank()) return@CouiCustomAuthorizerDialog

            if (selectCustomizeOnConfirm) {
                viewModel.dispatch(HomePageViewAction.EnableCustomizeAuthorizer(customizeAuthorizer))
            } else {
                viewModel.dispatch(HomePageViewAction.ChangeCustomizeAuthorizer(customizeAuthorizer))
            }
            selectCustomizeOnConfirm = false
        },
    )

    val backdrop = rememberCouiBlurBackdrop(useBlur)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.installerCouiBlurEffect(backdrop),
                color = backdrop.getCouiAppBarColor(),
                title = stringResource(R.string.home_stat_authorizers),
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
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
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
            item {
                CouiSettingsTipCard(text = stringResource(R.string.priv_page_what_is_this_desc))
            }

            item {
                CouiSettingsTipCard(text = stringResource(R.string.priv_page_notice_desc))
            }

            item {
                SmallTitle(text = stringResource(id = R.string.config_authorizer))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    // None / System App
                    CheckboxPreference(
                        checkboxLocation = CheckboxLocation.End,
                        title = if (uiState.isSystemApp) {
                            stringResource(
                                R.string.working_status_system_installer,
                            )
                        } else {
                            stringResource(R.string.config_authorizer_none)
                        },
                        summary = when {
                            !uiState.isSessionInstallSupported -> stringResource(R.string.unavailable)
                            uiState.isSystemApp -> stringResource(R.string.working_status_system_installer_desc)
                            else -> stringResource(R.string.working_status_none_authorizer_desc)
                        },
                        checked = uiState.globalAuthorizer == Authorizer.None,
                        enabled = uiState.isSessionInstallSupported,
                        onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.None)) },
                    )

                    // Root
                    val isRootAvailable = uiState.rootMode != RootMode.None
                    CheckboxPreference(
                        checkboxLocation = CheckboxLocation.End,
                        title = stringResource(R.string.config_authorizer_root),
                        summary = if (isRootAvailable) {
                            stringResource(R.string.available) + " (${uiState.rootMode.name})"
                        } else {
                            stringResource(R.string.unavailable)
                        },
                        checked = uiState.globalAuthorizer == Authorizer.Root,
                        onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Root)) },
                    )

                    // Shizuku
                    CheckboxPreference(
                        checkboxLocation = CheckboxLocation.End,
                        title = stringResource(R.string.config_authorizer_shizuku),
                        summary = when {
                            uiState.shizukuAuthorized -> stringResource(R.string.running) + " (${uiState.shizukuMode.desc})"
                            uiState.shizukuAvailable -> stringResource(R.string.shizuku_not_authorized)
                            else -> stringResource(R.string.shizuku_not_available)
                        },
                        checked = uiState.globalAuthorizer == Authorizer.Shizuku,
                        onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Shizuku)) },
                    )

                    // Dhizuku
                    CheckboxPreference(
                        checkboxLocation = CheckboxLocation.End,
                        title = stringResource(R.string.config_authorizer_dhizuku),
                        summary = when {
                            uiState.dhizukuAuthorized -> stringResource(R.string.running)
                            uiState.dhizukuAvailable -> stringResource(R.string.dhizuku_not_authorized)
                            else -> stringResource(R.string.dhizuku_not_available)
                        },
                        checked = uiState.globalAuthorizer == Authorizer.Dhizuku,
                        onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Dhizuku)) },
                    )

                    // Custom
                    val isCustomizeSelected = uiState.globalAuthorizer == Authorizer.Customize
                    CheckboxPreference(
                        checkboxLocation = CheckboxLocation.End,
                        title = stringResource(R.string.config_authorizer_customize),
                        summary = uiState.customizeAuthorizer.ifBlank {
                            stringResource(R.string.config_authorizer_customize)
                        },
                        checked = isCustomizeSelected,
                        onCheckedChange = {
                            if (isCustomizeSelected) {
                                selectCustomizeOnConfirm = false
                                showCustomizeAuthorizerDialog.value = true
                            } else if (uiState.customizeAuthorizer.isBlank()) {
                                selectCustomizeOnConfirm = true
                                showCustomizeAuthorizerDialog.value = true
                            } else {
                                viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Customize))
                            }
                        },
                    )
                }
            }

            item {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
