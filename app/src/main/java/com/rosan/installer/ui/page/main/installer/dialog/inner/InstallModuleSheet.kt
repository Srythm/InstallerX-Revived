// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.ui.util.KeyEventBlocker

/**
 * Module installation sheet implemented with Material 3 components.
 * Features an auto-scrolling log terminal and a bottom action button.
 */
@Composable
fun ModuleInstallSheetContent(
    rootMode: RootMode,
    outputLines: List<String>,
    isFinished: Boolean,
    onReboot: () -> Unit,
    onSoftReboot: () -> Unit,
    onClose: () -> Unit,
    colorScheme: ColorScheme,
) {
    var showRebootConfirmation by rememberSaveable { mutableStateOf(false) }

    KeyEventBlocker {
        it.key == Key.VolumeDown || it.key == Key.VolumeUp
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp), // Bottom padding for navigation bar/visual balance
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Title
        Text(
            text = if (isFinished) {
                stringResource(R.string.installer_install_complete)
            } else {
                stringResource(R.string.installer_installing_module)
            },
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onSurface,
        )

        // Terminal Log Container
        ModuleInstallLog(
            outputLines = outputLines,
            isFinished = isFinished,
            modifier = Modifier
                .fillMaxWidth()
                // Use weight to fill available remaining space without pushing bottom elements out of screen.
                // fill = false allows it to be smaller than the available space if log content is short.
                .weight(1f, fill = false)
                .heightIn(min = 300.dp),
            colorScheme = colorScheme,
        )

        // Action Button
        if (isFinished) {
            Column {
                Button(
                    onClick = { showRebootConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.reboot))
                }
                /*if (rootMode == RootMode.KernelSU)
                    Button(
                        onClick = onSoftReboot,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.reboot_soft_reboot))
                    }*/
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        } else {
            Button(
                enabled = false, // Disabled while installing
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.installer_installing))
                }
            }
        }
    }

    RebootConfirmationDialog(
        show = showRebootConfirmation,
        onDismiss = { showRebootConfirmation = false },
        onConfirm = {
            showRebootConfirmation = false
            onReboot()
        },
    )
}

/**
 * Full-screen module progress body. The package/module identity remains in the stable
 * full-screen header while the terminal consumes the rest of the viewport.
 */
@Composable
fun ModuleInstallFullScreenContent(
    outputLines: List<String>,
    isFinished: Boolean,
    colorScheme: ColorScheme,
    onReboot: () -> Unit,
    onClose: () -> Unit,
) {
    var showRebootConfirmation by rememberSaveable { mutableStateOf(false) }

    KeyEventBlocker {
        it.key == Key.VolumeDown || it.key == Key.VolumeUp
    }

    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = navigationBarPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isFinished) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = if (isFinished) {
                    stringResource(R.string.installer_install_complete)
                } else {
                    stringResource(R.string.installer_installing_module)
                },
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
            )
        }

        ModuleInstallLog(
            outputLines = outputLines,
            isFinished = isFinished,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colorScheme = colorScheme,
            shape = RoundedCornerShape(24.dp),
        )

        if (isFinished) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { showRebootConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.reboot))
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }

    RebootConfirmationDialog(
        show = showRebootConfirmation,
        onDismiss = { showRebootConfirmation = false },
        onConfirm = {
            showRebootConfirmation = false
            onReboot()
        },
    )
}

@Composable
private fun RebootConfirmationDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.module_reboot_confirm_title)) },
        text = { Text(stringResource(R.string.module_reboot_confirm_desc)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.reboot))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ModuleInstallLog(
    outputLines: List<String>,
    isFinished: Boolean,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
) {
    val lazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // A terminal should follow new output immediately. Animated scrolling can lag behind a fast
    // sequence of interactive prompts and make the selected answer appear under the next option.
    LaunchedEffect(outputLines.size, isFinished) {
        if (outputLines.isNotEmpty()) {
            lazyListState.scrollToItem(index = outputLines.size - 1)
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerHigh,
        ),
        shape = shape,
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            items(outputLines) { line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = if (line.startsWith("ERROR:")) {
                        colorScheme.error
                    } else {
                        colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
