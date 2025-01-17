/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.sketch.decode.internal

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.Px
import androidx.exifinterface.media.ExifInterface
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.DataFrom
import com.github.panpf.sketch.datasource.DataSource
import com.github.panpf.sketch.decode.BitmapDecodeResult
import com.github.panpf.sketch.decode.DecodeConfig
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.ImageInvalidException
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.resize.Precision.LESS_PIXELS
import com.github.panpf.sketch.resize.Resize
import com.github.panpf.sketch.resize.calculateResizeMapping
import com.github.panpf.sketch.util.Bytes
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.util.safeConfig
import com.github.panpf.sketch.util.scaled
import com.github.panpf.sketch.util.toHexString
import java.io.IOException
import kotlin.experimental.and
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

val maxBitmapSize: Size by lazy {
    OpenGLTextureHelper.maxSize?.let {
        Size(it, it)
    } ?: Canvas().let {
        Size(it.maximumBitmapWidth, it.maximumBitmapHeight)
    }
}

/**
 * Calculate the size of the sampled Bitmap, support for BitmapFactory or ImageDecoder
 */
fun calculateSampledBitmapSize(
    imageSize: Size, sampleSize: Int, mimeType: String? = null
): Size {
    val widthValue = imageSize.width / sampleSize.toDouble()
    val heightValue = imageSize.height / sampleSize.toDouble()
    val isPNGFormat = ImageFormat.PNG.matched(mimeType)
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

/**
 * Calculate the size of the sampled Bitmap, support for BitmapRegionDecoder
 */
fun calculateSampledBitmapSizeForRegion(
    regionSize: Size, sampleSize: Int, mimeType: String? = null, imageSize: Size? = null
): Size {
    val widthValue = regionSize.width / sampleSize.toDouble()
    val heightValue = regionSize.height / sampleSize.toDouble()
    val width: Int
    val height: Int
    val isPNGFormat = ImageFormat.PNG.matched(mimeType)
    if (!isPNGFormat && VERSION.SDK_INT >= VERSION_CODES.N && regionSize == imageSize) {
        width = ceil(widthValue).toInt()
        height = ceil(heightValue).toInt()
    } else {
        width = floor(widthValue).toInt()
        height = floor(heightValue).toInt()
    }
    return Size(width, height)
}


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
    return limitedSampleSizeByMaxBitmapSize(sampleSize, imageSize, mimeType)
}

/**
 * Calculate the sample size, support for BitmapRegionDecoder
 */
fun calculateSampleSizeForRegion(
    regionSize: Size, targetSize: Size, mimeType: String? = null, imageSize: Size? = null
): Int {
    val targetPixels = targetSize.width * targetSize.height
    var sampleSize = 1
    while (true) {
        val bitmapSize = calculateSampledBitmapSizeForRegion(
            regionSize, sampleSize, mimeType, imageSize
        )
        if (bitmapSize.width * bitmapSize.height <= targetPixels) {
            break
        } else {
            sampleSize *= 2
        }
    }
    return limitedSampleSizeByMaxBitmapSizeForRegion(
        regionSize, sampleSize, mimeType, imageSize
    )
}


/*
 * The width and height limit cannot be greater than the maximum size allowed by OpenGL, support for BitmapFactory or ImageDecoder
 */
fun limitedSampleSizeByMaxBitmapSize(
    sampleSize: Int, imageSize: Size, mimeType: String? = null
): Int {
    val maxBitmapSize = maxBitmapSize
    var finalSampleSize = sampleSize.coerceAtLeast(1)
    while (true) {
        val bitmapSize = calculateSampledBitmapSize(imageSize, finalSampleSize, mimeType)
        if (bitmapSize.width <= maxBitmapSize.width && bitmapSize.height <= maxBitmapSize.height) {
            break
        } else {
            finalSampleSize *= 2
        }
    }
    return finalSampleSize
}

/*
 * The width and height limit cannot be greater than the maximum size allowed by OpenGL, support for BitmapRegionDecoder
 */
