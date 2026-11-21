// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import android.content.ClipData
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.engine.model.install.sourcePath
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.coui.installer.components.AdaptiveInfoRow
import com.rosan.installer.ui.page.coui.installer.components.AppInfoSlot
import com.rosan.installer.ui.page.coui.installer.components.AppInfoState
import com.rosan.installer.ui.page.coui.installer.components.rememberCouiPreparedPackageState
import com.rosan.installer.ui.page.coui.widgets.CouiInfoChipGroup
import com.rosan.installer.ui.page.coui.widgets.CouiInstallerTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiNavigationItemWidget
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.theme.InstallerTheme.useCouiMonet
import com.rosan.installer.ui.theme.couiSheetCardColors
import com.rosan.installer.ui.util.formatSize
import com.rosan.installer.ui.util.isGestureNavigation
import com.rosan.installer.util.toast
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InstallPrepareContent(
    viewModel: InstallerViewModel,
    appInfo: AppInfoState,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onLongInstall: (() -> Unit)?,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val settings = uiState.viewSettings

    var isExpanded by remember { mutableStateOf(false) }

    val prepared = rememberCouiPreparedPackageState(uiState)
    if (prepared == null) {
        val statusText = if (uiState.analysisResults.none { it.packageName == uiState.currentPackageName }) {
            stringResource(id = R.string.loading)
        } else {
            "No main app entity found"
        }
        LoadingContent(statusText = statusText)
        return
    }

    val currentPackage = prepared.currentPackage
    val selectedEntities = prepared.selectedEntities
    val rawBaseEntity = prepared.rawBaseEntity
    val primaryEntity = prepared.primaryEntity
    val entityToInstall = prepared.entityToInstall
    val totalSelectedSize = prepared.totalSelectedSize
    val isSplitUpdateMode = prepared.isSplitUpdateMode
    val notices = prepared.installStateResult.notices
    val buttonTextId = prepared.installStateResult.buttonTextId

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            AppInfoSlot(
                appInfo = appInfo,
                onIconClick = if (settings.labTapIconToShare) {
                    {
                        // Trigger the share action using the already resolved primaryEntity
                        viewModel.dispatch(InstallerViewAction.ShareApp(primaryEntity))
                    }
                } else {
                    null
                },
            )
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            CouiInfoChipGroup(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                notices = notices,
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = couiSheetCardColors(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (primaryEntity) {
                        is AppEntity.BaseEntity -> {
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = primaryEntity.versionName,
                                oldValue = currentPackage.installedAppInfo?.versionName,
                                isUninstalled = currentPackage.installedAppInfo?.isUninstalled ?: false,
                                isArchived = currentPackage.installedAppInfo?.isArchived ?: false,
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = primaryEntity.versionCode.toString(),
                                oldValue = currentPackage.installedAppInfo?.versionCode?.toString(),
                                isUninstalled = currentPackage.installedAppInfo?.isUninstalled ?: false,
                                isArchived = currentPackage.installedAppInfo?.isArchived ?: false,
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )
                            SDKComparison(
                                entityToInstall = primaryEntity,
                                preInstallAppInfo = currentPackage.installedAppInfo,
                                displaySDK = config.displaySdk,
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )

                            AnimatedVisibility(visible = config.displaySize && primaryEntity.size > 0) {
                                val oldSize = currentPackage.installedAppInfo?.packageSize ?: 0L
                                val oldSizeStr = if (oldSize > 0 && !isSplitUpdateMode) oldSize.formatSize() else null
                                val newSizeStr = totalSelectedSize.formatSize()

                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = newSizeStr,
                                    oldValue = oldSizeStr,
                                    hideIdenticalComparison = settings.hideIdenticalComparisons,
                                )
                            }

                            val isOppoOrOnePlus = DeviceConfig.currentManufacturer == Manufacturer.OPPO ||
                                DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS
                            val shouldShowOppoRow = isOppoOrOnePlus &&
                                settings.showOPPOSpecial &&
                                primaryEntity.sourceType == DataType.APK &&
                                primaryEntity.minOsdkVersion != null

                            AnimatedVisibility(visible = shouldShowOppoRow) {
                                // At this point, minOsdkVersion is guaranteed to be non-null
                                primaryEntity.minOsdkVersion?.let { version ->
                                    AdaptiveInfoRow(
                                        labelResId = R.string.installer_package_minOsdkVersion_label,
                                        newValue = version,
                                        oldValue = null,
                                    )
                                }
                            }
                        }

                        is AppEntity.ModuleEntity -> {
                            val installedModuleInfo = currentPackage.installedModuleInfo
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_name_label,
                                newValue = primaryEntity.version,
                                oldValue = installedModuleInfo?.version,
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_version_code_label,
                                newValue = primaryEntity.versionCode.toString(),
                                oldValue = installedModuleInfo?.versionCode?.toString(),
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )
                            AnimatedVisibility(visible = config.displaySdk) {
                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_module_author_label,
                                    newValue = primaryEntity.author,
                                    oldValue = null,
                                )
                            }
                            AnimatedVisibility(visible = config.displaySize) {
                                val newSizeStr = totalSelectedSize.formatSize()
                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = newSizeStr,
                                    oldValue = null,
                                )
                            }
                        }

                        is AppEntity.SplitEntity -> {
                            // Split Name
                            AdaptiveInfoRow(
                                labelResId = R.string.installer_split_name_label,
                                newValue = primaryEntity.splitName,
                                oldValue = null,
                            )

                            // SDK Comparison (If splits define min/target SDK)
                            SDKComparison(
                                entityToInstall = primaryEntity,
                                preInstallAppInfo = currentPackage.installedAppInfo,
                                displaySDK = config.displaySdk,
                                hideIdenticalComparison = settings.hideIdenticalComparisons,
                            )

                            // Size
                            AnimatedVisibility(visible = config.displaySize) {
                                val newSizeStr = totalSelectedSize.formatSize()
                                AdaptiveInfoRow(
                                    labelResId = R.string.installer_package_size_label,
                                    newValue = newSizeStr,
                                    oldValue = null, // Don't compare with full app size, meaningless
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = (rawBaseEntity != null) && isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = couiSheetCardColors(),
                ) {
                    // Permissions List
                    if (rawBaseEntity?.permissions?.isNotEmpty() == true) {
                        CouiNavigationItemWidget(
                            title = stringResource(R.string.permission_list),
                            description = stringResource(R.string.permission_list_desc),
                            // insideMargin = PaddingValues(12.dp),
                            onClick = { viewModel.dispatch(InstallerViewAction.ShowCouiPermissionList) },
                        )
                    }

                    // Install Options
                    if (config.authorizer != Authorizer.Dhizuku &&
                        config.authorizer != Authorizer.None
                    ) {
                        CouiNavigationItemWidget(
                            title = stringResource(R.string.config_label_install_options),
                            description = stringResource(R.string.config_label_install_options_desc),
                            // insideMargin = PaddingValues(12.dp),
                            onClick = { viewModel.dispatch(InstallerViewAction.InstallExtendedMenu) },
                        )
                    }

                    // Select Splits
                    val hasSplits = currentPackage.appEntities.size > 1
                    if (hasSplits) {
                        CouiNavigationItemWidget(
                            title = stringResource(R.string.installer_select_split),
                            description = stringResource(R.string.installer_select_split_desc),
                            // insideMargin = PaddingValues(12.dp),
                            onClick = { viewModel.dispatch(InstallerViewAction.InstallChoice) },
                        )
                    }
                }
            }
        }

        val isInvalidSplitInstall = prepared.isInvalidSplitInstall

        item {
            AnimatedVisibility(
                visible = isInvalidSplitInstall,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                CouiInstallerTipCard(text = stringResource(R.string.installer_splits_invalid_tip))
            }
        }

        item {
            AnimatedVisibility(
                visible = isSplitUpdateMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                CouiInstallerTipCard(text = stringResource(R.string.installer_splits_only_tip))
            }
        }

        item {
            AnimatedVisibility(
                visible = (primaryEntity is AppEntity.ModuleEntity) &&
                    primaryEntity.description.isNotBlank() &&
                    config.displaySdk,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                CouiInstallerTipCard((primaryEntity as AppEntity.ModuleEntity).description)
            }
        }

        // Display lab info card if either setting is enabled
        item {
            AnimatedVisibility(
                visible = !isExpanded && (settings.labShowFilePath || settings.labShowInstallInitiator),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = couiSheetCardColors(),
                ) {
                    if (settings.labShowFilePath) {
                        // Safely extract the source path
                        val path = runCatching {
                            val rawPath = primaryEntity.data.sourcePath()
                            // If multiple files are selected (like Splits), show the parent directory
                            if (selectedEntities.size > 1 && rawPath != null) {
                                rawPath.substringBeforeLast("/") + " (Multi-part)"
                            } else {
                                rawPath
                            }
                        }.getOrNull() ?: stringResource(R.string.installer_label_unknown)

                        BasicComponent(
                            title = stringResource(R.string.lab_show_apk_path_label),
                            summary = path,
                            onClick = {
                                scope.launch {
                                    val clipData = ClipData.newPlainText("APK Path", path)
                                    clipboard.setClipEntry(clipData.toClipEntry())
                                    context.toast(R.string.copied_format, path)
                                }
                            },
                        )
                    }

                    if (settings.labShowInstallInitiator) {
                        // Use the runtime field initiatorPackageName from ConfigModel
                        val initiator = uiState.initiatorAppLabel ?: stringResource(R.string.installer_label_unknown)

                        BasicComponent(
                            title = stringResource(R.string.lab_show_install_initiator_label),
                            summary = initiator,
                            onClick = {
                                scope.launch {
                                    val clipData = ClipData.newPlainText("Install Initiator", initiator)
                                    clipboard.setClipEntry(clipData.toClipEntry())
                                    context.toast(R.string.copied_format, initiator)
                                }
                            },
                        )
                    }
                }
            }
        }

        val canInstall = prepared.canInstall

        // Even if we can't install (e.g. because Base is deselected), we might want to expand the menu to fix the selection.
        // We show the button if rawBaseEntity exists (Bundle/APK) and settings allow it.
        val showExpandButton = rawBaseEntity != null && settings.showExtendedMenu

        item {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            var hasLongPressed by remember { mutableStateOf(false) }
            val viewConfiguration = LocalViewConfiguration.current
            val hapticFeedback = LocalHapticFeedback.current

            // Handle the long press delay logic
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    hasLongPressed = false
                    // Wait for the system's default long press duration
                    delay(viewConfiguration.longPressTimeoutMillis.milliseconds)
                    onLongInstall?.let { action ->
                        hasLongPressed = true
                        // Perform haptic feedback immediately before the action
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Trigger the long press action
                        action()
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showExpandButton) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        text = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.more),
                        colors = ButtonDefaults.textButtonColors(
                            color = if (useCouiMonet) COUITheme.colorScheme.secondaryContainer else COUITheme.colorScheme.secondaryVariant,
                            textColor = COUITheme.colorScheme.labelPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    TextButton(
                        onClick = onCancel,
                        text = stringResource(R.string.cancel),
                        colors = ButtonDefaults.textButtonColors(
                            color = if (useCouiMonet) COUITheme.colorScheme.secondaryContainer else COUITheme.colorScheme.secondaryVariant,
                            textColor = COUITheme.colorScheme.labelPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(
                    onClick = {
                        // Only trigger normal install if long press didn't happen
                        if (!hasLongPressed) onInstall()
                        hasLongPressed = false
                    },
                    enabled = canInstall,
                    text = stringResource(buttonTextId),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                    interactionSource = interactionSource,
                )
            }
        }
    }
}

