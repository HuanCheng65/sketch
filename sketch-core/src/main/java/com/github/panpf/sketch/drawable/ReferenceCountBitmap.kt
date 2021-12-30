/*
 * Copyright (C) 2019 panpf <panpfpanpf@outlook.com>
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
package com.github.panpf.sketch.drawable

import android.graphics.Bitmap
import com.github.panpf.sketch.cache.BitmapPoolHelper
import com.github.panpf.sketch.request.ImageInfo
import com.github.panpf.sketch.util.SLog

/**
 * 引用 [Bitmap]，能够计算缓存引用、显示引用以及等待显示引用
 */
class ReferenceCountBitmap(
    initBitmap: Bitmap,
    val imageInfo: ImageInfo,
    val requestKey: String,
    private val bitmapPoolHelper: BitmapPoolHelper
) {

    companion object {
        private const val MODULE = "SketchRefBitmap"
    }

    private var bitmapHolder: Bitmap? = initBitmap
    private var memoryCacheRefCount = 0  // 内存缓存引用
    private var displayRefCount = 0 // 真正显示引用
    private var waitingUseRefCount = 0 // 等待使用引用

    @get:Synchronized
    val bitmap: Bitmap?
        get() = bitmapHolder

    @get:Synchronized
    val isRecycled: Boolean
        get() = bitmapHolder?.isRecycled ?: true

    /**
     * 设置显示引用
     *
     * @param callingStation 调用位置
     * @param displayed      显示
     */
    @Synchronized
    fun setIsDisplayed(callingStation: String, displayed: Boolean) {
        if (displayed) {
            displayRefCount++
            referenceChanged(callingStation)
        } else if (displayRefCount > 0) {
            displayRefCount--
            referenceChanged(callingStation)
        }
    }

    /**
     * 设置缓存引用
     *
     * @param callingStation 调用位置
     * @param cached         缓存
     */
    @Synchronized
    fun setIsCached(callingStation: String, cached: Boolean) {
        if (cached) {
            memoryCacheRefCount++
            referenceChanged(callingStation)
        } else if (memoryCacheRefCount > 0) {
            memoryCacheRefCount--
            referenceChanged(callingStation)
        }
    }

    /**
     * 设置等待使用引用
     *
     * @param callingStation 调用位置
     * @param waitingUse     等待使用
     */
    @Synchronized
    fun setIsWaitingUse(callingStation: String, waitingUse: Boolean) {
        if (waitingUse) {
            waitingUseRefCount++
            referenceChanged(callingStation)
        } else if (waitingUseRefCount > 0) {
            waitingUseRefCount--
            referenceChanged(callingStation)
        }
    }

    /**
     * 引用变化时执行此方法
     *
     * @param callingStation 调用位置
     */
    private fun referenceChanged(callingStation: String) {
        val bitmapHolder = this.bitmapHolder
        if (bitmapHolder == null || isRecycled) {
            SLog.emf(MODULE, "Recycled. %s. %s", callingStation, requestKey)
        } else if (memoryCacheRefCount == 0 && displayRefCount == 0 && waitingUseRefCount == 0) {
            bitmapPoolHelper.freeBitmapToPool(bitmapHolder)
            this.bitmapHolder = null
            SLog.dmf(MODULE, "Free. %s. %s", callingStation, requestKey)
        } else {
            SLog.dmf(
                MODULE,
                "Can't free. %s. references(%d,%d,%d). %s",
                callingStation,
                memoryCacheRefCount,
                displayRefCount,
                waitingUseRefCount,
                requestKey
            )
        }
    }
}