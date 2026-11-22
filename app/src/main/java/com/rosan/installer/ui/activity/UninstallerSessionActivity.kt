// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.rosan.installer.R
import com.rosan.installer.core.app.ActivityContracts.KEY_UNINSTALLER_ID
import com.rosan.installer.domain.device.model.PermissionType
import com.rosan.installer.domain.device.provider.PermissionChecker
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionManager
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.framework.auth.BiometricAuthBridge
import com.rosan.installer.ui.common.permission.PermissionRequester
import com.rosan.installer.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class UninstallerSessionActivity :
    ComponentActivity(),
    KoinComponent {
    private companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
    }

    private val themeStateProvider by inject<ThemeStateProvider>()
    private val sessionManager by inject<InstallerSessionManager>()
    private var session: InstallerSessionRepository? = null
    private var job: Job? = null
    private var latestProgress: ProgressEntity = ProgressEntity.Ready

    private val permissionChecker: PermissionChecker by inject()
    private lateinit var permissionRequester: PermissionRequester

    // Flag to track if the activity is stopped due to a permission request
    private var isRequestingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        Timber.d("UninstallerSessionActivity onCreate.")

        permissionRequester = PermissionRequester(this, permissionChecker)
        // Set up the callback to intercept the settings launch event
        permissionRequester.onBeforeLaunchSettings = {
            Timber.d("Launching settings for permission, preventing session closure in onStop.")
            isRequestingPermission = true
        }

        if (savedInstanceState == null) {
            startUninstallIntent(intent)
            return
        }

        val sessionId = savedInstanceState.getString(KEY_UNINSTALLER_ID)
        session = sessionManager.getOrCreate(sessionId)
        startCollectors()
        showContent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("UninstallerSessionActivity onNewIntent.")

        if (shouldDeferUninstallIntent()) {
            sessionManager.enqueueForegroundUninstall(Intent(intent).apply { removeExtra(KEY_UNINSTALLER_ID) })
            Timber.d("UninstallerSessionActivity deferred foreground uninstall intent.")
            return
        }

        startUninstallIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val currentId = session?.id
        outState.putString(KEY_UNINSTALLER_ID, currentId)
        Timber.d("UninstallerSessionActivity onSaveInstanceState: Saving id: $currentId")
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()

        if (BiometricAuthBridge.isAuthenticating) {
            Timber.d("onStop: Ignored session closure due to active biometric authentication.")
            return
        }
        // Check if the screen is currently on.
        // If the screen is off, onStop is triggered by locking the device.
        // We explicitly want to ignore this case.
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        if (!isScreenOn) {
            // The screen is turned off (locked), do nothing.
            Timber.d("onStop: Screen is turned off. Ignoring.")
            return
        }
        // Only strictly interpret as user leaving when not finishing and not changing configurations (e.g., rotation)
        if (!isFinishing && !isChangingConfigurations && !isRequestingPermission) {
            session?.let { session ->
                if (session.closeRequested.value) {
                    Timber.d("onStop: Close already requested. Ignoring session closure.")
                    return
                }
                Timber.d("onStop: User left UninstallerSessionActivity. Closing repository.")
                session.close()
            }
        } else if (isRequestingPermission) {
            Timber.d("onStop: Ignored session closure due to active permission request.")
            isRequestingPermission = false
        }
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
        if (isFinishing && !isChangingConfigurations) {
            session?.close()
        }
        Timber.d("UninstallerSessionActivity is being destroyed.")
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        overridePendingTransition(0, 0)
        super.finish()
    }

    private fun requestPermissionsAndProceed(packageName: String) {
        permissionRequester.requestEssentialPermissions(
            onGranted = {
                Timber.d("Permissions granted. Proceeding with uninstall for $packageName")
                session?.resolveUninstall(this@UninstallerSessionActivity, packageName)
            },
            onDenied = { reason ->
                when (reason) {
                    PermissionType.NOTIFICATION -> {
                        Timber.w("Notification permission was denied.")
                        this.toast(R.string.enable_notification_hint)
                    }

                    PermissionType.STORAGE -> {
                        Timber.w("Storage permission was denied.")
                        this.toast(R.string.enable_storage_permission_hint)
                    }
                }
                session?.close()
                finish()
            },
        )
    }

    private fun startUninstallIntent(intent: Intent) {
        this.intent = intent
        job?.cancel()
        val newSession = sessionManager.getOrCreate(null)
        newSession.background(false)
        session = newSession
        intent.putExtra(KEY_UNINSTALLER_ID, newSession.id)
        latestProgress = ProgressEntity.Ready

        val packageName = intent.uninstallPackageName()
        if (packageName.isNullOrBlank()) {
            Timber.e("UninstallerSessionActivity started without a package name.")
            newSession.close()
            finish()
            return
        }

        startCollectors()
        showContent()
        Timber.d("Target package to uninstall: $packageName")
        requestPermissionsAndProceed(packageName)
    }

    private fun Intent.uninstallPackageName(): String? {
        if (action == Intent.ACTION_SHOW_APP_INFO) {
            return getStringExtra(Intent.EXTRA_PACKAGE_NAME)
        }

        getStringExtra(EXTRA_PACKAGE_NAME)?.let { if (it.isNotBlank()) return it }

        @Suppress("DEPRECATION")
        val isUninstallAction = action == Intent.ACTION_UNINSTALL_PACKAGE || action == Intent.ACTION_DELETE
        if (!isUninstallAction) return null

        return data?.schemeSpecificPart
    }

    private fun shouldDeferUninstallIntent(): Boolean = session != null && latestProgress.isForegroundUninstallProgress()

    private fun launchNextPendingUninstall(): Boolean {
        val nextIntent = sessionManager.takeNextForegroundUninstall() ?: return false
        Timber.d("Launching deferred foreground uninstall intent.")
        startUninstallIntent(nextIntent)
        return true
    }

    private fun startCollectors() {
        job?.cancel()
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        job = scope.launch {
            session?.progress?.collect { progress ->
                Timber.d("[id=${session?.id}] Activity collected progress: ${progress::class.simpleName}")
                latestProgress = progress
                // Finish the activity on final states
                if (progress is ProgressEntity.Finish) {
                    if (!launchNextPendingUninstall() && !this@UninstallerSessionActivity.isFinishing) {
                        this@UninstallerSessionActivity.finish()
                    }
                }
            }
        }
    }

    private fun ProgressEntity.isForegroundUninstallProgress(): Boolean = this is ProgressEntity.UninstallResolving ||
        this is ProgressEntity.UninstallReady ||
        this is ProgressEntity.Uninstalling ||
        this is ProgressEntity.UninstallSuccess ||
        this is ProgressEntity.UninstallFailed ||
        this is ProgressEntity.UninstallResolveFailed

    private fun showContent() {
        setContent {
            val currentSession = session
            if (currentSession == null) {
                LaunchedEffect(Unit) {
                    finish()
                }
                return@setContent
            }

            InstallerActivityContent(session = currentSession, themeStateProvider = themeStateProvider)
        }
    }
}
