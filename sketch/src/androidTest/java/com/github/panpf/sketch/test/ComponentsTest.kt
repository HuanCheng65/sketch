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
package com.github.panpf.sketch.test

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.Components
import com.github.panpf.sketch.decode.BitmapDecodeInterceptor
import com.github.panpf.sketch.decode.DrawableDecodeInterceptor
import com.github.panpf.sketch.decode.GifAnimatedDrawableDecoder
import com.github.panpf.sketch.decode.internal.DefaultBitmapDecoder
import com.github.panpf.sketch.decode.internal.DefaultDrawableDecoder
import com.github.panpf.sketch.decode.internal.EngineBitmapDecodeInterceptor
import com.github.panpf.sketch.decode.internal.EngineDrawableDecodeInterceptor
import com.github.panpf.sketch.decode.internal.XmlDrawableBitmapDecoder
import com.github.panpf.sketch.fetch.AssetUriFetcher
import com.github.panpf.sketch.fetch.Base64UriFetcher
import com.github.panpf.sketch.fetch.HttpUriFetcher
import com.github.panpf.sketch.fetch.ResourceUriFetcher
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.RequestInterceptor
import com.github.panpf.sketch.request.internal.EngineRequestInterceptor
import com.github.panpf.sketch.request.internal.MemoryCacheRequestInterceptor
import com.github.panpf.sketch.request.internal.RequestContext
import com.github.panpf.sketch.test.utils.AllFetcher
import com.github.panpf.sketch.test.utils.Test2BitmapDecodeInterceptor
import com.github.panpf.sketch.test.utils.Test2DrawableDecodeInterceptor
import com.github.panpf.sketch.test.utils.Test2RequestInterceptor
import com.github.panpf.sketch.test.utils.Test3DrawableDecodeInterceptor
import com.github.panpf.sketch.test.utils.TestAssets
import com.github.panpf.sketch.test.utils.TestBitmapDecodeInterceptor
import com.github.panpf.sketch.test.utils.TestBitmapDecoder
import com.github.panpf.sketch.test.utils.TestDrawableDecodeInterceptor
import com.github.panpf.sketch.test.utils.TestDrawableDecoder
import com.github.panpf.sketch.test.utils.TestRequestInterceptor
import com.github.panpf.sketch.test.utils.getTestContext
import com.github.panpf.sketch.test.utils.getTestContextAndNewSketch
import com.github.panpf.sketch.test.utils.newSketch
import com.github.panpf.sketch.transform.internal.BitmapTransformationDecodeInterceptor
import com.github.panpf.tools4j.test.ktx.assertNoThrow
import com.github.panpf.tools4j.test.ktx.assertThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsTest {

    @Test
    fun testRequestInterceptors() {
        val (context, sketch) = getTestContextAndNewSketch()
        val emptyRequest = DisplayRequest(context, "")
        val notEmptyRequest = DisplayRequest(context, "") {
            components {
                addRequestInterceptor(TestRequestInterceptor())
                addRequestInterceptor(Test2RequestInterceptor())
            }
        }

        Components(sketch, ComponentRegistry.Builder().build()).apply {
            Assert.assertEquals(
                listOf<RequestInterceptor>(),
                getRequestInterceptorList(emptyRequest)
            )
            Assert.assertEquals(
                listOf(TestRequestInterceptor(), Test2RequestInterceptor()),
                getRequestInterceptorList(notEmptyRequest)
            )
        }

        Components(sketch, ComponentRegistry.Builder().apply {
            addRequestInterceptor(MemoryCacheRequestInterceptor())
            addRequestInterceptor(EngineRequestInterceptor())
        }.build()).apply {
            Assert.assertEquals(
                listOf(MemoryCacheRequestInterceptor(), EngineRequestInterceptor()),
                getRequestInterceptorList(emptyRequest)
            )
            Assert.assertEquals(
                listOf(
                    TestRequestInterceptor(),
                    Test2RequestInterceptor(),
                    MemoryCacheRequestInterceptor(),
                    EngineRequestInterceptor()
                ),
                getRequestInterceptorList(notEmptyRequest)
            )
        }
    }

    @Test
    fun testBitmapDecodeInterceptors() {
        val (context, sketch) = getTestContextAndNewSketch()
        val emptyRequest = DisplayRequest(context, "")
        val notEmptyRequest = DisplayRequest(context, "") {
            components {
                addBitmapDecodeInterceptor(TestBitmapDecodeInterceptor())
                addBitmapDecodeInterceptor(Test2BitmapDecodeInterceptor())
            }
        }

        Components(sketch, ComponentRegistry.Builder().build()).apply {
            Assert.assertEquals(
                listOf<BitmapDecodeInterceptor>(),
                getBitmapDecodeInterceptorList(emptyRequest)
            )
            Assert.assertEquals(
                listOf(
                    TestBitmapDecodeInterceptor(),
                    Test2BitmapDecodeInterceptor(),
                ),
                getBitmapDecodeInterceptorList(notEmptyRequest)
            )
        }

        Components(sketch, ComponentRegistry.Builder().apply {
            addBitmapDecodeInterceptor(BitmapTransformationDecodeInterceptor())
            addBitmapDecodeInterceptor(EngineBitmapDecodeInterceptor())
        }.build()).apply {
            Assert.assertEquals(
                listOf(
                    BitmapTransformationDecodeInterceptor(),
                    EngineBitmapDecodeInterceptor()
                ),
                getBitmapDecodeInterceptorList(emptyRequest)
            )
            Assert.assertEquals(
                listOf(
                    TestBitmapDecodeInterceptor(),
                    Test2BitmapDecodeInterceptor(),
                    BitmapTransformationDecodeInterceptor(),
                    EngineBitmapDecodeInterceptor()
                ),
                getBitmapDecodeInterceptorList(notEmptyRequest)
            )
        }
    }

    @Test
    fun testDrawableDecodeInterceptors() {
        val (context, sketch) = getTestContextAndNewSketch()
        val emptyRequest = DisplayRequest(context, "")
        val notEmptyRequest = DisplayRequest(context, "") {
            components {
                addDrawableDecodeInterceptor(TestDrawableDecodeInterceptor())
                addDrawableDecodeInterceptor(Test2DrawableDecodeInterceptor())
            }
        }

        Components(sketch, ComponentRegistry.Builder().build()).apply {
            Assert.assertEquals(
                listOf<DrawableDecodeInterceptor>(),
                getDrawableDecodeInterceptorList(emptyRequest)
            )
            Assert.assertEquals(
                listOf(
                    TestDrawableDecodeInterceptor(),
                    Test2DrawableDecodeInterceptor(),
                ),
                getDrawableDecodeInterceptorList(notEmptyRequest)
            )
        }

        Components(sketch, ComponentRegistry.Builder().apply {
            addDrawableDecodeInterceptor(Test3DrawableDecodeInterceptor())
            addDrawableDecodeInterceptor(EngineDrawableDecodeInterceptor())
        }.build()).apply {
            Assert.assertEquals(
                listOf(Test3DrawableDecodeInterceptor(), EngineDrawableDecodeInterceptor()),
                getDrawableDecodeInterceptorList(emptyRequest)
            )
            Assert.assertEquals(
                listOf(
                    TestDrawableDecodeInterceptor(),
                    Test2DrawableDecodeInterceptor(),
                    Test3DrawableDecodeInterceptor(),
                    EngineDrawableDecodeInterceptor()
                ),
                getDrawableDecodeInterceptorList(notEmptyRequest)
            )
        }
    }

    @Test
    fun testNewFetcher() {
        val context = getTestContext()
        val sketch = newSketch()

        Components(sketch, ComponentRegistry.Builder().build()).apply {
            assertThrow(IllegalStateException::class) {
                runBlocking(Dispatchers.Main) {
                    newFetcher(DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI))
                }
            }
        }

        Components(sketch, ComponentRegistry.Builder().build()).apply {
            assertThrow(IllegalArgumentException::class) {
                newFetcher(DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI))
            }
            assertThrow(IllegalArgumentException::class) {
                newFetcher(DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                    components {
                        addFetcher(HttpUriFetcher.Factory())
                    }
                })
            }
            assertNoThrow {
                newFetcher(DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                    components {
                        addFetcher(AssetUriFetcher.Factory())
                    }
                })
            }

            assertThrow(IllegalArgumentException::class) {
                newFetcher(DisplayRequest(context, "http://sample.com/sample.jpeg"))
            }
            assertThrow(IllegalArgumentException::class) {
                newFetcher(DisplayRequest(context, "http://sample.com/sample.jpeg") {
                    components {
                        addFetcher(AssetUriFetcher.Factory())
                    }
                })
            }
            assertNoThrow {
                newFetcher(DisplayRequest(context, "http://sample.com/sample.jpeg") {
                    components {
                        addFetcher(HttpUriFetcher.Factory())
                    }
                })
            }
        }

        Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(AssetUriFetcher.Factory())
            addFetcher(HttpUriFetcher.Factory())
        }.build()).apply {
            Assert.assertTrue(
                newFetcher(DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI)) is AssetUriFetcher
            )
            Assert.assertTrue(
                newFetcher(
                    DisplayRequest(context, "http://sample.com/sample.jpeg")
                ) is HttpUriFetcher
            )
            assertThrow(IllegalArgumentException::class) {
                newFetcher(DisplayRequest(context, "file:///sdcard/sample.jpeg"))
            }

            Assert.assertTrue(newFetcher(DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                components {
                    addFetcher(AllFetcher.Factory())
                }
            }) is AllFetcher)
            Assert.assertTrue(newFetcher(DisplayRequest(context, "http://sample.com/sample.jpeg") {
                components {
                    addFetcher(AllFetcher.Factory())
                }
            }) is AllFetcher)
            Assert.assertTrue(newFetcher(DisplayRequest(context, "file:///sdcard/sample.jpeg") {
                components {
                    addFetcher(AllFetcher.Factory())
                }
            }) is AllFetcher)
        }
    }

    @Test
    fun testNewBitmapDecoder() {
        val context = getTestContext()
        val sketch = newSketch()

        Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(AssetUriFetcher.Factory())
        }.build()).apply {
            assertThrow(IllegalStateException::class) {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI)
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                runBlocking(Dispatchers.Main) {
                    newBitmapDecoder(request, requestContext, fetchResult)
                }
            }
        }

        Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(AssetUriFetcher.Factory())
        }.build()).apply {
            assertThrow(IllegalArgumentException::class) {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI)
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                newBitmapDecoder(request, requestContext, fetchResult)
            }
            assertThrow(IllegalArgumentException::class) {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                    components {
                        addBitmapDecoder(XmlDrawableBitmapDecoder.Factory())
                    }
                }
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                newBitmapDecoder(request, requestContext, fetchResult)
            }
            assertNoThrow {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                    components {
                        addBitmapDecoder(DefaultBitmapDecoder.Factory())
                    }
                }
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                newBitmapDecoder(request, requestContext, fetchResult)
            }
        }

        Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(AssetUriFetcher.Factory())
            addBitmapDecoder(DefaultBitmapDecoder.Factory())
        }.build()).apply {
            assertNoThrow {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI)
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                Assert.assertTrue(
                    newBitmapDecoder(request, requestContext, fetchResult) is DefaultBitmapDecoder
                )
            }

            assertNoThrow {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                    components {
                        addBitmapDecoder(TestBitmapDecoder.Factory())
                    }
                }
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                Assert.assertTrue(
                    newBitmapDecoder(request, requestContext, fetchResult) is TestBitmapDecoder
                )
            }
        }
    }

    @Test
    fun testNewDrawableDecoder() {
        val context = getTestContext()
        val sketch = newSketch()

        Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(AssetUriFetcher.Factory())
        }.build()).apply {
            assertThrow(IllegalStateException::class) {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI)
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                runBlocking(Dispatchers.Main) {
                    newDrawableDecoder(request, requestContext, fetchResult)
                }
            }
        }

        Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(AssetUriFetcher.Factory())
        }.build()).apply {
            assertThrow(IllegalArgumentException::class) {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI)
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                newDrawableDecoder(request, requestContext, fetchResult)
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                assertThrow(IllegalArgumentException::class) {
                    val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                        components {
                            addDrawableDecoder(GifAnimatedDrawableDecoder.Factory())
                        }
                    }
                    val requestContext = RequestContext(request)
                    val fetchResult = runBlocking { newFetcher(request).fetch() }
                    newDrawableDecoder(request, requestContext, fetchResult)
                }
            }
            assertNoThrow {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                    components {
                        addDrawableDecoder(DefaultDrawableDecoder.Factory())
                    }
                }
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                newDrawableDecoder(request, requestContext, fetchResult)
            }
        }

        Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(AssetUriFetcher.Factory())
            addDrawableDecoder(DefaultDrawableDecoder.Factory())
        }.build()).apply {
            assertNoThrow {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI)
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                Assert.assertTrue(
                    newDrawableDecoder(
                        request,
                        requestContext,
                        fetchResult
                    ) is DefaultDrawableDecoder
                )
            }
            assertNoThrow {
                val request = DisplayRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                    components {
                        addDrawableDecoder(TestDrawableDecoder.Factory())
                    }
                }
                val requestContext = RequestContext(request)
                val fetchResult = runBlocking { newFetcher(request).fetch() }
                Assert.assertTrue(
                    newDrawableDecoder(request, requestContext, fetchResult) is TestDrawableDecoder
                )
            }
        }
    }

    @Test
    fun testToString() {
        val sketch = newSketch()
        Components(sketch, ComponentRegistry.Builder().build()).apply {
            Assert.assertEquals(
                "Components(ComponentRegistry(" +
                        "fetcherFactoryList=[]," +
                        "bitmapDecoderFactoryList=[]," +
                        "drawableDecoderFactoryList=[]," +
                        "requestInterceptorList=[]," +
                        "bitmapDecodeInterceptorList=[]," +
                        "drawableDecodeInterceptorList=[]" +
                        "))",
                toString()
            )
        }
        Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(HttpUriFetcher.Factory())
            addFetcher(Base64UriFetcher.Factory())
            addFetcher(ResourceUriFetcher.Factory())
            addBitmapDecoder(XmlDrawableBitmapDecoder.Factory())
            addBitmapDecoder(DefaultBitmapDecoder.Factory())
            addDrawableDecoder(DefaultDrawableDecoder.Factory())
            addRequestInterceptor(EngineRequestInterceptor())
            addBitmapDecodeInterceptor(EngineBitmapDecodeInterceptor())
            addBitmapDecodeInterceptor(BitmapTransformationDecodeInterceptor())
            addDrawableDecodeInterceptor(EngineDrawableDecodeInterceptor())
        }.build()).apply {
            Assert.assertEquals(
                "Components(ComponentRegistry(" +
                        "fetcherFactoryList=[HttpUriFetcher,Base64UriFetcher,ResourceUriFetcher]," +
                        "bitmapDecoderFactoryList=[XmlDrawableBitmapDecoder,DefaultBitmapDecoder]," +
                        "drawableDecoderFactoryList=[DefaultDrawableDecoder]," +
                        "requestInterceptorList=[EngineRequestInterceptor]," +
                        "bitmapDecodeInterceptorList=[EngineBitmapDecodeInterceptor,BitmapTransformationDecodeInterceptor]," +
                        "drawableDecodeInterceptorList=[EngineDrawableDecodeInterceptor]" +
                        "))",
                toString()
            )
        }
    }

    @Test
    fun testEquals() {
        val sketch = newSketch()
        val components0 = Components(sketch, ComponentRegistry.Builder().build())
        val components1 = Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(HttpUriFetcher.Factory())
        }.build())
        val components11 = Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(HttpUriFetcher.Factory())
        }.build())
        val components2 = Components(sketch, ComponentRegistry.Builder().apply {
            addBitmapDecoder(XmlDrawableBitmapDecoder.Factory())
        }.build())
        val components3 = Components(sketch, ComponentRegistry.Builder().apply {
            addDrawableDecoder(DefaultDrawableDecoder.Factory())
        }.build())
        val components4 = Components(sketch, ComponentRegistry.Builder().apply {
            addRequestInterceptor(EngineRequestInterceptor())
        }.build())
        val components5 = Components(sketch, ComponentRegistry.Builder().apply {
            addBitmapDecodeInterceptor(EngineBitmapDecodeInterceptor())
        }.build())
        val components6 = Components(sketch, ComponentRegistry.Builder().apply {
            addDrawableDecodeInterceptor(EngineDrawableDecodeInterceptor())
        }.build())

        Assert.assertEquals(components0, components0)
        Assert.assertEquals(components1, components11)
        Assert.assertNotEquals(components1, Any())
        Assert.assertNotEquals(components1, null)
        Assert.assertNotEquals(components0, components1)
        Assert.assertNotEquals(components0, components2)
        Assert.assertNotEquals(components0, components3)
        Assert.assertNotEquals(components0, components4)
        Assert.assertNotEquals(components0, components5)
        Assert.assertNotEquals(components0, components6)
        Assert.assertNotEquals(components1, components2)
        Assert.assertNotEquals(components1, components3)
        Assert.assertNotEquals(components1, components4)
        Assert.assertNotEquals(components1, components5)
        Assert.assertNotEquals(components1, components6)
        Assert.assertNotEquals(components2, components3)
        Assert.assertNotEquals(components2, components4)
        Assert.assertNotEquals(components2, components5)
        Assert.assertNotEquals(components2, components6)
        Assert.assertNotEquals(components3, components4)
        Assert.assertNotEquals(components3, components5)
        Assert.assertNotEquals(components3, components6)
        Assert.assertNotEquals(components4, components5)
        Assert.assertNotEquals(components4, components6)
        Assert.assertNotEquals(components5, components6)
    }

    @Test
    fun testHashCode() {
        val sketch = newSketch()
        val components0 = Components(sketch, ComponentRegistry.Builder().build())
        val components1 = Components(sketch, ComponentRegistry.Builder().apply {
            addFetcher(HttpUriFetcher.Factory())
        }.build())
        val components2 = Components(sketch, ComponentRegistry.Builder().apply {
            addBitmapDecoder(XmlDrawableBitmapDecoder.Factory())
        }.build())
        val components3 = Components(sketch, ComponentRegistry.Builder().apply {
            addDrawableDecoder(DefaultDrawableDecoder.Factory())
        }.build())
        val components4 = Components(sketch, ComponentRegistry.Builder().apply {
            addRequestInterceptor(EngineRequestInterceptor())
        }.build())
        val components5 = Components(sketch, ComponentRegistry.Builder().apply {
            addBitmapDecodeInterceptor(EngineBitmapDecodeInterceptor())
        }.build())
        val components6 = Components(sketch, ComponentRegistry.Builder().apply {
            addDrawableDecodeInterceptor(EngineDrawableDecodeInterceptor())
        }.build())

        Assert.assertNotEquals(components0.hashCode(), components1.hashCode())
        Assert.assertNotEquals(components0.hashCode(), components2.hashCode())
        Assert.assertNotEquals(components0.hashCode(), components3.hashCode())
        Assert.assertNotEquals(components0.hashCode(), components4.hashCode())
        Assert.assertNotEquals(components0.hashCode(), components5.hashCode())
        Assert.assertNotEquals(components0.hashCode(), components6.hashCode())
        Assert.assertNotEquals(components1.hashCode(), components2.hashCode())
        Assert.assertNotEquals(components1.hashCode(), components3.hashCode())
        Assert.assertNotEquals(components1.hashCode(), components4.hashCode())
        Assert.assertNotEquals(components1.hashCode(), components5.hashCode())
        Assert.assertNotEquals(components1.hashCode(), components6.hashCode())
        Assert.assertNotEquals(components2.hashCode(), components3.hashCode())
        Assert.assertNotEquals(components2.hashCode(), components4.hashCode())
        Assert.assertNotEquals(components2.hashCode(), components5.hashCode())
        Assert.assertNotEquals(components2.hashCode(), components6.hashCode())
        Assert.assertNotEquals(components3.hashCode(), components4.hashCode())
        Assert.assertNotEquals(components3.hashCode(), components5.hashCode())
        Assert.assertNotEquals(components3.hashCode(), components6.hashCode())
        Assert.assertNotEquals(components4.hashCode(), components5.hashCode())
        Assert.assertNotEquals(components4.hashCode(), components6.hashCode())
        Assert.assertNotEquals(components5.hashCode(), components6.hashCode())
    }
}