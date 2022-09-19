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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.exifinterface.media.ExifInterface
import com.github.panpf.sketch.zoom.internal.canSubsampling
import com.github.panpf.sketch.zoom.util.InBitmapHelper
import com.github.panpf.sketch.zoom.util.Logger
import com.github.panpf.sketch.zoom.util.Logger.Level.DEBUG
import com.github.panpf.sketch.zoom.util.Logger.Level.INFO
import com.github.panpf.sketch.zoom.util.Size
import com.github.panpf.sketch.zoom.util.realDecodeSource
import com.github.panpf.zoom.core.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle), ZoomAbilityContainer {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var zoomAbility: ZoomAbility? = null
    private var subsamplingAbility: SubsamplingAbility? = null
    private var lastDecodeSourceJob: Job? = null
    private var imageSource: ImageSource? = null
    private var imageInfo: ImageInfo? = null

    override var logger = Logger(if (BuildConfig.DEBUG) DEBUG else INFO)
    override var inBitmapHelper: InBitmapHelper = InBitmapHelper.Empty()

    init {
        zoomAbility = ZoomAbility(this, this)
    }

    override fun canScrollHorizontally(direction: Int): Boolean =
        zoomAbility?.canScrollHorizontally(direction) == true

    override fun canScrollVertically(direction: Int): Boolean =
        zoomAbility?.canScrollVertically(direction) == true

    final override fun superSetImageMatrix(matrix: Matrix?) {
        super.setImageMatrix(matrix)
    }

    final override fun superGetImageMatrix(): Matrix {
        return super.getImageMatrix()
    }

    final override fun superSetScaleType(scaleType: ScaleType) {
        super.setScaleType(scaleType)
    }

    final override fun superGetScaleType(): ScaleType {
        return super.getScaleType()
    }

    final override fun setScaleType(scaleType: ScaleType) {
        val zoomAbility = zoomAbility
        if (zoomAbility != null) {
            zoomAbility.onSetScaleType(scaleType)
        } else {
            super.setScaleType(scaleType)
        }
    }

    final override fun getScaleType(): ScaleType {
        return zoomAbility?.onGetScaleType() ?: super.getScaleType()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initialize()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroy()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        zoomAbility?.onSizeChanged()
        subsamplingAbility?.onSizeChanged()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        zoomAbility?.onDraw(canvas)
        subsamplingAbility?.onDraw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return zoomAbility?.onTouchEvent(event) == true || super.onTouchEvent(event)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        val oldDrawable = this.drawable
        super.setImageDrawable(drawable)
        val newDrawable = this.drawable
        if (oldDrawable !== newDrawable) {
            onDrawableChanged()
        }
    }

    override fun setImageURI(uri: Uri?) {
        val oldDrawable = this.drawable
        super.setImageURI(uri)
        val newDrawable = this.drawable
        if (oldDrawable !== newDrawable) {
            onDrawableChanged()
        }
    }

    protected fun onDrawableChanged() {
        zoomAbility?.onDrawableChanged()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        subsamplingAbility?.onVisibilityChanged()
    }

    fun setDataSource(imageSource: ImageSource?) {
        if (this.imageSource != imageSource) {
            destroy()
            this.imageSource = imageSource
            this.imageInfo = null
            initialize()
        }
    }

    private fun initialize() {
        val dataSource = imageSource
        val imageInfo = imageInfo
        if (dataSource != null && imageInfo == null && ViewCompat.isAttachedToWindow(this)) {
            decodeSource(dataSource)
        }
    }

    private fun destroy() {
        lastDecodeSourceJob?.cancel()
        subsamplingAbility?.let {
            it.destroy()
            zoomAbility?.removeOnMatrixChangeListener(it)
            this.subsamplingAbility = null
        }
    }

    protected fun decodeSource(imageSource: ImageSource) {
        lastDecodeSourceJob?.cancel()
        lastDecodeSourceJob = coroutineScope.launch(Dispatchers.Main.immediate) {
            val result = realDecodeSource(this@ZoomImageView, imageSource, inBitmapHelper)
                ?: return@launch
            val imageInfo = ImageInfo(result.imageSize, result.mimeType, result.exifOrientation)
            zoomAbility?.imageSize = result.imageSize
            this@ZoomImageView.imageInfo = imageInfo
            setImageBitmap(result.sampledBitmap)
            setupSubsampling(
                imageSource = imageSource,
                imageInfo = imageInfo,
                sampledSize = Size(result.sampledBitmap.width, result.sampledBitmap.height),
            )
        }
    }

    protected fun setupSubsampling(
        imageSource: ImageSource,
        imageInfo: ImageInfo,
        sampledSize: Size
    ) {
        this@ZoomImageView.subsamplingAbility =
            if (ExifInterface.isSupportedMimeType(imageInfo.mimeType)
                && canSubsampling(imageInfo.size, sampledSize)
            ) {
                SubsamplingAbility(
                    view = this@ZoomImageView,
                    container = this@ZoomImageView,
                    imageSource = imageSource,
                    imageInfo = imageInfo
                ).apply {
                    zoomAbility?.addOnMatrixChangeListener(this)
//                    showTileBounds = this@ZoomAbility.showTileBounds
                }
            } else {
                null
            }
    }
}