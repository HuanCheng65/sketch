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

import android.content.Context
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.widget.ImageView.ScaleType
import com.github.panpf.sketch.zoom.util.Logger
import com.github.panpf.sketch.zoom.util.Size
import com.github.panpf.sketch.zoom.ZoomAbility
import com.github.panpf.sketch.zoom.internal.Edge.BOTH
import com.github.panpf.sketch.zoom.internal.Edge.END
import com.github.panpf.sketch.zoom.internal.Edge.NONE
import com.github.panpf.sketch.zoom.internal.Edge.START
import com.github.panpf.sketch.zoom.internal.ScaleDragGestureDetector.OnActionListener
import com.github.panpf.sketch.zoom.internal.ScaleDragGestureDetector.OnGestureListener
import kotlin.math.abs
import kotlin.math.roundToInt

internal class ScaleDragHelper constructor(
    context: Context,
    private val zoomer: ZoomAbility,
    val onUpdateMatrix: () -> Unit,
    val onViewDrag: (dx: Float, dy: Float) -> Unit,
    val onDragFling: (startX: Float, startY: Float, velocityX: Float, velocityY: Float) -> Unit,
    val onScaleChanged: (scaleFactor: Float, focusX: Float, focusY: Float) -> Unit,
) {

    private val view = zoomer.view
    private val logger: Logger
        get() = zoomer.logger

    /* Stores default scale and translate information */
    private val baseMatrix = Matrix()

    /* Stores zoom, translate and externally set rotation information generated by the user through touch events */
    private val supportMatrix = Matrix()

    /* Store the fused information of baseMatrix and supportMatrix for drawing */
    private val drawMatrix = Matrix()
    private val drawRectF = RectF()

    /* Cache the coordinates of the last zoom gesture, used when restoring zoom */
    private var lastScaleFocusX: Float = 0f
    private var lastScaleFocusY: Float = 0f

    private var flingRunnable: FlingRunnable? = null
    private var locationRunnable: LocationRunnable? = null
    private var animatedScaleRunnable: AnimatedScaleRunnable? = null
    private val scaleDragGestureDetector: ScaleDragGestureDetector
    private var _horScrollEdge: Edge = NONE
    private var _verScrollEdge: Edge = NONE
    private var blockParentIntercept: Boolean = false
    private var dragging = false
    private var manualScaling = false

    val horScrollEdge: Edge
        get() = _horScrollEdge
    val verScrollEdge: Edge
        get() = _verScrollEdge

    val isScaling: Boolean
        get() = animatedScaleRunnable?.isRunning == true || manualScaling
    val baseScale: Float
        get() = baseMatrix.getScale()
    val supportScale: Float
        get() = supportMatrix.getScale()
    val scale: Float
        get() = drawMatrix.apply { getDrawMatrix(this) }.getScale()

    init {
        scaleDragGestureDetector = ScaleDragGestureDetector(context, object : OnGestureListener {
            override fun onDrag(dx: Float, dy: Float) = doDrag(dx, dy)

            override fun onFling(
                startX: Float, startY: Float, velocityX: Float, velocityY: Float
            ) = doFling(startX, startY, velocityX, velocityY)

            override fun onScaleBegin(): Boolean = doScaleBegin()

            override fun onScale(
                scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float
            ) = doScale(scaleFactor, focusX, focusY, dx, dy)

            override fun onScaleEnd() = doScaleEnd()
        }).apply {
            onActionListener = object : OnActionListener {
                override fun onActionDown(ev: MotionEvent) = actionDown()
                override fun onActionUp(ev: MotionEvent) = actionUp()
                override fun onActionCancel(ev: MotionEvent) = actionUp()
            }
        }
    }

    fun reset() {
        resetBaseMatrix()
        resetSupportMatrix()
        checkAndApplyMatrix()
    }

    fun recycle() {
        cancelFling()
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        /* Location operations cannot be interrupted */
        if (this.locationRunnable?.isRunning == true) {
            logger.v(ZoomAbility.MODULE) {
                "onTouchEvent. requestDisallowInterceptTouchEvent true. locating"
            }
            requestDisallowInterceptTouchEvent(true)
            return true
        }
        return scaleDragGestureDetector.onTouchEvent(event)
    }

    private fun resetBaseMatrix() {
        baseMatrix.reset()
        val viewSize = zoomer.viewSize.takeIf { !it.isEmpty } ?: return
        val (drawableWidth, drawableHeight) = zoomer.drawableSize.takeIf { !it.isEmpty }?.let {
            if (zoomer.rotateDegrees % 180 == 0) it else Size(it.height, it.width)
        } ?: return
        val drawableGreaterThanView =
            drawableWidth > viewSize.width || drawableHeight > viewSize.height
        val initZoomScale = zoomer.scales.init
        val scaleType = zoomer.scaleType
        when {
            zoomer.readModeEnabled && zoomer.readModeDecider?.should(
                drawableWidth, drawableHeight, viewSize.width, viewSize.height
            ) == true -> {
                baseMatrix.postScale(initZoomScale, initZoomScale)
            }
            scaleType == ScaleType.CENTER
                    || (scaleType == ScaleType.CENTER_INSIDE && !drawableGreaterThanView) -> {
                baseMatrix.postScale(initZoomScale, initZoomScale)
                val dx = (viewSize.width - drawableWidth) / 2f
                val dy = (viewSize.height - drawableHeight) / 2f
                baseMatrix.postTranslate(dx, dy)
            }
            scaleType == ScaleType.CENTER_CROP -> {
                baseMatrix.postScale(initZoomScale, initZoomScale)
                val dx = (viewSize.width - drawableWidth * initZoomScale) / 2f
                val dy = (viewSize.height - drawableHeight * initZoomScale) / 2f
                baseMatrix.postTranslate(dx, dy)
            }
            scaleType == ScaleType.FIT_START -> {
                baseMatrix.postScale(initZoomScale, initZoomScale)
                baseMatrix.postTranslate(0f, 0f)
            }
            scaleType == ScaleType.FIT_END -> {
                baseMatrix.postScale(initZoomScale, initZoomScale)
                baseMatrix.postTranslate(0f, viewSize.height - drawableHeight * initZoomScale)
            }
            scaleType == ScaleType.FIT_CENTER
                    || (scaleType == ScaleType.CENTER_INSIDE && drawableGreaterThanView) -> {
                baseMatrix.postScale(initZoomScale, initZoomScale)
                val dy = (viewSize.height - drawableHeight * initZoomScale) / 2f
                baseMatrix.postTranslate(0f, dy)
            }
            scaleType == ScaleType.FIT_XY -> {
                val srcRectF = RectF(0f, 0f, drawableWidth.toFloat(), drawableHeight.toFloat())
                val dstRectF = RectF(0f, 0f, viewSize.width.toFloat(), viewSize.height.toFloat())
                baseMatrix.setRectToRect(srcRectF, dstRectF, Matrix.ScaleToFit.FILL)
            }
        }
    }

    private fun resetSupportMatrix() {
        supportMatrix.reset()
        supportMatrix.postRotate(zoomer.rotateDegrees.toFloat())
    }

    private fun checkAndApplyMatrix() {
        if (checkMatrixBounds()) {
            onUpdateMatrix()
        }
    }

    private fun checkMatrixBounds(): Boolean {
        val drawRectF = drawRectF.apply { getDrawRect(this) }
        if (drawRectF.isEmpty) {
            _horScrollEdge = NONE
            _verScrollEdge = NONE
            return false
        }

        var deltaX = 0f
        val viewWidth = zoomer.viewSize.width
        val displayWidth = drawRectF.width()
        when {
            displayWidth.toInt() <= viewWidth -> {
                deltaX = when (zoomer.scaleType) {
                    ScaleType.FIT_START -> -drawRectF.left
                    ScaleType.FIT_END -> viewWidth - displayWidth - drawRectF.left
                    else -> (viewWidth - displayWidth) / 2 - drawRectF.left
                }
            }
            drawRectF.left.toInt() > 0 -> {
                deltaX = -drawRectF.left
            }
            drawRectF.right.toInt() < viewWidth -> {
                deltaX = viewWidth - drawRectF.right
            }
        }

        var deltaY = 0f
        val viewHeight = zoomer.viewSize.height
        val displayHeight = drawRectF.height()
        when {
            displayHeight.toInt() <= viewHeight -> {
                deltaY = when (zoomer.scaleType) {
                    ScaleType.FIT_START -> -drawRectF.top
                    ScaleType.FIT_END -> viewHeight - displayHeight - drawRectF.top
                    else -> (viewHeight - displayHeight) / 2 - drawRectF.top
                }
            }
            drawRectF.top.toInt() > 0 -> {
                deltaY = -drawRectF.top
            }
            drawRectF.bottom.toInt() < viewHeight -> {
                deltaY = viewHeight - drawRectF.bottom
            }
        }

        // Finally actually translate the matrix
        supportMatrix.postTranslate(deltaX, deltaY)

        _verScrollEdge = when {
            displayHeight.toInt() <= viewHeight -> BOTH
            drawRectF.top.toInt() >= 0 -> START
            drawRectF.bottom.toInt() <= viewHeight -> END
            else -> NONE
        }
        _horScrollEdge = when {
            displayWidth.toInt() <= viewWidth -> BOTH
            drawRectF.left.toInt() >= 0 -> START
            drawRectF.right.toInt() <= viewWidth -> END
            else -> NONE
        }
        return true
    }

    fun translateBy(dx: Float, dy: Float) {
        supportMatrix.postTranslate(dx, dy)
        checkAndApplyMatrix()
    }

    fun location(xInDrawable: Float, yInDrawable: Float, animate: Boolean) {
        locationRunnable?.cancel()
        cancelFling()

        val (viewWidth, viewHeight) = zoomer.viewSize.takeIf { !it.isEmpty } ?: return
        val pointF = PointF(xInDrawable, yInDrawable).apply {
            rotatePoint(this, zoomer.rotateDegrees, zoomer.drawableSize)
        }
        val newX = pointF.x
        val newY = pointF.y
        var nowScale = scale.format(2)
        val fullZoomScale = zoomer.fullScale.format(2)
        if (nowScale == fullZoomScale) {
            scale(
                scale = zoomer.originScale,
                focalX = zoomer.viewSize.width / 2f,
                focalY = zoomer.viewSize.height / 2f,
                animate = false
            )
        }

        val drawRectF = drawRectF.apply { getDrawRect(this) }
        nowScale = scale
        val scaleLocationX = (newX * nowScale).toInt()
        val scaleLocationY = (newY * nowScale).toInt()
        val scaledLocationX =
            scaleLocationX.coerceAtLeast(0).coerceAtMost(drawRectF.width().toInt())
        val scaledLocationY =
            scaleLocationY.coerceAtLeast(0).coerceAtMost(drawRectF.height().toInt())
        val centerLocationX = (scaledLocationX - viewWidth / 2).coerceAtLeast(0)
        val centerLocationY = (scaledLocationY - viewHeight / 2).coerceAtLeast(0)
        val startX = abs(drawRectF.left.toInt())
        val startY = abs(drawRectF.top.toInt())
        logger.v(ZoomAbility.MODULE) {
            "location. inDrawable=%dx%d, start=%dx%d, end=%dx%d"
                .format(xInDrawable, yInDrawable, startX, startY, centerLocationX, centerLocationY)
        }
        if (animate) {
            locationRunnable?.cancel()
            locationRunnable = LocationRunnable(
                zoomer = zoomer,
                scaleDragHelper = this@ScaleDragHelper,
                startX = startX,
                startY = startY,
                endX = centerLocationX,
                endY = centerLocationY
            )
            locationRunnable?.start()
        } else {
            val dx = -(centerLocationX - startX).toFloat()
            val dy = -(centerLocationY - startY).toFloat()
            translateBy(dx, dy)
        }
    }

    fun scale(scale: Float, focalX: Float, focalY: Float, animate: Boolean) {
        animatedScaleRunnable?.cancel()
        if (animate) {
            animatedScaleRunnable = AnimatedScaleRunnable(
                zoomer = zoomer,
                scaleDragHelper = this@ScaleDragHelper,
                startScale = zoomer.scale,
                endScale = scale,
                scaleFocalX = focalX,
                scaleFocalY = focalY
            )
            animatedScaleRunnable?.start()
        } else {
            val baseScale = baseScale
            val supportZoomScale = supportScale
            val finalScale = scale / baseScale
            val addScale = finalScale / supportZoomScale
            scaleBy(addScale, focalX, focalY)
        }
    }

    fun getDrawMatrix(matrix: Matrix) {
        matrix.set(baseMatrix)
        matrix.postConcat(supportMatrix)
    }

    fun getDrawRect(rectF: RectF) {
        val drawableSize = zoomer.drawableSize
        rectF[0f, 0f, drawableSize.width.toFloat()] = drawableSize.height.toFloat()
        drawMatrix.apply { getDrawMatrix(this) }.mapRect(rectF)
    }

    /**
     * Gets the area that the user can see on the drawable (not affected by rotation)
     */
    fun getVisibleRect(rect: Rect) {
        rect.setEmpty()
        val drawRectF = drawRectF.apply { getDrawRect(this) }.takeIf { !it.isEmpty } ?: return
        val viewSize = zoomer.viewSize.takeIf { !it.isEmpty } ?: return
        val drawableSize = zoomer.drawableSize.takeIf { !it.isEmpty } ?: return
        val (drawableWidth, drawableHeight) = drawableSize.let {
            if (zoomer.rotateDegrees % 180 == 0) it else Size(it.height, it.width)
        }
        val displayWidth = drawRectF.width()
        val displayHeight = drawRectF.height()
        val widthScale = displayWidth / drawableWidth
        val heightScale = displayHeight / drawableHeight
        var left: Float = if (drawRectF.left >= 0)
            0f else abs(drawRectF.left)
        var right: Float = if (displayWidth >= viewSize.width)
            viewSize.width + left else drawRectF.right - drawRectF.left
        var top: Float = if (drawRectF.top >= 0)
            0f else abs(drawRectF.top)
        var bottom: Float = if (displayHeight >= viewSize.height)
            viewSize.height + top else drawRectF.bottom - drawRectF.top
        left /= widthScale
        right /= widthScale
        top /= heightScale
        bottom /= heightScale
        rect.set(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
        reverseRotateRect(rect, zoomer.rotateDegrees, drawableSize)
    }

    fun touchPointToDrawablePoint(touchPoint: PointF): Point? {
        val drawableSize = zoomer.drawableSize.takeIf { !it.isEmpty } ?: return null
        val drawRect = RectF().apply { getDrawRect(this) }
        if (!drawRect.contains(touchPoint.x, touchPoint.y)) {
            return null
        }

        val zoomScale: Float = scale
        val drawableX =
            ((touchPoint.x - drawRect.left) / zoomScale).roundToInt().coerceAtLeast(0)
                .coerceAtMost(drawableSize.width)
        val drawableY =
            ((touchPoint.y - drawRect.top) / zoomScale).roundToInt().coerceAtLeast(0)
                .coerceAtMost(drawableSize.height)
        return Point(drawableX, drawableY)
    }

    /**
     * Whether you can scroll horizontally in the specified direction
     *
     * @param direction Negative to check scrolling left, positive to check scrolling right.
     */
    fun canScrollHorizontally(direction: Int): Boolean {
        return if (direction < 0) {
            horScrollEdge != START && horScrollEdge != BOTH
        } else {
            horScrollEdge != END && horScrollEdge != BOTH
        }
    }

    /**
     * Whether you can scroll vertically in the specified direction
     *
     * @param direction Negative to check scrolling up, positive to check scrolling down.
     */
    fun canScrollVertically(direction: Int): Boolean {
        return if (direction < 0) {
            verScrollEdge != START && horScrollEdge != BOTH
        } else {
            verScrollEdge != END && horScrollEdge != BOTH
        }
    }

    private fun doDrag(dx: Float, dy: Float) {
        logger.v(ZoomAbility.MODULE) { "onDrag. dx: $dx, dy: $dy" }

        if (scaleDragGestureDetector.isScaling) {
            logger.v(ZoomAbility.MODULE) { "onDrag. isScaling" }
            return
        }

        supportMatrix.postTranslate(dx, dy)
        checkAndApplyMatrix()

        onViewDrag(dx, dy)

        val scaling = scaleDragGestureDetector.isScaling
        val disallowParentInterceptOnEdge = !zoomer.allowParentInterceptOnEdge
        val blockParent = blockParentIntercept
        val disallow = if (dragging || scaling || blockParent || disallowParentInterceptOnEdge) {
            logger.d(ZoomAbility.MODULE) {
                "onDrag. DisallowParentIntercept. dragging=%s, scaling=%s, blockParent=%s, disallowParentInterceptOnEdge=%s"
                    .format(dragging, scaling, blockParent, disallowParentInterceptOnEdge)
            }
            true
        } else {
            val slop = 1f
            val result = (horScrollEdge == NONE && (dx >= slop || dx <= -slop))
                    || (horScrollEdge == START && dx <= -slop)
                    || (horScrollEdge == END && dx >= slop)
                    || (verScrollEdge == NONE && (dy >= slop || dy <= -slop))
                    || (verScrollEdge == START && dy <= -slop)
                    || (verScrollEdge == END && dy >= slop)
            if (result) {
                logger.d(ZoomAbility.MODULE) {
                    "onDrag. DisallowParentIntercept. scrollEdge=%s-%s, d=%sx%s"
                        .format(horScrollEdge, verScrollEdge, dx, dy)
                }
            } else {
                logger.d(ZoomAbility.MODULE) {
                    "onDrag. AllowParentIntercept. scrollEdge=%s-%s, d=%sx%s"
                        .format(horScrollEdge, verScrollEdge, dx, dy)
                }
            }
            dragging = result
            result
        }
        requestDisallowInterceptTouchEvent(disallow)
    }

    private fun doFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
        logger.v(ZoomAbility.MODULE) {
            "fling. startX=$startX, startY=$startY, velocityX=$velocityX, velocityY=$velocityY"
        }

        flingRunnable?.cancel()
        flingRunnable = FlingRunnable(
            zoomer = zoomer,
            scaleDragHelper = this@ScaleDragHelper,
            velocityX = velocityX.toInt(),
            velocityY = velocityY.toInt()
        )
        flingRunnable?.start()

        onDragFling(startX, startY, velocityX, velocityY)
    }

    private fun cancelFling() {
        flingRunnable?.cancel()
    }

    private fun doScaleBegin(): Boolean {
        logger.v(ZoomAbility.MODULE) { "onScaleBegin" }
        manualScaling = true
        return true
    }

    private fun scaleBy(addScale: Float, focalX: Float, focalY: Float) {
        supportMatrix.postScale(addScale, addScale, focalX, focalY)
        checkAndApplyMatrix()
    }

    internal fun doScale(scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float) {
        logger.v(ZoomAbility.MODULE) {
            "onScale. scaleFactor: $scaleFactor, focusX: $focusX, focusY: $focusY, dx: $dx, dy: $dy"
        }

        /* Simulate a rubber band effect when zoomed to max or min */
        var newScaleFactor = scaleFactor
        lastScaleFocusX = focusX
        lastScaleFocusY = focusY
        val oldSupportScale = supportScale
        var newSupportScale = oldSupportScale * newScaleFactor
        if (newScaleFactor > 1.0f) {
            // The maximum zoom has been reached. Simulate the effect of pulling a rubber band
            val maxSupportScale = zoomer.maxScale / baseMatrix.getScale()
            if (oldSupportScale >= maxSupportScale) {
                var addScale = newSupportScale - oldSupportScale
                addScale *= 0.4f
                newSupportScale = oldSupportScale + addScale
                newScaleFactor = newSupportScale / oldSupportScale
            }
        } else if (newScaleFactor < 1.0f) {
            // The minimum zoom has been reached. Simulate the effect of pulling a rubber band
            val minSupportScale = zoomer.minScale / baseMatrix.getScale()
            if (oldSupportScale <= minSupportScale) {
                var addScale = newSupportScale - oldSupportScale
                addScale *= 0.4f
                newSupportScale = oldSupportScale + addScale
                newScaleFactor = newSupportScale / oldSupportScale
            }
        }

        supportMatrix.postScale(newScaleFactor, newScaleFactor, focusX, focusY)
        supportMatrix.postTranslate(dx, dy)
        checkAndApplyMatrix()

        onScaleChanged(newScaleFactor, focusX, focusY)
    }

    private fun doScaleEnd() {
        logger.v(ZoomAbility.MODULE) { "onScaleEnd" }
        val currentScale = scale.format(2)
        val overMinZoomScale = currentScale < zoomer.minScale.format(2)
        val overMaxZoomScale = currentScale > zoomer.maxScale.format(2)
        if (!overMinZoomScale && !overMaxZoomScale) {
            manualScaling = false
            onUpdateMatrix()
        }
    }

    private fun actionDown() {
        logger.v(ZoomAbility.MODULE) {
            "onActionDown. disallow parent intercept touch event"
        }

        lastScaleFocusX = 0f
        lastScaleFocusY = 0f
        dragging = false

        requestDisallowInterceptTouchEvent(true)

        cancelFling()
    }

    private fun actionUp() {
        /* Roll back to minimum or maximum scaling */
        val currentScale = scale.format(2)
        val minZoomScale = zoomer.minScale.format(2)
        val maxZoomScale = zoomer.maxScale.format(2)
        if (currentScale < minZoomScale) {
            val drawRectF = drawRectF.apply { getDrawRect(this) }
            if (!drawRectF.isEmpty) {
                scale(minZoomScale, drawRectF.centerX(), drawRectF.centerY(), true)
            }
        } else if (currentScale > maxZoomScale) {
            val lastScaleFocusX = lastScaleFocusX
            val lastScaleFocusY = lastScaleFocusY
            if (lastScaleFocusX != 0f && lastScaleFocusY != 0f) {
                scale(maxZoomScale, lastScaleFocusX, lastScaleFocusY, true)
            }
        }
    }

    private fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        view.parent?.requestDisallowInterceptTouchEvent(disallowIntercept)
    }
}