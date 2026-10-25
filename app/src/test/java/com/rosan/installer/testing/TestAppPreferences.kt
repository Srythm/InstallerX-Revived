// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.testing

import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.BiometricAuthMode
import com.rosan.installer.domain.settings.model.config.NetworkSourceMode
import com.rosan.installer.domain.settings.model.preferences.AppPreferences
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.HttpProfile
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackExitDirection
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode

/**
 * A neutral [AppPreferences] for tests that need a real snapshot instead of a
 * proxied per-key stub.
 *
 * Every value is the inert one: no blacklists, no lab switches, no derived
 * authorizers. Pass [checkSourceStamp] = true only when the test exercises the
 * source-stamp verification path.
 */
fun testAppPreferences(checkSourceStamp: Boolean = false) = AppPreferences(
    authorizer = Authorizer.None,
    alwaysUseRootInSystem = false,
    customizeAuthorizer = "",
    hideIdenticalInstallComparisons = false,
    showDialogInstallExtendedMenu = false,
    showSmartSuggestion = false,
    disableNotificationForDialogInstall = false,
    showDialogWhenPressingNotification = false,
    closeSessionCountDown = 0,
    notificationSuccessAutoClearSeconds = 0,
    versionCompareInSingleLine = false,
    sdkCompareInMultiLine = false,
    showOPPOSpecial = false,
    checkAppSignature = false,
    checkSourceStamp = checkSourceStamp,
    checkSplitPackageSignatures = false,
    showSignatureInfoOnMatch = false,
    showSignatureDetails = false,
    installerRequireBiometricAuth = BiometricAuthMode.entries.first(),
    uninstallerRequireBiometricAuth = false,
    showLiveActivity = false,
    useMiIsland = false,
    useMiIslandBypassRestriction = false,
    useMiIslandOuterGlow = false,
    useMiIslandBlockingIntervalMs = 0,
    autoSilentInstall = false,
    longClickBackgroundInstall = false,
    tryMultipleAuthorizersOnInstall = false,
    smartAuthorizerCandidates = emptyList(),
    showCouiUI = false,
    preferSystemIcon = false,
    showLauncherIcon = false,
    userSetLSPosedActive = false,
    detectXposedModule = false,
    quickOpenLSPosed = false,
    managedInstallerPackages = emptyList(),
    managedBlacklistPackages = emptyList(),
    managedSharedUserIdBlacklist = emptyList(),
    managedSharedUserIdExemptedPackages = emptyList(),
    uninstallFlags = 0,
    networkSourceMode = NetworkSourceMode.entries.first(),
    networkSourceModeWarningAcknowledged = false,
    allowInternetAccess = false,
    githubUpdateChannel = GithubUpdateChannel.entries.first(),
    customGithubProxyUrl = "",
    labRootEnableModuleFlash = false,
    labRootShowModuleArt = false,
    labRootMode = RootMode.entries.first(),
    labHttpProfile = HttpProfile.entries.first(),
    labHttpSaveFile = false,
    labSetInstallRequester = false,
    labTapIconToShare = false,
    labShowFilePath = false,
    labShowInstallInitiator = false,
    labInstallWithoutUserAction = false,
    labRespectPlatformInstallPolicy = false,
    labEnableAbiOverride = false,
    labTryDifferentArchitecture = false,
    enableFileLogging = false,
    themeMode = ThemeMode.entries.first(),
    paletteStyle = PaletteStyle.entries.first(),
    colorSpec = ThemeColorSpec.entries.first(),
    useDynamicColor = false,
    useCouiMonet = false,
    useAppleFloatingBar = false,
    seedColorInt = 0,
    wallpaperSeedColorInt = 0,
    useDynColorFollowPkgIcon = false,
    useDynColorFollowPkgIconForLiveActivity = false,
    useBlur = false,
    predictiveBackAnimation = PredictiveBackAnimation.entries.first(),
    predictiveBackExitDirection = PredictiveBackExitDirection.entries.first(),
)
