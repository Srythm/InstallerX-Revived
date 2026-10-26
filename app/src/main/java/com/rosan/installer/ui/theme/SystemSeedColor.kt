// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject

/** Secure setting ColorOS and other OPlus ROMs use to store the active system palette. */
private const val SystemPaletteSetting = "theme_customization_overlay_packages"

/** JSON key holding the palette seed inside [SystemPaletteSetting]. */
private const val SystemPaletteKey = "android.theme.customization.system_palette"

/**
 * Reads the accent color the user picked in the system theme settings.
 *
 * On ColorOS and other OPlus ROMs the user's choice lives in [SystemPaletteSetting], which is
 * more accurate than the AOSP `system_accent1_500` bucket: the bucket is a quantized Monet
 * palette entry, while this value is the exact seed the system themed itself with. The setting
 * is observed, so the app follows the system while it is running.
 *
 * @return the seed color, or null when the ROM does not expose one. Callers should fall back to
 * a resource color in that case.
 */
@Composable
fun rememberSystemSeedColor(): Color? {
    val resolver = LocalContext.current.contentResolver

    fun readSeed(): Color? = runCatching {
        val raw = Settings.Secure.getString(resolver, SystemPaletteSetting) ?: return@runCatching null
        parsePaletteSeed(JSONObject(raw).optString(SystemPaletteKey))
    }.getOrNull()

    var seed by remember(resolver) { mutableStateOf(readSeed()) }

    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                seed = readSeed()
            }
        }
        val registered = runCatching {
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(SystemPaletteSetting),
                false,
                observer,
            )
        }.isSuccess
        // Re-read once after registering so a change between composition and registration is not lost.
        seed = readSeed()
        onDispose { if (registered) resolver.unregisterContentObserver(observer) }
    }

    return seed
}

/**
 * Parses a palette value such as `#2F6BFF`, `2F6BFF` or `FF2F6BFF` into an opaque [Color].
 *
 * @return the parsed color, or null when the value is not a valid 6 or 8 digit hex color.
 */
internal fun parsePaletteSeed(value: String): Color? {
    val hex = value.trim().removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    if (hex.any { it.digitToIntOrNull(16) == null }) return null
    val argb = hex.toLongOrNull(16) ?: return null
    return Color((argb or 0xFF000000L).toInt())
}
