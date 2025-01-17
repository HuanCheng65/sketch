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
package com.github.panpf.sketch.test.request

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.datasource.DataFrom.DOWNLOAD_CACHE
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.OneShotDisposable
import com.github.panpf.sketch.request.internal.requestManager
import com.github.panpf.sketch.test.utils.TestAssets
import com.github.panpf.sketch.test.utils.getTestContext
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DisposableTest {

    @Test
    fun testOneShotDisposable() {
        runBlocking {
            val job = async {
                delay(100)
                delay(100)
                delay(100)
            }
            val disposable = OneShotDisposable(job)
            Assert.assertFalse(disposable.isDisposed)
            delay(100)
            Assert.assertFalse(disposable.isDisposed)
            disposable.dispose()
            delay(100)
            Assert.assertTrue(disposable.isDisposed)
            disposable.dispose()
        }
    }

    @Test
    fun testViewTargetDisposable() {
        val context = getTestContext()
        runBlocking {
            val view = ImageView(context)
            val job = async<DisplayResult> {
                delay(100)
                delay(100)
                delay(100)
                DisplayResult.Success(
                    request = DisplayRequest(view, TestAssets.SAMPLE_JPEG_URI),
                    drawable = ColorDrawable(Color.BLACK),
                    imageInfo = ImageInfo(100, 100, "image/jpeg", 0),
                    dataFrom = DOWNLOAD_CACHE,
                    transformedList = null,
                    extras = null,
                )
            }

            val disposable = view.requestManager.getDisposable(job)
            Assert.assertFalse(disposable.isDisposed)
            delay(100)
            Assert.assertFalse(disposable.isDisposed)
            disposable.dispose()
            delay(100)
            Assert.assertTrue(disposable.isDisposed)
            disposable.dispose()

            disposable.job = job
        }
    }
}