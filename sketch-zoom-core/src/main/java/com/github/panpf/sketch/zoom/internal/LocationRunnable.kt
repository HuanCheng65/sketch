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
package com.github.panpf.sketch.zoom.internal

import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Scroller
import androidx.core.view.ViewCompat
import com.github.panpf.sketch.zoom.ZoomAbility

internal class LocationRunnable(
    private val zoomer: ZoomAbility,
    private val scaleDragHelper: ScaleDragHelper,
    private val startX: Int,
    private val startY: Int,
    private val endX: Int,
    private val endY: Int
) : Runnable {

    private val scroller = Scroller(zoomer.context, AccelerateDecelerateInterpolator())
    private var currentX = 0
    private var currentY = 0

    val isRunning: Boolean
        get() = !scroller.isFinished

    fun start() {
        cancel()

        currentX = startX
        currentY = startY
        scroller.startScroll(startX, startY, endX - startX, endY - startY, 300)
        zoomer.view.post(this)
    }

    fun cancel() {
        zoomer.view.removeCallbacks(this)
        scroller.forceFinished(true)
    }

    override fun run() {
        if (scroller.isFinished) {
            return
        }

        if (scroller.computeScrollOffset()) {
            val newX = scroller.currX
            val newY = scroller.currY
            val dx = (currentX - newX).toFloat()
            val dy = (currentY - newY).toFloat()
            scaleDragHelper.translateBy(dx, dy)
            currentX = newX
            currentY = newY
            ViewCompat.postOnAnimation(zoomer.view, this)
        }
    }
}