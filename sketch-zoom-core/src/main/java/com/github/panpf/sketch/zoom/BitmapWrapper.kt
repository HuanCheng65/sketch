package com.github.panpf.sketch.zoom

import android.graphics.Bitmap

interface BitmapWrapper {

    val bitmap: Bitmap?

    fun onAttached()

    fun onDetached()
}