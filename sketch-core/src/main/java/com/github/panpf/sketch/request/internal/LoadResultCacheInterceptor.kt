package com.github.panpf.sketch.request.internal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.cache.isReadOrWrite
import com.github.panpf.sketch.request.DataFrom.DISK_CACHE
import com.github.panpf.sketch.request.ImageInfo
import com.github.panpf.sketch.request.Interceptor
import com.github.panpf.sketch.request.LoadData
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.newDecodeOptionsByQualityParams
import com.github.panpf.sketch.util.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class LoadResultCacheInterceptor : Interceptor<LoadRequest, LoadData> {

    companion object {
        const val MODULE = "LoadResultCacheInterceptor"
    }

    override suspend fun intercept(
        sketch: Sketch,
        chain: Interceptor.Chain<LoadRequest, LoadData>,
    ): LoadData {
        val request = chain.request
        val resultCacheHelper = ResultCacheHelper.from(sketch, request)
        resultCacheHelper?.lock?.lock()
        try {
            return resultCacheHelper?.read(sketch.decodeTaskDispatcher)
                ?: chain.proceed(sketch, request).apply {
                    resultCacheHelper?.write(this, sketch.decodeTaskDispatcher)
                }
        } finally {
            resultCacheHelper?.lock?.unlock()
        }
    }

    private class ResultCacheHelper(
        private val request: LoadRequest,
        private val diskCache: DiskCache,
        private val logger: Logger,
        val encodedBitmapDataDiskCacheKey: String,
        val encodedMetaDataDiskCacheKey: String,
    ) {

        val lock: Mutex by lazy {
            diskCache.getOrCreateEditMutexLock(encodedBitmapDataDiskCacheKey)
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        suspend fun read(context: CoroutineContext): LoadData? = withContext(context) {
            if (request.resultDiskCachePolicy.readEnabled) {
                val bitmapDataDiskCacheEntry = diskCache[encodedBitmapDataDiskCacheKey]
                val metaDataDiskCacheEntry = diskCache[encodedMetaDataDiskCacheKey]
                try {
                    if (bitmapDataDiskCacheEntry != null && metaDataDiskCacheEntry != null) {
                        val jsonString = metaDataDiskCacheEntry.newInputStream().use {
                            it.bufferedReader().readText()
                        }
                        val imageInfo = ImageInfo.fromJsonString(jsonString)
                        val bitmap = BitmapFactory.decodeFile(
                            bitmapDataDiskCacheEntry.file.path,
                            request.newDecodeOptionsByQualityParams(imageInfo.mimeType)
                        )
                        if (bitmap.width > 1 && bitmap.height > 1) {
                            LoadData(bitmap, imageInfo, DISK_CACHE)
                        } else {
                            bitmap.recycle()
                            logger.e(
                                MODULE,
                                "Invalid image size in result cache. size=%dx%d, uri=%s, diskCacheKey=%s".format(
                                    bitmap.width,
                                    bitmap.height,
                                    request.uriString,
                                    encodedBitmapDataDiskCacheKey
                                )
                            )
                            bitmapDataDiskCacheEntry.delete()
                            metaDataDiskCacheEntry.delete()
                            null
                        }
                    } else {
                        bitmapDataDiskCacheEntry?.delete()
                        metaDataDiskCacheEntry?.delete()
                        null
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    bitmapDataDiskCacheEntry?.delete()
                    metaDataDiskCacheEntry?.delete()
                    null
                }
            } else {
                null
            }
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        suspend fun write(result: LoadData, context: CoroutineContext) {
            withContext(context) {
                if (request.resultDiskCachePolicy.writeEnabled) {
                    val bitmapDataEditor =
                        diskCache.edit(encodedBitmapDataDiskCacheKey) ?: return@withContext
                    try {
                        bitmapDataEditor.newOutputStream().use {
                            result.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        bitmapDataEditor.commit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        bitmapDataEditor.abort()
                        return@withContext
                    }

                    val metaDataEditor =
                        diskCache.edit(encodedMetaDataDiskCacheKey) ?: return@withContext
                    try {
                        metaDataEditor.newOutputStream().use {
                            it.bufferedWriter().write(result.info.toJsonString())
                        }
                        metaDataEditor.commit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        metaDataEditor.abort()
                        diskCache[encodedBitmapDataDiskCacheKey]?.delete()
                        return@withContext
                    }
                }
            }
        }

        companion object {

            @JvmStatic
            fun from(sketch: Sketch, request: LoadRequest): ResultCacheHelper? {
                if (!request.resultDiskCachePolicy.isReadOrWrite) return null
                val bitmapDataDiskCacheKey = request.resultDiskCacheKey ?: return null
                val diskCache: DiskCache = sketch.diskCache
                val metaDataDiskCacheKey = "${bitmapDataDiskCacheKey}_metadata"
                val encodedBitmapDataDiskCacheKey = diskCache.encodeKey(bitmapDataDiskCacheKey)
                val encodedMetaDataDiskCacheKey = diskCache.encodeKey(metaDataDiskCacheKey)
                return ResultCacheHelper(
                    request,
                    diskCache,
                    sketch.logger,
                    encodedBitmapDataDiskCacheKey,
                    encodedMetaDataDiskCacheKey
                )
            }
        }
    }
}