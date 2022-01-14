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
package com.github.panpf.sketch.datasource

import android.content.Context
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.request.DataFrom
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.internal.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.io.InputStream

/**
 * 数据源
 */
interface DataSource {

    val sketch: Sketch

    val request: ImageRequest

    val context: Context
        get() = sketch.appContext

    val from: DataFrom

    @Throws(IOException::class)
    fun length(): Long

    @Throws(IOException::class)
    fun newFileDescriptor(): FileDescriptor?

    @Throws(IOException::class)
    fun newInputStream(): InputStream

    @Throws(IOException::class)
    suspend fun file(): File = withContext(Dispatchers.IO) {
        val diskCache = sketch.diskCache
        val encodedKey = sketch.diskCache.encodeKey(request.uriString + "_data_source")
        sketch.diskCache.getOrCreateEditMutexLock(encodedKey).withLock {
            val entry = diskCache[encodedKey]
            if (entry != null) {
                entry
            } else {
                val editor = diskCache.edit(encodedKey)
                    ?: throw IllegalArgumentException("Disk cache cannot be used")
                try {
                    newInputStream().use { inputStream ->
                        editor.newOutputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    editor.commit()
                } catch (e: Throwable) {
                    editor.abort()
                    throw e
                }
                diskCache[encodedKey]
                    ?: throw IllegalArgumentException("Disk cache cannot be used after edit")
            }
        }.file
    }
}