package com.artboard.ui.selection.tools

import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import com.artboard.data.model.SelectionMask

/**
 * Lasso selection tool - freehand path drawing
 * User draws a path, and when released, the path is closed and converted to a selection
 */
class LassoTool {
    private val path = Path()
    private val pathPoints = mutableListOf<Offset>()
    private var isDrawing = false
    
    /**
     * Start drawing lasso path
     */
    fun onTouchDown(x: Float, y: Float) {
        path.reset()
        pathPoints.clear()
        
        path.moveTo(x, y)
        pathPoints.add(Offset(x, y))
        isDrawing = true
    }
    
    /**
     * Continue drawing lasso path
     */
    fun onTouchMove(x: Float, y: Float) {
        if (!isDrawing) return
        
        // Only add point if it's far enough from last point (smoothing)
        if (pathPoints.isNotEmpty()) {
            val lastPoint = pathPoints.last()
            val distance = kotlin.math.sqrt(
                (x - lastPoint.x) * (x - lastPoint.x) + 
                (y - lastPoint.y) * (y - lastPoint.y)
            )
            
            // Minimum distance threshold for smoothing (5dp)
            if (distance < 5f) {
                return
            }
        }
        
        path.lineTo(x, y)
        pathPoints.add(Offset(x, y))
    }
    
    /**
     * Finish drawing and create selection mask
     */
    fun onTouchUp(x: Float, y: Float, width: Int, height: Int, featherRadius: Float = 0f): SelectionMask {
        if (isDrawing) {
            // Close the path
            if (pathPoints.isNotEmpty()) {
                val firstPoint = pathPoints.first()
                path.lineTo(firstPoint.x, firstPoint.y)
            }
            path.close()
            isDrawing = false
        }
        
        // Create selection mask from path
        val mask = SelectionMask(width, height)
        mask.setFromPath(path, featherRadius)
        
        return mask
    }
    
    /**
     * Get current path for preview rendering
     */
    fun getCurrentPath(): Path = path
    
    /**
     * Get path points for preview
     */
    fun getPathPoints(): List<Offset> = pathPoints
    
    /**
     * Cancel current lasso operation
     */
    fun cancel() {
        path.reset()
        pathPoints.clear()
        isDrawing = false
    }
    
    /**
     * Check if currently drawing
     */
    fun isDrawing(): Boolean = isDrawing
    
    /**
     * Get approximate bounds of the lasso path
     */
    fun getBounds(): RectF {
        val bounds = RectF()
        path.computeBounds(bounds, true)
        return bounds
    }
}
