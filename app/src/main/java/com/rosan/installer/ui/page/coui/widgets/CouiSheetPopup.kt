// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import com.rosan.installer.ui.theme.rememberCouiPopupBlurProvider
import io.github.suqi8.coui.kmp.effect.LocalPopupMaterialBlur
import io.github.suqi8.coui.kmp.utils.COUIPopupUtils.Companion.COUIPopupHost
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * The popup counterpart of [CouiPopupBlurScope] for a `WindowBottomSheet`.
 *
 * A sheet hands its popups to a separate dialog, and a dialog cannot sample the panel it covers:
 * the recording only exists inside the sheet window. A menu opened there therefore has to stay in
 * that window, which is what [CouiSheetPopupHost] renders. This provides the blur provider for it;
 * wrap the whole `WindowBottomSheet` call, because the title bar builds its own actions outside the
 * sheet content and that is where a toolbar menu is usually opened from.
 *
 * Compose carries composition locals into a dialog's subcomposition, so the provider reaches both
 * the title bar and the content.
 *
 * @param backdrop The sheet's recording, without the popups themselves. Null (blur off, or no
 *   shader support) leaves the menus on their opaque surface, which is COUI's own fallback.
 */
@Composable
fun CouiSheetPopupProvider(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    val provider = rememberCouiPopupBlurProvider(backdrop)
    CompositionLocalProvider(LocalPopupMaterialBlur provides provider, content = content)
}

/**
 * COUI's in-window popup host for a `WindowBottomSheet`, rendered where the sheet's content sits.
 *
 * COUI resolves menu placement from window coordinates (`positionInWindow`), while this host starts
 * somewhere inside the panel, so the host measures itself as the full window and places its content
 * back at the window origin. It reports a 0x0 size, so the sheet keeps measuring exactly as it did
 * without it.
 *
 * The host emits nothing at all while no popup is open, so the measured child is optional: taking
 * `first()` there threw `NoSuchElementException` on the first frame of every sheet.
 */
@Composable
fun CouiSheetPopupHost(modifier: Modifier = Modifier) {
    val windowSize = LocalWindowInfo.current.containerSize
    // Where this host (and therefore the sheet content) sits inside the window.
    var originInWindow by remember { mutableStateOf(Offset.Zero) }

    Layout(
        content = { COUIPopupHost() },
        modifier = modifier.onGloballyPositioned { originInWindow = it.positionInWindow() },
    ) { measurables, _ ->
        val placeable = measurables.firstOrNull()
            ?.measure(Constraints.fixed(windowSize.width, windowSize.height))
        layout(0, 0) {
            placeable?.placeRelative(
                -originInWindow.x.toInt(),
                -originInWindow.y.toInt(),
            )
        }
    }
}
