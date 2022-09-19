package com.github.panpf.sketch.zoom

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.view.View
import androidx.annotation.MainThread
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import com.github.panpf.sketch.zoom.internal.TileDecoder
import com.github.panpf.sketch.zoom.internal.TileManager
import com.github.panpf.sketch.zoom.util.Size
import com.github.panpf.sketch.zoom.util.contentSize
import com.github.panpf.sketch.zoom.internal.getLifecycle
import com.github.panpf.sketch.zoom.internal.requiredMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SubsamplingAbility(
    val view: View,
    val container: ZoomAbilityContainer,
    val imageSource: ImageSource,
    val imageInfo: ImageInfo,
) : OnMatrixChangeListener {

    companion object {
        internal const val MODULE = "Subsampling"
    }

    private val tempDrawMatrix = Matrix()
    private val tempPreviewVisibleRect = Rect()
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            ON_START -> paused = true
            ON_STOP -> paused = false
            else -> {}
        }
    }

    private var _destroyed: Boolean = false
    private var tileManager: TileManager? = null
    private var lastPostResetTilesJob: Job? = null
    private var onTileChangedListenerList: MutableSet<OnTileChangedListener>? = null

    val destroyed: Boolean
        get() = _destroyed

    /**************************************** Configurations ********************************************/

    var lifecycle: Lifecycle? = null    // todo 请求开始时从 ImageRequest 中更新 lifecycle
        set(value) {
            if (field != value) {
                unregisterLifecycleObserver()
                field = value
                registerLifecycleObserver()
            }
        }
    var showTileBounds: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidateView()
            }
        }
    var paused = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    container.logger.d(MODULE) { "pause. ${imageSource.key}" }
                    tileManager?.clean()
                } else {
                    container.logger.d(MODULE) { "resume. ${imageSource.key}" }
                    refreshTiles()
                }
            }
        }

    init {
        lifecycle = view.context.getLifecycle()
        registerLifecycleObserver()
        val tileDecoder = TileDecoder(imageSource, imageInfo, container)
        tileManager =
            TileManager(imageSource.key, view.contentSize(), tileDecoder, container, this) {
                onTileChangedListenerList?.forEach {
                    it.onTileChanged(this@SubsamplingAbility)
                }
            }
        refreshTiles()
//        postResetTiles()
    }


    /*************************************** Internal ******************************************/

//    private fun postResetTiles() {
//        // Triggering the reset tiles frequently (such as changing the view size in shared element animations)
//        // can cause large fluctuations in memory, so delayed resets can avoid this problem
//        lastPostResetTilesJob?.cancel()
//        lastPostResetTilesJob = scope.launch(Dispatchers.Main) {
//            delay(60)
//            tiles?.destroy()
//            tiles = newTiles(zoomer)?.apply {
//                showTileBounds = this@ZoomAbility.showTileBounds
//                paused = this@ZoomAbility.lifecycle?.currentState?.isAtLeast(STARTED) == false
//            }
//        }
//    }

    override fun onMatrixChanged(zoomer: ZoomAbility) {
        refreshTiles()
    }

    @MainThread
    fun destroy() {
        requiredMainThread()
        unregisterLifecycleObserver()

        if (_destroyed) return
        container.logger.w(MODULE, "destroy")
        _destroyed = true
        scope.cancel()
        tileManager?.destroy()
        tileManager = null
    }

    private fun registerLifecycleObserver() {
        lifecycle?.addObserver(lifecycleObserver)
        paused = lifecycle?.currentState?.isAtLeast(STARTED) == false
    }

    private fun unregisterLifecycleObserver() {
        lifecycle?.removeObserver(lifecycleObserver)
        paused = false
    }

