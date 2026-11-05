package com.app.meal.planner.feature

import androidx.compose.runtime.*
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement

@Composable
actual fun PlatformDatePicker(value: String, onValue: (String) -> Unit, onDismiss: () -> Unit) {
    val selected by rememberUpdatedState(onValue)
    val dismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(Unit) {
        val overlay = document.createElement("div")
        overlay.setAttribute(
            "style",
            "position:fixed;inset:0;background:#0008;z-index:10000;display:flex;align-items:center;justify-content:center"
        )
        val panel = document.createElement("div")
        panel.setAttribute("role", "dialog"); panel.setAttribute("aria-label", "Choose expiry date")
        panel.setAttribute(
            "style",
            "background:#faf8f0;color:#233e29;padding:24px;border-radius:20px;font-family:system-ui,sans-serif;display:flex;flex-direction:column;gap:16px;min-width:250px"
        )
        val label = document.createElement("label"); label.textContent =
        "Expiry date"; label.setAttribute("for", "expiry-native-date")
        val input = document.createElement("input") as HTMLInputElement
        input.id = "expiry-native-date"; input.type = "date"; input.value = value
        input.setAttribute(
            "style",
            "font:inherit;padding:12px;border:1px solid #315c3a;border-radius:8px"
        )
        val done = document.createElement("button"); done.textContent = "Choose date"
        val cancel = document.createElement("button"); cancel.textContent = "Cancel"
        listOf(done, cancel).forEach {
            it.setAttribute(
                "style",
                "font:inherit;padding:12px;border-radius:8px;cursor:pointer"
            )
        }
        done.addEventListener("click", { if (input.value.isNotBlank()) selected(input.value) })
        cancel.addEventListener("click", { dismiss() })
        overlay.addEventListener("click", { if (it.target == overlay) dismiss() })
        panel.appendChild(label); panel.appendChild(input); panel.appendChild(done); panel.appendChild(
        cancel
    )
        // Keep native picker gestures and keyboard events out of the Compose canvas behind it.
        listOf(
            "pointerdown",
            "pointerup",
            "pointermove",
            "mousedown",
            "mouseup",
            "click",
            "keydown",
            "keyup",
            "keypress",
            "wheel",
            "touchstart",
            "touchend"
        ).forEach { type ->
            overlay.addEventListener(type, { it.stopPropagation() })
        }
        overlay.appendChild(panel); document.body!!.appendChild(overlay)
        input.focus()
        onDispose { overlay.remove() }
    }
}
