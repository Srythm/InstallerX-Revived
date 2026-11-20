// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rosan.installer.ui.theme.rememberCouiPopupBlurProvider
import io.github.suqi8.coui.kmp.effect.LocalPopupMaterialBlur
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * Installs the menu blur provider where COUI expects it, which is a narrower place than it looks.
 *
 * Two separate rules decide whether a dropdown gets the blurred surface:
 *
 * 1. `Scaffold` reads `LocalPopupMaterialBlur` at its own position to work out whether a popup
 *    would be sampling a page that is recording it (`hostInsideBackdrop`). A provider placed
 *    *above* the Scaffold therefore switches the blur off for every menu on the page — the local
 *    has to be absent at the Scaffold.
 * 2. The surface is resolved at the *call site* (`rememberPopupSurface` runs inside
 *    `ListPopupLayout`, before the popup host is composed), so the provider has to be present
 *    inside the page content.
 *
 * Wrapping the Scaffold's content, as this does, satisfies both at once. The menus themselves are
 * drawn in the Scaffold's popup slot, which is a sibling of the content, so they never end up in
 * the recording they sample.
 *
 * @param backdrop The page's recording. Null (blur off, or no shader support) leaves the menus on
 *   their opaque surface, which is COUI's own fallback.
 */
@Composable
fun CouiPopupBlurScope(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    val provider = rememberCouiPopupBlurProvider(backdrop)
    CompositionLocalProvider(LocalPopupMaterialBlur provides provider) {
        content()
    }
}
