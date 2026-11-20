// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
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
        isModule = viewModel.isInstallingModule,
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
    isModule: Boolean = false,
    entity: AppEntity.BaseEntity? = null,
    versionName: String? = null,
    sourceLabel: String? = null,
    permissions: List<String> = emptyList(),
    isClosing: Boolean = false,
    onClose: () -> Unit = {},
    onAction: (InstallerViewAction) -> Unit = {},
    settingsContent: @Composable () -> Unit = {},
) {
    val installing = page.stage.isBusy()
    val backAction: () -> Unit = when {
        installing || isClosing -> ({})
        page.subPage == FullScreenSubPage.Permissions -> ({ onAction(InstallerViewAction.HideCouiPermissionList) })
        page.subPage == FullScreenSubPage.Settings -> ({ onAction(InstallerViewAction.HideCouiSheetRightActionSettings) })
        else -> onClose
    }

    CouiPositionFullScreen(
        isClosing = isClosing,
        onBack = backAction,
        useBlur = useBlur,
    ) {
        // Sub pages are pushed, not faded in: the whole page travels, so the bar and the body stay
        // one surface. Only the stage body below uses a cross-fade.
        AnimatedContent(
            targetState = page.subPage,
            modifier = Modifier.fillMaxSize(),
            contentKey = { it },
            transitionSpec = { subPageTransition() },
            label = "FullScreenSubPage",
        ) { subPage ->
            when (subPage) {
                FullScreenSubPage.Main -> FullScreenMainPage(
                    page = page,
                    appInfo = appInfo,
                    isModule = isModule,
                    entity = entity,
                    versionName = versionName,
                    sourceLabel = sourceLabel,
                    permissions = permissions,
                    isClosing = isClosing,
                    onClose = onClose,
                    onAction = onAction,
                )

                FullScreenSubPage.Permissions -> FullScreenPageScaffold(
                    title = stringResource(R.string.permission_list),
                    onBack = {
                        if (!isClosing) onAction(InstallerViewAction.HideCouiPermissionList)
                    },
                ) {
                    CouiFullScreenPermissionContent(permissions)
                }

                FullScreenSubPage.Settings -> FullScreenPageScaffold(
                    title = stringResource(R.string.installer_settings),
                    onBack = {
                        if (!isClosing) onAction(InstallerViewAction.HideCouiSheetRightActionSettings)
                    },
                ) {
                    Box(Modifier.fillMaxSize().padding(horizontal = FullScreenHorizontalPadding)) {
                        settingsContent()
                    }
                }
            }
        }
    }
}

/**
 * COUI page push: the incoming page comes in from the right over a third of the screen with the
 * system spring, the outgoing one drifts a tenth of the way left and fades. Popping mirrors it.
 *
 * A plain cross-fade was the reason the sub pages looked like a dialog opening on top of another
 * dialog; a horizontal push keeps the navigation direction readable.
 */
private fun AnimatedContentTransitionScope<FullScreenSubPage>.subPageTransition(): ContentTransform = if (targetState != FullScreenSubPage.Main) {
    (
        slideInHorizontally(colorOsSpring(responseSeconds = 0.34f)) { it / 3 } +
            fadeIn(tween(durationMillis = 90))
        ) togetherWith (
        slideOutHorizontally(tween(durationMillis = 260)) { -it / 10 } +
            fadeOut(tween(durationMillis = 180))
        )
} else {
    (
        slideInHorizontally(colorOsSpring(responseSeconds = 0.34f)) { -it / 10 } +
            fadeIn(tween(durationMillis = 140))
        ) togetherWith (
        slideOutHorizontally(tween(durationMillis = 240)) { it / 3 } +
            fadeOut(tween(durationMillis = 160))
        )
}

