package com.artboard.data.model

import android.graphics.Bitmap
import java.util.UUID

enum class BlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    ADD,
    DARKEN,
    LIGHTEN,
    COLOR_DODGE,
    COLOR_BURN,
    SOFT_LIGHT,
    HARD_LIGHT,
    DIFFERENCE,
    EXCLUSION,
    HUE,
    SATURATION,
    COLOR,
    LUMINOSITY,
    XOR;
    
    fun displayName(): String = when (this) {
        NORMAL -> "Normal"
        MULTIPLY -> "Multiply"
        SCREEN -> "Screen"
        OVERLAY -> "Overlay"
        ADD -> "Add"
        DARKEN -> "Darken"
        LIGHTEN -> "Lighten"
        COLOR_DODGE -> "Color Dodge"
        COLOR_BURN -> "Color Burn"
        SOFT_LIGHT -> "Soft Light"
        HARD_LIGHT -> "Hard Light"
        DIFFERENCE -> "Difference"
        EXCLUSION -> "Exclusion"
        HUE -> "Hue"
        SATURATION -> "Saturation"
        COLOR -> "Color"
        LUMINOSITY -> "Luminosity"
        XOR -> "XOR"
    }
}

/**
 * Represents a single drawing layer
 */
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var bitmap: Bitmap,
    val opacity: Float = 1f,           // 0.0-1.0
    val blendMode: BlendMode = BlendMode.NORMAL,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val position: Int = 0,             // Layer stack position (0 = bottom)
    var thumbnail: Bitmap? = null      // 56×56dp preview (generated on demand)
) {
    /**
     * Create a copy with new opacity
     */
    fun withOpacity(newOpacity: Float): Layer {
        return copy(opacity = newOpacity.coerceIn(0f, 1f))
    }
    
    /**
     * Create a copy with visibility toggled
     */
    fun toggleVisibility(): Layer {
        return copy(isVisible = !isVisible)
    }
    
    /**
     * Create a copy with lock toggled
     */
    fun toggleLock(): Layer {
        return copy(isLocked = !isLocked)
    }
    
    /**
     * Create a copy with new blend mode
     */
    fun withBlendMode(newBlendMode: BlendMode): Layer {
        return copy(blendMode = newBlendMode)
    }
    
    /**
     * Create a copy with new name
     */
    fun withName(newName: String): Layer {
        return copy(name = newName)
    }
    
    /**
     * Create a copy with new position
     */
    fun withPosition(newPosition: Int): Layer {
        return copy(position = newPosition)
    }
    
    /**
     * Create a copy with new thumbnail
     */
    fun withThumbnail(newThumbnail: Bitmap): Layer {
        return copy(thumbnail = newThumbnail)
    }
    
    companion object {
        /**
         * Create a new blank layer
         */
        fun create(width: Int, height: Int, name: String = "Layer", position: Int = 0): Layer {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            return Layer(
                name = name,
                bitmap = bitmap,
                position = position
            )
        }
    }
}
