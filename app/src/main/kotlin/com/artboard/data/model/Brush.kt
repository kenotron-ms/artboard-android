package com.artboard.data.model

import kotlinx.serialization.Serializable

enum class BrushType {
    PENCIL,
    PEN,
    AIRBRUSH,
    ERASER,
    MARKER
}

/**
 * Brush configuration for stroke rendering
 */
@Serializable
data class Brush(
    val size: Float = 10f,              // 1-500px
    val opacity: Float = 1f,            // 0.0-1.0
    val hardness: Float = 0.8f,         // 0.0-1.0 (edge softness)
    val flow: Float = 1f,               // 0.0-1.0 (paint buildup)
    val spacing: Float = 0.1f,          // 0.01-1.0 (stamp spacing as % of size)
    val pressureSizeEnabled: Boolean = true,
    val pressureOpacityEnabled: Boolean = true,
    val type: BrushType = BrushType.PEN,
    val minPressureSize: Float = 0.2f,  // Min size multiplier at low pressure
    val minPressureOpacity: Float = 0.3f // Min opacity multiplier at low pressure
) {
    /**
     * Calculate actual brush size for given pressure
     */
    fun getEffectiveSize(pressure: Float): Float {
        if (!pressureSizeEnabled) return size
        val pressureFactor = minPressureSize + (1f - minPressureSize) * pressure
        return size * pressureFactor
    }
    
    /**
     * Calculate actual opacity for given pressure
     */
    fun getEffectiveOpacity(pressure: Float): Float {
        val baseOpacity = if (pressureOpacityEnabled) {
            val pressureFactor = minPressureOpacity + (1f - minPressureOpacity) * pressure
            opacity * pressureFactor
        } else {
            opacity
        }
        return baseOpacity.coerceIn(0f, 1f)
    }
    
    /**
     * Calculate spacing distance in pixels
     */
    fun getSpacingDistance(): Float {
        return size * spacing
    }
    
    companion object {
        /**
         * Create a pencil brush preset
         */
        fun pencil() = Brush(
            size = 2f,
            opacity = 0.7f,
            hardness = 0.3f,
            flow = 0.6f,
            spacing = 0.05f,
            type = BrushType.PENCIL,
            pressureSizeEnabled = true,
            pressureOpacityEnabled = true
        )
        
        /**
         * Create a pen brush preset
         */
        fun pen() = Brush(
            size = 3f,
            opacity = 1f,
            hardness = 0.9f,
            flow = 1f,
            spacing = 0.08f,
            type = BrushType.PEN,
            pressureSizeEnabled = true,
            pressureOpacityEnabled = false
        )
        
        /**
         * Create an airbrush preset
         */
        fun airbrush() = Brush(
            size = 50f,
            opacity = 0.3f,
            hardness = 0.1f,
            flow = 0.4f,
            spacing = 0.02f,
            type = BrushType.AIRBRUSH,
            pressureSizeEnabled = true,
            pressureOpacityEnabled = true
        )
        
        /**
         * Create a marker brush preset
         */
        fun marker() = Brush(
            size = 20f,
            opacity = 0.6f,
            hardness = 0.5f,
            flow = 0.8f,
            spacing = 0.1f,
            type = BrushType.MARKER,
            pressureSizeEnabled = false,
            pressureOpacityEnabled = false
        )
        
        /**
         * Create an eraser preset
         */
        fun eraser() = Brush(
            size = 30f,
            opacity = 1f,
            hardness = 0.8f,
            flow = 1f,
            spacing = 0.1f,
            type = BrushType.ERASER,
            pressureSizeEnabled = true,
            pressureOpacityEnabled = false
        )
    }
}
