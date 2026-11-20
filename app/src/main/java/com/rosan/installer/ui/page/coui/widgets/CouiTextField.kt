package com.rosan.installer.ui.page.coui.widgets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.suqi8.coui.kmp.basic.InputView

/**
 * COUI 2 card-style input (COUICardSingleInputView / COUICardMultiInputView).
 *
 * The card is part of the control, so callers must not wrap it in a Card, and only one layer may
 * own the outer margin. Length and logical-line limits stay with the caller's ViewModel:
 * `maxCount` is left at 0 on purpose, so an over-long value is rejected there instead of being
 * silently truncated inside the text field.
 *
 * @param title Always-visible title inside the card. Keep [label] empty unless a distinct
 *   placeholder is wanted, otherwise the hint just repeats the title.
 * @param errorMessage Empty hides the error line; [InputView] has no separate `isError` flag.
 * @param maxLines Only affects display; the ViewModel still enforces the logical line limit.
 */
@Composable
fun CouiInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    errorMessage: String = "",
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 8,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    InputView(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        title = title,
        label = label,
        enabled = enabled,
        readOnly = readOnly,
        // The library's clear button does not check readOnly itself.
        showClearButton = enabled && !readOnly,
        errorMessage = errorMessage,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
    )
}
