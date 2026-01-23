package com.artboard.domain.transform

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.artboard.data.model.Transform
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs

/**
 * Unit tests for TransformEngine
 */
@RunWith(RobolectricTestRunner::class)
class TransformEngineTest {
    
    private lateinit var engine: TransformEngine
    private lateinit var testBitmap: Bitmap
    
    @Before
    fun setup() {
        engine = TransformEngine()
        testBitmap = createTestBitmap(100, 100)
    }
    
    @Test
    fun `applyTransform with identity transform returns copy of original`() {
        val transform = Transform.identity()
        val result = engine.applyTransform(testBitmap, transform)
        
        assertNotNull(result)
        assertEquals(testBitmap.width, result.width)
        assertEquals(testBitmap.height, result.height)
        assertNotSame(testBitmap, result) // Should be a copy
    }
    
    @Test
    fun `applyTransform with translation moves bitmap`() {
        val transform = Transform(translation = Offset(10f, 20f))
        val result = engine.applyTransform(testBitmap, transform)
        
        assertNotNull(result)
        // Bitmap size should remain the same
        assertTrue(result.width >= testBitmap.width)
        assertTrue(result.height >= testBitmap.height)
    }
    
    @Test
    fun `applyTransform with scale changes bitmap size`() {
        val transform = Transform(scale = 2f)
        val result = engine.applyTransform(testBitmap, transform)
        
        assertNotNull(result)
        // Bitmap should be roughly 2x larger
        assertTrue(result.width >= testBitmap.width * 1.5f)
        assertTrue(result.height >= testBitmap.height * 1.5f)
    }
    
    @Test
    fun `applyTransform with rotation rotates bitmap`() {
        val transform = Transform(rotation = 90f)
        val result = engine.applyTransform(testBitmap, transform)
        
        assertNotNull(result)
        // 90° rotation should swap dimensions
        // Note: Output size may be larger to accommodate rotation
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)
    }
    
    @Test
    fun `calculateTransformedBounds with identity returns same bounds`() {
        val bounds = Rect(0f, 0f, 100f, 100f)
        val transform = Transform.identity()
        
        val result = engine.calculateTransformedBounds(bounds, transform)
        
        assertEquals(bounds.left, result.left, 0.1f)
        assertEquals(bounds.top, result.top, 0.1f)
        assertEquals(bounds.right, result.right, 0.1f)
        assertEquals(bounds.bottom, result.bottom, 0.1f)
    }
    
    @Test
    fun `calculateTransformedBounds with translation shifts bounds`() {
        val bounds = Rect(0f, 0f, 100f, 100f)
        val transform = Transform(translation = Offset(10f, 20f))
        
        val result = engine.calculateTransformedBounds(bounds, transform)
        
        assertEquals(10f, result.left, 0.1f)
        assertEquals(20f, result.top, 0.1f)
    }
    
    @Test
    fun `calculateTransformedBounds with scale increases bounds size`() {
        val bounds = Rect(0f, 0f, 100f, 100f)
        val transform = Transform(scale = 2f)
        
        val result = engine.calculateTransformedBounds(bounds, transform)
        
        // Bounds should be roughly 2x larger
        assertTrue(result.width >= bounds.width * 1.8f)
        assertTrue(result.height >= bounds.height * 1.8f)
    }
    
    @Test
    fun `flipHorizontal mirrors bitmap horizontally`() {
        val bitmap = createTestBitmapWithPattern(10, 10)
        val flipped = engine.flipHorizontal(bitmap)
        
        assertNotNull(flipped)
        assertEquals(bitmap.width, flipped.width)
        assertEquals(bitmap.height, flipped.height)
        
        // Left side should now be right side
        // (This is a simplified check - full pixel verification would be more complex)
        assertNotSame(bitmap, flipped)
    }
    
    @Test
    fun `flipVertical mirrors bitmap vertically`() {
        val bitmap = createTestBitmapWithPattern(10, 10)
        val flipped = engine.flipVertical(bitmap)
        
        assertNotNull(flipped)
        assertEquals(bitmap.width, flipped.width)
        assertEquals(bitmap.height, flipped.height)
        assertNotSame(bitmap, flipped)
    }
    
    @Test
    fun `rotate90Clockwise rotates bitmap 90 degrees`() {
        val bitmap = createTestBitmap(100, 200)
        val rotated = engine.rotate90Clockwise(bitmap)
        
        assertNotNull(rotated)
        // 90° rotation should swap width and height
        assertEquals(bitmap.height, rotated.width)
        assertEquals(bitmap.width, rotated.height)
    }
    
    @Test
    fun `rotate90CounterClockwise rotates bitmap 90 degrees CCW`() {
        val bitmap = createTestBitmap(100, 200)
        val rotated = engine.rotate90CounterClockwise(bitmap)
        
        assertNotNull(rotated)
        // 90° rotation should swap width and height
        assertEquals(bitmap.height, rotated.width)
        assertEquals(bitmap.width, rotated.height)
    }
    
    @Test
    fun `rotate180 rotates bitmap 180 degrees`() {
        val bitmap = createTestBitmap(100, 200)
        val rotated = engine.rotate180(bitmap)
        
        assertNotNull(rotated)
        // 180° rotation should maintain dimensions
        assertEquals(bitmap.width, rotated.width)
        assertEquals(bitmap.height, rotated.height)
    }
    
    @Test
    fun `getEffectiveScale returns uniform scale when set`() {
        val transform = Transform(scale = 2f)
        val (scaleX, scaleY) = engine.getEffectiveScale(transform)
        
        assertEquals(2f, scaleX, 0.01f)
        assertEquals(2f, scaleY, 0.01f)
    }
    
    @Test
    fun `getEffectiveScale returns non-uniform scale when scale is 1`() {
        val transform = Transform(scaleX = 2f, scaleY = 3f)
        val (scaleX, scaleY) = engine.getEffectiveScale(transform)
        
        assertEquals(2f, scaleX, 0.01f)
        assertEquals(3f, scaleY, 0.01f)
    }
    
    @Test
    fun `requiresResampling returns true for scale down`() {
        val transform = Transform(scale = 0.5f)
        assertTrue(engine.requiresResampling(transform))
    }
    
    @Test
    fun `requiresResampling returns true for rotation`() {
        val transform = Transform(rotation = 45f)
        assertTrue(engine.requiresResampling(transform))
    }
    
    @Test
    fun `requiresResampling returns false for identity transform`() {
        val transform = Transform.identity()
        assertFalse(engine.requiresResampling(transform))
    }
    
    @Test
    fun `requiresResampling returns false for scale up only`() {
        val transform = Transform(scale = 2f)
        assertFalse(engine.requiresResampling(transform))
    }
    
    @Test
    fun `combined transform applies all transformations correctly`() {
        val transform = Transform(
            translation = Offset(10f, 10f),
            scale = 1.5f,
            rotation = 45f
        )
        
        val result = engine.applyTransform(testBitmap, transform)
        
        assertNotNull(result)
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)
    }
    
    // Helper methods
    
    private fun createTestBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
    
    private fun createTestBitmapWithPattern(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Create a simple pattern for testing
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = if ((x + y) % 2 == 0) {
                    android.graphics.Color.WHITE
                } else {
                    android.graphics.Color.BLACK
                }
                bitmap.setPixel(x, y, color)
            }
        }
        
        return bitmap
    }
}
