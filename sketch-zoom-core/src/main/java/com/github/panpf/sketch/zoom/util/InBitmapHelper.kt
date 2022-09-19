package com.github.panpf.sketch.zoom.util

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapFactory

interface InBitmapHelper {

    fun putToPool(bitmap: Bitmap, caller: String?): Boolean

    fun getFromPoolOrCreate(width: Int, height: Int, config: Bitmap.Config): Bitmap

    fun setInBitmapForRegion(
        options: BitmapFactory.Options,
        regionSize: Size,
        imageMimeType: String?,
        imageSize: Size,
    )

    fun freeBitmap(
        bitmap: Bitmap?,
        caller: String? = null,
    ): Boolean

    fun isInBitmapError(throwable: Throwable): Boolean

    fun isSrcRectError(throwable: Throwable): Boolean

    class Empty : InBitmapHelper {

        override fun putToPool(bitmap: Bitmap, caller: String?): Boolean {
            return false
        }

        override fun getFromPoolOrCreate(width: Int, height: Int, config: Bitmap.Config): Bitmap {
            return createBitmap(width, height, config)
        }

        override fun setInBitmapForRegion(
            options: BitmapFactory.Options,
            regionSize: Size,
            imageMimeType: String?,
            imageSize: Size
        ) {

        }

        override fun freeBitmap(bitmap: Bitmap?, caller: String?): Boolean {
            return false
        }

        override fun isInBitmapError(throwable: Throwable): Boolean {
            return false
        }

        override fun isSrcRectError(throwable: Throwable): Boolean {
            return false
        }
    }
}