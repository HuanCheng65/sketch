package com.github.panpf.sketch.zoom.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import androidx.exifinterface.media.ExifInterface
import com.github.panpf.sketch.zoom.ImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

//internal fun View.size(): Size {
//    return Size(width, height)
//}

internal fun View.contentSize(): Size {
    return Size(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom)
}

class DecodeSourceResult(
    val mimeType: String,
    val imageSize: Size,
    val exifOrientation: Int,
    val sampledBitmap: Bitmap
)

internal suspend fun realDecodeSource(
    view: View,
    imageSource: ImageSource,
    inBitmapHelper: InBitmapHelper,
): DecodeSourceResult? =
    withContext(Dispatchers.Main.immediate) {
        val viewContentSize =
            view.resolveViewSize().takeIf { it.isNotEmpty } ?: return@withContext null

        try {
            withContext(Dispatchers.IO) {
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                imageSource.newInputStream().buffered().use {
                    BitmapFactory.decodeStream(it, null, boundsOptions)
                }
                val mimeType = boundsOptions.outMimeType
                val imageSize = boundsOptions.let { Size(it.outWidth, it.outHeight) }
                    .takeIf { it.isNotEmpty }

                if (imageSize != null) {
                    val sampleSize = calculateSampleSize(imageSize, viewContentSize, mimeType)
                    val sampleBitmap = imageSource.newInputStream().buffered().use {
                        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                        })
                    }
                    if (sampleBitmap != null) {
                        val exifOrientation = imageSource.readExifOrientationWithMimeType(mimeType)
                        DecodeSourceResult(mimeType, imageSize, exifOrientation, sampleBitmap)
                            .appliedExifOrientation(inBitmapHelper)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

fun DecodeSourceResult.appliedExifOrientation(
    inBitmapHelper: InBitmapHelper,
): DecodeSourceResult {
    if (exifOrientation == ExifInterface.ORIENTATION_UNDEFINED
        || exifOrientation == ExifInterface.ORIENTATION_NORMAL
    ) {
        return this
    }
    val exifOrientationHelper = ExifOrientationHelper(exifOrientation)
    val inputBitmap = sampledBitmap
    val newSampleBitmap =
        exifOrientationHelper.applyToBitmap(inputBitmap, inBitmapHelper) ?: return this
    inBitmapHelper.freeBitmap(inputBitmap, "appliedExifOrientation")
//    logger.d("appliedExifOrientation") {
//        "appliedExifOrientation. freeBitmap. bitmap=${inputBitmap.logString}. ${request.key}"
//    }

    val newSize = exifOrientationHelper.applyToSize(imageSize)
//    sketch.logger.d("appliedExifOrientation") {
//        "appliedExifOrientation. successful. ${newBitmap.logString}. ${imageInfo}. ${request.key}"
//    }
    return DecodeSourceResult(
        mimeType = mimeType,
        imageSize = newSize,
        exifOrientation = exifOrientation,
        sampledBitmap = newSampleBitmap,
    )
}

val Bitmap.logString: String
    get() = "Bitmap(${width}x${height},$config,@${Integer.toHexString(this.hashCode())})"

private suspend fun View.resolveViewSize(): Size = withContext(Dispatchers.Main.immediate) {
    var viewContentSize = contentSize()
    if (viewContentSize.isEmpty) {
        // Slow path: wait for the view to be measured.
        viewContentSize = suspendCancellableCoroutine { continuation ->
            val viewTreeObserver = viewTreeObserver

            val preDrawListener = object : OnPreDrawListener {
                private var isResumed = false

                override fun onPreDraw(): Boolean {
                    val size = contentSize()
                    if (size.isNotEmpty) {
                        viewTreeObserver.removePreDrawListenerSafe(this@resolveViewSize, this)
                        if (!isResumed) {
                            isResumed = true
                            continuation.resume(size)
                        }
                    }
                    return true
                }
            }

            viewTreeObserver.addOnPreDrawListener(preDrawListener)

            continuation.invokeOnCancellation {
                viewTreeObserver.removePreDrawListenerSafe(this@resolveViewSize, preDrawListener)
            }
        }
    }
    viewContentSize
}

private fun ViewTreeObserver.removePreDrawListenerSafe(view: View, victim: OnPreDrawListener) {
    if (isAlive) {
        removeOnPreDrawListener(victim)
    } else {
        view.viewTreeObserver.removeOnPreDrawListener(victim)
    }
}