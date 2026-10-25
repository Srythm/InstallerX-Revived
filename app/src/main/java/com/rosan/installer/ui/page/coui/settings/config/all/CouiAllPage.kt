// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.config.all

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBadge
import com.rosan.installer.ui.page.coui.widgets.CouiScopeTipCard
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewState
import com.rosan.installer.ui.page.main.widget.util.AllViewEventCollector
import com.rosan.installer.ui.theme.getCouiAppBarColor
import com.rosan.installer.ui.theme.installerCouiBlurEffect
import com.rosan.installer.ui.theme.rememberCouiBlurBackdrop
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.IconButton
import io.github.suqi8.coui.kmp.basic.InfiniteProgressIndicator
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.basic.SnackbarResult
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Delete
import io.github.suqi8.coui.kmp.icon.extended.Edit
import io.github.suqi8.coui.kmp.icon.extended.SelectAll
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
fun CouiAllPage(
    enableBlur: Boolean,
    viewModel: AllViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyGridState()
    val scrollBehavior = COUIScrollBehavior()

    AllViewEventCollector(
        viewModel = viewModel,
        navigator = navigator,
        onShowSnackbar = { message, actionLabel ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = true,
            )
            result == SnackbarResult.ActionPerformed
        },
    )

    val layoutDirection = LocalLayoutDirection.current

    val topBarBackdrop = rememberCouiBlurBackdrop(enableBlur)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.installerCouiBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getCouiAppBarColor(),
                title = title,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        when (uiState.data.progress) {
            is AllViewState.Data.Progress.Loading if uiState.data.configs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = outerPadding.calculateBottomPadding(),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        InfiniteProgressIndicator()
                        Text(
                            text = stringResource(id = R.string.loading),
                            style = COUITheme.textStyles.main,
                        )
                    }
                }
            }

            is AllViewState.Data.Progress.Loaded if uiState.data.configs.isEmpty() -> {
                // Since we don't allow removing default profile,
                // There is no need to handle an empty state.
            }

            else -> {
                val configs = uiState.data.configs
                val minId = configs.minByOrNull { it.id }?.id

                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    columns = GridCells.Adaptive(350.dp),
                    contentPadding = PaddingValues(
                        start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
                        end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        bottom = outerPadding.calculateBottomPadding() + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    overscrollEffect = null,
                    state = listState,
                ) {
                    if (!uiState.userReadScopeTips) {
                        item {
                            CouiScopeTipCard(viewModel = viewModel)
                        }
                    }
                    /*else item { Spacer(modifier = Modifier.size(6.dp)) }*/

                    items(configs) {
                        DataItemWidget(viewModel, it, it.id == minId)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataItemWidget(viewModel: AllViewModel, entity: ConfigModel, isDefault: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = entity.name,
                        style = COUITheme.textStyles.title4,
                        fontWeight = FontWeight.Medium,
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.size(8.dp))
                        CouiBadge(stringResource(id = R.string.config_global_default))
                    } else if (entity.scopeCount == 0) {
                        Spacer(modifier = Modifier.size(8.dp))
                        CouiBadge(
                            text = stringResource(id = R.string.config_status_inactive),
                            textColor = COUITheme.colorScheme.error,
                            containerColor = COUITheme.colorScheme.error.copy(alpha = 0.2f),
                        )
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))
                if (entity.description.isNotEmpty()) {
                    Text(
                        text = entity.description,
                        color = COUITheme.colorScheme.onSurfaceVariantSummary,
                        style = COUITheme.textStyles.subtitle,
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(
                minHeight = 35.dp,
                minWidth = 35.dp,
                backgroundColor = COUITheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                onClick = { viewModel.dispatch(AllViewAction.EditDataConfig(entity)) },
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = COUIIcons.Regular.Edit,
                    tint = COUITheme.colorScheme.onSecondaryContainer,
                    contentDescription = stringResource(id = R.string.edit),
                )
            }
            if (!isDefault) {
                IconButton(
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    backgroundColor = COUITheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    onClick = { viewModel.dispatch(AllViewAction.DeleteDataConfig(entity)) },
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = COUIIcons.Regular.Delete,
                        tint = COUITheme.colorScheme.onSecondaryContainer,
                        contentDescription = stringResource(id = R.string.delete),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    backgroundColor = COUITheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    onClick = { viewModel.dispatch(AllViewAction.ApplyConfig(entity)) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = COUIIcons.Regular.SelectAll,
                            tint = COUITheme.colorScheme.onSecondaryContainer,
                            contentDescription = stringResource(id = R.string.apply),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            modifier = Modifier.padding(end = 3.dp),
                            text = stringResource(R.string.config_scope),
                            color = COUITheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}
