package com.rosan.installer.ui.page.coui.widgets

import androidx.compose.runtime.Composable
import io.github.suqi8.coui.kmp.basic.DropdownColors
import io.github.suqi8.coui.kmp.basic.DropdownDefaults
import io.github.suqi8.coui.kmp.basic.DropdownImpl

// Code from weishu/KernelSU
@Composable
fun DropdownItem(
    text: String,
    optionSize: Int,
    index: Int,
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
    onSelectedIndexChange: (Int) -> Unit,
) {
    DropdownImpl(
        text = text,
        optionSize = optionSize,
        isSelected = false,
        index = index,
        dropdownColors = dropdownColors,
        onSelectedIndexChange = onSelectedIndexChange,
    )
}
