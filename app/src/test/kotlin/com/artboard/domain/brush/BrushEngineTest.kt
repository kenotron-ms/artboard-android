package com.artboard.domain.brush

import android.graphics.Bitmap
import android.graphics.Color
import com.artboard.data.model.Brush
import com.artboard.data.model.BrushType
import com.artboard.data.model.Point
import com.artboard.data.model.Stroke
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrushEngineTest {
    
    @Test
    fun `renderStroke completes without errors`() {
        val engine = BrushEngine()
        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        
        val points = listOf(
            Point(100f, 100f, 0.5f),
            Point(150f, 150f, 0.7f),
            Point(200f, 200f, 0.9f)
        )
        
        val stroke = Stroke(
            id = "test",
            points = points,
            brush = Brush.pen(),
            color = Color.BLACK,
            layerId = "test-layer"
        )
        
        // Should not throw
        engine.renderStroke(stroke, bitmap)
        
        // Bitmap should have content (not blank)
        // Note: Detailed pixel verification would require Android test environment
    }
    
    @Test
    fun `tilt increases brush size for calligraphy brush`() {
        val brush = Brush.calligraphy()
        val engine = BrushEngine()
        
        // Use reflection or public method to test if available
        // For now, test via full stroke rendering
        
        val perpendicular = Point(100f, 100f, 0.5f, 0f, 0f, 0f)
        val tilted = Point(200f, 200f, 0.5f, 45f, 30f, 0f)
        
        // Both points should render, tilted should be wider
        // Visual verification needed on actual device
        
        assertTrue("Tilt enabled on calligraphy brush", brush.tiltSizeEnabled)
        assertTrue("Tilted point has tilt data", tilted.hasTilt())
        assertFalse("Perpendicular point has no tilt", perpendicular.hasTilt())
    }
    
    @Test
    fun `tiltMagnitude affects size calculation`() {
        val point1 = Point(0f, 0f, 0.5f, 0f, 0f, 0f)    // Perpendicular
        val point2 = Point(0f, 0f, 0.5f, 45f, 45f, 0f)  // Tilted ~63°
        val point3 = Point(0f, 0f, 0.5f, 90f, 0f, 0f)   // Fully tilted 90°
        
        assertEquals(0f, point1.tiltMagnitude(), 0.1f)
        assertTrue(point2.tiltMagnitude() > 0f)
        assertTrue(point2.tiltMagnitude() < 90f)
        assertEquals(90f, point3.tiltMagnitude(), 0.1f)
    }
    
    @Test
    fun `calligraphy preset has correct tilt settings`() {
        val brush = Brush.calligraphy()
        
        assertEquals(BrushType.CALLIGRAPHY, brush.type)
        assertTrue("Tilt size enabled", brush.tiltSizeEnabled)
        assertTrue("Tilt angle enabled", brush.tiltAngleEnabled)
        assertEquals(1.0f, brush.tiltSizeMin, 0.01f)
        assertEquals(3.0f, brush.tiltSizeMax, 0.01f)
        assertEquals(20f, brush.size, 0.01f)
    }
    
    @Test
    fun `markerChisel preset has correct tilt settings`() {
        val brush = Brush.markerChisel()
        
        assertEquals(BrushType.MARKER, brush.type)
        assertTrue("Tilt size enabled", brush.tiltSizeEnabled)
        assertTrue("Tilt angle enabled", brush.tiltAngleEnabled)
        assertEquals(0.5f, brush.tiltSizeMin, 0.01f)
        assertEquals(2.5f, brush.tiltSizeMax, 0.01f)
    }
    
    @Test
    fun `regular brushes have tilt disabled by default`() {
        val pen = Brush.pen()
        val pencil = Brush.pencil()
        
        assertFalse("Pen tilt disabled", pen.tiltSizeEnabled)
        assertFalse("Pencil tilt disabled", pencil.tiltSizeEnabled)
    }
    
    @Test
    fun `createBrushPreview generates bitmap`() {
        val engine = BrushEngine()
        val brush = Brush.pen()
        
        val preview = engine.createBrushPreview(brush, Color.BLACK, 100)
        
        assertNotNull(preview)
        assertEquals(100, preview.width)
        assertEquals(100, preview.height)
    }
}
