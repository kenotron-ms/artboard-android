package com.artboard.ui.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.artboard.data.model.Brush
import com.artboard.data.model.BrushType
import com.artboard.data.model.Layer
import com.artboard.domain.engine.DrawingEngine
import com.artboard.domain.layer.LayerManager

/**
 * Custom view for drawing with low-latency touch handling.
 * Supports infinite canvas with pan, zoom, and rotate gestures.
 */
class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), CanvasGestureDetector.GestureListener {
    
    // Drawing state
    private var layers: List<Layer> = emptyList()
    private var activeLayerIndex: Int = 0
    private var backgroundColor: Int = Color.WHITE
    
    // Canvas dimensions (the actual artwork size, independent of view size)
    private var canvasWidth: Int = 0
    private var canvasHeight: Int = 0
    
    // Current drawing
    private var currentBrush: Brush = Brush.pen()
    private var currentColor: Int = Color.BLACK
    
    // Drawing engine
    private val drawingEngine = DrawingEngine()
    private val touchProcessor = TouchEventProcessor()
    private var layerManager: LayerManager? = null
    
    // Canvas transformation (pan, zoom, rotate)
    val canvasTransform = CanvasTransform()
    private val gestureDetector = CanvasGestureDetector(this)
    
    // Rendering
    private var compositeBitmap: Bitmap? = null
    private var drawingBitmap: Bitmap? = null
    private val paint = Paint().apply {
        // Nearest-neighbor scaling: preserves sharp pixels when zoomed
        isAntiAlias = false
        isFilterBitmap = false  // false = nearest-neighbor (crisp pixels)
        isDither = false
    }
    
    // Background pattern paint (checkerboard for transparency)
    private val checkerPaint = Paint().apply {
        isAntiAlias = false
    }
    private var checkerBitmap: Bitmap? = null
    