fun limitedSampleSizeByMaxBitmapSizeForRegion(
    regionSize: Size, sampleSize: Int, mimeType: String? = null, imageSize: Size? = null
): Int {
    val maximumBitmapSize = maxBitmapSize
    var finalSampleSize = sampleSize.coerceAtLeast(1)
    while (true) {
        val bitmapSize = calculateSampledBitmapSizeForRegion(
            regionSize, finalSampleSize, mimeType, imageSize
        )
        if (bitmapSize.width <= maximumBitmapSize.width && bitmapSize.height <= maximumBitmapSize.height) {
            break
        } else {
            finalSampleSize *= 2
        }
    }
    return finalSampleSize
}


fun computeSizeMultiplier(
    @Px srcWidth: Int,
    @Px srcHeight: Int,
    @Px dstWidth: Int,
    @Px dstHeight: Int,
    fitScale: Boolean
): Double {
    val widthPercent = dstWidth / srcWidth.toDouble()
    val heightPercent = dstHeight / srcHeight.toDouble()
    return if (fitScale) {
        min(widthPercent, heightPercent)
    } else {
        max(widthPercent, heightPercent)
    }
}

fun realDecode(
    request: ImageRequest,
    dataFrom: DataFrom,
    imageInfo: ImageInfo,
    decodeFull: (decodeConfig: DecodeConfig) -> Bitmap,
    decodeRegion: ((srcRect: Rect, decodeConfig: DecodeConfig) -> Bitmap)?
): BitmapDecodeResult {
    val exifOrientationHelper = ExifOrientationHelper(imageInfo.exifOrientation)
    val resize = request.resize
    val imageSize = Size(imageInfo.width, imageInfo.height)
    val appliedImageSize = exifOrientationHelper.applyToSize(imageSize)
    val addedResize = resize?.let { exifOrientationHelper.addToResize(it, appliedImageSize) }
    val decodeConfig = request.newDecodeConfigByQualityParams(imageInfo.mimeType)
    val transformedList = mutableListOf<String>()
    val precision = if (addedResize?.shouldClip(imageInfo.width, imageInfo.height) == true) {
        addedResize.getPrecision(imageInfo.width, imageInfo.height)
    } else {
        null
    }
    val bitmap = if (
        addedResize != null
        && precision != null
        && precision != LESS_PIXELS
        && decodeRegion != null
    ) {
        val scale = addedResize.getScale(imageInfo.width, imageInfo.height)
        val resizeMapping = calculateResizeMapping(
            imageWidth = imageInfo.width,
            imageHeight = imageInfo.height,
            resizeWidth = addedResize.width,
            resizeHeight = addedResize.height,
            precision = precision,
            resizeScale = scale,
        )
        val sampleSize = calculateSampleSizeForRegion(
            regionSize = Size(resizeMapping.srcRect.width(), resizeMapping.srcRect.height()),
            targetSize = Size(resizeMapping.destRect.width(), resizeMapping.destRect.height()),
            mimeType = imageInfo.mimeType,
            imageSize = imageSize
        )
        if (sampleSize > 1) {
            transformedList.add(createInSampledTransformed(sampleSize))
        }
        transformedList.add(createSubsamplingTransformed(resizeMapping.srcRect))
        decodeConfig.inSampleSize = sampleSize
        decodeRegion(resizeMapping.srcRect, decodeConfig)
    } else {
        val sampleSize = if (addedResize != null) {
            val targetSize = Size(addedResize.width, addedResize.height)
            calculateSampleSize(imageSize, targetSize, imageInfo.mimeType)
        } else {
            limitedSampleSizeByMaxBitmapSize(1, imageSize, imageInfo.mimeType)
        }
        if (sampleSize > 1) {
            transformedList.add(0, createInSampledTransformed(sampleSize))
        }
        decodeConfig.inSampleSize = sampleSize
        decodeFull(decodeConfig)
    }
    return BitmapDecodeResult(
        bitmap = bitmap,
        imageInfo = imageInfo,
        dataFrom = dataFrom,
        transformedList = transformedList.takeIf { it.isNotEmpty() }?.toList(),
        extras = null,
    )
}

