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
package com.github.panpf.sketch.test.decode.internal

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.panpf.sketch.datasource.AssetDataSource
import com.github.panpf.sketch.datasource.DataFrom.LOCAL
import com.github.panpf.sketch.datasource.DataFrom.MEMORY
import com.github.panpf.sketch.datasource.FileDataSource
import com.github.panpf.sketch.datasource.ResourceDataSource
import com.github.panpf.sketch.decode.BitmapDecodeResult
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.ImageInvalidException
import com.github.panpf.sketch.decode.internal.ImageFormat
import com.github.panpf.sketch.decode.internal.appliedExifOrientation
import com.github.panpf.sketch.decode.internal.appliedResize
import com.github.panpf.sketch.decode.internal.calculateSampleSize
import com.github.panpf.sketch.decode.internal.calculateSampleSizeForRegion
import com.github.panpf.sketch.decode.internal.calculateSampledBitmapSize
import com.github.panpf.sketch.decode.internal.calculateSampledBitmapSizeForRegion
import com.github.panpf.sketch.decode.internal.computeSizeMultiplier
import com.github.panpf.sketch.decode.internal.createInSampledTransformed
import com.github.panpf.sketch.decode.internal.createSubsamplingTransformed
import com.github.panpf.sketch.decode.internal.decodeBitmap
import com.github.panpf.sketch.decode.internal.decodeRegionBitmap
import com.github.panpf.sketch.decode.internal.getExifOrientationTransformed
import com.github.panpf.sketch.decode.internal.isAnimatedHeif
import com.github.panpf.sketch.decode.internal.isAnimatedWebP
import com.github.panpf.sketch.decode.internal.isGif
import com.github.panpf.sketch.decode.internal.isHeif
import com.github.panpf.sketch.decode.internal.isInBitmapError
import com.github.panpf.sketch.decode.internal.isSrcRectError
import com.github.panpf.sketch.decode.internal.isSupportInBitmap
import com.github.panpf.sketch.decode.internal.isSupportInBitmapForRegion
import com.github.panpf.sketch.decode.internal.isWebP
import com.github.panpf.sketch.decode.internal.limitedSampleSizeByMaxBitmapSize
import com.github.panpf.sketch.decode.internal.limitedSampleSizeByMaxBitmapSizeForRegion
import com.github.panpf.sketch.decode.internal.maxBitmapSize
import com.github.panpf.sketch.decode.internal.readImageInfoWithBitmapFactory
import com.github.panpf.sketch.decode.internal.readImageInfoWithBitmapFactoryOrNull
import com.github.panpf.sketch.decode.internal.readImageInfoWithBitmapFactoryOrThrow
import com.github.panpf.sketch.decode.internal.realDecode
import com.github.panpf.sketch.decode.internal.sizeString
import com.github.panpf.sketch.decode.internal.supportBitmapRegionDecoder
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.fetch.newResourceUri
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.resize.Precision.EXACTLY
import com.github.panpf.sketch.resize.Precision.LESS_PIXELS
import com.github.panpf.sketch.resize.Precision.SAME_ASPECT_RATIO
import com.github.panpf.sketch.resize.Resize
import com.github.panpf.sketch.sketch
import com.github.panpf.sketch.test.R
import com.github.panpf.sketch.test.utils.ExifOrientationTestFileHelper
import com.github.panpf.sketch.test.utils.TestAssets
import com.github.panpf.sketch.test.utils.corners
import com.github.panpf.sketch.test.utils.getTestContext
import com.github.panpf.sketch.test.utils.getTestContextAndNewSketch
import com.github.panpf.sketch.test.utils.size
import com.github.panpf.sketch.util.Bytes
import com.github.panpf.sketch.util.Size
import com.github.panpf.tools4j.test.ktx.assertThrow
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DecodeUtilsTest {

    @Test
    fun testCalculateSampledBitmapSize() {
        Assert.assertEquals(
            Size(503, 101),
            calculateSampledBitmapSize(
                imageSize = Size(1005, 201),
                sampleSize = 2
            )
        )
        Assert.assertEquals(
            Size(503, 101),
            calculateSampledBitmapSize(
                imageSize = Size(1005, 201),
                sampleSize = 2,
                mimeType = "image/jpeg"
            )
        )
        Assert.assertEquals(
            Size(502, 100),
            calculateSampledBitmapSize(
                imageSize = Size(1005, 201),
                sampleSize = 2,
                mimeType = "image/png"
            )
        )
        Assert.assertEquals(
            Size(503, 101),
            calculateSampledBitmapSize(
                imageSize = Size(1005, 201),
                sampleSize = 2,
                mimeType = "image/bmp"
            )
        )
        Assert.assertEquals(
            Size(503, 101),
            calculateSampledBitmapSize(
                imageSize = Size(1005, 201),
                sampleSize = 2,
                mimeType = "image/gif"
            )
        )
        Assert.assertEquals(
            Size(503, 101),
            calculateSampledBitmapSize(
                imageSize = Size(1005, 201),
                sampleSize = 2,
                mimeType = "image/webp"
            )
        )
        Assert.assertEquals(
            Size(503, 101),
            calculateSampledBitmapSize(
                imageSize = Size(1005, 201),
                sampleSize = 2,
                mimeType = "image/heic"
            )
        )
        Assert.assertEquals(
            Size(503, 101),
            calculateSampledBitmapSize(
                imageSize = Size(1005, 201),
                sampleSize = 2,
                mimeType = "image/heif"
            )
        )
    }

    @Test
    fun testCalculateSampledBitmapSizeForRegion() {
        Assert.assertEquals(
            if (VERSION.SDK_INT >= VERSION_CODES.N) Size(503, 101) else Size(502, 100),
            calculateSampledBitmapSizeForRegion(
                regionSize = Size(1005, 201),
                sampleSize = 2,
                mimeType = "image/jpeg",
                imageSize = Size(1005, 201)
            )
        )
        Assert.assertEquals(
            Size(502, 100),
            calculateSampledBitmapSizeForRegion(
                regionSize = Size(1005, 201),
                sampleSize = 2,
                mimeType = "image/png",
                imageSize = Size(1005, 201)
            )
        )
        Assert.assertEquals(
            Size(288, 100),
            calculateSampledBitmapSizeForRegion(
                regionSize = Size(577, 201),
                sampleSize = 2,
                mimeType = "image/jpeg",
                imageSize = Size(1005, 201)
            )
        )
        Assert.assertEquals(
            Size(502, 55),
            calculateSampledBitmapSizeForRegion(
                regionSize = Size(1005, 111),
                sampleSize = 2,
                mimeType = "image/jpeg",
                imageSize = Size(1005, 201)
            )
        )
        Assert.assertEquals(
            Size(288, 55),
            calculateSampledBitmapSizeForRegion(
                regionSize = Size(577, 111),
                sampleSize = 2,
                mimeType = "image/jpeg",
                imageSize = Size(1005, 201)
            )
        )
        Assert.assertEquals(
            Size(288, 55),
            calculateSampledBitmapSizeForRegion(
                regionSize = Size(577, 111),
                sampleSize = 2,
                mimeType = "image/jpeg",
            )
        )
    }

    @Test
    fun testCalculateSampleSize() {
        Assert.assertEquals(
            1,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(1006, 202),
            )
        )
        Assert.assertEquals(
            1,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(1005, 201),
            )
        )
        Assert.assertEquals(
            2,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(1004, 200),
            )
        )
        Assert.assertEquals(
            2,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(503, 101),
            )
        )
        Assert.assertEquals(
            4,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(502, 100),
            )
        )
        Assert.assertEquals(
            4,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(252, 51),
            )
        )
        Assert.assertEquals(
            8,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(251, 50),
            )
        )

        Assert.assertEquals(
            4,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(502, 100),
                mimeType = "image/jpeg"
            )
        )
        Assert.assertEquals(
            2,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(502, 100),
                mimeType = "image/png"
            )
        )
        Assert.assertEquals(
            4,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(502, 100),
                mimeType = "image/bmp"
            )
        )
        Assert.assertEquals(
            4,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(502, 100),
                mimeType = "image/webp"
            )
        )
        Assert.assertEquals(
            4,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(502, 100),
                mimeType = "image/gif"
            )
        )
        Assert.assertEquals(
            4,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(502, 100),
                mimeType = "image/heic"
            )
        )
        Assert.assertEquals(
            4,
            calculateSampleSize(
                imageSize = Size(1005, 201),
                targetSize = Size(502, 100),
                mimeType = "image/heif"
            )
        )
    }

    @Test
    fun testCalculateSampleSizeForRegion() {
        Assert.assertEquals(
            1,
            calculateSampleSizeForRegion(
                regionSize = Size(1005, 201),
                targetSize = Size(1006, 202),
            )
        )
        Assert.assertEquals(
            1,
            calculateSampleSizeForRegion(
                regionSize = Size(1005, 201),
                targetSize = Size(1005, 201),
            )
        )
        Assert.assertEquals(
            2,
            calculateSampleSizeForRegion(
                regionSize = Size(1005, 201),
                targetSize = Size(1004, 200),
                imageSize = Size(2005, 301),
            )
        )
        Assert.assertEquals(
            2,
            calculateSampleSizeForRegion(
                regionSize = Size(1005, 201),
                targetSize = Size(502, 100),
                imageSize = Size(2005, 301),
            )
        )
        Assert.assertEquals(
            4,
            calculateSampleSizeForRegion(
                regionSize = Size(1005, 201),
                targetSize = Size(501, 99),
                imageSize = Size(2005, 301),
            )
        )
        Assert.assertEquals(
            4,
            calculateSampleSizeForRegion(
                regionSize = Size(1005, 201),
                targetSize = Size(251, 50),
                imageSize = Size(2005, 301),
            )
        )
        Assert.assertEquals(
            8,
            calculateSampleSizeForRegion(
                regionSize = Size(1005, 201),
                targetSize = Size(250, 49),
                imageSize = Size(2005, 301),
            )
        )

        Assert.assertEquals(
            if (VERSION.SDK_INT >= VERSION_CODES.N) 4 else 2,
            calculateSampleSizeForRegion(
                regionSize = Size(1005, 201),
                targetSize = Size(502, 100),
                imageSize = Size(1005, 201),
            )
        )

        Assert.assertEquals(
            2,
            calculateSampleSizeForRegion(
                regionSize = Size(1005, 201),
                targetSize = Size(502, 100),
                mimeType = "image/png",
                imageSize = Size(1005, 201),
            )
        )
    }

    @Test
    fun testLimitedSampleSizeByMaxBitmapSize() {
        val maxSize = maxBitmapSize.width
        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSize(1, Size(maxSize - 1, maxSize))
        )
        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSize(1, Size(maxSize, maxSize - 1))
        )
        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSize(1, Size(maxSize - 1, maxSize - 1))
        )
        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSize(1, Size(maxSize, maxSize))
        )
        Assert.assertEquals(
            2,
            limitedSampleSizeByMaxBitmapSize(1, Size(maxSize + 1, maxSize))
        )
        Assert.assertEquals(
            2,
            limitedSampleSizeByMaxBitmapSize(1, Size(maxSize, maxSize + 1))
        )
        Assert.assertEquals(
            2,
            limitedSampleSizeByMaxBitmapSize(1, Size(maxSize + 1, maxSize + 1))
        )

        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSize(0, Size(maxSize, maxSize))
        )
        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSize(-1, Size(maxSize, maxSize))
        )
        Assert.assertEquals(
            2,
            limitedSampleSizeByMaxBitmapSize(-1, Size(maxSize + 1, maxSize + 1))
        )
        Assert.assertEquals(
            2,
            limitedSampleSizeByMaxBitmapSize(0, Size(maxSize + 1, maxSize + 1))
        )
    }

    @Test
    fun testLimitedSampleSizeByMaxBitmapSizeForRegion() {
        val maxSize = maxBitmapSize.width
        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSizeForRegion(Size(maxSize - 1, maxSize), 1)
        )
        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSizeForRegion(Size(maxSize, maxSize - 1), 1)
        )
        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSizeForRegion(
                Size(maxSize - 1, maxSize - 1),
                1
            )
        )
        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSizeForRegion(Size(maxSize, maxSize), 1)
        )
        Assert.assertEquals(
            2,
            limitedSampleSizeByMaxBitmapSizeForRegion(Size(maxSize + 1, maxSize), 1)
        )
        Assert.assertEquals(
            2,
            limitedSampleSizeByMaxBitmapSizeForRegion(Size(maxSize, maxSize + 1), 1)
        )
        Assert.assertEquals(
            2,
            limitedSampleSizeByMaxBitmapSizeForRegion(
                Size(maxSize + 1, maxSize + 1),
                1
            )
        )

        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSizeForRegion(Size(maxSize, maxSize), 0)
        )
        Assert.assertEquals(
            1,
            limitedSampleSizeByMaxBitmapSizeForRegion(Size(maxSize, maxSize), -1)
        )
        Assert.assertEquals(
            2,
            limitedSampleSizeByMaxBitmapSizeForRegion(
                Size(maxSize + 1, maxSize + 1),
                -1
            )
        )
        Assert.assertEquals(
            2,
            limitedSampleSizeByMaxBitmapSizeForRegion(
                Size(maxSize + 1, maxSize + 1),
                0
            )
        )
    }

    @Test
    fun testRealDecode() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val sketch = context.sketch

        val hasExifFile = ExifOrientationTestFileHelper(context, "sample.jpeg")
            .files().find { it.exifOrientation == ExifInterface.ORIENTATION_ROTATE_90 }!!

        @Suppress("ComplexRedundantLet")
        val result1 = LoadRequest(context, hasExifFile.file.path).let {
            realDecode(
                it,
                LOCAL,
                ImageInfo(1936, 1291, "image/jpeg", hasExifFile.exifOrientation),
                { config ->
                    runBlocking {
                        sketch.components.newFetcher(it).fetch()
                    }.dataSource.decodeBitmap(config.toBitmapOptions())!!
                }
            ) { rect, config ->
                runBlocking {
                    sketch.components.newFetcher(it).fetch()
                }.dataSource.decodeRegionBitmap(rect, config.toBitmapOptions())!!
            }
        }.apply {
            Assert.assertEquals(imageInfo.size, bitmap.size)
            Assert.assertEquals(
                ImageInfo(
                    1936,
                    1291,
                    "image/jpeg",
                    ExifInterface.ORIENTATION_ROTATE_90
                ), imageInfo
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNull(transformedList)
        }

        LoadRequest(context, hasExifFile.file.path).newLoadRequest {
            ignoreExifOrientation(true)
        }.let {
            realDecode(
                it,
                LOCAL,
                ImageInfo(1936, 1291, "image/jpeg", hasExifFile.exifOrientation),
                { config ->
                    runBlocking {
                        sketch.components.newFetcher(it).fetch()
                    }.dataSource.decodeBitmap(config.toBitmapOptions())!!
                }
            ) { rect, config ->
                runBlocking {
                    sketch.components.newFetcher(it).fetch()
                }.dataSource.decodeRegionBitmap(rect, config.toBitmapOptions())!!
            }
        }.apply {
            Assert.assertEquals(imageInfo.size, bitmap.size)
            Assert.assertEquals(
                ImageInfo(
                    1936,
                    1291,
                    "image/jpeg",
                    ExifInterface.ORIENTATION_ROTATE_90
                ), imageInfo
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNull(transformedList)
            Assert.assertEquals(result1.bitmap.corners(), bitmap.corners())
        }

        val result3 = LoadRequest(context, hasExifFile.file.path).newLoadRequest {
            resize(100, 200)
        }.let {
            realDecode(
                it,
                LOCAL,
                ImageInfo(1936, 1291, "image/jpeg", hasExifFile.exifOrientation),
                { config ->
                    runBlocking {
                        sketch.components.newFetcher(it).fetch()
                    }.dataSource.decodeBitmap(config.toBitmapOptions())!!
                }
            ) { rect, config ->
                runBlocking {
                    sketch.components.newFetcher(it).fetch()
                }.dataSource.decodeRegionBitmap(rect, config.toBitmapOptions())!!
            }
        }.apply {
            Assert.assertEquals(Size(121, 60), bitmap.size)
            Assert.assertEquals(
                ImageInfo(
                    1936,
                    1291,
                    "image/jpeg",
                    ExifInterface.ORIENTATION_ROTATE_90
                ), imageInfo
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertEquals(
                listOf(createInSampledTransformed(16), createSubsamplingTransformed(Rect(0,161,1936,1129))),
                transformedList
            )
        }

        LoadRequest(context, hasExifFile.file.path).newLoadRequest {
            resize(100, 200)
        }.let {
            realDecode(
                request = it,
                dataFrom = LOCAL,
                imageInfo = ImageInfo(1936, 1291, "image/jpeg", 0),
                decodeFull = { config ->
                    runBlocking {
                        sketch.components.newFetcher(it).fetch()
                    }.dataSource.decodeBitmap(config.toBitmapOptions())!!
                }
            ) { rect, config ->
                runBlocking {
                    sketch.components.newFetcher(it).fetch()
                }.dataSource.decodeRegionBitmap(rect, config.toBitmapOptions())!!
            }
        }.apply {
            Assert.assertEquals(Size(80, 161), bitmap.size)
            Assert.assertEquals(ImageInfo(1936, 1291, "image/jpeg", 0), imageInfo)
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertEquals(
                listOf(
                    createInSampledTransformed(8),
                    createSubsamplingTransformed(Rect(645,0,1290,1291))
                ),
                transformedList
            )
            Assert.assertNotEquals(result3.bitmap.corners(), bitmap.corners())
        }

        val result5 = LoadRequest(context, hasExifFile.file.path).newLoadRequest {
            resize(100, 200, SAME_ASPECT_RATIO)
        }.let {
            realDecode(
                it,
                LOCAL,
                ImageInfo(1936, 1291, "image/jpeg", hasExifFile.exifOrientation),
                { config ->
                    runBlocking {
                        sketch.components.newFetcher(it).fetch()
                    }.dataSource.decodeBitmap(config.toBitmapOptions())!!
                }
            ) { rect, config ->
                runBlocking {
                    sketch.components.newFetcher(it).fetch()
                }.dataSource.decodeRegionBitmap(rect, config.toBitmapOptions())!!
            }
        }.apply {
            Assert.assertEquals(Size(121, 60), bitmap.size)
            Assert.assertEquals(
                ImageInfo(
                    1936,
                    1291,
                    "image/jpeg",
                    ExifInterface.ORIENTATION_ROTATE_90
                ), imageInfo
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertEquals(
                listOf(
                    createInSampledTransformed(16),
                    createSubsamplingTransformed(Rect(0,161,1936,1129))
                ),
                transformedList
            )
        }

        LoadRequest(context, hasExifFile.file.path).newLoadRequest {
            resize(100, 200, SAME_ASPECT_RATIO)
        }.let {
            realDecode(
                request = it,
                dataFrom = LOCAL,
                imageInfo = ImageInfo(1936, 1291, "image/jpeg", 0),
                decodeFull = { config ->
                    runBlocking {
                        sketch.components.newFetcher(it).fetch()
                    }.dataSource.decodeBitmap(config.toBitmapOptions())!!
                }
            ) { rect, config ->
                runBlocking {
                    sketch.components.newFetcher(it).fetch()
                }.dataSource.decodeRegionBitmap(rect, config.toBitmapOptions())!!
            }
        }.apply {
            Assert.assertEquals(Size(80, 161), bitmap.size)
            Assert.assertEquals(ImageInfo(1936, 1291, "image/jpeg", 0), imageInfo)
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertEquals(
                listOf(
                    createInSampledTransformed(8),
                    createSubsamplingTransformed(Rect(645,0,1290,1291))
                ),
                transformedList
            )
            Assert.assertNotEquals(result5.bitmap.corners(), bitmap.corners())
        }

        val result7 = LoadRequest(context, hasExifFile.file.path).newLoadRequest {
            resize(100, 200, LESS_PIXELS)
        }.let {
            realDecode(
                it,
                LOCAL,
                ImageInfo(1936, 1291, "image/jpeg", hasExifFile.exifOrientation),
                { config ->
                    runBlocking {
                        sketch.components.newFetcher(it).fetch()
                    }.dataSource.decodeBitmap(config.toBitmapOptions())!!
                }
            ) { rect, config ->
                runBlocking {
                    sketch.components.newFetcher(it).fetch()
                }.dataSource.decodeRegionBitmap(rect, config.toBitmapOptions())!!
            }
        }.apply {
            Assert.assertEquals(Size(121, 81), bitmap.size)
            Assert.assertEquals(
                ImageInfo(
                    1936,
                    1291,
                    "image/jpeg",
                    ExifInterface.ORIENTATION_ROTATE_90
                ), imageInfo
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertEquals(listOf(createInSampledTransformed(16)), transformedList)
        }

        LoadRequest(context, hasExifFile.file.path).newLoadRequest {
            resize(100, 200, LESS_PIXELS)
        }.let {
            realDecode(
                request = it,
                dataFrom = LOCAL,
                imageInfo = ImageInfo(1936, 1291, "image/jpeg", 0),
                decodeFull = { config ->
                    runBlocking {
                        sketch.components.newFetcher(it).fetch()
                    }.dataSource.decodeBitmap(config.toBitmapOptions())!!
                }
            ) { rect, config ->
                runBlocking {
                    sketch.components.newFetcher(it).fetch()
                }.dataSource.decodeRegionBitmap(rect, config.toBitmapOptions())!!
            }
        }.apply {
            Assert.assertEquals(Size(121, 81), bitmap.size)
            Assert.assertEquals(ImageInfo(1936, 1291, "image/jpeg", 0), imageInfo)
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertEquals(listOf(createInSampledTransformed(16)), transformedList)
            Assert.assertEquals(result7.bitmap.corners(), bitmap.corners())
        }

        val result9 = LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(100, 200)
        }.let {
            realDecode(
                request = it,
                dataFrom = LOCAL,
                imageInfo = ImageInfo(700, 1012, "image/bmp", 0),
                decodeFull = { config ->
                    runBlocking {
                        sketch.components.newFetcher(it).fetch()
                    }.dataSource.decodeBitmap(config.toBitmapOptions())!!
                },
                decodeRegion = null
            )
        }.apply {
            Assert.assertEquals(Size(87, 126), bitmap.size)
            Assert.assertEquals(ImageInfo(700, 1012, "image/bmp", 0), imageInfo)
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertEquals(listOf(createInSampledTransformed(8)), transformedList)
        }

        LoadRequest(context, newAssetUri("sample.bmp")).newLoadRequest {
            resize(100, 200)
            ignoreExifOrientation(true)
        }.let {
            realDecode(
                request = it,
                dataFrom = LOCAL,
                imageInfo = ImageInfo(700, 1012, "image/jpeg", 0),
                decodeFull = { config ->
                    runBlocking {
                        sketch.components.newFetcher(it).fetch()
                    }.dataSource.decodeBitmap(config.toBitmapOptions())!!
                },
                decodeRegion = null
            )
        }.apply {
            Assert.assertEquals(Size(87, 126), bitmap.size)
            Assert.assertEquals(ImageInfo(700, 1012, "image/jpeg", 0), imageInfo)
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertEquals(listOf(createInSampledTransformed(8)), transformedList)
            Assert.assertEquals(result9.bitmap.corners(), bitmap.corners())
        }
    }

    @Test
    fun testAppliedExifOrientation() {
        val (context, sketch) = getTestContextAndNewSketch()
        val request = LoadRequest(context, TestAssets.SAMPLE_JPEG_URI)

        val hasExifFile = ExifOrientationTestFileHelper(context, "sample.jpeg")
            .files().find { it.exifOrientation == ExifInterface.ORIENTATION_ROTATE_90 }!!
        val bitmap = BitmapFactory.decodeFile(hasExifFile.file.path)

        val result = BitmapDecodeResult(
            bitmap = bitmap,
            imageInfo = ImageInfo(
                width = bitmap.width,
                height = bitmap.height,
                mimeType = "image/jpeg",
                exifOrientation = hasExifFile.exifOrientation
            ),
            dataFrom = LOCAL,
            transformedList = null,
            extras = null,
        )
        val resultCorners = result.bitmap.corners()
        Assert.assertNull(result.transformedList?.getExifOrientationTransformed())

        result.appliedExifOrientation(sketch, request).apply {
            Assert.assertNotSame(result, this)
            Assert.assertNotSame(result.bitmap, this.bitmap)
            Assert.assertEquals(Size(result.bitmap.height, result.bitmap.width), this.bitmap.size)
            Assert.assertEquals(
                Size(result.imageInfo.height, result.imageInfo.width),
                this.imageInfo.size
            )
            Assert.assertNotEquals(resultCorners, this.bitmap.corners())
            Assert.assertNotNull(this.transformedList?.getExifOrientationTransformed())
        }

        val noExifOrientationResult = result.newResult(
            imageInfo = result.imageInfo.newImageInfo(exifOrientation = 0)
        )
        noExifOrientationResult.appliedExifOrientation(sketch, request).apply {
            Assert.assertSame(noExifOrientationResult, this)
        }
    }

    @Test
    fun testAppliedResize() {
        val (context, sketch) = getTestContextAndNewSketch()
        val request = LoadRequest(context, TestAssets.SAMPLE_JPEG_URI)
        val newResult: () -> BitmapDecodeResult = {
            BitmapDecodeResult(
                bitmap = Bitmap.createBitmap(80, 50, ARGB_8888),
                imageInfo = ImageInfo(80, 50, "image/png", 0),
                dataFrom = MEMORY,
                transformedList = null,
                extras = null,
            )
        }

        /*
         * null
         */
        var resize: Resize? = null
        var result: BitmapDecodeResult = newResult()
        result.appliedResize(sketch, request, resize).apply {
            Assert.assertTrue(this === result)
        }

        /*
         * LESS_PIXELS
         */
        // small
        resize = Resize(40, 20, LESS_PIXELS)
        result = newResult()
        result.appliedResize(sketch, request, resize).apply {
            Assert.assertTrue(this !== result)
            Assert.assertEquals("20x13", this.bitmap.sizeString)
        }
        // big
        resize = Resize(50, 150, LESS_PIXELS)
        result = newResult()
        result.appliedResize(sketch, request, resize).apply {
            Assert.assertTrue(this === result)
        }

        /*
         * SAME_ASPECT_RATIO
         */
        // small
        resize = Resize(40, 20, SAME_ASPECT_RATIO)
        result = newResult()
        result.appliedResize(sketch, request, resize).apply {
            Assert.assertTrue(this !== result)
            Assert.assertEquals("40x20", this.bitmap.sizeString)
        }
        // big
        resize = Resize(50, 150, SAME_ASPECT_RATIO)
        result = newResult()
        result.appliedResize(sketch, request, resize).apply {
            Assert.assertTrue(this !== result)
            Assert.assertEquals("17x50", this.bitmap.sizeString)
        }

        /*
         * EXACTLY
         */
        // small
        resize = Resize(40, 20, EXACTLY)
        result = newResult()
        result.appliedResize(sketch, request, resize).apply {
            Assert.assertTrue(this !== result)
            Assert.assertEquals("40x20", this.bitmap.sizeString)
        }
        // big
        resize = Resize(50, 150, EXACTLY)
        result = newResult()
        result.appliedResize(sketch, request, resize).apply {
            Assert.assertTrue(this !== result)
            Assert.assertEquals("50x150", this.bitmap.sizeString)
        }
    }

    @Test
    fun testComputeSizeMultiplier() {
        Assert.assertEquals(0.2, computeSizeMultiplier(1000, 600, 200, 400, true), 0.1)
        Assert.assertEquals(0.6, computeSizeMultiplier(1000, 600, 200, 400, false), 0.1)
        Assert.assertEquals(0.3, computeSizeMultiplier(1000, 600, 400, 200, true), 0.1)
        Assert.assertEquals(0.4, computeSizeMultiplier(1000, 600, 400, 200, false), 0.1)

        Assert.assertEquals(0.6, computeSizeMultiplier(1000, 600, 2000, 400, true), 0.1)
        Assert.assertEquals(2.0, computeSizeMultiplier(1000, 600, 2000, 400, false), 0.1)
        Assert.assertEquals(0.4, computeSizeMultiplier(1000, 600, 400, 2000, true), 0.1)
        Assert.assertEquals(3.3, computeSizeMultiplier(1000, 600, 400, 2000, false), 0.1)

        Assert.assertEquals(2.0, computeSizeMultiplier(1000, 600, 2000, 4000, true), 0.1)
        Assert.assertEquals(6.6, computeSizeMultiplier(1000, 600, 2000, 4000, false), 0.1)
        Assert.assertEquals(3.3, computeSizeMultiplier(1000, 600, 4000, 2000, true), 0.1)
        Assert.assertEquals(4.0, computeSizeMultiplier(1000, 600, 4000, 2000, false), 0.1)
    }

    @Test
    fun testReadImageInfoWithBitmapFactory() {
        val (context, sketch) = getTestContextAndNewSketch()

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.jpeg")), "sample.jpeg")
            .readImageInfoWithBitmapFactory().apply {
                Assert.assertEquals(1291, width)
                Assert.assertEquals(1936, height)
                Assert.assertEquals("image/jpeg", mimeType)
                Assert.assertEquals(ExifInterface.ORIENTATION_NORMAL, exifOrientation)
            }

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.webp")), "sample.webp")
            .readImageInfoWithBitmapFactory().apply {
                Assert.assertEquals(1080, width)
                Assert.assertEquals(1344, height)
                if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                    Assert.assertEquals("image/webp", mimeType)
                } else {
                    Assert.assertEquals("", mimeType)
                }
                Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, exifOrientation)
            }

        ResourceDataSource(
            sketch,
            LoadRequest(context, newResourceUri(R.xml.network_security_config)),
            packageName = context.packageName,
            context.resources,
            R.xml.network_security_config
        ).readImageInfoWithBitmapFactory().apply {
            Assert.assertEquals(-1, width)
            Assert.assertEquals(-1, height)
            Assert.assertEquals("", mimeType)
            Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, exifOrientation)
        }

        ExifOrientationTestFileHelper(context, "exif_origin_clock_hor.jpeg").files().forEach {
            FileDataSource(sketch, LoadRequest(context, it.file.path), it.file)
                .readImageInfoWithBitmapFactory().apply {
                    Assert.assertEquals(it.exifOrientation, exifOrientation)
                }
            FileDataSource(sketch, LoadRequest(context, it.file.path), it.file)
                .readImageInfoWithBitmapFactory(true).apply {
                    Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, exifOrientation)
                }
        }
    }

    @Test
    fun testReadImageInfoWithBitmapFactoryOrThrow() {
        val (context, sketch) = getTestContextAndNewSketch()

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.jpeg")), "sample.jpeg")
            .readImageInfoWithBitmapFactoryOrThrow().apply {
                Assert.assertEquals(1291, width)
                Assert.assertEquals(1936, height)
                Assert.assertEquals("image/jpeg", mimeType)
                Assert.assertEquals(ExifInterface.ORIENTATION_NORMAL, exifOrientation)
            }
        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.webp")), "sample.webp")
            .readImageInfoWithBitmapFactoryOrThrow().apply {
                Assert.assertEquals(1080, width)
                Assert.assertEquals(1344, height)
                if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                    Assert.assertEquals("image/webp", mimeType)
                } else {
                    Assert.assertEquals("", mimeType)
                }
                Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, exifOrientation)
            }

        assertThrow(ImageInvalidException::class) {
            ResourceDataSource(
                sketch,
                LoadRequest(context, newResourceUri(R.xml.network_security_config)),
                packageName = context.packageName,
                context.resources,
                R.xml.network_security_config
            ).readImageInfoWithBitmapFactoryOrThrow()
        }

        ExifOrientationTestFileHelper(context, "exif_origin_clock_hor.jpeg").files().forEach {
            FileDataSource(sketch, LoadRequest(context, it.file.path), it.file)
                .readImageInfoWithBitmapFactoryOrThrow().apply {
                    Assert.assertEquals(it.exifOrientation, exifOrientation)
                }
            FileDataSource(sketch, LoadRequest(context, it.file.path), it.file)
                .readImageInfoWithBitmapFactoryOrThrow(true).apply {
                    Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, exifOrientation)
                }
        }
    }

    @Test
    fun testReadImageInfoWithBitmapFactoryOrNull() {
        val (context, sketch) = getTestContextAndNewSketch()

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.jpeg")), "sample.jpeg")
            .readImageInfoWithBitmapFactoryOrNull()!!.apply {
                Assert.assertEquals(1291, width)
                Assert.assertEquals(1936, height)
                Assert.assertEquals("image/jpeg", mimeType)
                Assert.assertEquals(ExifInterface.ORIENTATION_NORMAL, exifOrientation)
            }

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.webp")), "sample.webp")
            .readImageInfoWithBitmapFactoryOrNull()!!.apply {
                Assert.assertEquals(1080, width)
                Assert.assertEquals(1344, height)
                if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                    Assert.assertEquals("image/webp", mimeType)
                } else {
                    Assert.assertEquals("", mimeType)
                }
                Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, exifOrientation)
            }

        Assert.assertNull(
            ResourceDataSource(
                sketch,
                LoadRequest(context, newResourceUri(R.xml.network_security_config)),
                packageName = context.packageName,
                context.resources,
                R.xml.network_security_config
            ).readImageInfoWithBitmapFactoryOrNull()
        )

        ExifOrientationTestFileHelper(context, "exif_origin_clock_hor.jpeg").files().forEach {
            FileDataSource(sketch, LoadRequest(context, it.file.path), it.file)
                .readImageInfoWithBitmapFactoryOrNull()!!.apply {
                    Assert.assertEquals(it.exifOrientation, exifOrientation)
                }
            FileDataSource(sketch, LoadRequest(context, it.file.path), it.file)
                .readImageInfoWithBitmapFactoryOrNull(true)!!.apply {
                    Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, exifOrientation)
                }
        }
    }

    @Test
    fun testDecodeBitmap() {
        val (context, sketch) = getTestContextAndNewSketch()

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.jpeg")), "sample.jpeg")
            .decodeBitmap()!!.apply {
                Assert.assertEquals(1291, width)
                Assert.assertEquals(1936, height)
            }

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.jpeg")), "sample.jpeg")
            .decodeBitmap(BitmapFactory.Options().apply { inSampleSize = 2 })!!
            .apply {
                Assert.assertEquals(646, width)
                Assert.assertEquals(968, height)
            }

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.webp")), "sample.webp")
            .decodeBitmap()!!.apply {
                Assert.assertEquals(1080, width)
                Assert.assertEquals(1344, height)
            }

        Assert.assertNull(
            ResourceDataSource(
                sketch,
                LoadRequest(context, newResourceUri(R.xml.network_security_config)),
                packageName = context.packageName,
                context.resources,
                R.xml.network_security_config
            ).decodeBitmap()
        )
    }

    @Test
    fun testDecodeRegionBitmap() {
        val (context, sketch) = getTestContextAndNewSketch()

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.jpeg")), "sample.jpeg")
            .decodeRegionBitmap(Rect(500, 500, 600, 600))!!.apply {
                Assert.assertEquals(100, width)
                Assert.assertEquals(100, height)
            }

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.jpeg")), "sample.jpeg")
            .decodeRegionBitmap(
                Rect(500, 500, 600, 600),
                BitmapFactory.Options().apply { inSampleSize = 2 })!!
            .apply {
                Assert.assertEquals(50, width)
                Assert.assertEquals(50, height)
            }

        AssetDataSource(sketch, LoadRequest(context, newAssetUri("sample.webp")), "sample.webp")
            .decodeRegionBitmap(Rect(500, 500, 700, 700))!!.apply {
                Assert.assertEquals(200, width)
                Assert.assertEquals(200, height)
            }

        assertThrow(IOException::class) {
            ResourceDataSource(
                sketch,
                LoadRequest(context, newResourceUri(R.xml.network_security_config)),
                packageName = context.packageName,
                context.resources,
                R.xml.network_security_config
            ).decodeRegionBitmap(Rect(500, 500, 600, 600))
        }
    }

    @Test
    fun testSupportBitmapRegionDecoder() {
        if (VERSION.SDK_INT >= VERSION_CODES.P) {
            Assert.assertTrue(ImageFormat.HEIC.supportBitmapRegionDecoder())
        } else {
            Assert.assertFalse(ImageFormat.HEIC.supportBitmapRegionDecoder())
        }
        if (VERSION.SDK_INT >= VERSION_CODES.P) {
            Assert.assertTrue(ImageFormat.HEIF.supportBitmapRegionDecoder())
        } else {
            Assert.assertFalse(ImageFormat.HEIF.supportBitmapRegionDecoder())
        }
        Assert.assertFalse(ImageFormat.BMP.supportBitmapRegionDecoder())
        Assert.assertFalse(ImageFormat.GIF.supportBitmapRegionDecoder())
        Assert.assertTrue(ImageFormat.JPEG.supportBitmapRegionDecoder())
        Assert.assertTrue(ImageFormat.PNG.supportBitmapRegionDecoder())
        Assert.assertTrue(ImageFormat.WEBP.supportBitmapRegionDecoder())
    }

    @Test
    fun testIsInBitmapError() {
        Assert.assertTrue(
            isInBitmapError(IllegalArgumentException("Problem decoding into existing bitmap"))
        )
        Assert.assertTrue(
            isInBitmapError(IllegalArgumentException("bitmap"))
        )

        Assert.assertFalse(
            isInBitmapError(IllegalArgumentException("Problem decoding"))
        )
        Assert.assertFalse(
            isInBitmapError(IllegalStateException("Problem decoding into existing bitmap"))
        )
    }

    @Test
    fun testIsSrcRectError() {
        Assert.assertTrue(
            isSrcRectError(IllegalArgumentException("rectangle is outside the image srcRect"))
        )
        Assert.assertTrue(
            isSrcRectError(IllegalArgumentException("srcRect"))
        )

        Assert.assertFalse(
            isSrcRectError(IllegalStateException("rectangle is outside the image srcRect"))
        )
        Assert.assertFalse(
            isSrcRectError(IllegalArgumentException(""))
        )
    }

    @Test
    fun testIsSupportInBitmap() {
        Assert.assertEquals(VERSION.SDK_INT >= 16, isSupportInBitmap("image/jpeg", 1))
        Assert.assertEquals(VERSION.SDK_INT >= 19, isSupportInBitmap("image/jpeg", 2))

        Assert.assertEquals(VERSION.SDK_INT >= 16, isSupportInBitmap("image/png", 1))
        Assert.assertEquals(VERSION.SDK_INT >= 19, isSupportInBitmap("image/png", 2))

        Assert.assertEquals(VERSION.SDK_INT >= 19, isSupportInBitmap("image/gif", 1))
        Assert.assertEquals(VERSION.SDK_INT >= 21, isSupportInBitmap("image/gif", 2))

        Assert.assertEquals(VERSION.SDK_INT >= 19, isSupportInBitmap("image/webp", 1))
        Assert.assertEquals(VERSION.SDK_INT >= 19, isSupportInBitmap("image/webp", 2))

        Assert.assertEquals(VERSION.SDK_INT >= 19, isSupportInBitmap("image/bmp", 1))
        Assert.assertEquals(VERSION.SDK_INT >= 19, isSupportInBitmap("image/bmp", 2))

        Assert.assertEquals(false, isSupportInBitmap("image/heic", 1))
        Assert.assertEquals(false, isSupportInBitmap("image/heic", 2))

        Assert.assertEquals(VERSION.SDK_INT >= 28, isSupportInBitmap("image/heif", 1))
        Assert.assertEquals(VERSION.SDK_INT >= 28, isSupportInBitmap("image/heif", 2))

        Assert.assertEquals(VERSION.SDK_INT >= 32, isSupportInBitmap("image/svg", 1))
        Assert.assertEquals(VERSION.SDK_INT >= 32, isSupportInBitmap("image/svg", 2))
    }

    @Test
    fun testIsSupportInBitmapForRegion() {
        Assert.assertEquals(VERSION.SDK_INT >= 16, isSupportInBitmapForRegion("image/jpeg"))
        Assert.assertEquals(VERSION.SDK_INT >= 16, isSupportInBitmapForRegion("image/png"))
        Assert.assertEquals(false, isSupportInBitmapForRegion("image/gif"))
        Assert.assertEquals(VERSION.SDK_INT >= 16, isSupportInBitmapForRegion("image/webp"))
        Assert.assertEquals(false, isSupportInBitmapForRegion("image/bmp"))
        Assert.assertEquals(VERSION.SDK_INT >= 28, isSupportInBitmapForRegion("image/heic"))
        Assert.assertEquals(VERSION.SDK_INT >= 28, isSupportInBitmapForRegion("image/heif"))
        Assert.assertEquals(VERSION.SDK_INT >= 32, isSupportInBitmapForRegion("image/svg"))
    }

    @Test
    fun testIsWebP() {
        val context = getTestContext()

        Bytes(context.assets.open("sample.webp").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertTrue(isWebP())
        }
        Bytes(context.assets.open("sample_anim.webp").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertTrue(isWebP())
        }

        Bytes(context.assets.open("sample.webp").use {
            ByteArray(1024).apply { it.read(this) }.apply {
                set(8, 'V'.code.toByte())
            }
        }).apply {
            Assert.assertFalse(isWebP())
        }
        Bytes(context.assets.open("sample.jpeg").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertFalse(isWebP())
        }
    }

    @Test
    fun testIsAnimatedWebP() {
        val context = getTestContext()

        Bytes(context.assets.open("sample_anim.webp").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertTrue(isAnimatedWebP())
        }

        Bytes(context.assets.open("sample_anim.webp").use {
            ByteArray(1024).apply { it.read(this) }.apply {
                set(12, 'X'.code.toByte())
            }
        }).apply {
            Assert.assertFalse(isAnimatedWebP())
        }
        Bytes(context.assets.open("sample_anim.webp").use {
            ByteArray(1024).apply { it.read(this) }.apply {
                set(16, 0)
            }
        }).apply {
            Assert.assertFalse(isAnimatedWebP())
        }
        Bytes(context.assets.open("sample.webp").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertFalse(isAnimatedWebP())
        }
        Bytes(context.assets.open("sample.jpeg").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertFalse(isAnimatedWebP())
        }
    }

    @Test
    fun testIsHeif() {
        val context = getTestContext()

        Bytes(context.assets.open("sample.heic").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertTrue(isHeif())
        }

        Bytes(context.assets.open("sample_anim.webp").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertFalse(isHeif())
        }
        Bytes(context.assets.open("sample.jpeg").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertFalse(isHeif())
        }
    }

    @Test
    fun testIsAnimatedHeif() {
        val context = getTestContext()

        Bytes(context.assets.open("sample_anim.heif").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertTrue(isAnimatedHeif())
        }
        Bytes(context.assets.open("sample_anim.heif").use {
            ByteArray(1024).apply { it.read(this) }.apply {
                set(8, 'h'.code.toByte())
                set(9, 'e'.code.toByte())
                set(10, 'v'.code.toByte())
                set(11, 'c'.code.toByte())
            }
        }).apply {
            Assert.assertTrue(isAnimatedHeif())
        }
        Bytes(context.assets.open("sample_anim.heif").use {
            ByteArray(1024).apply { it.read(this) }.apply {
                set(8, 'h'.code.toByte())
                set(9, 'e'.code.toByte())
                set(10, 'v'.code.toByte())
                set(11, 'x'.code.toByte())
            }
        }).apply {
            Assert.assertTrue(isAnimatedHeif())
        }

        Bytes(context.assets.open("sample.heic").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertFalse(isAnimatedHeif())
        }
        Bytes(context.assets.open("sample_anim.webp").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertFalse(isAnimatedHeif())
        }
        Bytes(context.assets.open("sample.jpeg").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertFalse(isAnimatedHeif())
        }
    }

    @Test
    fun testIsGif() {
        val context = getTestContext()

        Bytes(context.assets.open("sample_anim.gif").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertTrue(isGif())
        }
        Bytes(context.assets.open("sample_anim.gif").use {
            ByteArray(1024).apply { it.read(this) }.apply {
                set(4, '7'.code.toByte())
            }
        }).apply {
            Assert.assertTrue(isGif())
        }

        Bytes(context.assets.open("sample_anim.webp").use {
            ByteArray(1024).apply { it.read(this) }
        }).apply {
            Assert.assertFalse(isGif())
        }
    }
}