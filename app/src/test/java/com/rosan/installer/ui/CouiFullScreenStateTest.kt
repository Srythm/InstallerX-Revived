// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui

import com.rosan.installer.ui.page.coui.installer.fullscreen.FullScreenPage
import com.rosan.installer.ui.page.coui.installer.fullscreen.FullScreenSubPage
import com.rosan.installer.ui.page.coui.installer.fullscreen.fullScreenProgress
import com.rosan.installer.ui.page.main.installer.InstallerStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CouiFullScreenStateTest {
    @Test
    fun unknownProgressIsNotConfusedWithZero() {
        assertNull(fullScreenProgress(null))
        assertNull(fullScreenProgress(-1f))
        assertNull(fullScreenProgress(Float.NaN))
        assertNull(fullScreenProgress(Float.POSITIVE_INFINITY))
        assertEquals(0f, fullScreenProgress(0f)!!, 0f)
        assertEquals(0.5f, fullScreenProgress(0.5f)!!, 0f)
        assertEquals(1f, fullScreenProgress(1f)!!, 0f)
    }

    @Test
    fun progressAndModuleOutputDoNotRestartPageTransitions() {
        val preparing = FullScreenPage(InstallerStage.Preparing(0f), "Preparing", progress = 0f)
        val advanced = preparing.copy(stage = InstallerStage.Preparing(0.6f), progress = 0.6f)
        assertEquals(preparing.key, advanced.key)

        val module = FullScreenPage(InstallerStage.InstallingModule(listOf("Starting")), "Installing")
        val finished = module.copy(
            stage = InstallerStage.InstallingModule(listOf("Starting", "ERROR: Failed"), isFinished = true, failed = true),
            title = "Failed",
        )
        assertEquals(module.key, finished.key)
    }

    @Test
    fun stageAndSubpageChangesHaveDistinctTransitionKeys() {
        val main = FullScreenPage(InstallerStage.InstallPrepare, "Prepare")
        val settings = main.copy(subPage = FullScreenSubPage.Settings)
        val permissions = main.copy(subPage = FullScreenSubPage.Permissions)
        val success = main.copy(stage = InstallerStage.InstallSuccess)
        assertNotEquals(main.key, settings.key)
        assertNotEquals(settings.key, permissions.key)
        assertNotEquals(main.key, success.key)
    }
}
