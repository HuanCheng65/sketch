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
package com.github.panpf.sketch.extensions.test.request

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.request.Depth.LOCAL
import com.github.panpf.sketch.request.Depth.MEMORY
import com.github.panpf.sketch.request.Depth.NETWORK
import com.github.panpf.sketch.request.DepthException
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.request.ImageRequest.Builder
import com.github.panpf.sketch.request.PAUSE_LOAD_WHEN_SCROLLING_KEY
import com.github.panpf.sketch.request.ignorePauseLoadWhenScrolling
import com.github.panpf.sketch.request.isCausedByPauseLoadWhenScrolling
import com.github.panpf.sketch.request.isDepthFromPauseLoadWhenScrolling
import com.github.panpf.sketch.request.isIgnoredPauseLoadWhenScrolling
import com.github.panpf.sketch.request.isPauseLoadWhenScrolling
import com.github.panpf.sketch.request.pauseLoadWhenScrolling
import com.github.panpf.sketch.util.UnknownException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PauseLoadWhenScrollingExtensionsTest {

    @Test
    fun testPauseLoadWhenScrolling() {
        val context = InstrumentationRegistry.getInstrumentation().context

        DisplayRequest(context, "http://sample.com/sample.jpeg").apply {
            Assert.assertFalse(isPauseLoadWhenScrolling)
        }

        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            (this as Builder).pauseLoadWhenScrolling()
        }.apply {
            Assert.assertTrue(isPauseLoadWhenScrolling)
        }
        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            (this as Builder).pauseLoadWhenScrolling(false)
        }.apply {
            Assert.assertFalse(isPauseLoadWhenScrolling)
        }

        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            pauseLoadWhenScrolling()
        }.apply {
            Assert.assertTrue(isPauseLoadWhenScrolling)
        }
        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            pauseLoadWhenScrolling(false)
        }.apply {
            Assert.assertFalse(isPauseLoadWhenScrolling)
        }

        ImageOptions().apply {
            Assert.assertFalse(isPauseLoadWhenScrolling)
        }

        ImageOptions {
            pauseLoadWhenScrolling()
        }.apply {
            Assert.assertTrue(isPauseLoadWhenScrolling)
        }
        ImageOptions {
            pauseLoadWhenScrolling(false)
        }.apply {
            Assert.assertFalse(isPauseLoadWhenScrolling)
        }

        val key1 = DisplayRequest(context, newAssetUri("sample.svg")).key
        val key2 = DisplayRequest(context, newAssetUri("sample.svg")) {
            pauseLoadWhenScrolling()
        }.key
        Assert.assertNotEquals(key1, key2)

        val cacheKey1 = DisplayRequest(context, newAssetUri("sample.svg")).cacheKey
        val cacheKey2 = DisplayRequest(context, newAssetUri("sample.svg")) {
            pauseLoadWhenScrolling(true)
        }.cacheKey
        Assert.assertEquals(cacheKey1, cacheKey2)
    }

    @Test
    fun testIgnorePauseLoadWhenScrolling() {
        val context = InstrumentationRegistry.getInstrumentation().context

        DisplayRequest(context, "http://sample.com/sample.jpeg").apply {
            Assert.assertFalse(isIgnoredPauseLoadWhenScrolling)
        }

        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            (this as Builder).ignorePauseLoadWhenScrolling()
        }.apply {
            Assert.assertTrue(isIgnoredPauseLoadWhenScrolling)
        }
        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            (this as Builder).ignorePauseLoadWhenScrolling(false)
        }.apply {
            Assert.assertFalse(isIgnoredPauseLoadWhenScrolling)
        }

        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            ignorePauseLoadWhenScrolling()
        }.apply {
            Assert.assertTrue(isIgnoredPauseLoadWhenScrolling)
        }
        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            ignorePauseLoadWhenScrolling(false)
        }.apply {
            Assert.assertFalse(isIgnoredPauseLoadWhenScrolling)
        }

        ImageOptions().apply {
            Assert.assertFalse(isIgnoredPauseLoadWhenScrolling)
        }

        ImageOptions {
            ignorePauseLoadWhenScrolling()
        }.apply {
            Assert.assertTrue(isIgnoredPauseLoadWhenScrolling)
        }
        ImageOptions {
            ignorePauseLoadWhenScrolling(false)
        }.apply {
            Assert.assertFalse(isIgnoredPauseLoadWhenScrolling)
        }

        val key1 = DisplayRequest(context, newAssetUri("sample.svg")).key
        val key2 = DisplayRequest(context, newAssetUri("sample.svg")) {
            ignorePauseLoadWhenScrolling()
        }.key
        Assert.assertNotEquals(key1, key2)

        val cacheKey1 = DisplayRequest(context, newAssetUri("sample.svg")).cacheKey
        val cacheKey2 = DisplayRequest(context, newAssetUri("sample.svg")) {
            ignorePauseLoadWhenScrolling(true)
        }.cacheKey
        Assert.assertEquals(cacheKey1, cacheKey2)
    }

    @Test
    fun testSetDepthFromPauseLoadWhenScrolling() {
        val context = InstrumentationRegistry.getInstrumentation().context

        DisplayRequest(context, "http://sample.com/sample.jpeg").apply {
            Assert.assertFalse(isDepthFromPauseLoadWhenScrolling)
        }

        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            (this as Builder).depth(NETWORK, PAUSE_LOAD_WHEN_SCROLLING_KEY)
        }.apply {
            Assert.assertTrue(isDepthFromPauseLoadWhenScrolling)
        }
        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            (this as Builder).depth(NETWORK, "$PAUSE_LOAD_WHEN_SCROLLING_KEY:error")
        }.apply {
            Assert.assertFalse(isDepthFromPauseLoadWhenScrolling)
        }

        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            depth(NETWORK, PAUSE_LOAD_WHEN_SCROLLING_KEY)
        }.apply {
            Assert.assertTrue(isDepthFromPauseLoadWhenScrolling)
        }
        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            depth(NETWORK, "$PAUSE_LOAD_WHEN_SCROLLING_KEY:error")
        }.apply {
            Assert.assertFalse(isDepthFromPauseLoadWhenScrolling)
        }

        ImageOptions().apply {
            Assert.assertFalse(isDepthFromPauseLoadWhenScrolling)
        }

        ImageOptions {
            depth(NETWORK, PAUSE_LOAD_WHEN_SCROLLING_KEY)
        }.apply {
            Assert.assertTrue(isDepthFromPauseLoadWhenScrolling)
        }
        ImageOptions {
            depth(NETWORK, "$PAUSE_LOAD_WHEN_SCROLLING_KEY:error")
        }.apply {
            Assert.assertFalse(isDepthFromPauseLoadWhenScrolling)
        }

        val key1 = DisplayRequest(context, newAssetUri("sample.svg")).key
        val key2 = DisplayRequest(context, newAssetUri("sample.svg")) {
            depth(NETWORK, PAUSE_LOAD_WHEN_SCROLLING_KEY)
        }.key
        Assert.assertNotEquals(key1, key2)

        val cacheKey1 = DisplayRequest(context, newAssetUri("sample.svg")).cacheKey
        val cacheKey2 = DisplayRequest(context, newAssetUri("sample.svg")) {
            depth(NETWORK, PAUSE_LOAD_WHEN_SCROLLING_KEY)
        }.cacheKey
        Assert.assertEquals(cacheKey1, cacheKey2)
    }

    @Test
    fun testIsCausedByPauseLoadWhenScrolling() {
        val context = InstrumentationRegistry.getInstrumentation().context

        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            depth(MEMORY, PAUSE_LOAD_WHEN_SCROLLING_KEY)
        }.apply {
            Assert.assertTrue(isCausedByPauseLoadWhenScrolling(this, DepthException("")))
        }

        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            depth(MEMORY, PAUSE_LOAD_WHEN_SCROLLING_KEY)
        }.apply {
            Assert.assertFalse(isCausedByPauseLoadWhenScrolling(this, UnknownException("")))
        }

        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            depth(LOCAL, PAUSE_LOAD_WHEN_SCROLLING_KEY)
        }.apply {
            Assert.assertFalse(isCausedByPauseLoadWhenScrolling(this, DepthException("")))
        }

        DisplayRequest(context, "http://sample.com/sample.jpeg") {
            depth(MEMORY)
        }.apply {
            Assert.assertFalse(isCausedByPauseLoadWhenScrolling(this, DepthException("")))
        }
    }
}