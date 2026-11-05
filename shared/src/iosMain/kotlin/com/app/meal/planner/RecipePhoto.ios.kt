@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.app.meal.planner

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.*
import org.jetbrains.skia.Image
import platform.Foundation.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.*
import platform.posix.memcpy

internal actual fun decodeRecipePhoto(bytes: ByteArray) =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

private class RecipePhotoDelegate(
    val onPhoto: (ByteArray?) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
        result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            val jpeg = data?.let { UIImage(data = it) }?.let { UIImageJPEGRepresentation(it, 0.75) }
            val bytes = jpeg?.takeIf { it.length <= MAX_PHOTO_BYTES.toULong() }?.let { image ->
                ByteArray(image.length.toInt()).also { array ->
                    if (array.isNotEmpty()) array.usePinned {
                        memcpy(it.addressOf(0), image.bytes, image.length)
                    }
                }
            }
            dispatch_async(dispatch_get_main_queue()) { onPhoto(bytes) }
        }
    }
}

@Composable
internal actual fun rememberRecipePhotoPicker(onPhoto: (ByteArray?) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onPhoto)
    val delegate = remember { RecipePhotoDelegate { callback(it) } }
    return {
        var presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (presenter?.presentedViewController != null) presenter =
            presenter.presentedViewController
        val config = PHPickerConfiguration().apply {
            selectionLimit = 1
            filter = PHPickerFilter.imagesFilter
        }
        val picker = PHPickerViewController(configuration = config)
        picker.delegate = delegate
        presenter?.presentViewController(picker, animated = true, completion = null)
    }
}