fun BitmapDecodeResult.appliedExifOrientation(
    sketch: Sketch,
    request: ImageRequest
): BitmapDecodeResult {
    if (imageInfo.exifOrientation == ExifInterface.ORIENTATION_UNDEFINED
        || imageInfo.exifOrientation == ExifInterface.ORIENTATION_NORMAL
    ) {
        return this
    }
    val exifOrientationHelper = ExifOrientationHelper(imageInfo.exifOrientation)
    val inputBitmap = bitmap
    val newBitmap =
        exifOrientationHelper.applyToBitmap(inputBitmap, sketch.bitmapPool) ?: return this
    freeBitmap(sketch.bitmapPool, sketch.logger, inputBitmap, "appliedExifOrientation")
    sketch.logger.d("appliedExifOrientation") {
        "appliedExifOrientation. freeBitmap. bitmap=${inputBitmap.logString}. ${request.key}"
    }

    val newSize = exifOrientationHelper.applyToSize(
        Size(imageInfo.width, imageInfo.height)
    )
    sketch.logger.d("appliedExifOrientation") {
        "appliedExifOrientation. successful. ${newBitmap.logString}. ${imageInfo}. ${request.key}"
    }
    return newResult(
        bitmap = newBitmap,
        imageInfo = imageInfo.newImageInfo(width = newSize.width, height = newSize.height)
    ) {
        addTransformed(createExifOrientationTransformed(imageInfo.exifOrientation))
    }
}

fun BitmapDecodeResult.appliedResize(
    sketch: Sketch,
    request: ImageRequest,
    resize: Resize?
): BitmapDecodeResult {
    if (resize == null) return this
    val inputBitmap = bitmap
    val precision = resize.getPrecision(inputBitmap.width, inputBitmap.height)
    val newBitmap = if (precision == LESS_PIXELS) {
        val sampleSize = calculateSampleSize(
            imageSize = Size(inputBitmap.width, inputBitmap.height),
            targetSize = Size(resize.width, resize.height)
        )
        if (sampleSize != 1) {
            inputBitmap.scaled(1 / sampleSize.toDouble(), sketch.bitmapPool)
        } else {
            null
        }
    } else if (resize.shouldClip(inputBitmap.width, inputBitmap.height)) {
        val scale = resize.getScale(inputBitmap.width, inputBitmap.height)
        val mapping = calculateResizeMapping(
            imageWidth = inputBitmap.width,
            imageHeight = inputBitmap.height,
            resizeWidth = resize.width,
            resizeHeight = resize.height,
            precision = precision,
            resizeScale = scale,
        )
        val config = inputBitmap.safeConfig
        sketch.bitmapPool.getOrCreate(mapping.newWidth, mapping.newHeight, config).apply {
            Canvas(this).drawBitmap(inputBitmap, mapping.srcRect, mapping.destRect, null)
        }
    } else {
        null
    }
    return if (newBitmap != null) {
        sketch.logger.d("appliedResize") {
            "appliedResize. successful. ${newBitmap.logString}. ${imageInfo}. ${request.key}"
        }
        freeBitmap(sketch.bitmapPool, sketch.logger, inputBitmap, "appliedResize")
        sketch.logger.d("appliedResize") {
            "appliedResize. freeBitmap. bitmap=${inputBitmap.logString}. ${request.key}"
        }
        newResult(bitmap = newBitmap) {
            addTransformed(createResizeTransformed(resize))
        }
    } else {
        this
    }
}

@Throws(IOException::class)
fun DataSource.readImageInfoWithBitmapFactory(ignoreExifOrientation: Boolean = false): ImageInfo {
    val boundOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    decodeBitmap(boundOptions)
    val mimeType = boundOptions.outMimeType.orEmpty()
    val exifOrientation = if (!ignoreExifOrientation) {
        readExifOrientationWithMimeType(mimeType)
    } else {
        ExifInterface.ORIENTATION_UNDEFINED
    }
    return ImageInfo(
        width = boundOptions.outWidth,
        height = boundOptions.outHeight,
        mimeType = mimeType,
        exifOrientation = exifOrientation,
    )
}

