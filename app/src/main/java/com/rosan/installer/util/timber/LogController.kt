// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.util.timber

import android.content.Context
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class LogController(
    private val context: Context,
    private val appSettingsRepo: AppSettingsRepository
) {
    private var fileLoggingTree: FileLoggingTree? = null

    // Use Main scope for collecting flow, file IO is handled internally by the Tree on IO dispatcher
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Evaluate the global logging configuration first
        if (AppConfig.isLogEnabled) {
            scope.launch {
                // Use the already-aggregated preferences flow instead of a separate DataStore
                // subscription for a single boolean. This avoids an extra collector and keeps
                // the logging decision aligned with the canonical AppPreferences state.
                appSettingsRepo.preferencesFlow.collectLatest { prefs ->
                    updateLoggingState(prefs.enableFileLogging)
                }
            }
        } else {
            // Force disable file logging if globally disabled
            updateLoggingState(false)
        }
    }

    private fun updateLoggingState(enabled: Boolean) {
        if (enabled) {
            if (fileLoggingTree == null) {
                val tree = FileLoggingTree(context)
                Timber.plant(tree)
                fileLoggingTree = tree
            }
        } else {
            fileLoggingTree?.let { tree ->
                Timber.uproot(tree)
                tree.release()
                fileLoggingTree = null
            }
        }
    }
}