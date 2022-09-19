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
package com.github.panpf.sketch.zoom.internal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.github.panpf.sketch.zoom.ImageSource
import com.github.panpf.sketch.zoom.SubsamplingAbility
import com.github.panpf.sketch.zoom.Tile
import com.github.panpf.sketch.zoom.util.ExifOrientationHelper
import com.github.panpf.sketch.zoom.util.Size
import com.github.panpf.sketch.zoom.util.logString
import com.github.panpf.sketch.zoom.ImageInfo
import com.github.panpf.sketch.zoom.ZoomAbilityContainer
import java.util.LinkedList

class TileDecoder internal constructor(
    private val imageSource: ImageSource,
    private val imageInfo: ImageInfo,
    private val container: ZoomAbilityContainer,
) {

    private val decoderPool = LinkedList<BitmapRegionDecoder>()
    private var _destroyed: Boolean = false
    private var disableInBitmap: Boolean = false
    private val exifOrientationHelper = ExifOrientationHelper(imageInfo.exifOrientation)
    private val addedImageSize: Size by lazy { exifOrientationHelper.addToSize(imageSize) }

    val imageSize: Size = imageInfo.size

    @Suppress("MemberVisibilityCanBePrivate")
    val destroyed: Boolean
        get() = _destroyed

    @WorkerThread
    fun decode(tile: Tile): Bitmap? {
        requiredWorkThread()

        if (_destroyed) return null
        return useDecoder { decoder ->
            decodeRegion(decoder, tile.srcRect, tile.inSampleSize)?.let {
                applyExifOrientation(it)
            }
        }
    }

    @WorkerThread
    private fun decodeRegion(
        regionDecoder: BitmapRegionDecoder,
        srcRect: Rect,
        inSampleSize: Int
    ): Bitmap? {
        requiredWorkThread()

        val imageSize = imageSize
        val newSrcRect = exifOrientationHelper.addToRect(srcRect, imageSize)
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        if (!disableInBitmap) {
            container.inBitmapHelper.setInBitmapForRegion(
                options = decodeOptions,
                regionSize = Size(newSrcRect.width(), newSrcRect.height()),
                imageMimeType = imageInfo.mimeType,
                imageSize = addedImageSize
            )
        }
        container.logger.d(SubsamplingAbility.MODULE) {
            "decodeRegion. inBitmap=${decodeOptions.inBitmap?.logString} ${imageSource.key}}"
        }

        return try {
            regionDecoder.decodeRegion(newSrcRect, decodeOptions)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            val inBitmap = decodeOptions.inBitmap
            if (inBitmap != null && container.inBitmapHelper.isInBitmapError(throwable)) {
                disableInBitmap = true
                container.logger.e(SubsamplingAbility.MODULE, throwable) {
                    "decodeRegion. Bitmap region decode inBitmap error. ${imageSource.key}"
                }

                container.inBitmapHelper.freeBitmap(inBitmap, "tile:decodeRegion:error")
                container.logger.d(SubsamplingAbility.MODULE) {
                    "decodeRegion. freeBitmap. inBitmap error. bitmap=${inBitmap.logString}. ${imageSource.key}"
                }

                decodeOptions.inBitmap = null
                try {
                    regionDecoder.decodeRegion(newSrcRect, decodeOptions)
                } catch (throwable1: Throwable) {
                    throwable1.printStackTrace()
                    container.logger.e(SubsamplingAbility.MODULE, throwable) {
                        "decodeRegion. Bitmap region decode error. srcRect=${newSrcRect}. ${imageSource.key}"
                    }
                    null
                }
            } else if (container.inBitmapHelper.isSrcRectError(throwable)) {
                container.logger.e(SubsamplingAbility.MODULE, throwable) {
                    "decodeRegion. Bitmap region decode srcRect error. imageSize=$imageSize, srcRect=$newSrcRect, inSampleSize=${decodeOptions.inSampleSize}. ${imageSource.key}"
                }
                null
            } else {
                null
            }
        }
    }

    @WorkerThread
    private fun applyExifOrientation(bitmap: Bitmap): Bitmap {
        requiredWorkThread()

        val newBitmap = exifOrientationHelper.applyToBitmap(bitmap, container.inBitmapHelper)
        return if (newBitmap != null && newBitmap != bitmap) {
            container.inBitmapHelper.freeBitmap(bitmap, "tile:applyExifOrientation")
            container.logger.d(SubsamplingAbility.MODULE) {
                "applyExifOrientation. freeBitmap. bitmap=${bitmap.logString}. ${imageSource.key}"
            }
            newBitmap
        } else {
            bitmap
        }
    }

    @MainThread
    fun destroy() {
        requiredMainThread()

        synchronized(decoderPool) {
            _destroyed = true
            decoderPool.forEach {
                it.recycle()
            }
            decoderPool.clear()
        }
    }

    @WorkerThread
    private fun useDecoder(block: (decoder: BitmapRegionDecoder) -> Bitmap?): Bitmap? {
        requiredWorkThread()

        synchronized(decoderPool) {
            if (destroyed) {
                return null
            }
        }

        val bitmapRegionDecoder = synchronized(decoderPool) {
            decoderPool.poll()
        } ?: imageSource.newInputStream().buffered().use {
            if (VERSION.SDK_INT >= VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(it)
            } else {
                @Suppress("DEPRECATION")
                BitmapRegionDecoder.newInstance(it, false)
            }
        } ?: return null

        val bitmap = block(bitmapRegionDecoder)

        synchronized(decoderPool) {
            if (destroyed) {
                bitmapRegionDecoder.recycle()
            } else {
                decoderPool.add(bitmapRegionDecoder)
            }
        }

        return bitmap
    }
}