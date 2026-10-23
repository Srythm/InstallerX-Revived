// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.network

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.NetworkSourceMode
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.HttpProfile
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.network.NetworkSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.network.NetworkSettingsViewModel
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiCustomGithubProxyUrlDialog
import com.rosan.installer.ui.page.coui.widgets.CouiGithubUpdateChannelSelectionDialog
import com.rosan.installer.ui.theme.getCouiAppBarColor
import com.rosan.installer.ui.theme.installerCouiBlurEffect
import com.rosan.installer.ui.theme.rememberCouiBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.coui.kmp.preference.SwitchPreference
import top.yukonga.coui.kmp.preference.WindowSpinnerPreference
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic
import io.github.suqi8.coui.kmp.window.WindowDialog

@Composable
fun CouiNetworkPage(useBlur: Boolean, viewModel: NetworkSettingsViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = COUIScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal)
        .asPaddingValues()
    val topBarBackdrop = rememberCouiBlurBackdrop(useBlur)
    val showChannelDialog = remember { mutableStateOf(false) }
    val showCustomProxyDialog = remember { mutableStateOf(false) }
    var pendingNetworkSourceMode by rememberSaveable { mutableStateOf<NetworkSourceMode?>(null) }
    var exitAfterInternetDisable by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(exitAfterInternetDisable, uiState.allowInternetAccess) {
        if (exitAfterInternetDisable && !uiState.allowInternetAccess) {
            navigator.pop()
        }
    }

    NetworkSourceModeWarningDialog(
        mode = pendingNetworkSourceMode,
        onDismiss = { pendingNetworkSourceMode = null },
        onConfirm = { mode ->
            pendingNetworkSourceMode = null
            viewModel.dispatch(NetworkSettingsAction.ConfirmNetworkSourceMode(mode))
        },
    )

    if (showChannelDialog.value) {
        CouiGithubUpdateChannelSelectionDialog(
            showState = showChannelDialog,
            currentSelection = uiState.githubUpdateChannel,
            onDismiss = { showChannelDialog.value = false },
            onConfirm = { channel ->
                showChannelDialog.value = false
                viewModel.dispatch(NetworkSettingsAction.ChangeGithubUpdateChannel(channel))
                if (channel == GithubUpdateChannel.CUSTOM) {
                    showCustomProxyDialog.value = true
                }
            },
        )
    }

    if (showCustomProxyDialog.value) {
        CouiCustomGithubProxyUrlDialog(
            showState = showCustomProxyDialog,
            initialUrl = uiState.customGithubProxyUrl,
            onDismiss = {
                showCustomProxyDialog.value = false
                if (uiState.customGithubProxyUrl.isEmpty()) {
                    viewModel.dispatch(
                        NetworkSettingsAction.ChangeGithubUpdateChannel(
                            GithubUpdateChannel.OFFICIAL,
                        ),
                    )
                }
            },
            onConfirm = { url ->
                showCustomProxyDialog.value = false
                viewModel.dispatch(NetworkSettingsAction.ChangeCustomGithubProxyUrl(url))
                if (url.isEmpty()) {
                    viewModel.dispatch(
                        NetworkSettingsAction.ChangeGithubUpdateChannel(
                            GithubUpdateChannel.OFFICIAL,
                        ),
                    )
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerCouiBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getCouiAppBarColor(),
                title = stringResource(R.string.network_settings),
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
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SwitchPreference(
                        checked = uiState.allowInternetAccess,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                exitAfterInternetDisable = true
                            }
                            viewModel.dispatch(
                                NetworkSettingsAction.ChangeInternetAccess(enabled),
                            )
                        },
                        title = stringResource(R.string.network_access),
                    )
                }
            }
            item { Spacer(modifier = Modifier.size(12.dp)) }
            if (uiState.allowInternetAccess) {
                item { SmallTitle(stringResource(R.string.internet_access_enabled)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        CouiNetworkSourceModePreference(
                            currentMode = uiState.networkSourceMode,
                            onModeChange = { mode ->
                                if (mode != NetworkSourceMode.Cache &&
                                    !uiState.networkSourceModeWarningAcknowledged
                                ) {
                                    pendingNetworkSourceMode = mode
                                } else {
                                    viewModel.dispatch(
                                        NetworkSettingsAction.ChangeNetworkSourceMode(mode),
                                    )
                                }
                            },
                        )
                        val allowSecureString = stringResource(R.string.lab_http_profile_secure)
                        val allowLocalString = stringResource(R.string.lab_http_profile_local)
                        val allowAllString = stringResource(R.string.lab_http_profile_all)
                        val profileData = remember(
                            allowSecureString,
                            allowLocalString,
                            allowAllString,
                        ) {
                            linkedMapOf(
                                HttpProfile.ALLOW_SECURE to allowSecureString,
                                HttpProfile.ALLOW_LOCAL to allowLocalString,
                                HttpProfile.ALLOW_ALL to allowAllString,
                            )
                        }
                        val profileEntries = remember(profileData) {
                            profileData.values.map { name -> DropdownItem(title = name) }
                        }
                        val profileIndex = profileData.keys.toList()
                            .indexOf(uiState.httpProfile)
                            .coerceAtLeast(0)

                        WindowSpinnerPreference(
                            title = stringResource(R.string.lab_http_profile),
                            items = profileEntries,
                            selectedIndex = profileIndex,
                            onSelectedIndexChange = { newIndex ->
                                profileData.keys.elementAtOrNull(newIndex)?.let { profile ->
                                    viewModel.dispatch(NetworkSettingsAction.ChangeHttpProfile(profile))
                                }
                            },
                        )

                        val channelSummary = when (uiState.githubUpdateChannel) {
                            GithubUpdateChannel.OFFICIAL -> stringResource(
                                R.string.lab_update_github_proxy_official,
                            )

                            GithubUpdateChannel.PROXY_7ED -> stringResource(
                                R.string.lab_update_github_proxy_7ed,
                            )

                            GithubUpdateChannel.CUSTOM -> uiState.customGithubProxyUrl.ifBlank {
                                stringResource(R.string.lab_update_github_proxy_custom)
                            }
                        }
                        BasicComponent(
                            title = stringResource(R.string.lab_update_github_proxy),
                            summary = channelSummary,
                            onClick = { showChannelDialog.value = true },
                        )
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun NetworkSourceModeWarningDialog(
    mode: NetworkSourceMode?,
    onDismiss: () -> Unit,
    onConfirm: (NetworkSourceMode) -> Unit,
) {
    WindowDialog(
        show = mode != null,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.network_source_mode_warning_title),
        content = {
            Column {
                Text(text = stringResource(R.string.network_source_mode_warning_desc))
                Spacer(modifier = Modifier.size(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        text = stringResource(R.string.cancel),
                    )
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = { mode?.let(onConfirm) },
                        text = stringResource(R.string.confirm),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        },
    )
}

@Composable
private fun CouiNetworkSourceModePreference(currentMode: NetworkSourceMode, onModeChange: (NetworkSourceMode) -> Unit) {
    val modes = linkedMapOf(
        NetworkSourceMode.Cache to stringResource(R.string.config_network_source_cache),
        NetworkSourceMode.Smart to stringResource(R.string.config_network_source_smart),
        NetworkSourceMode.LowStorage to stringResource(R.string.config_network_source_low_storage),
    )
    val descriptions = mapOf(
        NetworkSourceMode.Cache to stringResource(R.string.config_network_source_cache_desc),
        NetworkSourceMode.Smart to stringResource(R.string.config_network_source_smart_desc),
        NetworkSourceMode.LowStorage to stringResource(R.string.config_network_source_low_storage_desc),
    )
    val entries = modes.values.map { DropdownItem(title = it) }

    WindowSpinnerPreference(
        title = stringResource(R.string.config_network_source_mode),
        summary = descriptions.getValue(currentMode),
        items = entries,
        selectedIndex = modes.keys.indexOf(currentMode).coerceAtLeast(0),
        onSelectedIndexChange = { index ->
            modes.keys.elementAtOrNull(index)?.let(onModeChange)
        },
    )
}