@Throws(IOException::class, ImageInvalidException::class)
fun DataSource.readImageInfoWithBitmapFactoryOrThrow(ignoreExifOrientation: Boolean = false): ImageInfo {
    val imageInfo = readImageInfoWithBitmapFactory(ignoreExifOrientation)
    val width = imageInfo.width
    val height = imageInfo.height
    if (width <= 0 || height <= 0) {
        throw ImageInvalidException("Invalid image. size=${width}x${height}")
    }
    return imageInfo
}

fun DataSource.readImageInfoWithBitmapFactoryOrNull(ignoreExifOrientation: Boolean = false): ImageInfo? =
    try {
        readImageInfoWithBitmapFactory(ignoreExifOrientation).takeIf {
            it.width > 0 && it.height > 0
        }
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }


@Throws(IOException::class)
fun DataSource.decodeBitmap(options: BitmapFactory.Options? = null): Bitmap? =
    newInputStream().buffered().use {
        BitmapFactory.decodeStream(it, null, options)
    }

fun ImageFormat.supportBitmapRegionDecoder(): Boolean =
    this == ImageFormat.JPEG
            || this == ImageFormat.PNG
            || this == ImageFormat.WEBP
            || (this == ImageFormat.HEIC && VERSION.SDK_INT >= VERSION_CODES.P)
            || (this == ImageFormat.HEIF && VERSION.SDK_INT >= VERSION_CODES.P)

@Throws(IOException::class)
fun DataSource.decodeRegionBitmap(srcRect: Rect, options: BitmapFactory.Options? = null): Bitmap? =
    newInputStream().buffered().use {
        @Suppress("DEPRECATION")
        val regionDecoder = if (VERSION.SDK_INT >= VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(it)
        } else {
            BitmapRegionDecoder.newInstance(it, false)
        }
        try {
            regionDecoder?.decodeRegion(srcRect, options)
        } finally {
            regionDecoder?.recycle()
        }
    }

fun isInBitmapError(throwable: Throwable): Boolean =
    if (throwable is IllegalArgumentException) {
        val message = throwable.message.orEmpty()
        (message == "Problem decoding into existing bitmap" || message.contains("bitmap"))
    } else {
        false
    }

fun isSrcRectError(throwable: Throwable): Boolean =
    if (throwable is IllegalArgumentException) {
        val message = throwable.message.orEmpty()
        message == "rectangle is outside the image srcRect" || message.contains("srcRect")
    } else {
        false
    }

val Bitmap.logString: String
    get() = "Bitmap(${width}x${height},$config,@${toHexString()})"

val Bitmap.sizeString: String
    get() = "${width}x${height}"

fun ImageRequest.newDecodeConfigByQualityParams(mimeType: String): DecodeConfig =
    DecodeConfig().apply {
        @Suppress("DEPRECATION")
        if (VERSION.SDK_INT < VERSION_CODES.N && preferQualityOverSpeed) {
            inPreferQualityOverSpeed = true
        }

        val newConfig = bitmapConfig?.getConfig(mimeType)
        if (newConfig != null) {
            inPreferredConfig = newConfig
        }

        if (VERSION.SDK_INT >= VERSION_CODES.O && colorSpace != null) {
            inPreferredColorSpace = colorSpace
        }
    }

/**
 * If true, indicates that the given mimeType and sampleSize combination can be using 'inBitmap' in BitmapFactory
 *
 * Test results based on the BitmapFactoryTest.testInBitmapAndInSampleSize() method
 */
