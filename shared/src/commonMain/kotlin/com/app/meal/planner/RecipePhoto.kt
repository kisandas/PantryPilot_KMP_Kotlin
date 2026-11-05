package com.app.meal.planner

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal const val MAX_PHOTO_BYTES = 2 * 1024 * 1024
private const val PHOTO_PREFIX = "photo:"

@OptIn(ExperimentalEncodingApi::class)
internal fun encodeRecipePhoto(bytes: ByteArray): String? {
    if (bytes.isEmpty() || bytes.size > MAX_PHOTO_BYTES) return null
    if (decodeRecipePhoto(bytes) == null) return null
    return PHOTO_PREFIX + Base64.encode(bytes)
}

@OptIn(ExperimentalEncodingApi::class)
internal fun recipePhotoBitmap(value: String): ImageBitmap? {
    if (!value.startsWith(PHOTO_PREFIX)) return null
    return runCatching { decodeRecipePhoto(Base64.decode(value.removePrefix(PHOTO_PREFIX))) }.getOrNull()
}

internal expect fun decodeRecipePhoto(bytes: ByteArray): ImageBitmap?

@Composable
internal expect fun rememberRecipePhotoPicker(onPhoto: (ByteArray?) -> Unit): () -> Unit
