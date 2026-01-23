package com.artboard.ui.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Utility functions for color conversions and operations.
 * Provides HSB ↔ RGB conversion, hex parsing, and color formatting.
 */
object ColorUtils {
    /**
     * Convert RGB Color to HSB array
     * @return [hue (0-360), saturation (0-1), brightness (0-1)]
     */
    fun rgbToHsb(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        
        // Hue calculation (0-360 degrees)
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0) it + 360f else it }
        
        // Saturation (0-1)
        val saturation = if (max == 0f) 0f else delta / max
        
        // Brightness (0-1)
        val brightness = max
        
        return floatArrayOf(hue, saturation, brightness)
    }
    
    /**
     * Convert HSB to RGB Color
     * @param hue Hue in degrees (0-360)
     * @param saturation Saturation (0-1)
     * @param brightness Brightness (0-1)
     * @param alpha Alpha (0-1)
     * @return Color
     */
    fun hsbToColor(
        hue: Float,
        saturation: Float,
        brightness: Float,
        alpha: Float = 1f
    ): Color {
        return Color.hsv(hue, saturation, brightness, alpha)
    }
    
    /**
     * Parse hex color string (#RRGGBB or #AARRGGBB)
     * @param hex Hex color string with or without # prefix
     * @return Color or null if invalid
     */
    fun parseHex(hex: String): Color? {
        return try {
            val cleanHex = hex.removePrefix("#")
            val argb = when (cleanHex.length) {
                6 -> "FF$cleanHex" // Add full alpha
                8 -> cleanHex
                else -> return null
            }
            Color(android.graphics.Color.parseColor("#$argb"))
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convert Color to hex string
     * @param color Color to convert
     * @param includeAlpha If true, returns #AARRGGBB, else #RRGGBB
     * @return Hex string with # prefix
     */
    fun toHex(color: Color, includeAlpha: Boolean = false): String {
        val argb = color.toArgb()
        return if (includeAlpha) {
            "#${Integer.toHexString(argb).uppercase()}"
        } else {
            // Extract RGB only (skip alpha)
            val rgb = argb and 0x00FFFFFF
            "#${Integer.toHexString(rgb).uppercase().padStart(6, '0')}"
        }
    }
    
    /**
     * Get RGB components from Color
     * @return Triple of (red 0-255, green 0-255, blue 0-255)
     */
    fun getRgbComponents(color: Color): Triple<Int, Int, Int> {
        return Triple(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }
    
    /**
     * Create Color from RGB components (0-255 range)
     */
    fun fromRgb(red: Int, green: Int, blue: Int, alpha: Int = 255): Color {
        return Color(
            red = red / 255f,
            green = green / 255f,
            blue = blue / 255f,
            alpha = alpha / 255f
        )
    }
}
