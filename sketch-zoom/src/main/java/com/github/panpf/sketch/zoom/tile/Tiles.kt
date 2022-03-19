package com.github.panpf.sketch.zoom.tile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import com.github.panpf.sketch.sketch
import com.github.panpf.sketch.util.Logger
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.util.requiredMainThread
import com.github.panpf.sketch.zoom.Zoomer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Tiles constructor(
    private val context: Context,
    private val zoomer: Zoomer,
    private val imageUri: String,
    viewSize: Size,
    private val disabledExifOrientation: Boolean = false,
) {

    companion object {
        internal const val MODULE = "Tiles"
    }

    private val tempDrawMatrix = Matrix()
    private val tempVisibleRect = Rect()
    private val logger: Logger = context.sketch.logger
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var _destroyed: Boolean = false
    private var tileManager: TileManager? = null
    private var onTileChangedListenerList: List<OnTileChangedListener>? = null

    val destroyed: Boolean
        get() = _destroyed

    var showTileBounds = false
        set(value) {
            field = value
            invalidateView()
        }
    var viewSize: Size = viewSize
        internal set(value) {
            if (field != value) {
                field = value
                scope.launch(Dispatchers.Main) {
                    tileManager?.viewSize = value
                }
            }
        }
    var paused = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    logger.v(MODULE) { "pause. $imageUri" }
                    cleanMemory()
                } else {
                    logger.v(MODULE) { "resume. $imageUri" }
                    refreshTiles()
                }
            }
        }

    init {
        scope.launch(Dispatchers.Main) {
            val tileDecoder = withContext(Dispatchers.IO) {
                TileDecoder.Factory(context, imageUri, disabledExifOrientation).create()
            } ?: return@launch
            this@Tiles.tileManager =
                TileManager(context, imageUri, viewSize, tileDecoder, this@Tiles)
            refreshTiles()
        }

        zoomer.addOnMatrixChangeListener {
            refreshTiles()
        }
    }

    fun destroy() {
        _destroyed = true
        scope.cancel()
        tileManager?.destroy()
        tileManager = null
    }

    fun cleanMemory() {
        tileManager?.cleanMemory()
    }

    private fun refreshTiles() {
        requiredMainThread()
        if (destroyed) {
            logger.d(MODULE) { "refreshTiles. destroyed. $imageUri" }
            return
        }
        if (paused) {
            logger.d(MODULE) { "refreshTiles. paused. $imageUri" }
            return
        }
        val manager = tileManager
        if (manager == null) {
            logger.d(MODULE) { "refreshTiles. initializing. $imageUri" }
            return
        }
        if (zoomer.rotateDegrees % 90 != 0) {
            logger.w(MODULE, "rotate degrees must be in multiples of 90. $imageUri")
            return
        }

        val previewSize = zoomer.drawableSize
        val zooming = zoomer.isZooming
        val drawMatrix = tempDrawMatrix.apply {
            zoomer.getDrawMatrix(this)
        }
        val previewVisibleRect = tempVisibleRect.apply {
            zoomer.getVisibleRect(this)
        }

        if (previewVisibleRect.isEmpty) {
            logger.w(MODULE) {
                "refreshTiles. previewVisibleRect is empty. previewVisibleRect=${previewVisibleRect}. $imageUri"
            }
            cleanMemory()
            return
        }

        if (zooming) {
            logger.d(MODULE) {
                "refreshTiles. zooming. $imageUri"
            }
            return
        }

        tileManager?.refreshTiles(previewSize, previewVisibleRect, drawMatrix)
    }

    fun onDraw(canvas: Canvas) {
        if (destroyed) return
        val previewSize = zoomer.drawableSize
        val drawMatrix = tempDrawMatrix
        val previewVisibleRect = tempVisibleRect
        tileManager?.onDraw(canvas, previewSize, previewVisibleRect, drawMatrix)
    }

    internal fun invalidateView() {
        zoomer.view.invalidate()
    }
}