@Composable
private fun SDKComparison(
    entityToInstall: AppEntity,
    preInstallAppInfo: InstalledAppInfo?,
    displaySDK: Boolean,
    hideIdenticalComparison: Boolean,
) {
    AnimatedVisibility(visible = displaySDK) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Target SDK
            entityToInstall.targetSdk?.let { newTargetSdk ->
                SdkInfoRow(
                    labelResId = R.string.installer_package_target_sdk_label,
                    newSdk = newTargetSdk,
                    oldSdk = preInstallAppInfo?.targetSdk?.toString(),
                    isArchived = preInstallAppInfo?.isArchived ?: false,
                    hideIdenticalComparison = hideIdenticalComparison,
                    type = "target",
                )
            }
            // Min SDK
            entityToInstall.minSdk?.let { newMinSdk ->
                SdkInfoRow(
                    labelResId = R.string.installer_package_min_sdk_label,
                    newSdk = newMinSdk,
                    oldSdk = preInstallAppInfo?.minSdk?.toString(),
                    isUninstalled = preInstallAppInfo?.isUninstalled ?: false,
                    isArchived = preInstallAppInfo?.isArchived ?: false,
                    hideIdenticalComparison = hideIdenticalComparison,
                    type = "min",
                )
            }
        }
    }
}

