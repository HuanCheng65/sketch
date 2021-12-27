/*
 * Copyright (C) 2019 panpf <panpfpanpf@outlook.com>
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
package com.github.panpf.sketch.zoom

import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Scroller
import com.github.panpf.sketch.SLog
import com.github.panpf.sketch.SLog.Companion.isLoggable
import com.github.panpf.sketch.SLog.Companion.vm
import com.github.panpf.sketch.SLog.Companion.wm
import com.github.panpf.sketch.util.SketchUtils.Companion.postOnAnimation

/**
 * 定位执行器
 */
internal class LocationRunner(
    private val imageZoomer: ImageZoomer,
    private val scaleDragHelper: ScaleDragHelper
) : Runnable {

    private val scroller: Scroller =
        Scroller(imageZoomer.getImageView().context, AccelerateDecelerateInterpolator())
    private var currentX = 0
    private var currentY = 0

    /**
     * 定位到预览图上指定的位置
     */
    fun location(startX: Int, startY: Int, endX: Int, endY: Int) {
        currentX = startX
        currentY = startY
        scroller.startScroll(startX, startY, endX - startX, endY - startY, 300)
        val imageView = imageZoomer.getImageView()
        imageView.removeCallbacks(this)
        imageView.post(this)
    }

    override fun run() {
        // remaining post that should not be handled
        if (scroller.isFinished) {
            if (isLoggable(SLog.VERBOSE)) {
                vm(ImageZoomer.MODULE, "finished. location run")
            }
            return
        }
        if (!imageZoomer.isWorking) {
            wm(ImageZoomer.MODULE, "not working. location run")
            scroller.forceFinished(true)
            return
        }
        if (!scroller.computeScrollOffset()) {
            if (isLoggable(SLog.VERBOSE)) {
                vm(ImageZoomer.MODULE, "scroll finished. location run")
            }
            return
        }
        val newX = scroller.currX
        val newY = scroller.currY
        val dx = (currentX - newX).toFloat()
        val dy = (currentY - newY).toFloat()
        scaleDragHelper.translateBy(dx, dy)
        currentX = newX
        currentY = newY

        // Post On animation
        postOnAnimation(imageZoomer.getImageView(), this)
    }

    val isRunning: Boolean
        get() = !scroller.isFinished

    fun cancel() {
        scroller.forceFinished(true)
        val imageView = imageZoomer.getImageView()
        imageView.removeCallbacks(this)
    }
}