package com.github.panpf.sketch

import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.fetch.newResourceUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.Disposable
import com.github.panpf.sketch.util.SketchUtils
import java.io.File

fun ImageView.displayImage(
    uri: String?,
    configBlock: (DisplayRequest.Builder.() -> Unit)? = null
): Disposable<DisplayResult> =
    context.sketch.enqueue(DisplayRequest(uri, this, configBlock))

fun ImageView.displayImage(
    @DrawableRes drawableResId: Int?,
    configBlock: (DisplayRequest.Builder.() -> Unit)? = null
): Disposable<DisplayResult> =
    displayImage(drawableResId?.let { context.newResourceUri(it) }, configBlock)

fun ImageView.displayImage(
    uri: Uri?,
    configBlock: (DisplayRequest.Builder.() -> Unit)? = null
): Disposable<DisplayResult> =
    displayImage(uri?.toString(), configBlock)

fun ImageView.displayImage(
    file: File?,
    configBlock: (DisplayRequest.Builder.() -> Unit)? = null
): Disposable<DisplayResult> =
    displayImage(file?.let { newFileUri(it.path) }, configBlock)

fun ImageView.displayAssetImage(
    assetFileName: String?,
    configBlock: (DisplayRequest.Builder.() -> Unit)? = null
): Disposable<DisplayResult> =
    displayImage(assetFileName?.let { newAssetUri(assetFileName) }, configBlock)

fun ImageView.displayResourceImage(
    @DrawableRes drawableResId: Int?,
    configBlock: (DisplayRequest.Builder.() -> Unit)? = null
): Disposable<DisplayResult> =
    displayImage(drawableResId?.let { newResourceUri(context.packageName, it) }, configBlock)

fun ImageView.displayResourceImage(
    packageName: String,
    @DrawableRes drawableResId: Int,
    configBlock: (DisplayRequest.Builder.() -> Unit)? = null
): Disposable<DisplayResult> =
    displayImage(newResourceUri(packageName, drawableResId), configBlock)

/**
 * Dispose the request that's attached to this view (if there is one).
 */
@Suppress("NOTHING_TO_INLINE")
inline fun ImageView.dispose() {
    SketchUtils.dispose(this)
}

/**
 * Get the [DisplayResult] of the most recently executed image request that's attached to this view.
 */
inline val ImageView.result: DisplayResult?
    get() = SketchUtils.getResult(this)