//    private fun newTiles(zoomer: Zoomer?): Tiles? {
//        zoomer ?: return null
//        val viewSize = view.sizeWithoutPaddingOrNull
//        if (viewSize == null) {
//            logger.d(ZoomAbility.MODULE) { "Can't use Tiles. View size error" }
//            return null
//        }
//        val previewDrawable = view.drawable
//        val sketchDrawable = previewDrawable?.findLastSketchDrawable()?.takeIf { it !is Animatable }
//        if (sketchDrawable == null) {
//            logger.d(ZoomAbility.MODULE) { "Can't use Tiles. Drawable error" }
//            return null
//        }
//
//        val previewWidth = sketchDrawable.bitmapInfo.width
//        val previewHeight = sketchDrawable.bitmapInfo.height
//        val imageWidth = sketchDrawable.imageInfo.width
//        val imageHeight = sketchDrawable.imageInfo.height
//        val mimeType = sketchDrawable.imageInfo.mimeType
//        val key = sketchDrawable.requestKey
//
//        if (previewWidth >= imageWidth && previewHeight >= imageHeight) {
//            logger.d(ZoomAbility.MODULE) {
//                "Don't need to use Tiles. previewSize: %dx%d, imageSize: %dx%d, mimeType: %s. %s"
//                    .format(previewWidth, previewHeight, imageWidth, imageHeight, mimeType, key)
//            }
//            return null
//        }
//        if (!shouldUseTiles(imageWidth, imageHeight, previewWidth, previewHeight)) {
//            logger.d(ZoomAbility.MODULE) {
//                "Can't use Tiles. previewSize error. previewSize: %dx%d, imageSize: %dx%d, mimeType: %s. %s"
//                    .format(previewWidth, previewHeight, imageWidth, imageHeight, mimeType, key)
//            }
//            return null
//        }
//        if (ImageFormat.parseMimeType(mimeType)?.supportBitmapRegionDecoder() != true) {
//            logger.d(ZoomAbility.MODULE) {
//                "MimeType does not support Tiles. previewSize: %dx%d, imageSize: %dx%d, mimeType: %s. %s"
//                    .format(previewWidth, previewHeight, imageWidth, imageHeight, mimeType, key)
//            }
//            return null
//        }
//
//        logger.d(ZoomAbility.MODULE) {
//            "Use Tiles. previewSize: %dx%d, imageSize: %dx%d, mimeType: %s. %s"
//                .format(previewWidth, previewHeight, imageWidth, imageHeight, mimeType, key)
//        }
//        val exifOrientation: Int = sketchDrawable.imageInfo.exifOrientation
//        val imageUri = sketchDrawable.imageUri
//        return Tiles(
//            context = view.context,
//            sketch = sketch,
//            zoomer = zoomer,
//            imageUri = imageUri,
//            viewSize = viewSize,
//            disabledExifOrientation = exifOrientation == ExifInterface.ORIENTATION_UNDEFINED
//        ).apply {
//            this@ZoomAbility.onTileChangedListenerList?.forEach {
//                addOnTileChangedListener(it)
//            }
//        }
//    }

    @MainThread
    private fun refreshTiles() {
        requiredMainThread()

        if (destroyed) {
            container.logger.d(MODULE) { "refreshTiles. interrupted. destroyed. ${imageSource?.key}" }
            return
        }
        if (paused) {
            container.logger.d(MODULE) { "refreshTiles. interrupted. paused. ${imageSource?.key}" }
            return
        }
        val manager = tileManager
        if (manager == null) {
            container.logger.d(MODULE) { "refreshTiles. interrupted. initializing. ${imageSource?.key}" }
            return
        }
        if (zoomer.rotateDegrees % 90 != 0) {
            container.logger.w(
                MODULE,
                "refreshTiles. interrupted. rotate degrees must be in multiples of 90. ${imageSource?.key}"
            )
            return
        }

        val previewSize = zoomer.drawableSize
        val scaling = zoomer.isScaling
        val drawMatrix = tempDrawMatrix.apply {
            zoomer.getDrawMatrix(this)
        }
        val previewVisibleRect = tempPreviewVisibleRect.apply {
            zoomer.getVisibleRect(this)
        }

        if (previewVisibleRect.isEmpty) {
            container.logger.w(MODULE) {
                "refreshTiles. interrupted. previewVisibleRect is empty. previewVisibleRect=${previewVisibleRect}. ${imageSource?.key}"
            }
            tileManager?.clean()
            return
        }

        if (scaling) {
            container.logger.d(MODULE) {
                "refreshTiles. interrupted. scaling. ${imageSource?.key}"
            }
            return
        }

        if (zoomer.scale.format(2) <= zoomer.minScale.format(2)) {
            container.logger.d(MODULE) {
                "refreshTiles. interrupted. minScale. ${imageSource?.key}"
            }
            tileManager?.clean()
            return
        }

        tileManager?.refreshTiles(previewSize, previewVisibleRect, drawMatrix)
    }

    @MainThread
    internal fun invalidateView() {
        requiredMainThread()
        view.invalidate()
    }


    /**************************************** View Event ********************************************/

    fun onDetachedFromWindow() {
        destroy()
    }

    fun onSizeChanged() {
//        postResetTiles()
    }

    @MainThread
    fun onDraw(canvas: Canvas) {
        requiredMainThread()

        if (destroyed) return
        val previewSize = zoomer.drawableSize
        val drawMatrix = tempDrawMatrix
        val previewVisibleRect = tempPreviewVisibleRect
        tileManager?.onDraw(canvas, previewSize, previewVisibleRect, drawMatrix)
    }

    fun onVisibilityChanged() {
        paused = !view.isVisible
    }


    /*************************************** Listener ******************************************/

    fun addOnTileChangedListener(listener: OnTileChangedListener) {
        this.onTileChangedListenerList = (onTileChangedListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnTileChangedListener(listener: OnTileChangedListener): Boolean {
        return onTileChangedListenerList?.remove(listener) == true
    }

    fun eachTileList(action: (tile: Tile, load: Boolean) -> Unit) {
        val previewSize = zoomer.drawableSize.takeIf { !it.isEmpty } ?: return
        val previewVisibleRect = tempPreviewVisibleRect.apply {
            zoomer.getVisibleRect(this)
        }.takeIf { !it.isEmpty } ?: return
        tileManager?.eachTileList(previewSize, previewVisibleRect, action)
    }


    /***************************************** Information ****************************************/

    val tileList: List<Tile>?
        get() = tileManager?.tileList
    val imageSize: Size?
        get() = tileManager?.imageSize
}