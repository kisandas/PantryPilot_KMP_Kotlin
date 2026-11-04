package com.app.meal.planner

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        js("document.getElementById('startup-status')")?.remove()
        ComposeViewport("compose-root") {
            App()
        }
    }
}

