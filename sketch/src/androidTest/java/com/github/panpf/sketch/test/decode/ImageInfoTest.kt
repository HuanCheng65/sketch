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
package com.github.panpf.sketch.test.decode

import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.decode.ImageInfo
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageInfoTest {

    @Test
    fun testConstructor() {
        ImageInfo(57, 34, "image/jpeg", 0).apply {
            Assert.assertEquals(57, width)
            Assert.assertEquals(34, height)
            Assert.assertEquals("image/jpeg", mimeType)
            Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, exifOrientation)
        }

        ImageInfo(570, 340, "image/png", ExifInterface.ORIENTATION_ROTATE_90).apply {
            Assert.assertEquals(570, width)
            Assert.assertEquals(340, height)
            Assert.assertEquals("image/png", mimeType)
            Assert.assertEquals(ExifInterface.ORIENTATION_ROTATE_90, exifOrientation)
        }
    }

    @Test
    fun testNewResult() {
        val imageInfo = ImageInfo(300, 500, "image/jpeg", 0).apply {
            Assert.assertEquals(300, width)
            Assert.assertEquals(500, height)
            Assert.assertEquals("image/jpeg", mimeType)
            Assert.assertEquals(0, exifOrientation)
        }

        imageInfo.newImageInfo().apply {
            Assert.assertNotSame(imageInfo, this)
            Assert.assertEquals(imageInfo, this)
            Assert.assertEquals(300, width)
            Assert.assertEquals(500, height)
            Assert.assertEquals("image/jpeg", mimeType)
            Assert.assertEquals(0, exifOrientation)
        }

        imageInfo.newImageInfo(width = 200).apply {
            Assert.assertNotSame(imageInfo, this)
            Assert.assertNotEquals(imageInfo, this)
            Assert.assertEquals(200, width)
            Assert.assertEquals(500, height)
            Assert.assertEquals("image/jpeg", mimeType)
            Assert.assertEquals(0, exifOrientation)
        }

        imageInfo.newImageInfo(height = 400).apply {
            Assert.assertNotSame(imageInfo, this)
            Assert.assertNotEquals(imageInfo, this)
            Assert.assertEquals(300, width)
            Assert.assertEquals(400, height)
            Assert.assertEquals("image/jpeg", mimeType)
            Assert.assertEquals(0, exifOrientation)
        }

        imageInfo.newImageInfo(mimeType = "image/png").apply {
            Assert.assertNotSame(imageInfo, this)
            Assert.assertNotEquals(imageInfo, this)
            Assert.assertEquals(300, width)
            Assert.assertEquals(500, height)
            Assert.assertEquals("image/png", mimeType)
            Assert.assertEquals(0, exifOrientation)
        }

        imageInfo.newImageInfo(exifOrientation = 2).apply {
            Assert.assertNotSame(imageInfo, this)
            Assert.assertNotEquals(imageInfo, this)
            Assert.assertEquals(300, width)
            Assert.assertEquals(500, height)
            Assert.assertEquals("image/jpeg", mimeType)
            Assert.assertEquals(2, exifOrientation)
        }
    }

    @Test
    fun testToString() {
        ImageInfo(57, 34, "image/jpeg", 0).apply {
            Assert.assertEquals(
                "ImageInfo(width=57, height=34, mimeType='image/jpeg', exifOrientation=UNDEFINED)",
                toString()
            )
        }

        ImageInfo(570, 340, "image/png", ExifInterface.ORIENTATION_ROTATE_90).apply {
            Assert.assertEquals(
                "ImageInfo(width=570, height=340, mimeType='image/png', exifOrientation=ROTATE_90)",
                toString()
            )
        }
    }

    @Test
    fun testToShortString() {
        ImageInfo(57, 34, "image/jpeg", 0).apply {
            Assert.assertEquals(
                "ImageInfo(57x34,'image/jpeg',UNDEFINED)",
                toShortString()
            )
        }

        ImageInfo(570, 340, "image/png", ExifInterface.ORIENTATION_ROTATE_90).apply {
            Assert.assertEquals(
                "ImageInfo(570x340,'image/png',ROTATE_90)",
                toShortString()
            )
        }
    }
}