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
package com.github.panpf.sketch.zoom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.ImageView.ScaleType.FIT_CENTER
import com.github.panpf.sketch.zoom.util.Size
import com.github.panpf.sketch.zoom.internal.Edge
import com.github.panpf.sketch.zoom.internal.ScaleDragHelper
import com.github.panpf.sketch.zoom.internal.ScalesFactoryImpl
import com.github.panpf.sketch.zoom.internal.ScrollBarHelper
import com.github.panpf.sketch.zoom.internal.TapHelper

/**
 * The middle layer of ImageView and Zoomer
 * <br>
 * Based https://github.com/Baseflow/PhotoView git 565505d5 20210120
 */
class ZoomAbility(val view: ImageView, val container: ZoomAbilityContainer) {

    companion object {
        internal const val MODULE = "ZoomAbility"
    }

    private val imageMatrix = Matrix()

    private val tapHelper = TapHelper(view.context, this)
    private val scaleDragHelper: ScaleDragHelper
    private var scrollBarHelper: ScrollBarHelper? = ScrollBarHelper(view.context, this)
    private var _rotateDegrees = 0

    private var onMatrixChangeListenerList: MutableSet<OnMatrixChangeListener>? = null
    private var onRotateChangeListenerList: MutableSet<OnRotateChangeListener>? = null
    private var onDragFlingListenerList: MutableSet<OnDragFlingListener>? = null
    private var onViewDragListenerList: MutableSet<OnViewDragListener>? = null
    private var onScaleChangeListenerList: MutableSet<OnScaleChangeListener>? = null
    private var onDataSourceChangeListenerList: MutableSet<OnDataSourceChangeListener>? = null

    val context: Context = view.context

