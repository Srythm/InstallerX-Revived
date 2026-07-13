// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import android.content.Context
import android.os.IBinder
import com.rosan.installer.core.reflection.ReflectionProvider

import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import kotlinx.coroutines.CoroutineScope

class SystemAppInstallerRepoImpl(
    context: Context, reflect: ReflectionProvider, capabilityProvider: DeviceCapabilityProvider, postInstallTaskProvider: PostInstallTaskProvider, taskScope: CoroutineScope
) : IBinderAppInstallerRepoImpl(context, reflect, capabilityProvider, postInstallTaskProvider, taskScope) {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder = iBinder
}
