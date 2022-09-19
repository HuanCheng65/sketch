package com.github.panpf.sketch.zoom.util

import kotlin.math.ceil
import kotlin.math.floor


/**
 * Calculate the sample size, support for BitmapFactory or ImageDecoder
 */
fun calculateSampleSize(
    imageSize: Size, targetSize: Size, mimeType: String? = null
): Int {
    val targetPixels = targetSize.width * targetSize.height
    var sampleSize = 1
    while (true) {
        val bitmapSize = calculateSampledBitmapSize(imageSize, sampleSize, mimeType)
        if (bitmapSize.width * bitmapSize.height <= targetPixels) {
            break
        } else {
            sampleSize *= 2
        }
    }
    return sampleSize
}

/**
 * Calculate the size of the sampled Bitmap, support for BitmapFactory or ImageDecoder
 */
fun calculateSampledBitmapSize(
    imageSize: Size, sampleSize: Int, mimeType: String? = null
): Size {
    val widthValue = imageSize.width / sampleSize.toDouble()
    val heightValue = imageSize.height / sampleSize.toDouble()
    val isPNGFormat = "image/png".equals(mimeType, ignoreCase = true)
    val width: Int
    val height: Int
    if (isPNGFormat) {
        width = floor(widthValue).toInt()
        height = floor(heightValue).toInt()
    } else {
        width = ceil(widthValue).toInt()
        height = ceil(heightValue).toInt()
    }
    return Size(width, height)
}