@Composable
private fun FullScreenMainPage(
    page: FullScreenPage,
    appInfo: AppInfoState,
    isModule: Boolean,
    entity: AppEntity.BaseEntity?,
    versionName: String?,
    sourceLabel: String?,
    permissions: List<String>,
    isClosing: Boolean,
    onClose: () -> Unit,
    onAction: (InstallerViewAction) -> Unit,
) {
    val stage = page.stage
    val installing = stage.isBusy()
    // A task name, not a status line: keeping it constant is what stops the bar from flickering
    // every time the stage advances. The state itself is told by the body.
    val title = stringResource(if (isModule) R.string.installer_install_module else R.string.installer_install_app)
    val canLeave = !installing && !isClosing
    val settingsAvailable = stage is InstallerStage.InstallPrepare && !isClosing

    Column(Modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = title,
            centerTitle = true,
            color = Color.Transparent,
            defaultWindowInsetsPadding = false,
            showDivider = false,
            navigationIcon = {
                IconButton(
                    onClick = { if (canLeave) onClose() },
                    enabled = canLeave,
                ) {
                    Icon(
                        imageVector = AppCOUIIcons.Back,
                        contentDescription = stringResource(R.string.back),
                        tint = if (canLeave) {
                            COUITheme.colorScheme.labelPrimary
                        } else {
                            COUITheme.colorScheme.disabledNeutral
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            actions = {
                if (settingsAvailable) {
                    IconButton(onClick = { onAction(InstallerViewAction.ShowCouiSheetRightActionSettings) }) {
                        Icon(
                            imageVector = AppCOUIIcons.More,
                            contentDescription = stringResource(R.string.installer_settings),
                            tint = COUITheme.colorScheme.labelPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            },
        )
        // Fixed for the whole install: the identity block is the anchor the body changes under.
        CouiFullScreenAppHeader(appInfo, versionName)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            FullScreenStageBody(
                page = page,
                appInfo = appInfo,
                entity = entity,
                sourceLabel = sourceLabel,
                permissions = permissions,
                onAction = onAction,
            )
        }
        Box(Modifier.fillMaxWidth().height(FullScreenBottomBarHeight)) {
            FullScreenActionRow(
                stage = stage,
                onClose = onClose,
                onInstall = { onAction(InstallerViewAction.Install(true)) },
            )
        }
    }
}

/** A sub page owns its own bar so a push moves the title together with the content. */
@Composable
private fun FullScreenPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val enabled = LocalFullScreenActionsEnabled.current
    Column(Modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = title,
            centerTitle = true,
            color = Color.Transparent,
            defaultWindowInsetsPadding = false,
            showDivider = false,
            navigationIcon = {
                IconButton(onClick = { if (enabled) onBack() }, enabled = enabled) {
                    Icon(
                        imageVector = AppCOUIIcons.Back,
                        contentDescription = stringResource(R.string.back),
                        tint = if (enabled) COUITheme.colorScheme.labelPrimary else COUITheme.colorScheme.disabledNeutral,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        )
        Box(Modifier.fillMaxWidth().weight(1f)) { content() }
    }
}

/** What the body currently shows. Stages inside one bucket never rebuild the body. */
private enum class StageBucket { Prepare, Status }

@Composable
private fun FullScreenStageBody(
    page: FullScreenPage,
    appInfo: AppInfoState,
    entity: AppEntity.BaseEntity?,
    sourceLabel: String?,
    permissions: List<String>,
    onAction: (InstallerViewAction) -> Unit,
) {
    val bucket = if (page.stage is InstallerStage.InstallPrepare) StageBucket.Prepare else StageBucket.Status
    AnimatedContent(
        targetState = bucket,
        modifier = Modifier.fillMaxSize(),
        contentKey = { it },
        transitionSpec = {
            // Cross-fade with a 1/40 (a few dp) lift. Between prepare and progress there is no
            // shared element to morph, so fading is the honest transition — but nothing else on
            // the page may move while it happens.
            (
                fadeIn(tween(durationMillis = 240)) +
                    slideInVertically(tween(durationMillis = 260)) { it / 40 }
                ) togetherWith fadeOut(tween(durationMillis = 140))
        },
        label = "FullScreenStageBody",
    ) { bucket ->
        when (bucket) {
            StageBucket.Prepare -> CouiFullScreenPrepareContent(
                entity = entity,
                sourceLabel = sourceLabel,
                permissionCount = permissions.size,
                sensitivePermissionCount = permissions.sensitiveCount(),
                onOpenPermissions = { onAction(InstallerViewAction.ShowCouiPermissionList) },
            )

            StageBucket.Status -> FullScreenStatusBody(page = page, appInfo = appInfo)
        }
    }
}

/**
 * One persistent host for every progress and result stage.
 *
 * Because the bucket above does not change between "installing" and "success", this composable
 * survives the change: the ring fades out while the check mark fades in at the same coordinates,
 * and the labels cross-fade instead of the whole block being rebuilt.
 */
@Composable
private fun FullScreenStatusBody(page: FullScreenPage, appInfo: AppInfoState) {
    val stage = page.stage
    if (stage is InstallerStage.InstallingModule) {
        FullScreenModuleContent(stage)
        return
    }
    if (stage is InstallerStage.InstallCompleted) {
        FullScreenResultsContent(stage.results)
        return
    }

    val spec = statusSpecOf(page, appInfo)
    CouiFullScreenStatusContent(
        title = spec.title,
        subtitle = spec.subtitle,
        progress = spec.progress,
        icon = spec.icon,
        iconTint = spec.iconTint,
        haloColor = spec.halo,
    )
}

private data class StatusSpec(
    val title: String,
    val subtitle: String? = null,
    val progress: Float? = null,
    val icon: ImageVector? = null,
    val iconTint: Color = Color.Unspecified,
    val halo: Color = Color.Unspecified,
)

@Composable
private fun statusSpecOf(page: FullScreenPage, appInfo: AppInfoState): StatusSpec {
    val successTint = COUITheme.colorScheme.success
    val errorTint = COUITheme.colorScheme.error
    return when (val stage = page.stage) {
        is InstallerStage.Preparing -> StatusSpec(
            title = stringResource(R.string.installer_preparing),
            subtitle = listOfNotNull(
                stringResource(R.string.installer_preparing_desc),
                page.progress?.asPercent(),
            ).joinToString("\n"),
            progress = page.progress,
        )

        is InstallerStage.Resolving -> StatusSpec(title = stringResource(R.string.installer_resolving))

        is InstallerStage.Analysing -> StatusSpec(title = stringResource(R.string.installer_analysing))

        is InstallerStage.Installing -> StatusSpec(
            title = stringResource(
                if (stage.phase == InstallPhase.WRITING) R.string.installer_writing else R.string.installer_installing,
            ),
            subtitle = listOfNotNull(
                if (stage.total > 1) {
                    stringResource(
                        R.string.installing_progress_text,
                        stage.appLabel ?: appInfo.label,
                        stage.current,
                        stage.total,
                    )
                } else {
                    null
                },
                page.progress?.asPercent(),
            ).joinToString("\n"),
            progress = page.progress,
        )

        is InstallerStage.InstallSuccess -> StatusSpec(
            title = stringResource(R.string.installer_install_success),
            icon = AppCOUIIcons.Ok,
            iconTint = successTint,
            halo = successTint.copy(alpha = 0.12f),
        )

        is InstallerStage.InstallFailed -> StatusSpec(
            title = stringResource(R.string.installer_install_failed),
            subtitle = page.error?.help(),
            icon = AppCOUIIcons.Close,
            iconTint = errorTint,
            halo = errorTint.copy(alpha = 0.12f),
        )

        else -> StatusSpec(title = stringResource(R.string.loading))
    }
}

private fun Float.asPercent(): String = NumberFormat.getPercentInstance().format(toDouble())

private fun InstallerStage.isBusy(): Boolean = when (this) {
    is InstallerStage.Installing -> true
    is InstallerStage.InstallingModule -> !isFinished
    is InstallerStage.Preparing, is InstallerStage.Resolving, is InstallerStage.Analysing -> true
    else -> false
}

/** The action set for a stage. Keyed so the row only re-renders when the buttons really change. */
private enum class ActionSet { CancelInstall, CancelOnly, None, Finish, Close }

private fun actionSetOf(stage: InstallerStage): ActionSet = when (stage) {
    is InstallerStage.Installing -> ActionSet.None
    is InstallerStage.InstallingModule -> if (stage.isFinished) ActionSet.Close else ActionSet.None
    is InstallerStage.InstallPrepare -> ActionSet.CancelInstall
    is InstallerStage.Preparing, is InstallerStage.Resolving, is InstallerStage.Analysing -> ActionSet.CancelOnly
    else -> ActionSet.Finish
}

@Composable
private fun FullScreenActionRow(stage: InstallerStage, onClose: () -> Unit, onInstall: () -> Unit) {
    val enabled = LocalFullScreenActionsEnabled.current
    AnimatedContent(
        targetState = actionSetOf(stage),
        modifier = Modifier.fillMaxSize(),
        contentKey = { it },
        // Buttons swap by fading in place: the row keeps its height and its slots, so the page
        // never reflows when the primary action changes meaning.
        transitionSpec = {
            fadeIn(tween(durationMillis = 180)) togetherWith fadeOut(tween(durationMillis = 120))
        },
        label = "FullScreenActions",
    ) { actions ->
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = FullScreenHorizontalPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (actions) {
                ActionSet.CancelInstall -> {
                    SecondaryAction(stringResource(R.string.cancel), onClose, enabled)
                    PrimaryAction(stringResource(R.string.install), onInstall, enabled)
                }

                ActionSet.CancelOnly -> SecondaryAction(stringResource(R.string.cancel), onClose, enabled)

                ActionSet.Finish -> PrimaryAction(stringResource(R.string.finish), onClose, enabled)

                ActionSet.Close -> PrimaryAction(stringResource(R.string.close), onClose, enabled)

                ActionSet.None -> Unit
            }
        }
    }
}

@Composable
private fun RowScope.PrimaryAction(text: String, onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColorsPrimary(),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text = text, style = COUITheme.textStyles.buttonL, maxLines = 1)
    }
}

@Composable
private fun RowScope.SecondaryAction(text: String, onClick: () -> Unit, enabled: Boolean) {
    TextButton(
        text = text,
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.textButtonColors(),
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
