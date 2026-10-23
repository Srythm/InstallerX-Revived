// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.install.InstallPhase
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.coui.installer.components.AppInfoSlot
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import com.rosan.installer.ui.page.coui.widgets.ProgressButton
import com.rosan.installer.ui.page.coui.widgets.ProgressButtonDefaults
import com.rosan.installer.ui.util.isGestureNavigation
import io.github.suqi8.coui.kmp.basic.InfiniteProgressIndicator
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.COUITheme.isDynamicColor

@Composable
fun InstallingContent(
    stage: InstallerStage.Installing,
    appInfo: AppInfoState,
    onButtonClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val displayLabel = stage.appLabel ?: appInfo.label

        AppInfoSlot(appInfo = appInfo)

        Spacer(modifier = Modifier.height(32.dp))

        val progressText = if (stage.total > 1) {
            // Batch install: "Installing AppName (1/5)"
            stringResource(
                R.string.installing_progress_text,
                displayLabel,
                stage.current,
                stage.total,
            )
        } else {
            null
        }

        progressText?.let {
            Text(
                text = it,
                style = COUITheme.textStyles.footnote1,
                color = COUITheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }

        // Animate the progress value just like in InstallPreparingContent
        val animatedProgress by animateFloatAsState(
            targetValue = stage.progress,
            animationSpec = tween(durationMillis = 300),
            label = "ProgressAnimation",
        )

        // Dynamic content color based on progress fill
        val contentColor = if (animatedProgress < 0.45f) {
            if (isDynamicColor) COUITheme.colorScheme.onSecondaryContainer else COUITheme.colorScheme.onSecondaryVariant
        } else {
            COUITheme.colorScheme.onPrimary
        }

        ProgressButton(
            progress = animatedProgress,
            onClick = onButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
            colors = ProgressButtonDefaults.progressButtonColors(
                trackColor = if (isDynamicColor) COUITheme.colorScheme.secondaryContainer else COUITheme.colorScheme.secondaryVariant,
                progressColor = COUITheme.colorScheme.primary,
                contentColor = contentColor,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfiniteProgressIndicator(
                    color = contentColor,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    color = contentColor,
                    text = stringResource(
                        if (stage.total == 1 && stage.phase == InstallPhase.WRITING) {
                            R.string.installer_writing
                        } else {
                            R.string.installer_installing
                        },
                    ),
                )
            }
        }
    }
}
