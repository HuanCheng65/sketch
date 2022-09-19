package com.github.panpf.sketch.zoom.util

import androidx.exifinterface.media.ExifInterface
import com.github.panpf.sketch.zoom.ImageSource


fun ImageSource.readExifOrientationWithMimeType(mimeType: String): Int =
    if (ExifInterface.isSupportedMimeType(mimeType)) {
        newInputStream().buffered().use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        }
    } else {
        ExifInterface.ORIENTATION_UNDEFINED
    }