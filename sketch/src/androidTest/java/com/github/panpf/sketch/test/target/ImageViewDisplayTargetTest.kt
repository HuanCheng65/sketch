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
package com.github.panpf.sketch.test.target

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.RGB_565
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.panpf.sketch.cache.CountBitmap
import com.github.panpf.sketch.datasource.DataFrom.LOCAL
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.drawable.SketchCountBitmapDrawable
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.sketch
import com.github.panpf.sketch.target.ImageViewDisplayTarget
import com.github.panpf.sketch.test.utils.getTestContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageViewDisplayTargetTest {

    @Test
    fun testDrawable() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val sketch = context.sketch
        val request = DisplayRequest(context, newAssetUri("sample.jpeg"))

        val imageView = ImageView(context)
        Assert.assertNull(imageView.drawable)

        val imageViewTarget = ImageViewDisplayTarget(imageView)
        Assert.assertNull(imageViewTarget.drawable)

        val countBitmap = CountBitmap(
            cacheKey = request.cacheKey,
            bitmap = Bitmap.createBitmap(100, 100, RGB_565),
            logger = sketch.logger,
            bitmapPool = sketch.bitmapPool,
        )
        val sketchCountBitmapDrawable = SketchCountBitmapDrawable(
            resources = context.resources,
            countBitmap = countBitmap,
            imageUri = request.uriString,
            requestKey = request.key,
            requestCacheKey = request.cacheKey,
            imageInfo = ImageInfo(100, 100, "image/jpeg", 0),
            transformedList = null,
            extras = null,
            dataFrom = LOCAL
        )
        val countBitmap2 = CountBitmap(
            cacheKey = request.cacheKey,
            bitmap = Bitmap.createBitmap(100, 100, RGB_565),
            logger = sketch.logger,
            bitmapPool = sketch.bitmapPool,
        )
        val sketchCountBitmapDrawable2 = SketchCountBitmapDrawable(
            resources = context.resources,
            countBitmap = countBitmap2,
            imageUri = request.uriString,
            requestKey = request.key,
            requestCacheKey = request.cacheKey,
            imageInfo = ImageInfo(100, 100, "image/jpeg", 0),
            transformedList = null,
            extras = null,
            dataFrom = LOCAL
        )

        runBlocking(Dispatchers.Main) {
            Assert.assertEquals(0, countBitmap.getDisplayedCount())
            Assert.assertEquals(0, countBitmap2.getDisplayedCount())
        }

        runBlocking(Dispatchers.Main) {
            imageViewTarget.drawable = sketchCountBitmapDrawable
        }

        Assert.assertSame(sketchCountBitmapDrawable, imageView.drawable)
        Assert.assertSame(sketchCountBitmapDrawable, imageViewTarget.drawable)
        runBlocking(Dispatchers.Main) {
            Assert.assertEquals(1, countBitmap.getDisplayedCount())
            Assert.assertEquals(0, countBitmap2.getDisplayedCount())
        }

        runBlocking(Dispatchers.Main) {
            imageViewTarget.drawable = sketchCountBitmapDrawable2
        }

        Assert.assertSame(sketchCountBitmapDrawable2, imageView.drawable)
        Assert.assertSame(sketchCountBitmapDrawable2, imageViewTarget.drawable)
        runBlocking(Dispatchers.Main) {
            Assert.assertEquals(0, countBitmap.getDisplayedCount())
            Assert.assertEquals(1, countBitmap2.getDisplayedCount())
        }

        runBlocking(Dispatchers.Main) {
            imageViewTarget.drawable = null
        }
        Assert.assertNull(imageView.drawable)
        Assert.assertNull(imageViewTarget.drawable)
    }

    @Test
    fun testEqualsAndHashCode() {
        val context = getTestContext()
        val imageView1 = ImageView(context)
        val imageView2 = ImageView(context)
        val element1 = ImageViewDisplayTarget(imageView1)
        val element11 = ImageViewDisplayTarget(imageView1)
        val element2 = ImageViewDisplayTarget(imageView2)

        Assert.assertNotSame(element1, element11)
        Assert.assertNotSame(element1, element2)
        Assert.assertNotSame(element2, element11)

        Assert.assertEquals(element1, element1)
        Assert.assertEquals(element1, element11)
        Assert.assertNotEquals(element1, element2)
        Assert.assertNotEquals(element2, element11)
        Assert.assertNotEquals(element1, null)
        Assert.assertNotEquals(element1, Any())

        Assert.assertEquals(element1.hashCode(), element1.hashCode())
        Assert.assertEquals(element1.hashCode(), element11.hashCode())
        Assert.assertNotEquals(element1.hashCode(), element2.hashCode())
        Assert.assertNotEquals(element2.hashCode(), element11.hashCode())
    }
}