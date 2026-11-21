// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.install.InstallPhase
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.session.model.InstallResult
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import com.rosan.installer.ui.page.main.installer.InstallerStage
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme

enum class FullScreenPreviewCase {
    Preparing,
    Resolving,
    Analysing,
    Prepare,
    Permissions,
    ZeroProgress,
    UnknownProgress,
    BatchWriting,
    Success,
    Failure,
    BatchResults,
    ModuleRunning,
    ModuleFailed,
}

class FullScreenPreviewProvider : PreviewParameterProvider<FullScreenPreviewCase> {
    override val values = FullScreenPreviewCase.entries.asSequence()
}

@Preview(name = "Full-screen stages", widthDp = 360, heightDp = 720, showBackground = true)
@Composable
private fun FullScreenStagesPreview(
    @PreviewParameter(FullScreenPreviewProvider::class) case: FullScreenPreviewCase,
) {
    FullScreenPreview(case)
}

@Preview(name = "Dark batch", widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large Chinese text", widthDp = 320, heightDp = 640, locale = "zh-rCN", fontScale = 1.5f)
@Composable
private fun FullScreenBatchPreview() {
    FullScreenPreview(FullScreenPreviewCase.BatchWriting)
}

@Composable
private fun FullScreenPreview(case: FullScreenPreviewCase) {
    val entity = previewApp("com.example.app", "Example application")
    val error = IllegalStateException("Package verification failed")
    val stage = when (case) {
        FullScreenPreviewCase.Preparing -> InstallerStage.Preparing(0.35f)

        FullScreenPreviewCase.Resolving -> InstallerStage.Resolving

        FullScreenPreviewCase.Analysing -> InstallerStage.Analysing

        FullScreenPreviewCase.Prepare, FullScreenPreviewCase.Permissions -> InstallerStage.InstallPrepare

        FullScreenPreviewCase.ZeroProgress -> InstallerStage.Installing(0f, 1, 1, entity.label, InstallPhase.WRITING)

        FullScreenPreviewCase.UnknownProgress -> InstallerStage.Installing(0f, 1, 1, entity.label, InstallPhase.INSTALLING)

        FullScreenPreviewCase.BatchWriting -> InstallerStage.Installing(0.42f, 2, 4, entity.label, InstallPhase.WRITING)

        FullScreenPreviewCase.Success -> InstallerStage.InstallSuccess

        FullScreenPreviewCase.Failure -> InstallerStage.InstallFailed

        FullScreenPreviewCase.BatchResults -> InstallerStage.InstallCompleted(
            listOf(
                InstallResult(SelectInstallEntity(entity, true), true),
                InstallResult(SelectInstallEntity(previewApp("com.example.second", "Second application"), true), false, error),
            ),
        )

        FullScreenPreviewCase.ModuleRunning -> InstallerStage.InstallingModule(listOf("- Extracting module", "- Installing files"))

        FullScreenPreviewCase.ModuleFailed -> InstallerStage.InstallingModule(
            listOf("- Extracting module", "- Installing files", "ERROR: Installation failed"),
            isFinished = true,
            failed = true,
        )
    }
    val title = stringResource(
        when (stage) {
            is InstallerStage.InstallPrepare -> R.string.installer_prepare_install
            is InstallerStage.Preparing -> R.string.installer_preparing
            is InstallerStage.Resolving -> R.string.installer_resolving
            is InstallerStage.Analysing -> R.string.installer_analysing
            is InstallerStage.InstallSuccess -> R.string.installer_install_success
            is InstallerStage.InstallFailed -> R.string.installer_install_failed
            is InstallerStage.InstallCompleted -> R.string.finish
            is InstallerStage.InstallingModule -> if (stage.failed) R.string.installer_install_failed else R.string.installer_installing_module
            else -> R.string.installer_installing
        },
    )
    COUITheme(colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        CouiFullScreenInstallerContent(
            page = FullScreenPage(
                stage = stage,
                title = title,
                subPage = if (case == FullScreenPreviewCase.Permissions) FullScreenSubPage.Permissions else FullScreenSubPage.Main,
                progress = when (case) {
                    FullScreenPreviewCase.Preparing -> 0.35f
                    FullScreenPreviewCase.ZeroProgress -> 0f
                    FullScreenPreviewCase.BatchWriting -> 0.42f
                    else -> null
                },
                error = error,
            ),
            appInfo = AppInfoState(null, entity.label.orEmpty(), entity.packageName, entity),
            entity = entity,
            versionLine = "${entity.versionName} (${entity.versionCode})",
            sourceLabel = "Files",
            permissions = listOf("android.permission.CAMERA", "android.permission.ACCESS_NETWORK_STATE"),
        )
    }
}

private fun previewApp(packageName: String, label: String) = AppEntity.BaseEntity(
    packageName = packageName,
    sharedUserId = null,
    data = DataEntity.FileEntity("/preview/app.apk"),
    versionCode = 200,
    versionName = "2.0",
    label = label,
    icon = null,
    targetSdk = "36",
    minSdk = "26",
    size = 12_000_000L,
    signatureHash = "11223344556677889900aabbccddeeff",
)
