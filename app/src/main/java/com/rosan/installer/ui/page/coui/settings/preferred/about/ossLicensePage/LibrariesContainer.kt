// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.about.ossLicensePage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.rosan.installer.R
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.squircle.LocalSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.utils.PressFeedbackType
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic
import io.github.suqi8.coui.kmp.window.WindowDialog

@Composable
fun LibrariesContainer(modifier: Modifier = Modifier, contentPadding: PaddingValues, libraries: Libs?) {
    val uriHandler = LocalUriHandler.current
    var selectedLibrary by remember { mutableStateOf<Library?>(null) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        overscrollEffect = null,
    ) {
        item { Spacer(modifier = Modifier.size(16.dp)) }
        items(libraries?.libraries ?: listOf()) { library ->
            LibraryCard(
                library = library,
                onClick = {
                    selectedLibrary = library
                    showLicenseDialog = true
                },
            )
        }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }

    selectedLibrary?.let { library ->
        val licenseSummary = stringResource(
            R.string.license,
            library.licenses.joinToString(", ") { it.name },
        )

        WindowDialog(
            show = showLicenseDialog,
            title = library.name,
            summary = licenseSummary,
            onDismissRequest = { showLicenseDialog = false },
            onDismissFinished = { selectedLibrary = null },
            content = {
                val dismissState = LocalDismissState.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .scrollEndHaptic()
                            .overScrollVertical(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        overscrollEffect = null,
                    ) {
                        items(library.licenses.toList()) { license ->
                            CompositionLocalProvider(LocalSquircleEnabled provides false) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.defaultColors(
                                        color = COUITheme.colorScheme.surfaceContainerHighest,
                                    ),
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = license.name,
                                            style = COUITheme.textStyles.title4,
                                            color = COUITheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                license.url?.let { uriHandler.openUri(it) }
                                            },
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = license.licenseContent
                                                ?: stringResource(R.string.no_license_text),
                                            style = COUITheme.textStyles.body2,
                                            color = COUITheme.colorScheme.onSurfaceContainer,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (!library.website.isNullOrBlank()) {
                            TextButton(
                                modifier = Modifier.weight(1f),
                                onClick = { uriHandler.openUri(library.website!!) },
                                text = stringResource(R.string.visit_home_page),
                            )
                        }

                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { dismissState?.invoke() },
                            text = stringResource(R.string.close),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            },
        )
    }
}

@Composable
fun LibraryCard(library: Library, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = library.name,
                    style = COUITheme.textStyles.title2,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 5.dp),
                )
                library.artifactVersion?.let {
                    Text(
                        text = it,
                        style = COUITheme.textStyles.headline1,
                        color = COUITheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = library.author,
                style = COUITheme.textStyles.body1,
                color = COUITheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            library.licenses.forEach { license ->
                Card(
                    colors = CardDefaults.defaultColors(
                        color = COUITheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        contentColor = COUITheme.colorScheme.onSurface,
                    ),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = license.name,
                            color = COUITheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