    /** Allows the parent ViewGroup to intercept events while sliding to an edge */
    var allowParentInterceptOnEdge: Boolean = true
    var zoomInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    var onViewLongPressListener: OnViewLongPressListener? = null
    var onViewTapListener: OnViewTapListener? = null
    var viewSize = Size(0, 0)
        private set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    var imageSize = Size(0, 0)
        set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    var drawableSize = Size(0, 0)
        private set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    var scaleType: ScaleType = FIT_CENTER
        internal set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }

    var readModeDecider: ReadModeDecider? = null
        internal set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    var readModeEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                if (value && readModeDecider == null) {
                    readModeDecider = longImageReadMode()
                } else {
                    reset()
                }
            }
        }
    var scalesFactory: ScalesFactory = ScalesFactoryImpl()
        internal set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    var scrollBarEnabled: Boolean
        get() = scrollBarHelper != null
        internal set(value) {
            val enabled = scrollBarHelper != null
            if (enabled != value) {
                scrollBarHelper = if (value) {
                    ScrollBarHelper(context, this).apply { reset() }
                } else {
                    null
                }
            }
        }
    var zoomAnimationDuration: Int = 200
        internal set(value) {
            if (value > 0 && field != value) {
                field = value
            }
        }
    var scales: Scales = Scales.EMPTY
        private set

    init {
        addOnMatrixChangeListener { zoomer ->
            container.superSetImageMatrix(imageMatrix.apply { zoomer.getDrawMatrix(this) })
        }

        scaleDragHelper = ScaleDragHelper(
            context,
            this,
            onUpdateMatrix = {
                scrollBarHelper?.onMatrixChanged()
                onMatrixChangeListenerList?.forEach { listener ->
                    listener.onMatrixChanged(this)
                }
            },
            onViewDrag = { dx: Float, dy: Float ->
                onViewDragListenerList?.forEach {
                    it.onDrag(dx, dy)
                }
            },
            onDragFling = { startX: Float, startY: Float, velocityX: Float, velocityY: Float ->
                onDragFlingListenerList?.forEach {
                    it.onFling(startX, startY, velocityX, velocityY)
                }
            },
            onScaleChanged = { scaleFactor: Float, focusX: Float, focusY: Float ->
                onScaleChangeListenerList?.forEach {
                    it.onScaleChanged(scaleFactor, focusX, focusY)
                }
            }
        )

        val scaleType = container.superGetScaleType()
        require(scaleType != ScaleType.MATRIX) { "ScaleType cannot be MATRIX" }
        container.superSetScaleType(ScaleType.MATRIX)
        this.scaleType = scaleType

        resetDrawableSize()
        resetViewSize()
    }


    /*************************************** Internal ******************************************/

    private fun reset() {
        scales = scalesFactory.create(
            viewSize = viewSize,
            imageSize = imageSize,
            drawableSize = drawableSize,
            rotateDegrees = _rotateDegrees,
            scaleType = scaleType,
            readModeDecider = if (readModeEnabled) readModeDecider else null
        )
        scaleDragHelper.reset()
        container.logger.d(MODULE) {
            "reset. viewSize=$viewSize, imageSize=$imageSize, drawableSize=$drawableSize, " +
                    "rotateDegrees=$rotateDegrees, scaleType=$scaleType, readModeEnabled=$readModeEnabled, " +
                    "readModeDecider=$readModeDecider, scales=$scales"
        }
    }

    private fun resetDrawableSize() {
        val previewDrawable = view.drawable
        drawableSize = if (previewDrawable != null) {
            Size(previewDrawable.intrinsicWidth, previewDrawable.intrinsicHeight)
        } else {
            Size(0, 0)
        }
    }

    private fun resetViewSize() {
        viewSize = Size(
            view.width - view.paddingLeft - view.paddingRight,
            view.height - view.paddingTop - view.paddingBottom
        )
    }


    /**************************************** View Event ********************************************/

    fun onDrawableChanged() {
        resetDrawableSize()
    }

    fun onSizeChanged() {
        resetViewSize()
    }

    fun onDraw(canvas: Canvas) {
        scrollBarHelper?.onDraw(canvas)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawableSize.isEmpty) return false
        val scaleAndDragConsumed = scaleDragHelper.onTouchEvent(event)
        val tapConsumed = tapHelper.onTouchEvent(event)
        return scaleAndDragConsumed || tapConsumed
    }

    fun onSetScaleType(scaleType: ScaleType) {
        this.scaleType = scaleType
    }

    fun onGetScaleType(): ScaleType = scaleType


    /*************************************** Listener ******************************************/

    fun addOnMatrixChangeListener(listener: OnMatrixChangeListener) {
        this.onMatrixChangeListenerList = (onMatrixChangeListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnMatrixChangeListener(listener: OnMatrixChangeListener): Boolean {
        return onMatrixChangeListenerList?.remove(listener) == true
    }

    fun addOnRotateChangeListener(listener: OnRotateChangeListener) {
        this.onRotateChangeListenerList = (onRotateChangeListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnRotateChangeListener(listener: OnRotateChangeListener): Boolean {
        return onRotateChangeListenerList?.remove(listener) == true
    }

    fun addOnDragFlingListener(listener: OnDragFlingListener) {
        this.onDragFlingListenerList = (onDragFlingListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnDragFlingListener(listener: OnDragFlingListener): Boolean {
        return onDragFlingListenerList?.remove(listener) == true
    }

    fun addOnViewDragListener(listener: OnViewDragListener) {
        this.onViewDragListenerList = (onViewDragListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnViewDragListener(listener: OnViewDragListener): Boolean {
        return onViewDragListenerList?.remove(listener) == true
    }

    fun addOnScaleChangeListener(listener: OnScaleChangeListener) {
        this.onScaleChangeListenerList = (onScaleChangeListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnScaleChangeListener(listener: OnScaleChangeListener): Boolean {
        return onScaleChangeListenerList?.remove(listener) == true
    }

    fun addOnDataSourceChangeListener(listener: OnDataSourceChangeListener) {
        this.onDataSourceChangeListenerList =
            (onDataSourceChangeListenerList ?: LinkedHashSet()).apply {
                add(listener)
            }
    }

    fun removeOnDataSourceChangeListener(listener: OnDataSourceChangeListener): Boolean {
        return onDataSourceChangeListenerList?.remove(listener) == true
    }


    /*************************************** Function ******************************************/

    /**
     * Locate to the location specified on the preview image. You don't have to worry about scaling and rotation
     *
     * @param x Preview the x coordinate on the diagram
     * @param y Preview the y-coordinate on the diagram
     */
    fun location(x: Float, y: Float, animate: Boolean = false) {
        scaleDragHelper.location(x, y, animate)
    }

    /**
     * Scale to the specified scale. You don't have to worry about rotation degrees
     *
     * @param focalX  Scale the x coordinate of the center point on the view
     * @param focalY  Scale the y coordinate of the center point on the view
     */
    fun scale(scale: Float, focalX: Float, focalY: Float, animate: Boolean) {
        val finalScale = scale
            .coerceAtLeast(scales.min)
            .coerceAtMost(scales.max)
        scaleDragHelper.scale(finalScale, focalX, focalY, animate)
    }

    /**
     * Scale to the specified scale. You don't have to worry about rotation degrees
     */
    fun scale(scale: Float, animate: Boolean = false) {
        scale(scale, (view.right / 2).toFloat(), (view.bottom / 2).toFloat(), animate)
    }

    /**
     * Rotate the image to the specified degrees
     *
     * @param degrees Rotation degrees, can only be 90°, 180°, 270°, 360°
     */
    fun rotateTo(degrees: Int) {
        require(degrees % 90 == 0) { "degrees must be in multiples of 90: $degrees" }
        if (_rotateDegrees == degrees) return

        var newDegrees = degrees % 360
        if (newDegrees <= 0) {
            newDegrees = 360 - newDegrees
        }
        _rotateDegrees = newDegrees
        reset()
        onRotateChangeListenerList?.forEach {
            it.onRotateChanged(this)
        }
    }

    /**
     * Rotate an degrees based on the current rotation degrees
     *
     * @param addDegrees Rotation degrees, can only be 90°, 180°, 270°, 360°
     */
    fun rotateBy(addDegrees: Int) {
        return rotateTo(_rotateDegrees + addDegrees)
    }


    /***************************************** Information ****************************************/

    val rotateDegrees: Int
        get() = _rotateDegrees

    fun canScrollHorizontally(direction: Int): Boolean =
        scaleDragHelper.canScrollHorizontally(direction)

    fun canScrollVertically(direction: Int): Boolean =
        scaleDragHelper.canScrollVertically(direction)

    val horScrollEdge: Edge
        get() = scaleDragHelper.horScrollEdge

    val verScrollEdge: Edge
        get() = scaleDragHelper.verScrollEdge

    val scale: Float
        get() = scaleDragHelper.scale

    val baseScale: Float
        get() = scaleDragHelper.baseScale

    val supportScale: Float
        get() = scaleDragHelper.supportScale

    /** Zoom ratio that makes the image fully visible */
    val fullScale: Float
        get() = scales.full

    /** Gets the zoom that fills the image with the ImageView display */
    val fillScale: Float
        get() = scales.fill

    /** Gets the scale that allows the image to be displayed at scale to scale */
    val originScale: Float
        get() = scales.origin

    val minScale: Float
        get() = scales.min

    val maxScale: Float
        get() = scales.max

    val stepScales: FloatArray
        get() = scales.steps

    val isScaling: Boolean
        get() = scaleDragHelper.isScaling

    fun getDrawMatrix(matrix: Matrix) = scaleDragHelper.getDrawMatrix(matrix)

    fun getDrawRect(rectF: RectF) = scaleDragHelper.getDrawRect(rectF)

    /** Gets the area that the user can see on the preview (not affected by rotation) */
    fun getVisibleRect(rect: Rect) = scaleDragHelper.getVisibleRect(rect)

    fun touchPointToDrawablePoint(touchPoint: PointF): Point? {
        return scaleDragHelper.touchPointToDrawablePoint(touchPoint)
    }
}