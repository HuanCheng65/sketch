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
package com.github.panpf.sketch.test.fetch

import android.content.res.Resources
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.fetch.ResourceUriFetcher
import com.github.panpf.sketch.fetch.newResourceUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.test.R.drawable
import com.github.panpf.sketch.test.utils.getTestContext
import com.github.panpf.sketch.test.utils.getTestContextAndNewSketch
import com.github.panpf.tools4j.test.ktx.assertNoThrow
import com.github.panpf.tools4j.test.ktx.assertThrow
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResourceUriFetcherTest {

    @Test
    fun testNewUri() {
        val context = getTestContext()

        Assert.assertEquals(
            "android.resource://resource?resType=drawable&resName=ic_launcher",
            newResourceUri("drawable", "ic_launcher")
        )
        Assert.assertEquals(
            "android.resource://resource?resType=drawable1&resName=ic_launcher1",
            newResourceUri("drawable1", "ic_launcher1")
        )

        Assert.assertEquals(
            "android.resource://resource?resId=55345",
            newResourceUri(55345)
        )
        Assert.assertEquals(
            "android.resource://resource?resId=55346",
            newResourceUri(55346)
        )

        Assert.assertEquals(
            "android.resource://resource?packageName=testPackage&resType=drawable&resName=ic_launcher",
            newResourceUri("testPackage", "drawable", "ic_launcher")
        )
        Assert.assertEquals(
            "android.resource://resource?packageName=testPackage1&resType=drawable1&resName=ic_launcher1",
            newResourceUri("testPackage1", "drawable1", "ic_launcher1")
        )

        Assert.assertEquals(
            "android.resource://resource?packageName=testPackage&resId=55345",
            newResourceUri("testPackage", 55345)
        )
        Assert.assertEquals(
            "android.resource://resource?packageName=testPackage1&resId=55346",
            newResourceUri("testPackage1", 55346)
        )

        Assert.assertEquals(
            "android.resource://resource?packageName=${context.packageName}&resType=drawable&resName=ic_launcher",
            context.newResourceUri("drawable", "ic_launcher")
        )
        Assert.assertEquals(
            "android.resource://resource?packageName=${context.packageName}&resType=drawable1&resName=ic_launcher1",
            context.newResourceUri("drawable1", "ic_launcher1")
        )

        Assert.assertEquals(
            "android.resource://resource?packageName=${context.packageName}&resId=55345",
            context.newResourceUri(55345)
        )
        Assert.assertEquals(
            "android.resource://resource?packageName=${context.packageName}&resId=55346",
            context.newResourceUri(55346)
        )
    }

    @Test
    fun testFactory() {
        val (context, sketch) = getTestContextAndNewSketch()
        val testAppPackage = context.packageName
        val fetcherFactory = ResourceUriFetcher.Factory()
        val androidResUriByName = newResourceUri(testAppPackage, "drawable", "ic_launcher")
        val androidResUriById = newResourceUri(testAppPackage, drawable.ic_launcher)
        val httpUri = "http://sample.com/sample.jpg"
        val contentUri = "content://sample_app/sample"

        fetcherFactory.create(sketch, LoadRequest(context, androidResUriByName))!!.apply {
            Assert.assertEquals(androidResUriByName, this.contentUri.toString())
        }
        fetcherFactory.create(sketch, LoadRequest(context, androidResUriById))!!.apply {
            Assert.assertEquals(androidResUriById, this.contentUri.toString())
        }
        fetcherFactory.create(sketch, DisplayRequest(context, androidResUriByName))!!.apply {
            Assert.assertEquals(androidResUriByName, this.contentUri.toString())
        }
        fetcherFactory.create(sketch, DisplayRequest(context, androidResUriById))!!.apply {
            Assert.assertEquals(androidResUriById, this.contentUri.toString())
        }
        fetcherFactory.create(sketch, DownloadRequest(context, androidResUriByName))!!.apply {
            Assert.assertEquals(androidResUriByName, this.contentUri.toString())
        }
        fetcherFactory.create(sketch, DownloadRequest(context, androidResUriById))!!.apply {
            Assert.assertEquals(androidResUriById, this.contentUri.toString())
        }
        Assert.assertNull(fetcherFactory.create(sketch, LoadRequest(context, httpUri)))
        Assert.assertNull(fetcherFactory.create(sketch, LoadRequest(context, contentUri)))
    }

    @Test
    fun testFactoryEqualsAndHashCode() {
        val element1 = ResourceUriFetcher.Factory()
        val element11 = ResourceUriFetcher.Factory()

        Assert.assertEquals(element1, element1)
        Assert.assertEquals(element1, element11)

        Assert.assertNotEquals(element1, Any())
        Assert.assertNotEquals(element1, null)

        Assert.assertEquals(element1.hashCode(), element1.hashCode())
        Assert.assertEquals(element1.hashCode(), element11.hashCode())
    }

    @Test
    fun testFetch() {
        val (context, sketch) = getTestContextAndNewSketch()

        assertNoThrow {
            runBlocking {
                newResourceUri("drawable", "ic_launcher").let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }
        assertNoThrow {
            runBlocking {
                newResourceUri(drawable.ic_launcher).let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }

        assertNoThrow {
            runBlocking {
                newResourceUri(context.packageName, "drawable", "ic_launcher").let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }
        assertNoThrow {
            runBlocking {
                newResourceUri(context.packageName, drawable.ic_launcher).let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }

        assertNoThrow {
            runBlocking {
                context.newResourceUri("drawable", "ic_launcher").let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }
        assertNoThrow {
            runBlocking {
                context.newResourceUri(drawable.ic_launcher).let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }

        assertThrow(Resources.NotFoundException::class) {
            runBlocking {
                "${ResourceUriFetcher.SCHEME}://".let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }

        assertThrow(Resources.NotFoundException::class) {
            runBlocking {
                "${ResourceUriFetcher.SCHEME}://resource?packageName=fakePackageName".let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }

        assertThrow(Resources.NotFoundException::class) {
            runBlocking {
                "${ResourceUriFetcher.SCHEME}://resource?packageName=${context.packageName}&resId=errorResId".let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }

        assertThrow(Resources.NotFoundException::class) {
            runBlocking {
                "${ResourceUriFetcher.SCHEME}://resource?packageName=${context.packageName}&resType=drawable&resName=34&error".let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }

        assertThrow(Resources.NotFoundException::class) {
            runBlocking {
                "${ResourceUriFetcher.SCHEME}://resource?packageName=${context.packageName}&resType=drawable&resName=0".let {
                    ResourceUriFetcher(sketch, LoadRequest(context, it), Uri.parse(it))
                }.fetch()
            }
        }
    }
}