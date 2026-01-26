package com.artboard.ui.canvas

import android.graphics.Matrix
import android.graphics.PointF

/**
 * Manages canvas transformation state (zoom, pan, rotate) for infinite canvas feel.
 * All transformations are accumulated in a matrix for efficient rendering.
 */
class CanvasTransform {
    
    var scale: Float = 1f
        private set
    var translateX: Float = 0f
        private set
    var translateY: Float = 0f
        private set
    var rotation: Float = 0f
        private set
    
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    private var inverseMatrixDirty = true
    
    // Limits
    companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 20f
        const val ROTATION_SNAP_THRESHOLD = 5f // Snap to 0° when within this range
    }
    
    /**
     * Apply zoom around a pivot point (typically pinch center)
     */
    fun zoom(scaleFactor: Float, pivotX: Float, pivotY: Float) {
        val newScale = (scale * scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
        val actualFactor = newScale / scale
        
        if (actualFactor == 1f) return
        
        scale = newScale
        
        // Zoom around pivot point - adjust translation so pivot stays fixed
        translateX = pivotX - (pivotX - translateX) * actualFactor
        translateY = pivotY - (pivotY - translateY) * actualFactor
        
        updateMatrix()
    }
    
    /**
     * Pan the canvas by delta amounts
     */
    fun pan(dx: Float, dy: Float) {
        translateX += dx
        translateY += dy
        updateMatrix()
    }
    
    /**
     * Rotate around a pivot point (typically gesture center)
     */
    fun rotate(degrees: Float, pivotX: Float, pivotY: Float) {
        rotation = (rotation + degrees) % 360f
        
        // Normalize to -180 to 180
        if (rotation > 180f) rotation -= 360f
        if (rotation < -180f) rotation += 360f
        
        // The pivot adjustment is handled in the matrix composition
        // We need to recalculate translation to keep the pivot point fixed
        val cos = kotlin.math.cos(Math.toRadians(degrees.toDouble())).toFloat()
        val sin = kotlin.math.sin(Math.toRadians(degrees.toDouble())).toFloat()
        
        val dx = translateX - pivotX
        val dy = translateY - pivotY
        
        translateX = pivotX + dx * cos - dy * sin
        translateY = pivotY + dx * sin + dy * cos
        
        updateMatrix()
    }
    
    /**
     * Reset all transformations to default
     */
    fun reset() {
        scale = 1f
        translateX = 0f
        translateY = 0f
        rotation = 0f
        updateMatrix()
    }
    
    /**
     * Reset only rotation (snap back to upright)
     */
    fun resetRotation(pivotX: Float, pivotY: Float) {
        if (rotation != 0f) {
            rotate(-rotation, pivotX, pivotY)
        }
    }
    
    /**
     * Snap rotation to 0° if close enough
     */
    fun snapRotationIfNeeded(pivotX: Float, pivotY: Float) {
        if (kotlin.math.abs(rotation) < ROTATION_SNAP_THRESHOLD) {
            resetRotation(pivotX, pivotY)
        }
    }
    
    /**
     * Fit canvas to show entire content within viewport
     */
    fun fitToViewport(
        canvasWidth: Float,
        canvasHeight: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        padding: Float = 50f
    ) {
        val availableWidth = viewportWidth - padding * 2
        val availableHeight = viewportHeight - padding * 2
        
        val scaleX = availableWidth / canvasWidth
        val scaleY = availableHeight / canvasHeight
        
        scale = minOf(scaleX, scaleY, 1f) // Don't zoom in beyond 100%
        rotation = 0f
        
        // Center the canvas
        translateX = (viewportWidth - canvasWidth * scale) / 2f
        translateY = (viewportHeight - canvasHeight * scale) / 2f
        
        updateMatrix()
    }
    
    /**
     * Center canvas in viewport at current scale
     */
    fun centerInViewport(
        canvasWidth: Float,
        canvasHeight: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ) {
        translateX = (viewportWidth - canvasWidth * scale) / 2f
        translateY = (viewportHeight - canvasHeight * scale) / 2f
        updateMatrix()
    }
    
    private fun updateMatrix() {
        matrix.reset()
        
        // Order matters: translate -> scale -> rotate (applied in reverse)
        matrix.postTranslate(translateX, translateY)
        matrix.postScale(scale, scale, translateX, translateY)
        matrix.postRotate(rotation, translateX + scale * 0.5f, translateY + scale * 0.5f)
        
        // Actually, let's use a cleaner approach with proper pivot handling
        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postRotate(rotation)
        matrix.postTranslate(translateX, translateY)
        
        inverseMatrixDirty = true
    }
    
    private fun ensureInverseMatrix() {
        if (inverseMatrixDirty) {
            matrix.invert(inverseMatrix)
            inverseMatrixDirty = false
        }
    }
    
    /**
     * Convert screen coordinates to canvas coordinates
     * Use this to map touch events to actual drawing positions
     */
    fun screenToCanvas(screenX: Float, screenY: Float): PointF {
        ensureInverseMatrix()
        val point = floatArrayOf(screenX, screenY)
        inverseMatrix.mapPoints(point)
        return PointF(point[0], point[1])
    }
    
    /**
     * Convert canvas coordinates to screen coordinates
     */
    fun canvasToScreen(canvasX: Float, canvasY: Float): PointF {
        val point = floatArrayOf(canvasX, canvasY)
        matrix.mapPoints(point)
        return PointF(point[0], point[1])
    }
    
    /**
     * Get the current transformation matrix for rendering
     */
    fun getMatrix(): Matrix = Matrix(matrix)
    
    /**
     * Check if canvas is at default state (no transform)
     */
    fun isDefault(): Boolean {
        return scale == 1f && translateX == 0f && translateY == 0f && rotation == 0f
    }
    
    /**
     * Get current zoom level as percentage
     */
    fun getZoomPercent(): Int = (scale * 100).toInt()
}