@Composable
private fun SdkInfoRow(
    @StringRes labelResId: Int,
    newSdk: String,
    oldSdk: String?,
    isUninstalled: Boolean = false,
    isArchived: Boolean = false,
    hideIdenticalComparison: Boolean,
    type: String, // "min" or "target"
) {
    val newSdkInt = newSdk.toIntOrNull()
    val oldSdkInt = oldSdk?.toIntOrNull()
    val showComparison = oldSdkInt != null && newSdkInt != null &&
        (!hideIdenticalComparison || newSdkInt != oldSdkInt)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Label to the left.
        Text(
            text = stringResource(labelResId),
            style = COUITheme.textStyles.bodyM,
            fontWeight = FontWeight.SemiBold,
        )

        // Label to the right.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showComparison) {
                val oldText = when {
                    isUninstalled -> stringResource(R.string.old_version_uninstalled)
                    isArchived -> stringResource(R.string.old_version_archived)
                    else -> oldSdk
                }

                Text(text = oldText, style = COUITheme.textStyles.bodyM)

                Icon(
                    imageVector = AppIcons.ArrowIndicator,
                    contentDescription = "to",
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp),
                )

                Text(text = newSdk, style = COUITheme.textStyles.bodyM)
            } else {
                val isIncompatible = type == "min" && newSdkInt != null && newSdkInt > android.os.Build.VERSION.SDK_INT
                val color = if (isIncompatible) COUITheme.colorScheme.error else Color.Unspecified

                Text(text = newSdk, color = color, style = COUITheme.textStyles.bodyM)
            }
        }
    }
}
