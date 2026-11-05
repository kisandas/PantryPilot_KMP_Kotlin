@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.app.meal.planner.feature

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import platform.Foundation.*
import platform.UIKit.*

@Composable
actual fun PlatformDatePicker(value: String, onValue: (String) -> Unit, onDismiss: () -> Unit) {
    val formatter = remember {
        NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd"; locale = NSLocale("en_US_POSIX")
        }
    }
    val picker = remember {
        UIDatePicker().apply {
            datePickerMode = UIDatePickerMode.UIDatePickerModeDate
            preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
            formatter.dateFromString(value)?.let { setDate(it, false) }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Expiry date") },
        text = {
            UIKitView(factory = { picker }, modifier = Modifier.fillMaxWidth().height(200.dp))
        },
        confirmButton = { TextButton({ onValue(formatter.stringFromDate(picker.date)) }) { Text("Done") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}
