// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.fullscreen

import android.content.ClipData
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.engine.model.install.sourcePath
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.engine.model.packageinfo.InstalledModuleInfo
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.ui.icons.AppCOUIIcons
import com.rosan.installer.ui.page.coui.widgets.CouiInfoChipGroup
import com.rosan.installer.ui.page.coui.widgets.CouiInstallerTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsCard
import com.rosan.installer.ui.page.main.widget.chip.NoticeModel
import com.rosan.installer.ui.theme.colorOsSpring
import com.rosan.installer.ui.util.formatSize
import com.rosan.installer.util.toast
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

private data class DetailEntry(
    val label: String,
    val value: String,
    val valueColor: Color = Color.Unspecified,
    val onClick: (() -> Unit)? = null,
)

private data class NavigationEntry(
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val onClick: () -> Unit,
    val expanded: Boolean = false,
    val expandedContent: (@Composable () -> Unit)? = null,
)

/** App/module/split details, notices, and navigation destinations for the full-screen prepare step. */
@Composable
fun CouiFullScreenPrepareContent(
    entity: AppEntity?,
    sourceLabel: String?,
    permissions: List<String>,
    sensitivePermissionCount: Int,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    notices: List<NoticeModel> = emptyList(),
    installedAppInfo: InstalledAppInfo? = null,
    installedModuleInfo: InstalledModuleInfo? = null,
    totalSelectedSize: Long = entity?.size ?: 0L,
    selectedEntityCount: Int = 1,
    displaySdk: Boolean = true,
    displaySize: Boolean = true,
    hideIdenticalComparisons: Boolean = true,
    showOppoSpecial: Boolean = false,
    labShowFilePath: Boolean = false,
    labShowInstallInitiator: Boolean = true,
    isSplitUpdateMode: Boolean = false,
    isInvalidSplitInstall: Boolean = false,
    installOptionsContent: (@Composable () -> Unit)? = null,
    onOpenSplitChoice: (() -> Unit)? = null,
    contentTopPadding: Dp = 0.dp,
    contentBottomPadding: Dp = 20.dp,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val actionsEnabled = LocalFullScreenActionsEnabled.current
    var permissionsExpanded by rememberSaveable { mutableStateOf(false) }
    var installOptionsExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = actionsEnabled && (permissionsExpanded || installOptionsExpanded)) {
        permissionsExpanded = false
        installOptionsExpanded = false
    }

    val isOppoOrOnePlus = DeviceConfig.currentManufacturer == Manufacturer.OPPO ||
        DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS

    val oldArchived = stringResource(R.string.old_version_archived)
    val oldUninstalled = stringResource(R.string.old_version_uninstalled)

    fun formatComparison(
        newValue: String,
        oldValue: String?,
        isUninstalled: Boolean = false,
        isArchived: Boolean = false,
    ): String {
        val showComparison = oldValue != null && (!hideIdenticalComparisons || newValue != oldValue)
        if (!showComparison) return newValue
        val oldText = when {
            isArchived -> oldArchived
            isUninstalled -> if (oldValue.isNullOrEmpty()) oldUninstalled else oldValue
            else -> oldValue.orEmpty()
        }
        return "$oldText → $newValue"
    }

    val details = buildList {
        when (entity) {
            is AppEntity.BaseEntity -> {
                val isUninstalled = installedAppInfo?.isUninstalled ?: false
                val isArchived = installedAppInfo?.isArchived ?: false
                add(
                    DetailEntry(
                        label = stringResource(R.string.installer_version_name_label),
                        value = formatComparison(
                            newValue = entity.versionName,
                            oldValue = installedAppInfo?.versionName,
                            isUninstalled = isUninstalled,
                            isArchived = isArchived,
                        ),
                    ),
                )
                add(
                    DetailEntry(
                        label = stringResource(R.string.installer_version_code_label),
                        value = formatComparison(
                            newValue = entity.versionCode.toString(),
                            oldValue = installedAppInfo?.versionCode?.toString(),
                            isUninstalled = isUninstalled,
                            isArchived = isArchived,
                        ),
                    ),
                )
                if (displaySize && totalSelectedSize > 0L) {
                    val oldSize = installedAppInfo?.packageSize ?: 0L
                    val oldSizeStr = if (oldSize > 0L && !isSplitUpdateMode) oldSize.formatSize() else null
                    val newSizeStr = totalSelectedSize.formatSize()
                    add(
                        DetailEntry(
                            label = stringResource(R.string.installer_detail_size),
                            value = formatComparison(newValue = newSizeStr, oldValue = oldSizeStr),
                        ),
                    )
                }
                if (displaySdk) {
                    entity.targetSdk?.takeIf { it.isNotBlank() }?.let { sdk ->
                        add(
                            DetailEntry(
                                label = stringResource(R.string.installer_detail_target_sdk),
                                value = formatComparison(
                                    newValue = sdk,
                                    oldValue = installedAppInfo?.targetSdk?.toString(),
                                    isArchived = isArchived,
                                ),
                            ),
                        )
                    }
                    entity.minSdk?.takeIf { it.isNotBlank() }?.let { sdk ->
                        val minInt = sdk.toIntOrNull()
                        val incompatible = minInt != null && minInt > Build.VERSION.SDK_INT
                        add(
                            DetailEntry(
                                label = stringResource(R.string.installer_detail_min_sdk),
                                value = formatComparison(
                                    newValue = sdk,
                                    oldValue = installedAppInfo?.minSdk?.toString(),
                                    isUninstalled = isUninstalled,
                                    isArchived = isArchived,
                                ),
                                valueColor = if (incompatible) COUITheme.colorScheme.error else Color.Unspecified,
                            ),
                        )
                    }
                }
                entity.arch?.toString()?.takeIf { it.isNotBlank() && it != "NONE" }?.let {
                    add(DetailEntry(stringResource(R.string.installer_detail_arch), it))
                }
                if (isOppoOrOnePlus && showOppoSpecial && entity.sourceType == DataType.APK) {
                    entity.minOsdkVersion?.takeIf { it.isNotBlank() }?.let { osdk ->
                        add(DetailEntry(stringResource(R.string.installer_package_minOsdkVersion_label), osdk))
                    }
                }
                entity.signatureHash?.let {
                    add(DetailEntry(stringResource(R.string.installer_detail_signature), it.toShortFingerprint()))
                }
            }

            is AppEntity.ModuleEntity -> {
                add(
                    DetailEntry(
                        label = stringResource(R.string.installer_version_name_label),
                        value = formatComparison(
                            newValue = entity.version,
                            oldValue = installedModuleInfo?.version,
                        ),
                    ),
                )
                add(
                    DetailEntry(
                        label = stringResource(R.string.installer_version_code_label),
                        value = formatComparison(
                            newValue = entity.versionCode.toString(),
                            oldValue = installedModuleInfo?.versionCode?.toString(),
                        ),
                    ),
                )
                if (displaySdk && entity.author.isNotBlank()) {
                    add(DetailEntry(stringResource(R.string.installer_module_author_label), entity.author))
                }
                if (displaySize && totalSelectedSize > 0L) {
                    add(DetailEntry(stringResource(R.string.installer_detail_size), totalSelectedSize.formatSize()))
                }
            }

            is AppEntity.SplitEntity -> {
                add(DetailEntry(stringResource(R.string.installer_split_name_label), entity.splitName))
                if (displaySdk) {
                    entity.targetSdk?.takeIf { it.isNotBlank() }?.let { sdk ->
                        add(
                            DetailEntry(
                                label = stringResource(R.string.installer_detail_target_sdk),
                                value = formatComparison(
                                    newValue = sdk,
                                    oldValue = installedAppInfo?.targetSdk?.toString(),
                                    isArchived = installedAppInfo?.isArchived ?: false,
                                ),
                            ),
                        )
                    }
                    entity.minSdk?.takeIf { it.isNotBlank() }?.let { sdk ->
                        val minInt = sdk.toIntOrNull()
                        val incompatible = minInt != null && minInt > Build.VERSION.SDK_INT
                        add(
                            DetailEntry(
                                label = stringResource(R.string.installer_detail_min_sdk),
                                value = formatComparison(
                                    newValue = sdk,
                                    oldValue = installedAppInfo?.minSdk?.toString(),
                                    isUninstalled = installedAppInfo?.isUninstalled ?: false,
                                    isArchived = installedAppInfo?.isArchived ?: false,
                                ),
                                valueColor = if (incompatible) COUITheme.colorScheme.error else Color.Unspecified,
                            ),
                        )
                    }
                }
                if (displaySize && totalSelectedSize > 0L) {
                    add(DetailEntry(stringResource(R.string.installer_detail_size), totalSelectedSize.formatSize()))
                }
            }

            else -> Unit
        }

        if (labShowInstallInitiator) {
            sourceLabel?.takeIf { it.isNotBlank() }?.let {
                add(DetailEntry(stringResource(R.string.installer_detail_source), it))
            }
        }

        if (labShowFilePath && entity != null) {
            val path = runCatching {
                val rawPath = entity.data.sourcePath()
                if (selectedEntityCount > 1 && rawPath != null) {
                    rawPath.substringBeforeLast("/") + " (Multi-part)"
                } else {
                    rawPath
                }
            }.getOrNull() ?: stringResource(R.string.installer_label_unknown)
            add(
                DetailEntry(
                    label = stringResource(R.string.lab_show_apk_path_label),
                    value = path,
                    onClick = {
                        scope.launch {
                            val clipData = ClipData.newPlainText("APK Path", path)
                            clipboard.setClipEntry(clipData.toClipEntry())
                            context.toast(R.string.copied_format, path)
                        }
                    },
                ),
            )
        }
    }

    val permissionCount = permissions.size
    val optionsContent = installOptionsContent
    val navigationEntries = buildList {
        if (permissionCount > 0) {
            add(
                NavigationEntry(
                    icon = AppCOUIIcons.Info,
                    title = stringResource(R.string.permission_list),
                    summary = if (sensitivePermissionCount > 0) {
                        stringResource(
                            R.string.installer_permission_entry_summary_sensitive,
                            permissionCount,
                            sensitivePermissionCount,
                        )
                    } else {
                        stringResource(R.string.installer_permission_entry_summary, permissionCount)
                    },
                    onClick = { permissionsExpanded = !permissionsExpanded },
                    expanded = permissionsExpanded,
                    expandedContent = {
                        CouiFullScreenPermissionContent(permissions = permissions, inline = true)
                    },
                ),
            )
        }
        if (optionsContent != null) {
            add(
                NavigationEntry(
                    icon = AppCOUIIcons.Tune,
                    title = stringResource(R.string.config_label_install_options),
                    summary = stringResource(R.string.config_label_install_options_desc),
                    onClick = { installOptionsExpanded = !installOptionsExpanded },
                    expanded = installOptionsExpanded,
                    expandedContent = optionsContent,
                ),
            )
        }
        if (onOpenSplitChoice != null) {
            add(
                NavigationEntry(
                    icon = AppCOUIIcons.Settings,
                    title = stringResource(R.string.installer_select_split),
                    summary = stringResource(R.string.installer_select_split_desc),
                    onClick = onOpenSplitChoice,
                ),
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(top = contentTopPadding, bottom = contentBottomPadding),
    ) {
        if (notices.isNotEmpty()) {
            CouiInfoChipGroup(
                modifier = Modifier.padding(horizontal = FullScreenHorizontalPadding, vertical = 6.dp),
                notices = notices,
            )
        }

        if (isInvalidSplitInstall) {
            Column(Modifier.padding(horizontal = FullScreenHorizontalPadding)) {
                CouiInstallerTipCard(text = stringResource(R.string.installer_splits_invalid_tip))
            }
        }
        if (isSplitUpdateMode) {
            Column(Modifier.padding(horizontal = FullScreenHorizontalPadding)) {
                CouiInstallerTipCard(text = stringResource(R.string.installer_splits_only_tip))
            }
        }
        if (entity is AppEntity.ModuleEntity && entity.description.isNotBlank() && displaySdk) {
            Column(Modifier.padding(horizontal = FullScreenHorizontalPadding)) {
                CouiInstallerTipCard(text = entity.description)
            }
        }

        if (details.isNotEmpty()) {
            SectionTitle(stringResource(R.string.installer_detail_title))
            CouiSettingsCard(Modifier.fillMaxWidth().padding(horizontal = FullScreenHorizontalPadding)) {
                details.forEachIndexed { index, entry ->
                    DetailRow(entry = entry)
                    if (index != details.lastIndex) {
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                    }
                }
            }
        }

        if (navigationEntries.isNotEmpty()) {
            Spacer(Modifier.size(12.dp))
            CouiSettingsCard(Modifier.fillMaxWidth().padding(horizontal = FullScreenHorizontalPadding)) {
                navigationEntries.forEachIndexed { index, entry ->
                    NavigationRow(
                        entry = entry,
                        enabled = actionsEnabled,
                    )
                    entry.expandedContent?.let { expandedContent ->
                        AnimatedVisibility(
                            visible = entry.expanded,
                            enter = fadeIn(tween(durationMillis = 160)) +
                                expandVertically(colorOsSpring(responseSeconds = 0.28f)),
                            exit = fadeOut(tween(durationMillis = 120)) +
                                shrinkVertically(tween(durationMillis = 160)),
                        ) {
                            Column {
                                HorizontalDivider(Modifier.padding(start = 52.dp))
                                expandedContent()
                            }
                        }
                    }
                    if (index != navigationEntries.lastIndex) {
                        HorizontalDivider(Modifier.padding(start = 52.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = COUITheme.textStyles.bodyM,
        color = COUITheme.colorScheme.labelSecondaryVariant,
        modifier = Modifier.padding(
            start = FullScreenHorizontalPadding,
            end = FullScreenHorizontalPadding,
            top = 4.dp,
            bottom = 8.dp,
        ),
    )
}

@Composable
private fun DetailRow(entry: DetailEntry) {
    val rowModifier = if (entry.onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = entry.label,
            style = COUITheme.textStyles.headlineXS,
            color = COUITheme.colorScheme.labelPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = entry.value,
            style = COUITheme.textStyles.bodyM,
            color = if (entry.valueColor == Color.Unspecified) {
                COUITheme.colorScheme.labelSecondaryVariant
            } else {
                entry.valueColor
            },
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.6f),
        )
    }
}

@Composable
private fun NavigationRow(
    entry: NavigationEntry,
    enabled: Boolean,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = when {
            entry.expandedContent == null -> 180f
            entry.expanded -> 90f
            else -> 270f
        },
        animationSpec = tween(durationMillis = 160),
        label = "FullScreenNavigationChevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = entry.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = COUITheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = COUITheme.textStyles.headlineXS,
                color = COUITheme.colorScheme.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.summary,
                style = COUITheme.textStyles.bodyM,
                color = COUITheme.colorScheme.labelSecondaryVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = AppCOUIIcons.Back,
            contentDescription = null,
            tint = COUITheme.colorScheme.labelTertiary,
            modifier = Modifier.size(16.dp).rotate(chevronRotation),
        )
    }
}

private fun String.toShortFingerprint(): String = if (length <= 14) this else "${take(6)}…${takeLast(4)}"
