package com.github.panpf.sketch.target

import android.graphics.Bitmap
import androidx.annotation.MainThread
import com.github.panpf.sketch.util.SketchException

interface LoadTarget : Target {

    /**
     * Called when the request starts.
     */
    @MainThread
    fun onStart() {
    }

    /**
     * Called if the request completes successfully.
     */
    @MainThread
    fun onSuccess(result: Bitmap) {
    }

    /**
     * Called if an error occurs while executing the request.
     */
    @MainThread
    fun onError(exception: SketchException) {
    }
}