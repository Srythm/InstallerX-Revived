// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.ui.page.main.installer.InstallerPage
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.isPhoneDevice
import com.rosan.installer.ui.util.requestPortraitOrientationOnPhoneSafely
import org.koin.compose.koinInject

@Composable
fun InstallerActivityContent(
    session: InstallerSessionRepository,
    themeStateProvider: ThemeStateProvider = koinInject()
) {
    val uiState by themeStateProvider.themeStateFlow.collectAsStateWithLifecycle(initialValue = ThemeState())
    if (!uiState.isLoaded) return

    val context = LocalContext.current
    val isPhone = context.isPhoneDevice
    LaunchedEffect(context, isPhone) {
        (context as? Activity)?.requestPortraitOrientationOnPhoneSafely(isPhone)
    }

    InstallerTheme(
        themeMode = uiState.themeMode,
        paletteStyle = uiState.paletteStyle,
        colorSpec = uiState.colorSpec,
        useDynamicColor = uiState.useDynamicColor,
        seedColor = androidx.compose.ui.graphics.Color(uiState.seedColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            InstallerPage(session)
        }
    }
}
