package com.artboard.ui.selection.tools

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import com.artboard.data.model.SelectionMask
import kotlin.math.max
import kotlin.math.min

/**
 * Rectangle selection tool
 * User drags from start point to end point to create a rectangular selection
 */
class RectangleTool {
    private var startPoint: Offset? = null
    private var currentPoint: Offset? = null
    private var isDrawing = false
    
    /**
     * Start rectangle selection
     */
    fun onTouchDown(x: Float, y: Float) {
        startPoint = Offset(x, y)
        currentPoint = Offset(x, y)
        isDrawing = true
    }
    
    /**
     * Update rectangle as user drags
     */
    fun onTouchMove(x: Float, y: Float) {
        if (!isDrawing) return
        currentPoint = Offset(x, y)
    }
    
    /**
     * Finish rectangle and create selection mask
     */
    fun onTouchUp(x: Float, y: Float, width: Int, height: Int, featherRadius: Float = 0f): SelectionMask {
        currentPoint = Offset(x, y)
        isDrawing = false
        
        val rect = getCurrentRect()
        
        // Create selection mask from rectangle
        val mask = SelectionMask(width, height)
        mask.setFromRect(rect, featherRadius)
        
        return mask
    }
    
    /**
     * Get current rectangle for preview
     */
    fun getCurrentRect(): RectF {
        val start = startPoint ?: return RectF(0f, 0f, 0f, 0f)
        val current = currentPoint ?: start
        
        val left = min(start.x, current.x)
        val top = min(start.y, current.y)
        val right = max(start.x, current.x)
        val bottom = max(start.y, current.y)
        
        return RectF(left, top, right, bottom)
    }
    
    /**
     * Cancel current rectangle operation
     */
    fun cancel() {
        startPoint = null
        currentPoint = null
        isDrawing = false
    }
    
    /**
     * Check if currently drawing
     */
    fun isDrawing(): Boolean = isDrawing
    
    /**
     * Get start point
     */
    fun getStartPoint(): Offset? = startPoint
    
    /**
     * Get current point
     */
    fun getCurrentPoint(): Offset? = currentPoint
}
