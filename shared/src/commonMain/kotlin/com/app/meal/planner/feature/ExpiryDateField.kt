package com.app.meal.planner.feature

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable expect fun PlatformDatePicker(value: String,onValue: (String)->Unit,onDismiss: ()->Unit)

@Composable fun ExpiryDateField(value: String,enabled: Boolean=true,error: String?=null,onValue: (String)->Unit) {
    var show by remember {mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("Expiry date (optional)",style=MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton({show=true},enabled=enabled,modifier=Modifier.weight(1f)) {Text(value.ifBlank {"Choose date"})}
            if(value.isNotBlank()) OutlinedButton({onValue("")},enabled=enabled) {Text("Clear")}
        }
        if(error!=null) Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    }
    if(show) PlatformDatePicker(value,{onValue(it);show=false},{show=false})
}
