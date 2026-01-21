package com.artboard.data.model

import android.graphics.Bitmap
import java.util.UUID

enum class BlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    ADD
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
    val isLocked: Boolean = false
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
    
    companion object {
        /**
         * Create a new blank layer
         */
        fun create(width: Int, height: Int, name: String = "Layer"): Layer {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            return Layer(
                name = name,
                bitmap = bitmap
            )
        }
    }
}
