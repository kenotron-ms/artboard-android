package com.artboard.ui.color

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ColorUtils
 * Tests HSB↔RGB conversion, hex parsing, and color formatting
 */
class ColorUtilsTest {
    
    @Test
    fun `rgbToHsb converts red correctly`() {
        val red = Color.Red
        val hsb = ColorUtils.rgbToHsb(red)
        
        assertEquals(0f, hsb[0], 1f) // Hue = 0° (red)
        assertEquals(1f, hsb[1], 0.01f) // Full saturation
        assertEquals(1f, hsb[2], 0.01f) // Full brightness
    }
    
    @Test
    fun `rgbToHsb converts green correctly`() {
        val green = Color.Green
        val hsb = ColorUtils.rgbToHsb(green)
        
        assertEquals(120f, hsb[0], 1f) // Hue = 120° (green)
        assertEquals(1f, hsb[1], 0.01f) // Full saturation
        assertEquals(1f, hsb[2], 0.01f) // Full brightness
    }
    
    @Test
    fun `rgbToHsb converts blue correctly`() {
        val blue = Color.Blue
        val hsb = ColorUtils.rgbToHsb(blue)
        
        assertEquals(240f, hsb[0], 1f) // Hue = 240° (blue)
        assertEquals(1f, hsb[1], 0.01f) // Full saturation
        assertEquals(1f, hsb[2], 0.01f) // Full brightness
    }
    
    @Test
    fun `rgbToHsb converts white correctly`() {
        val white = Color.White
        val hsb = ColorUtils.rgbToHsb(white)
        
        // Hue is undefined for white (can be any value)
        assertEquals(0f, hsb[1], 0.01f) // Zero saturation
        assertEquals(1f, hsb[2], 0.01f) // Full brightness
    }
    
    @Test
    fun `rgbToHsb converts black correctly`() {
        val black = Color.Black
        val hsb = ColorUtils.rgbToHsb(black)
        
        // Hue is undefined for black
        assertEquals(0f, hsb[1], 0.01f) // Zero saturation
        assertEquals(0f, hsb[2], 0.01f) // Zero brightness
    }
    
    @Test
    fun `hsbToColor creates correct color`() {
        // Test red
        val red = ColorUtils.hsbToColor(0f, 1f, 1f)
        assertEquals(1f, red.red, 0.01f)
        assertEquals(0f, red.green, 0.01f)
        assertEquals(0f, red.blue, 0.01f)
        
        // Test green
        val green = ColorUtils.hsbToColor(120f, 1f, 1f)
        assertEquals(0f, green.red, 0.01f)
        assertEquals(1f, green.green, 0.01f)
        assertEquals(0f, green.blue, 0.01f)
        
        // Test blue
        val blue = ColorUtils.hsbToColor(240f, 1f, 1f)
        assertEquals(0f, blue.red, 0.01f)
        assertEquals(0f, blue.green, 0.01f)
        assertEquals(1f, blue.blue, 0.01f)
    }
    
    @Test
    fun `hsbToColor handles alpha correctly`() {
        val color = ColorUtils.hsbToColor(0f, 1f, 1f, 0.5f)
        assertEquals(0.5f, color.alpha, 0.01f)
    }
    
    @Test
    fun `parseHex handles 6 character format`() {
        val color = ColorUtils.parseHex("#FF0000")
        assertNotNull(color)
        assertEquals(Color.Red, color)
    }
    
    @Test
    fun `parseHex handles format without hash`() {
        val color = ColorUtils.parseHex("00FF00")
        assertNotNull(color)
        assertEquals(Color.Green, color)
    }
    
    @Test
    fun `parseHex handles 8 character format with alpha`() {
        val color = ColorUtils.parseHex("#80FF0000")
        assertNotNull(color)
        assertEquals(0.5f, color!!.alpha, 0.02f) // 0x80 = ~0.5
        assertEquals(1f, color.red, 0.01f)
    }
    
    @Test
    fun `parseHex returns null for invalid format`() {
        assertNull(ColorUtils.parseHex("#FFF")) // Too short
        assertNull(ColorUtils.parseHex("#FFFFFFFFF")) // Too long
        assertNull(ColorUtils.parseHex("invalid"))
        assertNull(ColorUtils.parseHex("#GGGGGG")) // Invalid characters
    }
    
    @Test
    fun `toHex formats color correctly without alpha`() {
        val hex = ColorUtils.toHex(Color.Red, includeAlpha = false)
        assertEquals("#FF0000", hex)
        
        val hex2 = ColorUtils.toHex(Color.Green, includeAlpha = false)
        assertEquals("#00FF00", hex2)
        
        val hex3 = ColorUtils.toHex(Color.Blue, includeAlpha = false)
        assertEquals("#0000FF", hex3)
    }
    
    @Test
    fun `toHex formats color correctly with alpha`() {
        val color = Color(1f, 0f, 0f, 0.5f)
        val hex = ColorUtils.toHex(color, includeAlpha = true)
        assertTrue(hex.startsWith("#"))
        assertTrue(hex.length == 9) // #AARRGGBB
    }
    
    @Test
    fun `getRgbComponents returns correct values`() {
        val (r, g, b) = ColorUtils.getRgbComponents(Color.Red)
        assertEquals(255, r)
        assertEquals(0, g)
        assertEquals(0, b)
        
        val (r2, g2, b2) = ColorUtils.getRgbComponents(Color(0.5f, 0.5f, 0.5f))
        assertEquals(127, r2, 1) // Allow small rounding error
        assertEquals(127, g2, 1)
        assertEquals(127, b2, 1)
    }
    
    @Test
    fun `fromRgb creates correct color`() {
        val red = ColorUtils.fromRgb(255, 0, 0)
        assertEquals(Color.Red, red)
        
        val gray = ColorUtils.fromRgb(128, 128, 128)
        assertEquals(0.5f, gray.red, 0.01f)
        assertEquals(0.5f, gray.green, 0.01f)
        assertEquals(0.5f, gray.blue, 0.01f)
    }
    
    @Test
    fun `fromRgb handles alpha correctly`() {
        val color = ColorUtils.fromRgb(255, 0, 0, alpha = 128)
        assertEquals(Color.Red.red, color.red, 0.01f)
        assertEquals(0.5f, color.alpha, 0.01f)
    }
    
    @Test
    fun `hsb to rgb round trip preserves color`() {
        val original = Color(0.7f, 0.3f, 0.9f, 1f)
        val hsb = ColorUtils.rgbToHsb(original)
        val converted = ColorUtils.hsbToColor(hsb[0], hsb[1], hsb[2], original.alpha)
        
        assertEquals(original.red, converted.red, 0.01f)
        assertEquals(original.green, converted.green, 0.01f)
        assertEquals(original.blue, converted.blue, 0.01f)
        assertEquals(original.alpha, converted.alpha, 0.01f)
    }
    
    @Test
    fun `hex round trip preserves color`() {
        val original = Color.Cyan
        val hex = ColorUtils.toHex(original, includeAlpha = false)
        val parsed = ColorUtils.parseHex(hex)
        
        assertNotNull(parsed)
        assertEquals(original.red, parsed!!.red, 0.01f)
        assertEquals(original.green, parsed.green, 0.01f)
        assertEquals(original.blue, parsed.blue, 0.01f)
    }
}
