// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Strips task-affinity and result-forwarding flags so [UninstallerSessionActivity] runs in an
 * isolated task and cannot be closed when the caller tears down its own task after package removal.
 */
internal fun computeDetachedUninstallFlags(sourceFlags: Int): Int {
    val clearedFlags = Intent.FLAG_ACTIVITY_FORWARD_RESULT or
        Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP or
        Intent.FLAG_ACTIVITY_NO_HISTORY or
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_CLEAR_TASK
    return (sourceFlags and clearedFlags.inv()) or
        Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
}

/**
 * Stateless trampoline entry point for external and in-app uninstall intents. Detaches incoming
 * flags and forwards the intent to [UninstallerSessionActivity] without `resultTo`.
 */
class UninstallerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        val forwardedIntent = Intent(intent ?: Intent()).apply {
            setClass(this@UninstallerActivity, UninstallerSessionActivity::class.java)
            flags = computeDetachedUninstallFlags(flags)
        }
        startActivity(forwardedIntent)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
