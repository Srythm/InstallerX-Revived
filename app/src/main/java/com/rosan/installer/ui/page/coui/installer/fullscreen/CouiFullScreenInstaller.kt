// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.install.InstallPhase
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.session.model.InstallResult
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.icons.AppCOUIIcons
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import com.rosan.installer.ui.page.coui.installer.sheetcontent.PrepareSettingsContent
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.theme.colorOsSpring
import com.rosan.installer.ui.util.KeyEventBlocker
import com.rosan.installer.util.help
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.theme.COUITheme
import java.text.NumberFormat

internal enum class FullScreenSubPage { Main, Permissions, Settings }

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

// InstallerStage folds unknown progress into zero; the session retains the nullable byte progress.
@Composable
fun CouiFullScreenInstaller(
    stage: InstallerStage,
    title: String,
    appInfo: AppInfoState,
    entity: AppEntity.BaseEntity?,
    versionName: String?,
    sourceLabel: String?,
    permissions: List<String>,
    showPermissions: Boolean,
    showSettings: Boolean,
    isClosing: Boolean,
    viewModel: InstallerViewModel,
    session: InstallerSessionRepository,
    onClose: () -> Unit,
) {
    val sessionProgress by session.progress.collectAsStateWithLifecycle(initialValue = ProgressEntity.Ready)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subPage = when {
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
        is InstallerStage.Installing -> if (stage.phase == InstallPhase.WRITING) rawInstall?.overallProgress() else null
        else -> null
    }
    val currentPage = FullScreenPage(stage, title, subPage, progress, uiState.error)
    // Keep the visible page through teardown, rather than showing Ready mid-fade.
    val lastOpenPage = remember { mutableStateOf(currentPage) }
    SideEffect { if (!isClosing) lastOpenPage.value = currentPage }

    CouiFullScreenInstallerContent(
        page = if (isClosing) lastOpenPage.value else currentPage,
        appInfo = appInfo,
        useBlur = uiState.viewSettings.useBlur,
        entity = entity,
        versionName = versionName,
        sourceLabel = sourceLabel,
        permissions = permissions,
        isClosing = isClosing,
        onClose = { if (!viewModel.isClosingFullscreen.value) onClose() },
        onAction = { if (!viewModel.isClosingFullscreen.value) viewModel.dispatch(it) },
        settingsContent = { PrepareSettingsContent(viewModel = viewModel) },
    )
}

@Composable
internal fun CouiFullScreenInstallerContent(
    page: FullScreenPage,
    appInfo: AppInfoState,
    useBlur: Boolean = false,
    entity: AppEntity.BaseEntity? = null,
    versionName: String? = null,
    sourceLabel: String? = null,
    permissions: List<String> = emptyList(),
    isClosing: Boolean = false,
    onClose: () -> Unit = {},
    onAction: (InstallerViewAction) -> Unit = {},
    settingsContent: @Composable () -> Unit = {},
) {
    val stage = page.stage
    val subPage = page.subPage != FullScreenSubPage.Main
    val installing = stage is InstallerStage.Installing ||
        (stage is InstallerStage.InstallingModule && !stage.isFinished)
    val backAction: (() -> Unit)? = when {
        installing || isClosing -> null
        page.subPage == FullScreenSubPage.Permissions -> ({ onAction(InstallerViewAction.HideCouiPermissionList) })
        page.subPage == FullScreenSubPage.Settings -> ({ onAction(InstallerViewAction.HideCouiSheetRightActionSettings) })
        else -> onClose
    }
    CouiPositionFullScreen(
        title = when (page.subPage) {
            FullScreenSubPage.Permissions -> stringResource(R.string.permission_list)
            FullScreenSubPage.Settings -> stringResource(R.string.installer_settings)
            FullScreenSubPage.Main -> page.title
        },
        isClosing = isClosing,
        onBack = backAction,
        useBlur = useBlur,
        onMore = if (!subPage && stage is InstallerStage.InstallPrepare && !isClosing) {
            { onAction(InstallerViewAction.ShowCouiSheetRightActionSettings) }
        } else {
            null
        },
        header = if (!subPage) {
            { CouiFullScreenAppHeader(appInfo, versionName) }
        } else {
            null
        },
        bottomBar = if (!subPage) {
            {
                FullScreenActionRow(
                    stage = stage,
                    onClose = onClose,
                    onInstall = { onAction(InstallerViewAction.Install(true)) },
                )
            }
        } else {
            null
        },
    ) {
        AnimatedContent(
            targetState = page,
            modifier = Modifier.fillMaxSize(),
            contentKey = { it.key },
            // ColorOS page change: the incoming page rises a little and settles with the system
            // spring while the outgoing one fades and drifts back, instead of a flat cross-fade.
            transitionSpec = {
                (
                    fadeIn(colorOsSpring(responseSeconds = 0.30f)) +
                        slideInVertically(colorOsSpring(responseSeconds = 0.34f)) { it / 22 } +
                        scaleIn(colorOsSpring(responseSeconds = 0.34f), initialScale = 0.97f)
                    ) togetherWith (
                    fadeOut(tween(durationMillis = 140)) +
                        scaleOut(tween(durationMillis = 200), targetScale = 1.02f)
                    )
            },
            label = "CouiFullScreenPage",
        ) { displayed ->
            // Always render this slot's snapshot, never the incoming live stage.
            when (displayed.subPage) {
                FullScreenSubPage.Settings -> Box(
                    Modifier.fillMaxSize().padding(horizontal = FullScreenHorizontalPadding),
                ) { settingsContent() }

                FullScreenSubPage.Permissions -> CouiFullScreenPermissionContent(permissions)

                FullScreenSubPage.Main -> when (val displayedStage = displayed.stage) {
                    is InstallerStage.InstallPrepare -> CouiFullScreenPrepareContent(
                        entity = entity,
                        sourceLabel = sourceLabel,
                        permissionCount = permissions.size,
                        sensitivePermissionCount = permissions.sensitiveCount(),
                        onOpenPermissions = { onAction(InstallerViewAction.ShowCouiPermissionList) },
                    )

                    is InstallerStage.Preparing -> CouiFullScreenStatusContent(
                        title = stringResource(R.string.installer_preparing),
                        subtitle = listOfNotNull(
                            stringResource(R.string.installer_preparing_desc),
                            displayed.progress?.asPercent(),
                        ).joinToString("\n"),
                        progress = displayed.progress,
                    )

                    is InstallerStage.Resolving -> CouiFullScreenStatusContent(stringResource(R.string.installer_resolving))

                    is InstallerStage.Analysing -> CouiFullScreenStatusContent(stringResource(R.string.installer_analysing))

                    is InstallerStage.Installing -> CouiFullScreenStatusContent(
                        title = stringResource(
                            if (displayedStage.phase == InstallPhase.WRITING) R.string.installer_writing else R.string.installer_installing,
                        ),
                        subtitle = listOfNotNull(
                            if (displayedStage.total > 1) {
                                stringResource(
                                    R.string.installing_progress_text,
                                    displayedStage.appLabel ?: appInfo.label,
                                    displayedStage.current,
                                    displayedStage.total,
                                )
                            } else {
                                null
                            },
                            displayed.progress?.asPercent(),
                        ).joinToString("\n"),
                        progress = displayed.progress,
                    )

                    is InstallerStage.InstallingModule -> FullScreenModuleContent(displayedStage)

                    is InstallerStage.InstallCompleted -> FullScreenResultsContent(displayedStage.results)

                    is InstallerStage.InstallSuccess -> CouiFullScreenStatusContent(
                        title = stringResource(R.string.installer_install_success),
                        icon = AppCOUIIcons.Ok,
                    )

                    is InstallerStage.InstallFailed -> CouiFullScreenStatusContent(
                        title = stringResource(R.string.installer_install_failed),
                        subtitle = displayed.error?.help(),
                        icon = AppCOUIIcons.Close,
                        iconTint = COUITheme.colorScheme.error,
                        haloColor = COUITheme.colorScheme.error.copy(alpha = 0.12f),
                    )

                    else -> Unit // Other workflows remain on their original sheet.
                }
            }
        }
    }
}

