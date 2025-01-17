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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.ProgressListeners
import com.github.panpf.sketch.test.utils.DownloadProgressListenerSupervisor
import com.github.panpf.sketch.test.utils.getTestContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressListenersTest {

    @Test
    fun test() {
        val context = getTestContext()
        val request = DownloadRequest(context, "http://sample.com/sample.jpeg")

        val list = listOf(
            DownloadProgressListenerSupervisor("2"),
            DownloadProgressListenerSupervisor("3"),
            DownloadProgressListenerSupervisor("1"),
        )
        Assert.assertEquals(listOf<String>(), list.flatMap { it.callbackActionList })

        val listeners = ProgressListeners(*list.toTypedArray())
        Assert.assertEquals(list, listeners.progressListenerList)

        runBlocking(Dispatchers.Main) {
            listeners.onUpdateProgress(request, 100, 10)
        }
        Assert.assertEquals(
            listOf("10:2", "10:3", "10:1"),
            list.flatMap { it.callbackActionList })

        runBlocking(Dispatchers.Main) {
            listeners.onUpdateProgress(request, 100, 20)
        }
        Assert.assertEquals(
            listOf("10:2", "20:2", "10:3", "20:3", "10:1", "20:1"),
            list.flatMap { it.callbackActionList })
    }
}