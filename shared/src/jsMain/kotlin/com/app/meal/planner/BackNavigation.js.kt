package com.app.meal.planner

import androidx.compose.runtime.*

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val latest by rememberUpdatedState(onBack)
    DisposableEffect(enabled) {
        val window = js("window")
        val listener: (dynamic) -> Unit = { latest() }
        if (enabled) {
            window.history.pushState(null, "", window.location.href)
            window.addEventListener("popstate", listener)
        }
        onDispose { if (enabled) window.removeEventListener("popstate", listener) }
    }
}
