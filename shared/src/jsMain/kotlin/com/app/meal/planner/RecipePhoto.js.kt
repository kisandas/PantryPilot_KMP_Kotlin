package com.app.meal.planner

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import org.jetbrains.skia.Image
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

internal actual fun decodeRecipePhoto(bytes: ByteArray) =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

@Composable
internal actual fun rememberRecipePhotoPicker(onPhoto: (ByteArray?) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onPhoto)
    val input = remember {
        (document.createElement("input") as HTMLInputElement).apply {
            type = "file"
            accept = "image/*"
            style.display = "none"
        }
    }
    DisposableEffect(input) {
        input.onchange = {
            val file = input.files?.item(0)
            if (file != null) {
                if (file.size.toDouble() > MAX_PHOTO_BYTES) callback(null)
                else FileReader().apply {
                    onload = {
                        val buffer = result as ArrayBuffer
                        val bytes = Int8Array(buffer)
                        callback(ByteArray(buffer.byteLength) { index -> bytes.asDynamic()[index] as Byte })
                        Unit
                    }
                    onerror = { callback(null); Unit }
                    readAsArrayBuffer(file)
                }
            }
            Unit
        }
        document.body?.appendChild(input)
        onDispose { input.remove() }
    }
    return { input.value = ""; input.click() }
}
