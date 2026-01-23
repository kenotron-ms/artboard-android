package com.artboard.ui.selection

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.artboard.ui.selection.tools.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for selection tools
 * Tests lasso, rectangle, ellipse, and magic wand tools
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SelectionToolsTest {
    
    private val canvasWidth = 500
    private val canvasHeight = 500
    
    @Test
    fun `test LassoTool creates path from touch points`() {
        val lasso = LassoTool()
        
        assertFalse("Should not be drawing initially", lasso.isDrawing())
        
        lasso.onTouchDown(100f, 100f)
        assertTrue("Should be drawing after touch down", lasso.isDrawing())
        
        lasso.onTouchMove(150f, 150f)
        lasso.onTouchMove(200f, 150f)
        lasso.onTouchMove(200f, 100f)
        
        val mask = lasso.onTouchUp(100f, 100f, canvasWidth, canvasHeight)
        
        assertFalse("Should not be drawing after touch up", lasso.isDrawing())
        assertNotNull("Mask should be created", mask)
        assertFalse("Mask should not be empty", mask.isEmpty())
        
        mask.recycle()
    }
    
    @Test
    fun `test LassoTool smooths path points`() {
        val lasso = LassoTool()
        
        lasso.onTouchDown(100f, 100f)
        
        // Add points very close together (should be filtered)
        lasso.onTouchMove(101f, 100f)  // Too close, should be filtered
        lasso.onTouchMove(102f, 100f)  // Too close, should be filtered
        lasso.onTouchMove(110f, 100f)  // Far enough, should be added
        
        val pathPoints = lasso.getPathPoints()
        
        // Should have fewer points than attempted due to smoothing
        assertTrue("Path should be smoothed", pathPoints.size < 4)
        
        lasso.cancel()
    }
    
    @Test
    fun `test LassoTool cancel clears state`() {
        val lasso = LassoTool()
        
        lasso.onTouchDown(100f, 100f)
        lasso.onTouchMove(150f, 150f)
        
        assertTrue("Should be drawing", lasso.isDrawing())
        
        lasso.cancel()
        
        assertFalse("Should not be drawing after cancel", lasso.isDrawing())
        assertTrue("Path points should be empty", lasso.getPathPoints().isEmpty())
    }
    
    @Test
    fun `test RectangleTool creates rectangular selection`() {
        val rectangle = RectangleTool()
        
        rectangle.onTouchDown(100f, 100f)
        assertTrue("Should be drawing", rectangle.isDrawing())
        
        rectangle.onTouchMove(300f, 200f)
        
        val currentRect = rectangle.getCurrentRect()
        assertEquals("Left should be 100", 100f, currentRect.left, 0.01f)
        assertEquals("Top should be 100", 100f, currentRect.top, 0.01f)
        assertEquals("Right should be 300", 300f, currentRect.right, 0.01f)
        assertEquals("Bottom should be 200", 200f, currentRect.bottom, 0.01f)
        
        val mask = rectangle.onTouchUp(300f, 200f, canvasWidth, canvasHeight)
        
        assertFalse("Should not be drawing", rectangle.isDrawing())
        assertNotNull("Mask should be created", mask)
        
        // Test that rectangle area is selected
        assertTrue("Center should be selected", mask.isSelected(200, 150))
        assertFalse("Outside should not be selected", mask.isSelected(50, 50))
        
        mask.recycle()
    }
    
    @Test
    fun `test RectangleTool handles inverted drag`() {
        val rectangle = RectangleTool()
        
        // Start at bottom-right, drag to top-left
        rectangle.onTouchDown(300f, 200f)
        rectangle.onTouchMove(100f, 100f)
        
        val currentRect = rectangle.getCurrentRect()
        
        // Rect should normalize coordinates
        assertEquals("Left should be 100", 100f, currentRect.left, 0.01f)
        assertEquals("Top should be 100", 100f, currentRect.top, 0.01f)
        assertEquals("Right should be 300", 300f, currentRect.right, 0.01f)
        assertEquals("Bottom should be 200", 200f, currentRect.bottom, 0.01f)
        
        rectangle.cancel()
    }
    
    @Test
    fun `test EllipseTool creates elliptical selection`() {
        val ellipse = EllipseTool()
        
        ellipse.onTouchDown(100f, 100f)
        ellipse.onTouchMove(300f, 200f)
        
        val mask = ellipse.onTouchUp(300f, 200f, canvasWidth, canvasHeight)
        
        assertNotNull("Mask should be created", mask)
        
        // Center of ellipse should be selected
        val center = ellipse.getCenterPoint()
        assertNotNull("Center should be calculated", center)
        
        val centerX = center!!.x.toInt()
        val centerY = center.y.toInt()
        assertTrue("Center should be selected", mask.isSelected(centerX, centerY))
        
        mask.recycle()
    }
    
    @Test
    fun `test EllipseTool calculates radius correctly`() {
        val ellipse = EllipseTool()
        
        ellipse.onTouchDown(100f, 100f)
        ellipse.onTouchMove(300f, 200f)
        
        val (radiusX, radiusY) = ellipse.getRadius()
        
        assertEquals("Radius X should be 100", 100f, radiusX, 0.01f)
        assertEquals("Radius Y should be 50", 50f, radiusY, 0.01f)
        
        ellipse.cancel()
    }
    
    @Test
    fun `test MagicWandTool selects similar colors`() {
        // Create a bitmap with distinct color regions
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        // Top half red
        for (y in 0 until 50) {
            for (x in 0 until 100) {
                bitmap.setPixel(x, y, Color.RED)
            }
        }
        
        // Bottom half blue
        for (y in 50 until 100) {
            for (x in 0 until 100) {
                bitmap.setPixel(x, y, Color.BLUE)
            }
        }
        
        val magicWand = MagicWandTool(tolerance = 32)
        
        // Tap in red area
        val mask = magicWand.onTap(50f, 25f, bitmap)
        
        assertNotNull("Mask should be created", mask)
        
        // Red area should be selected
        assertTrue("Red area should be selected", mask.isSelected(50, 25))
        
        // Blue area should not be selected
        assertFalse("Blue area should not be selected", mask.isSelected(50, 75))
        
        mask.recycle()
        bitmap.recycle()
    }
    
    @Test
    fun `test MagicWandTool tolerance affects selection size`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        // Create gradient from red to orange
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                val red = 255
                val green = (x * 255) / 100  // Gradient
                val blue = 0
                bitmap.setPixel(x, y, Color.rgb(red, green, blue))
            }
        }
        
        // Low tolerance - should select fewer pixels
        val lowTolerance = MagicWandTool(tolerance = 10)
        val lowMask = lowTolerance.onTap(0f, 50f, bitmap)
        val lowBounds = lowMask.getBounds()
        
        // High tolerance - should select more pixels
        val highTolerance = MagicWandTool(tolerance = 100)
        val highMask = highTolerance.onTap(0f, 50f, bitmap)
        val highBounds = highMask.getBounds()
        
        // High tolerance should select larger area
        assertTrue("High tolerance should select more", 
            highBounds.width() > lowBounds.width())
        
        lowMask.recycle()
        highMask.recycle()
        bitmap.recycle()
    }
    
    @Test
    fun `test MagicWandTool handles invalid coordinates`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val magicWand = MagicWandTool()
        
        // Test out of bounds coordinates
        assertFalse("Negative X should be invalid", 
            magicWand.isValidCoordinate(-1f, 50f, bitmap))
        assertFalse("Negative Y should be invalid", 
            magicWand.isValidCoordinate(50f, -1f, bitmap))
        assertFalse("Too large X should be invalid", 
            magicWand.isValidCoordinate(100f, 50f, bitmap))
        assertFalse("Too large Y should be invalid", 
            magicWand.isValidCoordinate(50f, 100f, bitmap))
        
        assertTrue("Valid coordinates should be valid", 
            magicWand.isValidCoordinate(50f, 50f, bitmap))
        
        bitmap.recycle()
    }
    
    @Test
    fun `test MagicWandTool setTolerance clamps values`() {
        val magicWand = MagicWandTool()
        
        magicWand.setTolerance(300)  // Too high
        assertEquals("Tolerance should be clamped to 255", 255, magicWand.getTolerance())
        
        magicWand.setTolerance(-10)  // Too low
        assertEquals("Tolerance should be clamped to 0", 0, magicWand.getTolerance())
        
        magicWand.setTolerance(50)  // Valid
        assertEquals("Tolerance should be set", 50, magicWand.getTolerance())
    }
    
    @Test
    fun `test selection tool performance`() {
        val lasso = LassoTool()
        
        lasso.onTouchDown(10f, 10f)
        
        // Simulate drawing a complex path
        val startTime = System.currentTimeMillis()
        for (i in 0..1000) {
            val x = 10f + (i % 100) * 2f
            val y = 10f + (i / 100) * 2f
            lasso.onTouchMove(x, y)
        }
        
        val mask = lasso.onTouchUp(10f, 10f, 500, 500)
        val duration = System.currentTimeMillis() - startTime
        
        // Should complete in less than 100ms as per spec
        assertTrue("Lasso should complete in <100ms, took ${duration}ms", duration < 100)
        
        mask.recycle()
    }
}
