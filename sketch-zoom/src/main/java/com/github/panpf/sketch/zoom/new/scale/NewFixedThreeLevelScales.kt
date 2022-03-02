package com.github.panpf.sketch.zoom.new.scale

import android.content.Context
import android.widget.ImageView.ScaleType
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.zoom.ReadModeDecider

/**
 * 固定的三级缩放比例
 */
class NewFixedThreeLevelScales : NewZoomScales {

    private val scales = floatArrayOf(1.0f, 2.0f, 3.0f)
    private var fullZoomScaleCache = 0f  // 能够看到图片全貌的缩放比例
    private var fillZoomScaleCache = 0f  // 能够让图片填满宽或高的缩放比例
    private var originZoomScaleCache = 0f    // 能够让图片按照真实尺寸一比一显示的缩放比例

    override val minZoomScale = 1.0f

    override val maxZoomScale = 3.0f

    override val initZoomScale = 1.0f

    override val fullZoomScale = fullZoomScaleCache

    override val fillZoomScale = fillZoomScaleCache

    override val originZoomScale = originZoomScaleCache

    override val zoomScales = scales

    override fun reset(
        context: Context,
        viewSize: Size,
        imageSize: Size,
        drawableSize: Size,
        scaleType: ScaleType,
        rotateDegrees: Float,
        readModeDecider: ReadModeDecider?,
    ) {
        val drawableWidth =
            if (rotateDegrees % 180 == 0f) drawableSize.width else drawableSize.height
        val drawableHeight =
            if (rotateDegrees % 180 == 0f) drawableSize.height else drawableSize.width
        val imageWidth =
            if (rotateDegrees % 180 == 0f) imageSize.width else imageSize.height
        val imageHeight =
            if (rotateDegrees % 180 == 0f) imageSize.height else imageSize.width
        val widthScale = viewSize.width.toFloat() / drawableWidth
        val heightScale = viewSize.height.toFloat() / drawableHeight

        // 小的是完整显示比例，大的是充满比例
        fullZoomScaleCache = widthScale.coerceAtMost(heightScale)
        fillZoomScaleCache = widthScale.coerceAtLeast(heightScale)
        originZoomScaleCache =
            (imageWidth.toFloat() / drawableWidth).coerceAtLeast(imageHeight.toFloat() / drawableHeight)
    }

    override fun clean() {
        originZoomScaleCache = 1.0f
        fillZoomScaleCache = originZoomScale
        fullZoomScaleCache = fillZoomScale
    }
}