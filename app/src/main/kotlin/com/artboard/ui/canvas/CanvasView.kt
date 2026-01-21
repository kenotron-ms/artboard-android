package com.artboard.ui.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.artboard.data.model.Brush
import com.artboard.data.model.Layer
import com.artboard.domain.engine.DrawingEngine
import com.artboard.domain.layer.LayerManager

/**
 * Custom view for drawing with low-latency touch handling
 */
class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Drawing state
    private var layers: List<Layer> = emptyList()
    private var activeLayerIndex: Int = 0
    private var backgroundColor: Int = Color.WHITE
    
    // Current drawing
    private var currentBrush: Brush = Brush.pen()
    private var currentColor: Int = Color.BLACK
    
    // Drawing engine
    private val drawingEngine = DrawingEngine()
    private val touchProcessor = TouchEventProcessor()
    private var layerManager: LayerManager? = null
    
    // Rendering
    private var compositeBitmap: Bitmap? = null
    private var drawingBitmap: Bitmap? = null
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    // Callbacks
    var onStrokeBegin: (() -> Unit)? = null
    var onStrokeEnd: (() -> Unit)? = null
    var onStrokeUpdate: (() -> Unit)? = null
    
    // Performance tracking
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0f
    
    init {
        // Enable hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * Initialize with canvas dimensions
     */
    fun initialize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        
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
        
        // Draw background
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
        
        // Track FPS
        updateFps()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(event)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handleTouchUp(event)
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
        
        // Start new stroke
        val points = touchProcessor.extractPoints(event)
        if (points.isNotEmpty()) {
            val point = points.first()
            drawingEngine.beginStroke(
                point.x,
                point.y,
                touchProcessor.normalizePressure(point.pressure),
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
        
        // Extract all points (including historical for smoothness)
        val points = touchProcessor.extractPoints(event)
        
        points.forEach { point ->
            drawingEngine.continueStroke(
                point.x,
                point.y,
                touchProcessor.normalizePressure(point.pressure)
            )
        }
        
        // Render incrementally to drawing bitmap
        drawingBitmap?.let { bitmap ->
            drawingEngine.renderCurrentStroke(bitmap)
        }
        
        onStrokeUpdate?.invoke()
        invalidate()
    }
    
    private fun handleTouchUp(event: MotionEvent) {
        if (!drawingEngine.isDrawing()) return
        
        val activeLayer = getActiveLayer() ?: return
        if (activeLayer.isLocked) return
        
        // Process final points
        val points = touchProcessor.extractPoints(event)
        points.forEach { point ->
            drawingEngine.continueStroke(
                point.x,
                point.y,
                touchProcessor.normalizePressure(point.pressure)
            )
        }
        
        // Finalize stroke
        val stroke = drawingEngine.endStroke()
        
        if (stroke != null) {
            // Render final stroke to active layer
            drawingEngine.renderStroke(stroke, activeLayer.bitmap)
            
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
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeBitmap?.recycle()
        drawingBitmap?.recycle()
        compositeBitmap = null
        drawingBitmap = null
    }
}
