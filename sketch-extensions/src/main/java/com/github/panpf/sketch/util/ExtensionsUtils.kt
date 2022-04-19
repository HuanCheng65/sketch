package com.github.panpf.sketch.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.github.panpf.sketch.cache.BitmapPool
import java.math.BigDecimal

internal fun Float.format(newScale: Int): Float =
    BigDecimal(toDouble()).setScale(newScale, BigDecimal.ROUND_HALF_UP).toFloat()


/**
 * Drawable into Bitmap. Each time a new bitmap is drawn
 */
internal fun Drawable.toBitmap(lowQuality: Boolean = false, bitmapPool: BitmapPool? = null): Bitmap {
    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    val config = if (lowQuality) Bitmap.Config.ARGB_4444 else Bitmap.Config.ARGB_8888
    val bitmap: Bitmap = bitmapPool?.getOrCreate(intrinsicWidth, intrinsicHeight, config)
        ?: Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, config)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}