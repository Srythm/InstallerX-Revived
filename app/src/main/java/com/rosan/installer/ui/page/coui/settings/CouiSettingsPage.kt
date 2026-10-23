// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.library.FloatingBottomBar
import com.rosan.installer.ui.library.FloatingBottomBarDefaults
import com.rosan.installer.ui.library.FloatingBottomBarItem
import com.rosan.installer.ui.library.FloatingBottomBarMode
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.MainPagerState
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.coui.settings.config.all.CouiAllPage
import com.rosan.installer.ui.page.coui.settings.history.CouiHistoryPage
import com.rosan.installer.ui.page.coui.settings.home.CouiHomePage
import com.rosan.installer.ui.page.coui.settings.preferred.CouiPreferredPage
import io.github.suqi8.coui.kmp.basic.FloatingActionButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.NavigationBar
import io.github.suqi8.coui.kmp.basic.NavigationBarItem
import io.github.suqi8.coui.kmp.basic.NavigationItem
import io.github.suqi8.coui.kmp.basic.NavigationRail
import io.github.suqi8.coui.kmp.basic.NavigationRailDisplayMode
import io.github.suqi8.coui.kmp.basic.NavigationRailItem
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SnackbarHost
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop as CouiLayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop as couiLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Reusable Floating Bottom Bar
 */
@Composable
private fun SettingsFloatingBottomBar(
    mainPagerState: MainPagerState,
    navigationItems: List<NavigationItem>,
    floatingBottomBarMode: FloatingBottomBarMode,
    floatingBackdrop: CouiLayerBackdrop,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        FloatingBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding(),
                ),
            selectedIndex = { mainPagerState.selectedPage },
            onSelected = { index ->
                mainPagerState.animateToPage(index)
            },
            backdrop = floatingBackdrop,
            tabsCount = navigationItems.size,
            mode = floatingBottomBarMode,
            colors = FloatingBottomBarDefaults.colors(
                containerColor = COUITheme.colorScheme.surfaceContainer,
                indicatorColor = COUITheme.colorScheme.primary,
                contentColor = COUITheme.colorScheme.onSurface,
            ),
        ) {
            navigationItems.forEachIndexed { index, item ->
                FloatingBottomBarItem(
                    onClick = { mainPagerState.animateToPage(index) },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                    )
                }
            }
        }
    }
}

/**
 * Compact Screen Layout (Portrait/Phone)
 */
@Composable
fun SettingsCompactLayout(
    configCount: Int,
    mainPagerState: MainPagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    useFloatingBottomBar: Boolean,
    floatingBottomBarMode: FloatingBottomBarMode,
    floatingBackdrop: CouiLayerBackdrop?,
    couiBackdrop: CouiLayerBackdrop?,
) {
    val navigator = LocalNavigator.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (useFloatingBottomBar && floatingBackdrop != null) {
                SettingsFloatingBottomBar(
                    mainPagerState = mainPagerState,
                    navigationItems = navigationItems,
                    floatingBottomBarMode = floatingBottomBarMode,
                    floatingBackdrop = floatingBackdrop,
                )
            } else if (!useFloatingBottomBar) {
                val blurActive = couiBackdrop != null
                val barColor = if (blurActive) Color.Transparent else COUITheme.colorScheme.surface
                Box(
                    modifier = Modifier
                        .then(
                            if (blurActive) {
                                Modifier.textureBlur(
                                    backdrop = couiBackdrop,
                                    shape = RectangleShape,
                                    blurRadius = 25f,
                                    colors = BlurColors(
                                        blendColors = listOf(
                                            BlendColorEntry(color = COUITheme.colorScheme.surface.copy(0.8f)),
                                        ),
                                    ),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .background(barColor),
                ) {
                    NavigationBar(
                        color = barColor,
                    ) {
                        navigationItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = mainPagerState.selectedPage == index,
                                onClick = {
                                    mainPagerState.animateToPage(index)
                                },
                                icon = item.icon,
                                label = item.label,
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = mainPagerState.selectedPage == 1,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(
                    modifier = Modifier.padding(end = 16.dp),
                    containerColor = if (COUITheme.isDynamicColor) COUITheme.colorScheme.primary else COUITheme.colorScheme.background,
                    shadowElevation = 2.dp,
                    onClick = { navigator.push(Route.EditConfig(-1)) },
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        modifier = Modifier.size(40.dp),
                        contentDescription = stringResource(id = R.string.add),
                        tint = if (COUITheme.isDynamicColor) COUITheme.colorScheme.onPrimary else COUITheme.colorScheme.primary,
                    )
                }
            }
        },
    ) { paddingValues ->
        SettingsPagerContent(
            configCount = configCount,
            mainPagerState = mainPagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            outerPadding = paddingValues,
            useFloatingBottomBar = useFloatingBottomBar,
            floatingBackdrop = floatingBackdrop,
            couiBackdrop = couiBackdrop,
        )
    }
}

/**
 * Wide Screen Layout (Tablet/Landscape)
 */
@Composable
fun SettingsWideScreenLayout(
    configCount: Int,
    mainPagerState: MainPagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    useFloatingBottomBar: Boolean,
    floatingBottomBarMode: FloatingBottomBarMode,
    floatingBackdrop: CouiLayerBackdrop?,
    couiBackdrop: CouiLayerBackdrop?,
) {
    if (useFloatingBottomBar) {
        SettingsWideContent(
            configCount = configCount,
            mainPagerState = mainPagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            useFloatingBottomBar = true,
            floatingBottomBarMode = floatingBottomBarMode,
            floatingBackdrop = floatingBackdrop,
            couiBackdrop = couiBackdrop,
        )
    } else {
        val expandRail = shouldExpandNavigationRail()
        val railMode =
            if (expandRail) NavigationRailDisplayMode.IconAndText else NavigationRailDisplayMode.IconOnly
        val contentWindowInsets = WindowInsets.systemBars.union(
            WindowInsets.displayCutout.exclude(
                WindowInsets.displayCutout.only(WindowInsetsSides.Start),
            ),
        )
        val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Start)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(COUITheme.colorScheme.surface),
        ) {
            NavigationRail(
                mode = railMode,
            ) {
                navigationItems.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = mainPagerState.selectedPage == index,
                        onClick = {
                            mainPagerState.animateToPage(index)
                        },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .consumeWindowInsets(startInsets),
            ) {
                SettingsWideContent(
                    configCount = configCount,
                    mainPagerState = mainPagerState,
                    navigationItems = navigationItems,
                    snackbarHostState = snackbarHostState,
                    useFloatingBottomBar = false,
                    floatingBottomBarMode = FloatingBottomBarMode.None,
                    floatingBackdrop = floatingBackdrop,
                    couiBackdrop = couiBackdrop,
                    contentWindowInsets = contentWindowInsets,
                )
            }
        }
    }
}

