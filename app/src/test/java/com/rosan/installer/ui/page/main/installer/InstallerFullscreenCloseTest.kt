// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import androidx.lifecycle.viewModelScope
import com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl
import com.rosan.installer.domain.engine.usecase.GetAppIconColorUseCase
import com.rosan.installer.domain.engine.usecase.GetAppIconUseCase
import com.rosan.installer.domain.engine.usecase.GetAppLabelUseCase
import com.rosan.installer.domain.privileged.usecase.GetAvailableUsersUseCase
import com.rosan.installer.testing.testAppPreferences
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class InstallerFullscreenCloseTest {
    @Test
    fun `fullscreen close defers session close until fade out finishes`() = fixture { scope ->
        vm.setFullscreenPresentation(true)
        vm.dispatch(InstallerViewAction.Close)
        scope.runCurrent()

        assertTrue(vm.isClosingFullscreen.value)
        assertTrue(vm.uiState.value.isClosingFullscreen)
        assertTrue(session.closeRequested.value)
        assertEquals(0, onCloseCalls)

        scope.advanceTimeBy(InstallerViewModel.FullscreenCloseFadeOutMs)
        scope.runCurrent()

        assertTrue(vm.isClosingFullscreen.value)
        assertTrue(vm.uiState.value.isClosingFullscreen)
        assertEquals(0, onCloseCalls)

        vm.completeFullscreenClose()
        scope.runCurrent()

        assertEquals(1, onCloseCalls)
        assertFalse(vm.isClosingFullscreen.value)
        assertFalse(vm.uiState.value.isClosingFullscreen)
    }

    @Test
    fun `fullscreen close falls back to timeout when animation callback never arrives`() = fixture { scope ->
        vm.setFullscreenPresentation(true)
        vm.dispatch(InstallerViewAction.Close)
        scope.runCurrent()

        assertTrue(vm.isClosingFullscreen.value)
        assertTrue(vm.uiState.value.isClosingFullscreen)
        assertEquals(0, onCloseCalls)

        scope.advanceTimeBy(InstallerViewModel.FullscreenCloseFallbackTimeoutMs - 1)
        scope.runCurrent()

        assertTrue(vm.isClosingFullscreen.value)
        assertTrue(vm.uiState.value.isClosingFullscreen)
        assertEquals(0, onCloseCalls)

        scope.advanceTimeBy(1)
        scope.runCurrent()

        assertEquals(1, onCloseCalls)
        assertFalse(vm.isClosingFullscreen.value)
        assertFalse(vm.uiState.value.isClosingFullscreen)
    }

    @Test
    fun `non-fullscreen close tears down immediately without fade out`() = fixture {
        vm.setFullscreenPresentation(false)
        vm.dispatch(InstallerViewAction.Close)

        assertFalse(vm.isClosingFullscreen.value)
        assertFalse(vm.uiState.value.isClosingFullscreen)
        assertTrue(session.closeRequested.value)
        assertEquals(1, onCloseCalls)
    }

    @Test
    fun `collecting a new session during fullscreen fade cancels pending close`() = fixture { scope ->
        vm.setFullscreenPresentation(true)
        vm.dispatch(InstallerViewAction.Close)

        scope.advanceTimeBy(InstallerViewModel.FullscreenCloseFallbackTimeoutMs / 2)
        scope.runCurrent()

        assertTrue(vm.isClosingFullscreen.value)
        assertTrue(vm.uiState.value.isClosingFullscreen)
        assertTrue(session.closeRequested.value)
        assertEquals(0, onCloseCalls)

        var session2Closed = 0
        val session2 = InstallerSessionRepositoryImpl("session-2") { session2Closed++ }
        vm.dispatch(InstallerViewAction.CollectSession(session2))
        scope.runCurrent()

        assertFalse(vm.isClosingFullscreen.value)
        assertFalse(vm.uiState.value.isClosingFullscreen)

        scope.advanceTimeBy(InstallerViewModel.FullscreenCloseFallbackTimeoutMs * 2)
        scope.runCurrent()

        assertEquals(0, onCloseCalls)
        assertEquals(0, session2Closed)
        assertFalse(session2.closeRequested.value)
    }

    private fun fixture(block: Harness.(TestScope) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val harness = Harness()
        val subscription = launch(UnconfinedTestDispatcher(testScheduler)) {
            harness.vm.uiState.collect {}
        }
        try {
            runCurrent()
            block(harness, this)
            runCurrent()
        } finally {
            harness.vm.viewModelScope.cancel()
            subscription.cancel()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    private class Harness {
        var onCloseCalls = 0
        val session = InstallerSessionRepositoryImpl("fullscreen-close-test") { onCloseCalls++ }
        val vm = InstallerViewModel(
            session = session,
            appSettingsRepo = stub { name, _ ->
                when (name) {
                    "getPreferencesFlow" -> flowOf(testAppPreferences())
                    "getBoolean" -> flowOf(false)
                    else -> error("Unexpected settings call: $name")
                }
            },
            getAvailableUsers = GetAvailableUsersUseCase(stub()),
            getAppIcon = GetAppIconUseCase(stub()),
            getAppIconColor = GetAppIconColorUseCase(stub()),
            getAppLabel = GetAppLabelUseCase(
                stub { name, _ ->
                    check(name == "getAppLabel")
                    null
                },
            ),
            deviceCapabilityProvider = stub { name, _ ->
                check(name == "isSystemApp")
                true
            },
        )
    }

    companion object {
        private inline fun <reified T> stub(
            crossinline answer: (String, Array<out Any?>?) -> Any? = { name, _ -> error("Unexpected dependency call: $name") },
        ): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            answer(method.name, args)
        } as T
    }
}
