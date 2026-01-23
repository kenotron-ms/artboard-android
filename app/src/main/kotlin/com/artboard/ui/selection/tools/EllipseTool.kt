package com.artboard.ui.selection.tools

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import com.artboard.data.model.SelectionMask
import kotlin.math.max
import kotlin.math.min

/**
 * Ellipse selection tool
 * User drags from start point to end point to create an elliptical selection
 * The ellipse is inscribed in the rectangle defined by start and end points
 */
class EllipseTool {
    private var startPoint: Offset? = null
    private var currentPoint: Offset? = null
    private var isDrawing = false
    
    /**
     * Start ellipse selection
     */
    fun onTouchDown(x: Float, y: Float) {
        startPoint = Offset(x, y)
        currentPoint = Offset(x, y)
        isDrawing = true
    }
    
    /**
     * Update ellipse as user drags
     */
    fun onTouchMove(x: Float, y: Float) {
        if (!isDrawing) return
        currentPoint = Offset(x, y)
    }
    
    /**
     * Finish ellipse and create selection mask
     */
    fun onTouchUp(x: Float, y: Float, width: Int, height: Int, featherRadius: Float = 0f): SelectionMask {
        currentPoint = Offset(x, y)
        isDrawing = false
        
        val rect = getCurrentRect()
        
        // Create selection mask from ellipse
        val mask = SelectionMask(width, height)
        mask.setFromEllipse(rect, featherRadius)
        
        return mask
    }
    
    /**
     * Get current bounding rectangle for ellipse preview
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
     * Cancel current ellipse operation
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
    
    /**
     * Get center point of the ellipse
     */
    fun getCenterPoint(): Offset? {
        val start = startPoint ?: return null
        val current = currentPoint ?: start
        
        return Offset(
            (start.x + current.x) / 2f,
            (start.y + current.y) / 2f
        )
    }
    
    /**
     * Get radius (width and height) of the ellipse
     */
    fun getRadius(): Pair<Float, Float> {
        val start = startPoint ?: return Pair(0f, 0f)
        val current = currentPoint ?: start
        
        val radiusX = kotlin.math.abs(current.x - start.x) / 2f
        val radiusY = kotlin.math.abs(current.y - start.y) / 2f
        
        return Pair(radiusX, radiusY)
    }
}
