package com.app.meal.planner.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.app.meal.planner.isWebPlatform

private val LocalFormFields = compositionLocalOf<MutableList<FocusRequester>?> { null }

@Composable
internal fun FormKeyboardScope(content: @Composable () -> Unit) {
    val fields = remember { mutableStateListOf<FocusRequester>() }
    CompositionLocalProvider(LocalFormFields provides fields, content = content)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    minLines: Int = 1,
) {
    val fields = LocalFormFields.current
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    if (!isWebPlatform && fields != null && enabled) {
        DisposableEffect(fields, requester) {
            fields.add(requester)
            onDispose { fields.remove(requester) }
        }
    }
    val index = fields?.indexOf(requester) ?: -1
    val previous = if (index > 0) fields?.getOrNull(index - 1) else null
    val next = if (index >= 0) fields?.getOrNull(index + 1) else null
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val done = {
        focus.clearFocus()
        keyboard?.hide()
        Unit
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = if (isWebPlatform) modifier else modifier
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused },
        enabled = enabled,
        label = label,
        supportingText = if (isWebPlatform || !focused || !enabled) supportingText else {
            {
                Column {
                    supportingText?.invoke()
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (previous != null) TextButton(onClick = { previous.requestFocus() }) { Text("Previous") }
                        if (next != null) TextButton(onClick = { next.requestFocus() }) { Text("Next") }
                        TextButton(onClick = done) { Text("Done") }
                        TextButton(onClick = { keyboard?.hide() }) { Text("Dismiss") }
                    }
                }
            }
        },
        isError = isError,
        keyboardOptions = if (isWebPlatform || !singleLine) keyboardOptions else
            keyboardOptions.copy(imeAction = if (next == null) ImeAction.Done else ImeAction.Next),
        keyboardActions = if (isWebPlatform) KeyboardActions.Default else KeyboardActions(
            onNext = { next?.requestFocus() },
            onDone = { done() },
        ),
        singleLine = singleLine,
        minLines = minLines,
    )
}
