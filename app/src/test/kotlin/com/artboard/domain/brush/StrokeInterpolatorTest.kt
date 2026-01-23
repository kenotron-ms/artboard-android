package com.artboard.domain.brush

import com.artboard.data.model.Point
import org.junit.Test
import org.junit.Assert.*

class StrokeInterpolatorTest {
    
    @Test
    fun `interpolate preserves tilt data`() {
        val interpolator = StrokeInterpolator()
        
        // Create points with tilt
        val p1 = Point(
            x = 100f,
            y = 100f,
            pressure = 0.5f,
            tiltX = 30f,
            tiltY = 40f,
            orientation = 45f
        )
        val p2 = Point(
            x = 200f,
            y = 200f,
            pressure = 0.7f,
            tiltX = 35f,
            tiltY = 45f,
            orientation = 50f
        )
        
        // Interpolate with large spacing to get intermediate points
        val result = interpolator.interpolate(listOf(p1, p2), spacing = 20f)
        
        // Should have original points plus interpolated points
        assertTrue("Should have multiple points", result.size > 2)
        
        // Check that interpolated points have tilt data
        result.forEach { point ->
            // Tilt values should be between p1 and p2
            assertTrue("TiltX should be interpolated", point.tiltX >= 30f && point.tiltX <= 35f)
            assertTrue("TiltY should be interpolated", point.tiltY >= 40f && point.tiltY <= 45f)
            assertTrue("Orientation should be interpolated", point.orientation >= 45f && point.orientation <= 50f)
        }
    }
    
    @Test
    fun `interpolate works with zero tilt`() {
        val interpolator = StrokeInterpolator()
        
        // Points without tilt (defaults to 0)
        val p1 = Point(100f, 100f, 0.5f)
        val p2 = Point(200f, 200f, 0.7f)
        
        val result = interpolator.interpolate(listOf(p1, p2), spacing = 20f)
        
        assertTrue(result.isNotEmpty())
        
        // All points should have zero tilt
        result.forEach { point ->
            assertEquals(0f, point.tiltX, 0.01f)
            assertEquals(0f, point.tiltY, 0.01f)
            assertEquals(0f, point.orientation, 0.01f)
        }
    }
    
    @Test
    fun `linear interpolation preserves tilt`() {
        val interpolator = StrokeInterpolator()
        
        val p1 = Point(
            x = 0f,
            y = 0f,
            pressure = 0.5f,
            tiltX = 0f,
            tiltY = 0f,
            orientation = 0f
        )
        val p2 = Point(
            x = 100f,
            y = 100f,
            pressure = 1.0f,
            tiltX = 90f,
            tiltY = 90f,
            orientation = 180f
        )
        
        // Should use linear interpolation for 2 points
        val result = interpolator.interpolate(listOf(p1, p2), spacing = 25f)
        
        assertTrue(result.size > 2)
        
        // Midpoint should have ~half the tilt
        val midpoint = result[result.size / 2]
        assertTrue("TiltX around midpoint", midpoint.tiltX > 30f && midpoint.tiltX < 60f)
        assertTrue("TiltY around midpoint", midpoint.tiltY > 30f && midpoint.tiltY < 60f)
    }
    
    @Test
    fun `catmull-rom interpolation smooth for tilt`() {
        val interpolator = StrokeInterpolator()
        
        // Create curve with varying tilt
        val points = listOf(
            Point(0f, 0f, 0.5f, 0f, 0f, 0f),
            Point(50f, 50f, 0.7f, 30f, 20f, 45f),
            Point(100f, 100f, 0.9f, 60f, 40f, 90f),
            Point(150f, 150f, 1.0f, 90f, 60f, 135f)
        )
        
        val result = interpolator.interpolate(points, spacing = 10f)
        
        // Should have many interpolated points
        assertTrue(result.size > points.size)
        
        // Tilt should increase smoothly (no jumps)
        for (i in 1 until result.size) {
            val tiltChange = kotlin.math.abs(result[i].tiltX - result[i-1].tiltX)
            assertTrue("Tilt changes smoothly", tiltChange < 30f) // Max 30° jump between stamps
        }
    }
    
    @Test
    fun `simplify preserves tilt data in kept points`() {
        val interpolator = StrokeInterpolator()
        
        val points = listOf(
            Point(0f, 0f, 0.5f, 10f, 10f, 0f),
            Point(50f, 50f, 0.7f, 20f, 20f, 45f),
            Point(100f, 100f, 0.9f, 30f, 30f, 90f)
        )
        
        val simplified = interpolator.simplify(points, tolerance = 5f)
        
        // Should keep at least first and last
        assertTrue(simplified.size >= 2)
        
        // Points that are kept should preserve their tilt
        assertTrue(simplified.first().tiltX == 10f)
        assertTrue(simplified.last().tiltX == 30f)
    }
}