    // Canvas border paint
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.GRAY
    }
    
    // Shadow paint for canvas
    private val shadowPaint = Paint().apply {
        color = Color.argb(50, 0, 0, 0)
    }
    
    // Callbacks
    var onStrokeBegin: (() -> Unit)? = null
    var onStrokeEnd: (() -> Unit)? = null
    var onStrokeUpdate: (() -> Unit)? = null
    var onTwoFingerTap: (() -> Unit)? = null  // For undo
    var onTransformChanged: ((CanvasTransform) -> Unit)? = null
    
    // Performance tracking
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0f
    
    // Touch state
    private var activePointerId = -1
    
    init {
        // Use NONE to avoid hardware layer caching which causes pixelation when transformed
        // The view will still be hardware accelerated, but won't cache as a texture
        setLayerType(LAYER_TYPE_NONE, null)
        
        // Create checkerboard pattern for transparency
        createCheckerPattern()
    }
    
    private fun createCheckerPattern() {
        val size = 16
        checkerBitmap = Bitmap.createBitmap(size * 2, size * 2, Bitmap.Config.ARGB_8888).apply {
            val c = Canvas(this)
            val light = Color.rgb(240, 240, 240)
            val dark = Color.rgb(200, 200, 200)
            
            c.drawColor(light)
            val p = Paint().apply { color = dark }
            c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
            c.drawRect(size.toFloat(), size.toFloat(), size * 2f, size * 2f, p)
        }
    }
    
    /**
     * Initialize with canvas dimensions
     */
    fun initialize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        
        canvasWidth = width
        canvasHeight = height
        
        // Clean up old bitmaps
        compositeBitmap?.recycle()
        drawingBitmap?.recycle()
        
        // Create new bitmaps
        compositeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        layerManager = LayerManager(width, height)
        
        // Initial layer
        if (layers.isEmpty()) {
            val initialLayer = Layer.create(width, height, "Background")
            layers = listOf(initialLayer)
            activeLayerIndex = 0
        }
        
        updateComposite()
        
        // Center canvas in view after layout
        post {
            if (this.width > 0 && this.height > 0) {
                canvasTransform.fitToViewport(
                    canvasWidth.toFloat(),
                    canvasHeight.toFloat(),
                    this.width.toFloat(),
                    this.height.toFloat()
                )
                invalidate()
            }
        }
    }
    
    /**
     * Update layers
     */
    fun setLayers(newLayers: List<Layer>, activeIndex: Int = 0) {
        layers = newLayers
        activeLayerIndex = activeIndex.coerceIn(0, layers.size - 1)
        updateComposite()
        invalidate()
    }
    
    /**
     * Set current brush
     */
    fun setBrush(brush: Brush) {
        currentBrush = brush
    }
    
    /**
     * Set current color
     */
    fun setColor(color: Int) {
        currentColor = color
    }
    
    /**
     * Set canvas background color
     */
    fun setCanvasBackgroundColor(color: Int) {
        backgroundColor = color
        invalidate()
    }
    
    /**
     * Get current active layer
     */
    private fun getActiveLayer(): Layer? {
        return layers.getOrNull(activeLayerIndex)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Fill view background (outside canvas area)
        canvas.drawColor(Color.rgb(45, 45, 45))
        
        // Save canvas state before applying transform
        canvas.save()
        
        // Apply canvas transformation
        canvas.concat(canvasTransform.getMatrix())
        
        // Draw shadow behind canvas
        canvas.drawRect(
            8f, 8f,
            canvasWidth + 8f, canvasHeight + 8f,
            shadowPaint
        )
        
        // Draw checkerboard pattern (for transparency)
        checkerBitmap?.let { checker ->
            val shader = android.graphics.BitmapShader(
                checker,
                android.graphics.Shader.TileMode.REPEAT,
                android.graphics.Shader.TileMode.REPEAT
            )
            checkerPaint.shader = shader
            canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), checkerPaint)
        }
        
        // Draw canvas background
        canvas.drawColor(backgroundColor)
        
        // Draw composite of all layers
        compositeBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }
        
        // Draw current stroke on top
        if (drawingEngine.isDrawing()) {
            drawingBitmap?.let { bitmap ->
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
        }
        
        // Draw canvas border
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), borderPaint)
        
        // Restore canvas state
        canvas.restore()
        
        // Draw UI overlays here (not transformed) - e.g., zoom percentage
        
        // Track FPS
        updateFps()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // First, let gesture detector handle multi-touch
        if (gestureDetector.onTouchEvent(event)) {
            // Gesture consumed the event
            return true
        }
        
        // Handle single-finger drawing
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Only start drawing if single touch
                if (event.pointerCount == 1) {
                    activePointerId = event.getPointerId(0)
                    handleTouchDown(event)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Only continue if we're tracking this pointer and it's single touch
                if (activePointerId != -1 && event.pointerCount == 1) {
                    handleTouchMove(event)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activePointerId != -1) {
                    handleTouchUp(event)
                    activePointerId = -1
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Second finger down - cancel current stroke if any
                if (drawingEngine.isDrawing()) {
                    drawingEngine.cancelStroke()
                    drawingBitmap?.let { bitmap ->
                        val c = Canvas(bitmap)
                        c.drawColor(0, PorterDuff.Mode.CLEAR)
                    }
                    invalidate()
                }
                activePointerId = -1
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun handleTouchDown(event: MotionEvent) {
        val activeLayer = getActiveLayer() ?: return
        if (activeLayer.isLocked) return
        
        // Clear drawing bitmap
        drawingBitmap?.let { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        }
        
        // Convert screen coordinates to canvas coordinates
        val screenPoint = PointF(event.x, event.y)
        val canvasPoint = canvasTransform.screenToCanvas(screenPoint.x, screenPoint.y)
        
        // Extract points and transform them
        val points = touchProcessor.extractPoints(event)
        if (points.isNotEmpty()) {
            val point = points.first()
            drawingEngine.beginStroke(
                canvasPoint.x,
                canvasPoint.y,
                point.pressure,
                point.tiltX,
                point.tiltY,
                point.orientation,
                currentBrush,
                currentColor,
                activeLayer.id
            )
            
            onStrokeBegin?.invoke()
        }
        
        invalidate()
    }
    
    private fun handleTouchMove(event: MotionEvent) {
        if (!drawingEngine.isDrawing()) return
        
        val activeLayer = getActiveLayer() ?: return
        if (activeLayer.isLocked) return
        
        // Extract all points with full tilt data (including historical for smoothness)
        val rawPoints = touchProcessor.extractPoints(event)
        
        // Transform each point from screen to canvas coordinates
        rawPoints.forEach { point ->
            val canvasPoint = canvasTransform.screenToCanvas(point.x, point.y)
            val transformedPoint = point.copy(x = canvasPoint.x, y = canvasPoint.y)
            drawingEngine.continueStroke(transformedPoint)
        }
        
        // Eraser needs to render directly to layer for real-time feedback
        // (PorterDuff.CLEAR only works when erasing actual content)
        if (currentBrush.type == BrushType.ERASER) {
            drawingEngine.renderCurrentStroke(activeLayer.bitmap)
            updateComposite()
        } else {
            // Regular brushes render to temporary drawing bitmap
            drawingBitmap?.let { bitmap ->
                drawingEngine.renderCurrentStroke(bitmap)
            }
        }
        
        onStrokeUpdate?.invoke()
        invalidate()
    }
    
    private fun handleTouchUp(event: MotionEvent) {
        if (!drawingEngine.isDrawing()) return
        
        val activeLayer = getActiveLayer() ?: return
        if (activeLayer.isLocked) return
        
        val isEraser = currentBrush.type == BrushType.ERASER
        
        // Process final points with coordinate transformation
        val rawPoints = touchProcessor.extractPoints(event)
        rawPoints.forEach { point ->
            val canvasPoint = canvasTransform.screenToCanvas(point.x, point.y)
            val transformedPoint = point.copy(x = canvasPoint.x, y = canvasPoint.y)
            drawingEngine.continueStroke(transformedPoint)
        }
        
        // Render final points for eraser (already rendering to layer)
        if (isEraser) {
            drawingEngine.renderCurrentStroke(activeLayer.bitmap)
        }
        
        // Finalize stroke
        val stroke = drawingEngine.endStroke()
        
        if (stroke != null) {
            // Only render to layer if not eraser (eraser already rendered in real-time)
            if (!isEraser) {
                drawingEngine.renderStroke(stroke, activeLayer.bitmap)
            }
            
            // Clear drawing bitmap
            drawingBitmap?.let { bitmap ->
                val canvas = Canvas(bitmap)
                canvas.drawColor(0, PorterDuff.Mode.CLEAR)
            }
            
            // Update composite
            updateComposite()
            
            onStrokeEnd?.invoke()
        }
        
        invalidate()
    }
    
    // CanvasGestureDetector.GestureListener implementation
    
    override fun onZoom(scaleFactor: Float, focusX: Float, focusY: Float) {
        canvasTransform.zoom(scaleFactor, focusX, focusY)
        onTransformChanged?.invoke(canvasTransform)
        invalidate()
    }
    
    override fun onPan(dx: Float, dy: Float) {
        canvasTransform.pan(dx, dy)
        onTransformChanged?.invoke(canvasTransform)
        invalidate()
    }
    
    override fun onRotate(degrees: Float, focusX: Float, focusY: Float) {
        canvasTransform.rotate(degrees, focusX, focusY)
        onTransformChanged?.invoke(canvasTransform)
        invalidate()
    }
    
    override fun onTwoFingerTap() {
        onTwoFingerTap?.invoke()
    }
    
    override fun onGestureBegin() {
        // Cancel any active drawing stroke when gesture begins
        if (drawingEngine.isDrawing()) {
            drawingEngine.cancelStroke()
            drawingBitmap?.let { bitmap ->
                val c = Canvas(bitmap)
                c.drawColor(0, PorterDuff.Mode.CLEAR)
            }
            invalidate()
        }
    }
    
    override fun onGestureEnd() {
        // Snap rotation to 0° if close
        val centerX = width / 2f
        val centerY = height / 2f
        canvasTransform.snapRotationIfNeeded(centerX, centerY)
        onTransformChanged?.invoke(canvasTransform)
        invalidate()
    }
    
    // Public transform controls
    
    /**
     * Reset canvas to fit in view
     */
    fun resetTransform() {
        canvasTransform.fitToViewport(
            canvasWidth.toFloat(),
            canvasHeight.toFloat(),
            width.toFloat(),
            height.toFloat()
        )
        onTransformChanged?.invoke(canvasTransform)
        invalidate()
    }
    
    /**
     * Reset rotation only
     */
    fun resetRotation() {
        val centerX = width / 2f
        val centerY = height / 2f
        canvasTransform.resetRotation(centerX, centerY)
        onTransformChanged?.invoke(canvasTransform)
        invalidate()
    }
    
    /**
     * Zoom to specific level (1.0 = 100%)
     */
    fun setZoom(scale: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        val currentScale = canvasTransform.scale
        canvasTransform.zoom(scale / currentScale, centerX, centerY)
        onTransformChanged?.invoke(canvasTransform)
        invalidate()
    }
    
    /**
     * Update the composite bitmap from all layers
     */
    private fun updateComposite() {
        compositeBitmap?.let { bitmap ->
            layerManager?.let { manager ->
                val composite = manager.composite(layers)
                val canvas = Canvas(bitmap)
                canvas.drawColor(0, PorterDuff.Mode.CLEAR)
                canvas.drawBitmap(composite, 0f, 0f, paint)
                composite.recycle()
            }
        }
    }
    
    /**
     * Force update and redraw
     */
    fun refresh() {
        updateComposite()
        invalidate()
    }
    
    /**
     * Get the current composite as a bitmap
     */
    fun getCompositeBitmap(): Bitmap? {
        return compositeBitmap?.copy(Bitmap.Config.ARGB_8888, false)
    }
    
    /**
     * Track FPS
     */
    private fun updateFps() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFpsTime
        
        if (elapsed >= 1000) {
            currentFps = (frameCount * 1000f) / elapsed
            frameCount = 0
            lastFpsTime = currentTime
        }
    }
    
    /**
     * Get current FPS
     */
    fun getFps(): Float = currentFps
    
    /**
     * Get current zoom percentage
     */
    fun getZoomPercent(): Int = canvasTransform.getZoomPercent()
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeBitmap?.recycle()
        drawingBitmap?.recycle()
        checkerBitmap?.recycle()
        compositeBitmap = null
        drawingBitmap = null
        checkerBitmap = null
    }
}
