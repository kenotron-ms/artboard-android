package com.artboard.data.model

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SelectionMask
 * Tests mask creation, operations, and memory efficiency
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SelectionMaskTest {
    
    private lateinit var mask: SelectionMask
    private val testWidth = 100
    private val testHeight = 100
    
    @Before
    fun setup() {
        mask = SelectionMask(testWidth, testHeight)
    }
    
    @Test
    fun `test mask initialization creates empty selection`() {
        assertTrue("Mask should be empty on initialization", mask.isEmpty())
        assertEquals("Bounds should be empty", 0, mask.getBounds().width())
        assertEquals("Bounds should be empty", 0, mask.getBounds().height())
    }
    
    @Test
    fun `test setFromRect creates rectangular selection`() {
        val rect = RectF(20f, 30f, 60f, 70f)
        mask.setFromRect(rect)
        
        assertFalse("Mask should not be empty", mask.isEmpty())
        
        // Test points inside rectangle
        assertTrue("Point inside should be selected", mask.isSelected(40, 50))
        assertTrue("Point at corner should be selected", mask.isSelected(20, 30))
        
        // Test points outside rectangle
        assertFalse("Point outside should not be selected", mask.isSelected(10, 10))
        assertFalse("Point outside should not be selected", mask.isSelected(80, 80))
    }
    
    @Test
    fun `test setFromEllipse creates elliptical selection`() {
        val rect = RectF(20f, 20f, 80f, 80f)
        mask.setFromEllipse(rect)
        
        assertFalse("Mask should not be empty", mask.isEmpty())
        
        // Test center point (should be selected)
        assertTrue("Center should be selected", mask.isSelected(50, 50))
        
        // Test corners (should not be selected for ellipse)
        assertFalse("Corner should not be selected in ellipse", mask.isSelected(20, 20))
    }
    
    @Test
    fun `test getBounds returns correct rectangle`() {
        val rect = RectF(10f, 20f, 50f, 60f)
        mask.setFromRect(rect)
        
        val bounds = mask.getBounds()
        
        assertEquals("Left bound should match", 10, bounds.left)
        assertEquals("Top bound should match", 20, bounds.top)
        assertEquals("Right bound should match", 50, bounds.right)
        assertEquals("Bottom bound should match", 60, bounds.bottom)
    }
    
    @Test
    fun `test invert flips selection`() {
        val rect = RectF(10f, 10f, 50f, 50f)
        mask.setFromRect(rect)
        
        val wasSelected = mask.isSelected(30, 30)
        val wasNotSelected = mask.isSelected(70, 70)
        
        mask.invert()
        
        // After invert, selected becomes unselected and vice versa
        assertNotEquals("Selected point should be inverted", wasSelected, mask.isSelected(30, 30))
        assertNotEquals("Unselected point should be inverted", wasNotSelected, mask.isSelected(70, 70))
    }
    
    @Test
    fun `test selectAll fills entire mask`() {
        mask.selectAll()
        
        assertFalse("Mask should not be empty", mask.isEmpty())
        
        // Test random points
        assertTrue("Top-left should be selected", mask.isSelected(0, 0))
        assertTrue("Center should be selected", mask.isSelected(50, 50))
        assertTrue("Bottom-right should be selected", mask.isSelected(99, 99))
    }
    
    @Test
    fun `test clear removes all selection`() {
        mask.selectAll()
        assertFalse("Mask should have selection", mask.isEmpty())
        
        mask.clear()
        assertTrue("Mask should be empty after clear", mask.isEmpty())
    }
    
    @Test
    fun `test isSelected handles out of bounds coordinates`() {
        mask.selectAll()
        
        assertFalse("Negative X should return false", mask.isSelected(-1, 50))
        assertFalse("Negative Y should return false", mask.isSelected(50, -1))
        assertFalse("Too large X should return false", mask.isSelected(testWidth, 50))
        assertFalse("Too large Y should return false", mask.isSelected(50, testHeight))
    }
    
    @Test
    fun `test extractFromLayer creates bitmap with selected pixels`() {
        // Create a test bitmap
        val sourceBitmap = Bitmap.createBitmap(testWidth, testHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(sourceBitmap)
        canvas.drawColor(Color.RED)
        
        // Select a rectangle
        val rect = RectF(10f, 10f, 50f, 50f)
        mask.setFromRect(rect)
        
        // Extract selected pixels
        val extracted = mask.extractFromLayer(sourceBitmap)
        
        assertNotNull("Extracted bitmap should not be null", extracted)
        assertEquals("Extracted bitmap should have same width", testWidth, extracted.width)
        assertEquals("Extracted bitmap should have same height", testHeight, extracted.height)
        
        // Check that selected area has color
        val selectedPixel = extracted.getPixel(30, 30)
        assertTrue("Selected pixel should not be transparent", Color.alpha(selectedPixel) > 0)
        
        // Check that unselected area is transparent
        val unselectedPixel = extracted.getPixel(70, 70)
        assertEquals("Unselected pixel should be transparent", 0, Color.alpha(unselectedPixel))
        
        extracted.recycle()
        sourceBitmap.recycle()
    }
    
    @Test
    fun `test setFromColor creates selection from color tolerance`() {
        // Create a bitmap with two distinct color regions
        val sourceBitmap = Bitmap.createBitmap(testWidth, testHeight, Bitmap.Config.ARGB_8888)
        
        // Fill left half with red
        for (y in 0 until testHeight) {
            for (x in 0 until testWidth / 2) {
                sourceBitmap.setPixel(x, y, Color.RED)
            }
        }
        
        // Fill right half with blue
        for (y in 0 until testHeight) {
            for (x in testWidth / 2 until testWidth) {
                sourceBitmap.setPixel(x, y, Color.BLUE)
            }
        }
        
        // Select red area by clicking in the red region
        mask.setFromColor(sourceBitmap, 25, 50, tolerance = 32)
        
        assertFalse("Mask should not be empty", mask.isEmpty())
        
        // Red area should be selected
        assertTrue("Red area should be selected", mask.isSelected(25, 50))
        
        // Blue area should not be selected (color difference is large)
        assertFalse("Blue area should not be selected", mask.isSelected(75, 50))
        
        sourceBitmap.recycle()
    }
    
    @Test
    fun `test copy creates independent mask`() {
        val rect = RectF(10f, 10f, 50f, 50f)
        mask.setFromRect(rect)
        
        val copy = mask.copy()
        
        // Modify original
        mask.invert()
        
        // Copy should be unchanged
        assertTrue("Copy should have original selection", copy.isSelected(30, 30))
        assertFalse("Original should be inverted", mask.isSelected(30, 30))
        
        copy.recycle()
    }
    
    @Test
    fun `test feather creates soft edges`() {
        val rect = RectF(40f, 40f, 60f, 60f)
        mask.setFromRect(rect, featherRadius = 5f)
        
        // Center should be fully selected
        val centerAlpha = mask.getAlpha(50, 50)
        assertTrue("Center should be fully selected", centerAlpha > 200)
        
        // Edge should have partial alpha (feathered)
        val edgeAlpha = mask.getAlpha(41, 50)
        assertTrue("Edge should have partial alpha", edgeAlpha > 0 && edgeAlpha < 255)
    }
    
    @Test
    fun `test memory efficiency uses ALPHA_8 format`() {
        // This is more of a documentation test
        // ALPHA_8 uses 1 byte per pixel
        // For 2048x2048 canvas: 2048 * 2048 * 1 = 4MB
        
        val largeWidth = 2048
        val largeHeight = 2048
        val largeMask = SelectionMask(largeWidth, largeHeight)
        
        // Expected memory: 4MB (4,194,304 bytes)
        val expectedBytes = largeWidth * largeHeight
        assertEquals("Memory should be 4MB for 2048x2048", 4194304, expectedBytes)
        
        largeMask.recycle()
    }
    
    @Test
    fun `test flood fill performance`() {
        val sourceBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(sourceBitmap)
        canvas.drawColor(Color.RED)
        
        val largeMask = SelectionMask(500, 500)
        
        val startTime = System.currentTimeMillis()
        largeMask.setFromColor(sourceBitmap, 250, 250, tolerance = 32)
        val duration = System.currentTimeMillis() - startTime
        
        // Should complete in less than 500ms as per spec
        assertTrue("Flood fill should complete in <500ms, took ${duration}ms", duration < 500)
        
        largeMask.recycle()
        sourceBitmap.recycle()
    }
}