@SuppressLint("ObsoleteSdkInt")
fun isSupportInBitmap(mimeType: String?, sampleSize: Int): Boolean =
    when (ImageFormat.parseMimeType(mimeType)) {
        ImageFormat.JPEG -> if (sampleSize == 1) VERSION.SDK_INT >= 16 else VERSION.SDK_INT >= 19
        ImageFormat.PNG -> if (sampleSize == 1) VERSION.SDK_INT >= 16 else VERSION.SDK_INT >= 19
        ImageFormat.GIF -> if (sampleSize == 1) VERSION.SDK_INT >= 19 else VERSION.SDK_INT >= 21
        ImageFormat.WEBP -> VERSION.SDK_INT >= 19
//        ImageFormat.WEBP -> VERSION.SDK_INT >= 26 animated
        ImageFormat.BMP -> VERSION.SDK_INT >= 19
        ImageFormat.HEIC -> false
        ImageFormat.HEIF -> VERSION.SDK_INT >= 28
        else -> VERSION.SDK_INT >= 32   // Compatible with new image types supported in the future
    }

/**
 * If true, indicates that the given mimeType can be using 'inBitmap' in BitmapRegionDecoder
 *
 * Test results based on the BitmapRegionDecoderTest.testInBitmapAndInSampleSize() method
 */
@SuppressLint("ObsoleteSdkInt")
fun isSupportInBitmapForRegion(mimeType: String?): Boolean =
    when (ImageFormat.parseMimeType(mimeType)) {
        ImageFormat.JPEG -> VERSION.SDK_INT >= 16
        ImageFormat.PNG -> VERSION.SDK_INT >= 16
        ImageFormat.GIF -> false
        ImageFormat.WEBP -> VERSION.SDK_INT >= 16
//        ImageFormat.WEBP -> VERSION.SDK_INT >= 26 animated
        ImageFormat.BMP -> false
        ImageFormat.HEIC -> VERSION.SDK_INT >= 28
        ImageFormat.HEIF -> VERSION.SDK_INT >= 28
        else -> VERSION.SDK_INT >= 32   // Compatible with new image types supported in the future
    }


// https://developers.google.com/speed/webp/docs/riff_container
private val WEBP_HEADER_RIFF = "RIFF".toByteArray()
private val WEBP_HEADER_WEBP = "WEBP".toByteArray()
private val WEBP_HEADER_VP8X = "VP8X".toByteArray()

// https://nokiatech.github.io/heif/technical.html
private val HEIF_HEADER_FTYP = "ftyp".toByteArray()
private val HEIF_HEADER_MSF1 = "msf1".toByteArray()
private val HEIF_HEADER_HEVC = "hevc".toByteArray()
private val HEIF_HEADER_HEVX = "hevx".toByteArray()

// https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
private val GIF_HEADER_87A = "GIF87a".toByteArray()
private val GIF_HEADER_89A = "GIF89a".toByteArray()

/**
 * Return 'true' if the [Bytes] contains a WebP image.
 */
fun Bytes.isWebP(): Boolean =
    rangeEquals(0, WEBP_HEADER_RIFF) && rangeEquals(8, WEBP_HEADER_WEBP)

/**
 * Return 'true' if the [Bytes] contains an animated WebP image.
 */
fun Bytes.isAnimatedWebP(): Boolean =
    isWebP() && rangeEquals(12, WEBP_HEADER_VP8X) && (get(16) and 0b00000010) > 0

/**
 * Return 'true' if the [Bytes] contains an HEIF image. The [Bytes] is not consumed.
 */
fun Bytes.isHeif(): Boolean = rangeEquals(4, HEIF_HEADER_FTYP)

/**
 * Return 'true' if the [Bytes] contains an animated HEIF image sequence.
 */
fun Bytes.isAnimatedHeif(): Boolean = isHeif()
        && (rangeEquals(8, HEIF_HEADER_MSF1)
        || rangeEquals(8, HEIF_HEADER_HEVC)
        || rangeEquals(8, HEIF_HEADER_HEVX))

/**
 * Return 'true' if the [Bytes] contains a GIF image.
 */
fun Bytes.isGif(): Boolean =
    rangeEquals(0, GIF_HEADER_89A) || rangeEquals(0, GIF_HEADER_87A)