@Composable
private fun SettingsWideContent(
    configCount: Int,
    mainPagerState: MainPagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    useFloatingBottomBar: Boolean,
    floatingBottomBarMode: FloatingBottomBarMode,
    floatingBackdrop: CouiLayerBackdrop?,
    couiBackdrop: CouiLayerBackdrop?,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
) {
    val navigator = LocalNavigator.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = contentWindowInsets,
        bottomBar = {
            if (useFloatingBottomBar && floatingBackdrop != null) {
                SettingsFloatingBottomBar(
                    mainPagerState = mainPagerState,
                    navigationItems = navigationItems,
                    floatingBottomBarMode = floatingBottomBarMode,
                    floatingBackdrop = floatingBackdrop,
                )
            }
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = mainPagerState.selectedPage == 1,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(
                    modifier = Modifier.padding(end = 16.dp),
                    containerColor = if (COUITheme.isDynamicColor) COUITheme.colorScheme.primary else COUITheme.colorScheme.background,
                    shadowElevation = 2.dp,
                    onClick = { navigator.push(Route.EditConfig(-1)) },
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        modifier = Modifier.size(40.dp),
                        contentDescription = stringResource(id = R.string.add),
                        tint = if (COUITheme.isDynamicColor) COUITheme.colorScheme.onPrimary else COUITheme.colorScheme.primary,
                    )
                }
            }
        },
    ) { paddingValues ->
        SettingsPagerContent(
            configCount = configCount,
            mainPagerState = mainPagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            outerPadding = paddingValues,
            useFloatingBottomBar = useFloatingBottomBar,
            floatingBackdrop = floatingBackdrop,
            couiBackdrop = couiBackdrop,
        )
    }
}

@Composable
private fun shouldExpandNavigationRail(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        windowInfo.containerSize.width.toDp() >= 1200.dp
    }
}

@Composable
private fun SettingsPagerContent(
    configCount: Int,
    modifier: Modifier = Modifier,
    mainPagerState: MainPagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    outerPadding: PaddingValues,
    useFloatingBottomBar: Boolean,
    floatingBackdrop: CouiLayerBackdrop?,
    couiBackdrop: CouiLayerBackdrop?,
) {
    HorizontalPager(
        state = mainPagerState.pagerState,
        userScrollEnabled = true,
        overscrollEffect = null,
        beyondViewportPageCount = 1,
        modifier = modifier
            .then(if (useFloatingBottomBar && floatingBackdrop != null) Modifier.couiLayerBackdrop(floatingBackdrop) else Modifier)
            .then(if (!useFloatingBottomBar && couiBackdrop != null) Modifier.couiLayerBackdrop(couiBackdrop) else Modifier),
    ) { page ->
        val useBlur = floatingBackdrop != null && couiBackdrop != null
        when (page) {
            0 -> CouiHomePage(
                enableBlur = useBlur,
                title = navigationItems[page].label,
                configCount = configCount,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState,
                onNavigateToProfiles = { mainPagerState.animateToPage(1) },
            )

            1 -> CouiAllPage(
                enableBlur = useBlur,
                title = navigationItems[page].label,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState,
            )

            2 -> CouiHistoryPage(
                enableBlur = useBlur,
                title = navigationItems[page].label,
                outerPadding = outerPadding,
            )

            3 -> CouiPreferredPage(
                enableBlur = useBlur,
                title = navigationItems[page].label,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}
