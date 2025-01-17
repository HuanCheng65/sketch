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

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.exifinterface.media.ExifInterface
import com.github.panpf.sketch.datasource.AssetDataSource
import com.github.panpf.sketch.datasource.ByteArrayDataSource
import com.github.panpf.sketch.datasource.ContentDataSource
import com.github.panpf.sketch.datasource.DataSource
import com.github.panpf.sketch.datasource.ResourceDataSource
import com.github.panpf.sketch.decode.DrawableDecodeResult
import com.github.panpf.sketch.decode.DrawableDecoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.drawable.SketchAnimatableDrawable
import com.github.panpf.sketch.drawable.internal.ScaledAnimatedImageDrawable
import com.github.panpf.sketch.request.ANIMATION_REPEAT_INFINITE
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.animatable2CompatCallbackOf
import com.github.panpf.sketch.request.animatedTransformation
import com.github.panpf.sketch.request.animationEndCallback
import com.github.panpf.sketch.request.animationStartCallback
import com.github.panpf.sketch.request.repeatCount
import com.github.panpf.sketch.transform.asPostProcessor
import com.github.panpf.sketch.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Only the following attributes are supported:
 *
 * resize.size
 *
 * resize.precision: It is always LESS_PIXELS
 *
 * colorSpace
 *
 * repeatCount
 *
 * animatedTransformation
 *
 * onAnimationStart
 *
 * onAnimationEnd
 */
@RequiresApi(Build.VERSION_CODES.P)
abstract class BaseAnimatedImageDrawableDecoder(
    private val request: ImageRequest,
    private val dataSource: DataSource,
) : DrawableDecoder {

    @WorkerThread
    override suspend fun decode(): DrawableDecodeResult {
        val source = when (dataSource) {
            is AssetDataSource -> {
                ImageDecoder.createSource(request.context.assets, dataSource.assetFileName)
            }
            is ResourceDataSource -> {
                ImageDecoder.createSource(dataSource.resources, dataSource.drawableId)
            }
            is ContentDataSource -> {
                ImageDecoder.createSource(request.context.contentResolver, dataSource.contentUri)
            }
            is ByteArrayDataSource -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ImageDecoder.createSource(dataSource.data)
                } else {
                    ImageDecoder.createSource(ByteBuffer.wrap(dataSource.data))
                }
            }
            else -> {
                // Currently running on a limited number of IO contexts, so this warning can be ignored
                @Suppress("BlockingMethodInNonBlockingContext")
                ImageDecoder.createSource(dataSource.file())
            }
        }

        var imageInfo: ImageInfo? = null
        var inSampleSize = 1
        var imageDecoder: ImageDecoder? = null
        val drawable = try {
            // Currently running on a limited number of IO contexts, so this warning can be ignored
            @Suppress("BlockingMethodInNonBlockingContext")
            ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                imageDecoder = decoder
                imageInfo = ImageInfo(
                    info.size.width,
                    info.size.height,
                    info.mimeType,
                    ExifInterface.ORIENTATION_UNDEFINED
                )
                val resize = request.resize
                if (resize != null) {
                    inSampleSize = calculateSampleSize(
                        imageSize = Size(info.size.width, info.size.height),
                        targetSize = Size(resize.width, resize.height)
                    )
                    decoder.setTargetSampleSize(inSampleSize)

                    request.colorSpace?.let {
                        decoder.setTargetColorSpace(it)
                    }

                    // Set the animated transformation to be applied on each frame.
                    decoder.postProcessor = request.animatedTransformation?.asPostProcessor()
                }
            }
        } finally {
            imageDecoder?.close()
        }
        if (drawable !is AnimatedImageDrawable) {
            throw Exception("Only support AnimatedImageDrawable")
        }
        drawable.repeatCount = request.repeatCount
            ?.takeIf { it != ANIMATION_REPEAT_INFINITE }
            ?: AnimatedImageDrawable.REPEAT_INFINITE

        val transformedList =
            if (inSampleSize != 1) listOf(createInSampledTransformed(inSampleSize)) else null
        val animatableDrawable = SketchAnimatableDrawable(
            // AnimatedImageDrawable cannot be scaled using bounds, which will be exposed in the ResizeDrawable
            // Use ScaledAnimatedImageDrawable package solution to this it
            animatableDrawable = ScaledAnimatedImageDrawable(drawable),
            imageUri = request.uriString,
            requestKey = request.key,
            requestCacheKey = request.cacheKey,
            imageInfo = imageInfo!!,
            dataFrom = dataSource.dataFrom,
            transformedList = transformedList,
            extras = null,
        ).apply {
            val onStart = request.animationStartCallback
            val onEnd = request.animationEndCallback
            if (onStart != null || onEnd != null) {
                withContext(Dispatchers.Main) {
                    registerAnimationCallback(animatable2CompatCallbackOf(onStart, onEnd))
                }
            }
        }
        return DrawableDecodeResult(
            drawable = animatableDrawable,
            imageInfo = animatableDrawable.imageInfo,
            dataFrom = animatableDrawable.dataFrom,
            transformedList = animatableDrawable.transformedList,
            extras = animatableDrawable.extras,
        )
    }
}