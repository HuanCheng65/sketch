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
package com.github.panpf.sketch.test.datasource

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.datasource.ByteArrayDataSource
import com.github.panpf.sketch.datasource.DataFrom
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.test.utils.getTestContextAndNewSketch
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ByteArrayDataSourceTest {

    @Test
    fun testConstructor() {
        val (context, sketch) = getTestContextAndNewSketch()
        val request = LoadRequest(context, "http://sample.jpeg")
        ByteArrayDataSource(
            sketch = sketch,
            request = request,
            dataFrom = DataFrom.MEMORY,
            data = "fd5717876ab046b8aa889c9aaac4b56c8j5f3".toByteArray()
        ).apply {
            Assert.assertTrue(sketch === this.sketch)
            Assert.assertTrue(request === this.request)
            Assert.assertEquals(37, this.length())
            Assert.assertEquals(DataFrom.MEMORY, this.dataFrom)
        }
    }

    @Test
    fun testLength() {
        val (context, sketch) = getTestContextAndNewSketch()
        val request = LoadRequest(context, "http://sample.jpeg")

        ByteArrayDataSource(
            sketch = sketch,
            request = request,
            dataFrom = DataFrom.MEMORY,
            data = "fd5717876ab046b8aa889c9aaac4b56c8j5f3".toByteArray()
        ).apply {
            Assert.assertEquals(37, length())
            Assert.assertEquals(37, length())
        }
    }

    @Test
    fun testNewInputStream() {
        val (context, sketch) = getTestContextAndNewSketch()
        ByteArrayDataSource(
            sketch = sketch,
            request = LoadRequest(context, "http://sample.jpeg"),
            dataFrom = DataFrom.MEMORY,
            data = "fd5717876ab046b8aa889c9aaac4b56c8j5f3".toByteArray()
        ).apply {
            newInputStream().close()
        }
    }

    @Test
    fun testFile() {
        val (context, sketch) = getTestContextAndNewSketch()
        ByteArrayDataSource(
            sketch = sketch,
            request = LoadRequest(context, "http://sample.jpeg"),
            dataFrom = DataFrom.MEMORY,
            data = "fd5717876ab046b8aa889c9aaac4b56c8j5f3".toByteArray()
        ).apply {
            val file = file()
            Assert.assertEquals("369c0aa172a8ac158a372f9b00fbd220.0", file.name)
        }
    }

    @Test
    fun testToString() {
        val (context, sketch) = getTestContextAndNewSketch()
        ByteArrayDataSource(
            sketch = sketch,
            request = LoadRequest(context, "http://sample.jpeg"),
            dataFrom = DataFrom.MEMORY,
            data = "fd5717876ab046b8aa889c9aaac4b56c8j5f3".toByteArray()
        ).apply {
            Assert.assertEquals(
                "ByteArrayDataSource(from=MEMORY,length=37)",
                toString()
            )
        }
    }
}