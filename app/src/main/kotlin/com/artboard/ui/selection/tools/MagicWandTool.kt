package com.artboard.ui.selection.tools

import android.graphics.Bitmap
import com.artboard.data.model.SelectionMask

/**
 * Magic Wand selection tool
 * Selects contiguous pixels with similar colors based on tolerance threshold
 * Uses flood fill algorithm from SelectionMask
 */
class MagicWandTool(
    private var tolerance: Int = 32  // Default tolerance (0-255)
) {
    /**
     * Create selection from a tap point on the source bitmap
     */
    fun onTap(
        x: Float,
        y: Float,
        sourceBitmap: Bitmap,
        featherRadius: Float = 0f
    ): SelectionMask {
        val mask = SelectionMask(sourceBitmap.width, sourceBitmap.height)
        
        // Convert float coordinates to int
        val seedX = x.toInt().coerceIn(0, sourceBitmap.width - 1)
        val seedY = y.toInt().coerceIn(0, sourceBitmap.height - 1)
        
        // Use flood fill to select similar colors
        mask.setFromColor(sourceBitmap, seedX, seedY, tolerance)
        
        // Apply feathering if requested
        if (featherRadius > 0f) {
            // Feathering is already applied in setFromColor, but we can apply additional
            // This is a bit redundant, but kept for API consistency
        }
        
        return mask
    }
    
    /**
     * Set tolerance for color matching (0-255)
     * Higher tolerance = more colors selected
     */
    fun setTolerance(newTolerance: Int) {
        tolerance = newTolerance.coerceIn(0, 255)
    }
    
    /**
     * Get current tolerance
     */
    fun getTolerance(): Int = tolerance
    
    /**
     * Create selection with custom tolerance (one-time use)
     */
    fun onTapWithTolerance(
        x: Float,
        y: Float,
        sourceBitmap: Bitmap,
        customTolerance: Int,
        featherRadius: Float = 0f
    ): SelectionMask {
        val mask = SelectionMask(sourceBitmap.width, sourceBitmap.height)
        
        val seedX = x.toInt().coerceIn(0, sourceBitmap.width - 1)
        val seedY = y.toInt().coerceIn(0, sourceBitmap.height - 1)
        
        mask.setFromColor(sourceBitmap, seedX, seedY, customTolerance.coerceIn(0, 255))
        
        return mask
    }
    
    /**
     * Check if coordinates are valid for the bitmap
     */
    fun isValidCoordinate(x: Float, y: Float, bitmap: Bitmap): Boolean {
        val ix = x.toInt()
        val iy = y.toInt()
        return ix in 0 until bitmap.width && iy in 0 until bitmap.height
    }
}
