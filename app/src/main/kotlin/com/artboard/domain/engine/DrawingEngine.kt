package com.artboard.domain.engine

import android.graphics.Bitmap
import com.artboard.data.model.Brush
import com.artboard.data.model.Point
import com.artboard.data.model.Stroke
import com.artboard.domain.brush.BrushEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Core drawing engine that manages stroke creation and rendering
 */
class DrawingEngine(
    private val brushEngine: BrushEngine = BrushEngine()
) {
    private var currentStroke: MutableStroke? = null
    private var lastRenderedPointCount = 0
    
    /**
     * Begin a new stroke
     */
    fun beginStroke(
        x: Float,
        y: Float,
        pressure: Float,
        brush: Brush,
        color: Int,
        layerId: String
    ) {
        currentStroke = MutableStroke(
            id = UUID.randomUUID().toString(),
            brush = brush,
            color = color,
            layerId = layerId
        )
        currentStroke?.addPoint(Point(x, y, pressure))
        lastRenderedPointCount = 0
    }
    
    /**
     * Add a point to the current stroke
     */
    fun continueStroke(x: Float, y: Float, pressure: Float) {
        currentStroke?.addPoint(Point(x, y, pressure))
    }
    
    /**
     * Finish the current stroke and return it
     */
    fun endStroke(): Stroke? {
        val stroke = currentStroke?.toStroke()
        currentStroke = null
        lastRenderedPointCount = 0
        return stroke
    }
    
    /**
     * Get the current stroke being drawn
     */
    fun getCurrentStroke(): Stroke? {
        return currentStroke?.toStroke()
    }
    
    /**
     * Render the current stroke incrementally to a bitmap
     * Only renders points added since last render
     */
    fun renderCurrentStroke(targetBitmap: Bitmap): Boolean {
        val stroke = currentStroke?.toStroke() ?: return false
        
        if (stroke.points.size <= lastRenderedPointCount) {
            return false
        }
        
        lastRenderedPointCount = brushEngine.renderStrokeIncremental(
            stroke,
            targetBitmap,
            lastRenderedPointCount
        )
        
        return true
    }
    
    /**
     * Render a complete stroke to a bitmap
     */
    fun renderStroke(stroke: Stroke, targetBitmap: Bitmap) {
        brushEngine.renderStroke(stroke, targetBitmap)
    }
    
    /**
     * Render a stroke asynchronously
     */
    fun renderStrokeAsync(
        stroke: Stroke,
        targetBitmap: Bitmap,
        scope: CoroutineScope,
        onComplete: () -> Unit = {}
    ) {
        scope.launch(Dispatchers.Default) {
            brushEngine.renderStroke(stroke, targetBitmap)
            onComplete()
        }
    }
    
    /**
     * Cancel the current stroke
     */
    fun cancelStroke() {
        currentStroke = null
        lastRenderedPointCount = 0
    }
    
    /**
     * Check if a stroke is currently being drawn
     */
    fun isDrawing(): Boolean {
        return currentStroke != null
    }
}

/**
 * Internal mutable stroke representation for building strokes
 */
private class MutableStroke(
    val id: String,
    val brush: Brush,
    val color: Int,
    val layerId: String
) {
    private val points = mutableListOf<Point>()
    
    fun addPoint(point: Point) {
        // Simple noise reduction: skip points too close to the last one
        if (points.isNotEmpty()) {
            val lastPoint = points.last()
            val distance = point.distanceTo(lastPoint)
            
            // Skip if too close (less than 0.5 pixels)
            if (distance < 0.5f) {
                return
            }
        }
        
        points.add(point)
    }
    
    fun toStroke(): Stroke {
        return Stroke(
            id = id,
            points = points.toList(),
            brush = brush,
            color = color,
            layerId = layerId
        )
    }
}
