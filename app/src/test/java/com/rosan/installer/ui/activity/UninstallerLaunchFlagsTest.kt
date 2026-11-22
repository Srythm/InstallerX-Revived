// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.content.Intent
import com.rosan.installer.core.app.ActivityContracts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UninstallerLaunchFlagsTest {
    @Test
    fun `detached uninstall flags strip caller result and history flags while isolating task`() {
        val incomingFlags = Intent.FLAG_ACTIVITY_FORWARD_RESULT or
            Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP or
            Intent.FLAG_ACTIVITY_NO_HISTORY or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_CLEAR_TASK or
            Intent.FLAG_GRANT_READ_URI_PERMISSION

        val detached = computeDetachedUninstallFlags(incomingFlags)

        assertEquals(0, detached and Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        assertEquals(0, detached and Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP)
        assertEquals(0, detached and Intent.FLAG_ACTIVITY_NO_HISTORY)
        assertEquals(0, detached and Intent.FLAG_ACTIVITY_CLEAR_TOP)
        assertEquals(0, detached and Intent.FLAG_ACTIVITY_CLEAR_TASK)
        assertNotEquals(0, detached and Intent.FLAG_ACTIVITY_NEW_TASK)
        assertNotEquals(0, detached and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        assertNotEquals(0, detached and Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    @Test
    fun `uninstaller session activity contract matches class name`() {
        assertEquals(
            UninstallerSessionActivity::class.java.name,
            ActivityContracts.UNINSTALLER_SESSION_ACTIVITY,
        )
    }
}
