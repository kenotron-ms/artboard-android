package com.artboard.data.model

import androidx.compose.ui.geometry.Offset

/**
 * Transform modes for different transformation behaviors
 */
enum class TransformType {
    FREE,       // Move + scale + rotate freely
    UNIFORM,    // Scale maintains aspect ratio
    DISTORT,    // Free corner manipulation (perspective)
    WARP        // Mesh-based warping (advanced - Phase 4)
}

/**
 * Represents a 2D transformation (translate, scale, rotate)
 */
data class Transform(
    val translation: Offset = Offset.Zero,
    val scale: Float = 1f,              // Uniform scale
    val scaleX: Float = 1f,             // Non-uniform X scale
    val scaleY: Float = 1f,             // Non-uniform Y scale
    val rotation: Float = 0f,           // Degrees (0-360)
    val pivotX: Float = 0.5f,           // 0-1 normalized (0.5 = center)
    val pivotY: Float = 0.5f            // 0-1 normalized (0.5 = center)
) {
    /**
     * Check if transform is identity (no changes)
     */
    fun isIdentity(): Boolean {
        return translation == Offset.Zero &&
                scale == 1f &&
                scaleX == 1f &&
                scaleY == 1f &&
                rotation == 0f
    }
    
    /**
     * Create a copy with translation offset
     */
    fun withTranslation(delta: Offset): Transform {
        return copy(translation = translation + delta)
    }
    
    /**
     * Create a copy with uniform scale
     */
    fun withUniformScale(newScale: Float, pivot: Offset = Offset(0.5f, 0.5f)): Transform {
        return copy(
            scale = newScale.coerceIn(0.1f, 10f),
            pivotX = pivot.x,
            pivotY = pivot.y
        )
    }
    
    /**
     * Create a copy with non-uniform scale
     */
    fun withFreeScale(newScaleX: Float, newScaleY: Float): Transform {
        return copy(
            scaleX = newScaleX.coerceIn(0.1f, 10f),
            scaleY = newScaleY.coerceIn(0.1f, 10f)
        )
    }
    
    /**
     * Create a copy with rotation
     */
    fun withRotation(newRotation: Float): Transform {
        return copy(rotation = newRotation % 360f)
    }
    
    /**
     * Apply angle snapping (15°, 45°, 90°)
     */
    fun withSnappedRotation(threshold: Float = 5f): Transform {
        val snapAngles = listOf(0f, 15f, 30f, 45f, 60f, 75f, 90f, 105f, 120f, 135f, 
                                150f, 165f, 180f, 195f, 210f, 225f, 240f, 255f, 
                                270f, 285f, 300f, 315f, 330f, 345f)
        
        val normalizedRotation = rotation % 360f
        val snapped = snapAngles.minByOrNull { kotlin.math.abs(normalizedRotation - it) }
        
        return if (snapped != null && kotlin.math.abs(normalizedRotation - snapped) < threshold) {
            copy(rotation = snapped)
        } else {
            this
        }
    }
    
    companion object {
        /**
         * Create identity transform (no changes)
         */
        fun identity(): Transform = Transform()
        
        /**
         * Snap angles for rotation (every 15°)
         */
        val SNAP_ANGLES = listOf(0f, 15f, 30f, 45f, 60f, 75f, 90f, 105f, 120f, 135f,
                                 150f, 165f, 180f, 195f, 210f, 225f, 240f, 255f,
                                 270f, 285f, 300f, 315f, 330f, 345f)
        
        /**
         * Strong snap angles (45° increments)
         */
        val STRONG_SNAP_ANGLES = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
    }
}

/**
 * Handle positions for transform bounds
 */
enum class Handle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    NONE
}