private fun Float.asPercent(): String = NumberFormat.getPercentInstance().format(toDouble())

@Composable
private fun FullScreenActionRow(stage: InstallerStage, onClose: () -> Unit, onInstall: () -> Unit) {
    val enabled = LocalFullScreenActionsEnabled.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FullScreenHorizontalPadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (stage) {
            is InstallerStage.Installing -> Unit

            // Keep the shell's fixed empty slot.
            is InstallerStage.InstallingModule -> if (stage.isFinished) {
                FullScreenActionButton(stringResource(R.string.close), onClose, Modifier.weight(1f), enabled)
            }

            is InstallerStage.InstallPrepare -> {
                FullScreenActionButton(
                    stringResource(R.string.cancel),
                    onClose,
                    Modifier.weight(1f),
                    enabled,
                    primary = false,
                )
                FullScreenActionButton(
                    stringResource(R.string.install),
                    onInstall,
                    Modifier.weight(1f),
                    enabled,
                )
            }

            is InstallerStage.Preparing, is InstallerStage.Resolving, is InstallerStage.Analysing ->
                FullScreenActionButton(stringResource(R.string.cancel), onClose, Modifier.weight(1f), enabled, primary = false)

            else -> FullScreenActionButton(stringResource(R.string.finish), onClose, Modifier.weight(1f), enabled)
        }
    }
}

@Composable
private fun FullScreenActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    primary: Boolean = true,
) {
    TextButton(
        text = text,
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier = modifier,
        colors = if (primary) ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors(),
    )
}

@Composable
private fun FullScreenModuleContent(stage: InstallerStage.InstallingModule) {
    KeyEventBlocker { it.key == Key.VolumeDown || it.key == Key.VolumeUp }
    val listState = rememberLazyListState()
    LaunchedEffect(stage.output.size, stage.isFinished) {
        if (stage.output.isNotEmpty()) listState.scrollToItem(stage.output.lastIndex)
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = FullScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                if (stage.isFinished) {
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
            Text(
                text = stringResource(
                    when {
                        !stage.isFinished -> R.string.installer_installing_module
                        stage.failed -> R.string.installer_install_failed
                        else -> R.string.installer_install_success
                    },
                ),
                style = COUITheme.textStyles.headlineXS,
                color = COUITheme.colorScheme.labelPrimary,
            )
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
private fun FullScreenResultsContent(results: List<InstallResult>) {
    val successes = results.count { it.success }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = FullScreenHorizontalPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.installer_completed_subtitle, successes, results.size - successes),
                style = COUITheme.textStyles.headlineS,
                color = COUITheme.colorScheme.labelPrimary,
            )
        }
        // Mixed batches must retain individual failures rather than show a generic success.
        items(results) { result ->
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
