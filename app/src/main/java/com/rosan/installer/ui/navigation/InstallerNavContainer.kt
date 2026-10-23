// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.ui.animation.predictiveback.installerNavTransition
import com.rosan.installer.ui.page.main.settings.SettingsSharedViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyPage
import com.rosan.installer.ui.page.main.settings.config.edit.EditPage
import com.rosan.installer.ui.page.main.settings.home.installer.DefaultInstallerPage
import com.rosan.installer.ui.page.main.settings.home.priv.PrivPage
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutPage
import com.rosan.installer.ui.page.main.settings.preferred.about.OpenSourceLicensePage
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.DialogSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabPage
import com.rosan.installer.ui.page.main.settings.preferred.network.NetworkPage
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.coui.settings.config.apply.CouiApplyPage
import com.rosan.installer.ui.page.coui.settings.config.edit.CouiEditPage
import com.rosan.installer.ui.page.coui.settings.home.installer.CouiDefaultInstallerPage
import com.rosan.installer.ui.page.coui.settings.home.priv.CouiPrivPage
import com.rosan.installer.ui.page.coui.settings.preferred.about.CouiAboutPage
import com.rosan.installer.ui.page.coui.settings.preferred.about.ossLicensePage.CouiOpenSourceLicensePage
import com.rosan.installer.ui.page.coui.settings.preferred.installer.CouiInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.coui.settings.preferred.installer.authorizer.CouiAuthorizerCustPage
import com.rosan.installer.ui.page.coui.settings.preferred.installer.dialog.CouiDialogSettingsPage
import com.rosan.installer.ui.page.coui.settings.preferred.installer.notification.CouiNotificationSettingsPage
import com.rosan.installer.ui.page.coui.settings.preferred.lab.CouiLabPage
import com.rosan.installer.ui.page.coui.settings.preferred.network.CouiNetworkPage
import com.rosan.installer.ui.page.coui.settings.preferred.theme.COUIThemeSettingsPage
import com.rosan.installer.ui.page.coui.settings.preferred.uninstaller.CouiUninstallerGlobalSettingsPage
import com.rosan.installer.ui.util.rememberDeviceCornerRadius
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import io.github.suqi8.coui.kmp.theme.COUITheme

@Composable
fun InstallerNavContainer(uiState: ThemeState) {
    val sharedViewModel: SettingsSharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity,
    )

    val backStack = rememberNavBackStack<Route>(Route.Main)
    val navigator = remember(backStack) { Navigator(backStack) }
    val onBack = remember(navigator) { { navigator.pop() } }
    val useBlur = uiState.useBlur

    CompositionLocalProvider(
        LocalNavigator provides navigator,
    ) {
        val navCornerRadius = rememberDeviceCornerRadius(defaultRadius = 0.dp)
        val roundAllCorners = uiState.predictiveBackAnimation == PredictiveBackAnimation.AOSP ||
            uiState.predictiveBackAnimation == PredictiveBackAnimation.Scale ||
            uiState.predictiveBackAnimation == PredictiveBackAnimation.Classic
        val backdropColor = if (uiState.useCoui) {
            COUITheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
        val effects = remember(navCornerRadius, roundAllCorners, backdropColor) {
            NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = if (roundAllCorners && navCornerRadius <= 0.dp) 32.dp else navCornerRadius,
                cornerClipMode = if (roundAllCorners) NavCornerClipMode.All else NavCornerClipMode.Leading,
                dimAmount = 0.5f,
                backdropColor = backdropColor,
                blockInputDuringTransition = false,
            )
        }
        val transition = remember(uiState.predictiveBackAnimation, uiState.predictiveBackExitDirection) {
            installerNavTransition(
                animation = uiState.predictiveBackAnimation,
                exitDirection = uiState.predictiveBackExitDirection,
            )
        }
        val swipeBackDirection = when (LocalLayoutDirection.current) {
            LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
            LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
        }
        val interceptPredictiveBack =
            uiState.predictiveBackAnimation == PredictiveBackAnimation.None && backStack.size > 1

        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            transition = transition,
            effects = effects,
        ) {
            entry<Route.Main> {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiMainPageWrapper(uiState, sharedViewModel)
                    } else {
                        Material3MainPageWrapper(uiState, sharedViewModel)
                    }
                }
            }
            entry<Route.EditConfig>(swipeDismiss = NavSwipeDirection.None) { key ->
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    val id = key.id
                    if (uiState.useCoui) {
                        CouiEditPage(
                            id = if (id != -1L) id else null,
                            useBlur = useBlur,
                        )
                    } else {
                        EditPage(
                            id = if (id != -1L) id else null,
                            useBlur = useBlur,
                        )
                    }
                }
            }
            entry<Route.ApplyConfig>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    val id = key.id
                    if (uiState.useCoui) {
                        CouiApplyPage(id, useBlur)
                    } else {
                        ApplyPage(id, useBlur)
                    }
                }
            }
            entry<Route.About>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiAboutPage(useBlur)
                    } else {
                        AboutPage(useBlur)
                    }
                }
            }
            entry<Route.OpenSourceLicense>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiOpenSourceLicensePage(useBlur)
                    } else {
                        OpenSourceLicensePage(useBlur)
                    }
                }
            }
            entry<Route.Theme>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        COUIThemeSettingsPage()
                    } else {
                        ThemeSettingsPage()
                    }
                }
            }
            entry<Route.Network>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiNetworkPage(useBlur)
                    } else {
                        NetworkPage(useBlur)
                    }
                }
            }
            entry<Route.InstallerGlobal>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiInstallerGlobalSettingsPage(useBlur)
                    } else {
                        InstallerGlobalSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.AuthorizerCust>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiAuthorizerCustPage(useBlur)
                    } else {
                        AuthorizerCustPage(useBlur)
                    }
                }
            }
            entry<Route.DialogSettings>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiDialogSettingsPage(useBlur)
                    } else {
                        DialogSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.NotificationSettings>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiNotificationSettingsPage(useBlur)
                    } else {
                        NotificationSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.UninstallerGlobal>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiUninstallerGlobalSettingsPage(useBlur)
                    } else {
                        UninstallerGlobalSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.Lab>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiLabPage(useBlur)
                    } else {
                        LabPage(useBlur)
                    }
                }
            }
            entry<Route.DefaultInstaller>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiDefaultInstallerPage(useBlur)
                    } else {
                        DefaultInstallerPage(useBlur)
                    }
                }
            }
            entry<Route.Priv>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    if (uiState.useCoui) {
                        CouiPrivPage(useBlur)
                    } else {
                        PrivPage(useBlur)
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallerNavEntry(interceptPredictiveBack: Boolean, onBack: () -> Unit, content: @Composable () -> Unit) {
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = interceptPredictiveBack,
        onBackCompleted = onBack,
    )
    content()
}
