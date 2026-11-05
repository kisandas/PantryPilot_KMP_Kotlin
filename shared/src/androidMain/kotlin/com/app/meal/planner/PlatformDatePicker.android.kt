package com.app.meal.planner.feature

import android.app.DatePickerDialog
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

@Composable actual fun PlatformDatePicker(value: String,onValue: (String)->Unit,onDismiss: ()->Unit) {
    val context=LocalContext.current
    val selected by rememberUpdatedState(onValue)
    val dismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(Unit) {
        val date=runCatching {LocalDate.parse(value)}.getOrElse {LocalDate.now()}
        val dialog=DatePickerDialog(context,{_,year,month,day -> selected(LocalDate.of(year,month+1,day).toString())},date.year,date.monthValue-1,date.dayOfMonth)
        dialog.setOnDismissListener {dismiss()}
        dialog.show()
        onDispose {dialog.setOnDismissListener(null);dialog.dismiss()}
    }
}
