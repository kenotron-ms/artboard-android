package com.artboard.data.model

import org.junit.Test
import org.junit.Assert.*

class PointTest {
    
    @Test
    fun `point stores tilt data correctly`() {
        val point = Point(
            x = 100f,
            y = 200f,
            pressure = 0.7f,
            tiltX = 30f,
            tiltY = 40f,
            orientation = 45f,
            timestamp = 1000L
        )
        
        assertEquals(100f, point.x, 0.01f)
        assertEquals(200f, point.y, 0.01f)
        assertEquals(0.7f, point.pressure, 0.01f)
        assertEquals(30f, point.tiltX, 0.01f)
        assertEquals(40f, point.tiltY, 0.01f)
        assertEquals(45f, point.orientation, 0.01f)
        assertEquals(1000L, point.timestamp)
    }
    
    @Test
    fun `point defaults tilt to zero when not provided`() {
        val point = Point(
            x = 100f,
            y = 200f,
            pressure = 0.5f
        )
        
        assertEquals(0f, point.tiltX, 0.01f)
        assertEquals(0f, point.tiltY, 0.01f)
        assertEquals(0f, point.orientation, 0.01f)
    }
    
    @Test
    fun `tiltMagnitude calculates Euclidean distance`() {
        val point = Point(
            x = 0f,
            y = 0f,
            pressure = 0.5f,
            tiltX = 30f,
            tiltY = 40f
        )
        
        // sqrt(30^2 + 40^2) = sqrt(900 + 1600) = sqrt(2500) = 50
        assertEquals(50f, point.tiltMagnitude(), 0.1f)
    }
    
    @Test
    fun `tiltMagnitude is zero for perpendicular stylus`() {
        val point = Point(0f, 0f, 0.5f, 0f, 0f, 0f)
        
        assertEquals(0f, point.tiltMagnitude(), 0.01f)
    }
    
    @Test
    fun `tiltDirection calculates angle correctly`() {
        // Tilt to the right (positive X, zero Y) = 0 degrees
        val pointRight = Point(0f, 0f, 0.5f, 45f, 0f, 0f)
        assertEquals(0f, pointRight.tiltDirection(), 1f)
        
        // Tilt down (zero X, positive Y) = 90 degrees
        val pointDown = Point(0f, 0f, 0.5f, 0f, 45f, 0f)
        assertEquals(90f, pointDown.tiltDirection(), 1f)
        
        // Tilt left (negative X, zero Y) = 180 degrees
        val pointLeft = Point(0f, 0f, 0.5f, -45f, 0f, 0f)
        assertEquals(180f, pointLeft.tiltDirection(), 1f)
        
        // Tilt up (zero X, negative Y) = 270 degrees
        val pointUp = Point(0f, 0f, 0.5f, 0f, -45f, 0f)
        assertEquals(270f, pointUp.tiltDirection(), 1f)
    }
    
    @Test
    fun `hasTilt returns true when tilted`() {
        val tilted = Point(0f, 0f, 0.5f, 10f, 5f, 0f)
        assertTrue(tilted.hasTilt())
    }
    
    @Test
    fun `hasTilt returns false for perpendicular`() {
        val perpendicular = Point(0f, 0f, 0.5f, 0f, 0f, 0f)
        assertFalse(perpendicular.hasTilt())
    }
    
    @Test
    fun `hasTilt returns true for tilt in one direction only`() {
        val tiltXOnly = Point(0f, 0f, 0.5f, 20f, 0f, 0f)
        assertTrue(tiltXOnly.hasTilt())
        
        val tiltYOnly = Point(0f, 0f, 0.5f, 0f, 20f, 0f)
        assertTrue(tiltYOnly.hasTilt())
    }
    
    @Test
    fun `distanceTo calculates correctly`() {
        val point1 = Point(0f, 0f, 0.5f)
        val point2 = Point(3f, 4f, 0.5f)
        
        // 3-4-5 triangle
        assertEquals(5f, point1.distanceTo(point2), 0.01f)
    }
    
    @Test
    fun `velocityTo calculates pixels per millisecond`() {
        val point1 = Point(
            x = 100f,
            y = 100f,
            pressure = 0.5f,
            timestamp = 1000L
        )
        val point2 = Point(
            x = 150f,
            y = 150f,
            pressure = 0.5f,
            timestamp = 1016L // 16ms later
        )
        
        val distance = point1.distanceTo(point2) // ~70.7 pixels
        val velocity = point1.velocityTo(point2)
        
        // ~70.7 / 16 = ~4.4 pixels/ms
        assertEquals(4.4f, velocity, 0.5f)
    }
}
