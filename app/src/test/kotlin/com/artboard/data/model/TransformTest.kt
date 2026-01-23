package com.artboard.data.model

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Transform data model
 */
class TransformTest {
    
    @Test
    fun `identity transform has no changes`() {
        val transform = Transform.identity()
        
        assertTrue(transform.isIdentity())
        assertEquals(Offset.Zero, transform.translation)
        assertEquals(1f, transform.scale, 0.01f)
        assertEquals(1f, transform.scaleX, 0.01f)
        assertEquals(1f, transform.scaleY, 0.01f)
        assertEquals(0f, transform.rotation, 0.01f)
    }
    
    @Test
    fun `isIdentity returns false for translated transform`() {
        val transform = Transform(translation = Offset(10f, 10f))
        assertFalse(transform.isIdentity())
    }
    
    @Test
    fun `isIdentity returns false for scaled transform`() {
        val transform = Transform(scale = 2f)
        assertFalse(transform.isIdentity())
    }
    
    @Test
    fun `isIdentity returns false for rotated transform`() {
        val transform = Transform(rotation = 45f)
        assertFalse(transform.isIdentity())
    }
    
    @Test
    fun `withTranslation adds offset`() {
        val transform = Transform.identity()
        val delta = Offset(10f, 20f)
        
        val result = transform.withTranslation(delta)
        
        assertEquals(Offset(10f, 20f), result.translation)
    }
    
    @Test
    fun `withTranslation accumulates offsets`() {
        val transform = Transform(translation = Offset(5f, 5f))
        val delta = Offset(10f, 20f)
        
        val result = transform.withTranslation(delta)
        
        assertEquals(Offset(15f, 25f), result.translation)
    }
    
    @Test
    fun `withUniformScale sets scale value`() {
        val transform = Transform.identity()
        
        val result = transform.withUniformScale(2f)
        
        assertEquals(2f, result.scale, 0.01f)
    }
    
    @Test
    fun `withUniformScale clamps to minimum 0_1`() {
        val transform = Transform.identity()
        
        val result = transform.withUniformScale(0.05f)
        
        assertEquals(0.1f, result.scale, 0.01f)
    }
    
    @Test
    fun `withUniformScale clamps to maximum 10`() {
        val transform = Transform.identity()
        
        val result = transform.withUniformScale(15f)
        
        assertEquals(10f, result.scale, 0.01f)
    }
    
    @Test
    fun `withUniformScale updates pivot`() {
        val transform = Transform.identity()
        val pivot = Offset(0.25f, 0.75f)
        
        val result = transform.withUniformScale(2f, pivot)
        
        assertEquals(0.25f, result.pivotX, 0.01f)
        assertEquals(0.75f, result.pivotY, 0.01f)
    }
    
    @Test
    fun `withFreeScale sets independent X and Y scales`() {
        val transform = Transform.identity()
        
        val result = transform.withFreeScale(2f, 3f)
        
        assertEquals(2f, result.scaleX, 0.01f)
        assertEquals(3f, result.scaleY, 0.01f)
    }
    
    @Test
    fun `withFreeScale clamps values`() {
        val transform = Transform.identity()
        
        val result = transform.withFreeScale(0.05f, 15f)
        
        assertEquals(0.1f, result.scaleX, 0.01f)
        assertEquals(10f, result.scaleY, 0.01f)
    }
    
    @Test
    fun `withRotation sets rotation angle`() {
        val transform = Transform.identity()
        
        val result = transform.withRotation(45f)
        
        assertEquals(45f, result.rotation, 0.01f)
    }
    
    @Test
    fun `withRotation normalizes angle to 0-360`() {
        val transform = Transform.identity()
        
        val result = transform.withRotation(450f)
        
        assertEquals(90f, result.rotation, 0.01f)
    }
    
    @Test
    fun `withRotation handles negative angles`() {
        val transform = Transform.identity()
        
        val result = transform.withRotation(-45f)
        
        assertTrue(result.rotation >= 0f && result.rotation < 360f)
    }
    
    @Test
    fun `withSnappedRotation snaps to 0 degrees`() {
        val transform = Transform(rotation = 2f)
        
        val result = transform.withSnappedRotation(threshold = 5f)
        
        assertEquals(0f, result.rotation, 0.01f)
    }
    
    @Test
    fun `withSnappedRotation snaps to 45 degrees`() {
        val transform = Transform(rotation = 43f)
        
        val result = transform.withSnappedRotation(threshold = 5f)
        
        assertEquals(45f, result.rotation, 0.01f)
    }
    
    @Test
    fun `withSnappedRotation snaps to 90 degrees`() {
        val transform = Transform(rotation = 92f)
        
        val result = transform.withSnappedRotation(threshold = 5f)
        
        assertEquals(90f, result.rotation, 0.01f)
    }
    
    @Test
    fun `withSnappedRotation does not snap when outside threshold`() {
        val transform = Transform(rotation = 50f)
        
        val result = transform.withSnappedRotation(threshold = 5f)
        
        assertEquals(50f, result.rotation, 0.01f)
    }
    
    @Test
    fun `withSnappedRotation snaps to closest angle`() {
        val transform = Transform(rotation = 47f)
        
        val result = transform.withSnappedRotation(threshold = 5f)
        
        assertEquals(45f, result.rotation, 0.01f)
    }
    
    @Test
    fun `SNAP_ANGLES contains 15 degree increments`() {
        assertTrue(Transform.SNAP_ANGLES.contains(0f))
        assertTrue(Transform.SNAP_ANGLES.contains(15f))
        assertTrue(Transform.SNAP_ANGLES.contains(30f))
        assertTrue(Transform.SNAP_ANGLES.contains(45f))
        assertTrue(Transform.SNAP_ANGLES.contains(90f))
        assertTrue(Transform.SNAP_ANGLES.contains(180f))
        assertTrue(Transform.SNAP_ANGLES.contains(270f))
    }
    
    @Test
    fun `STRONG_SNAP_ANGLES contains 45 degree increments`() {
        assertEquals(8, Transform.STRONG_SNAP_ANGLES.size)
        assertTrue(Transform.STRONG_SNAP_ANGLES.contains(0f))
        assertTrue(Transform.STRONG_SNAP_ANGLES.contains(45f))
        assertTrue(Transform.STRONG_SNAP_ANGLES.contains(90f))
        assertTrue(Transform.STRONG_SNAP_ANGLES.contains(135f))
        assertTrue(Transform.STRONG_SNAP_ANGLES.contains(180f))
        assertTrue(Transform.STRONG_SNAP_ANGLES.contains(225f))
        assertTrue(Transform.STRONG_SNAP_ANGLES.contains(270f))
        assertTrue(Transform.STRONG_SNAP_ANGLES.contains(315f))
    }
    
    @Test
    fun `transform maintains immutability`() {
        val original = Transform(
            translation = Offset(10f, 10f),
            scale = 2f,
            rotation = 45f
        )
        
        val modified = original.withRotation(90f)
        
        // Original should be unchanged
        assertEquals(45f, original.rotation, 0.01f)
        // Modified should have new value
        assertEquals(90f, modified.rotation, 0.01f)
    }
    
    @Test
    fun `transform copy preserves other properties`() {
        val original = Transform(
            translation = Offset(10f, 10f),
            scale = 2f,
            rotation = 45f,
            pivotX = 0.25f,
            pivotY = 0.75f
        )
        
        val modified = original.withRotation(90f)
        
        // Other properties should be preserved
        assertEquals(original.translation, modified.translation)
        assertEquals(original.scale, modified.scale, 0.01f)
        assertEquals(original.pivotX, modified.pivotX, 0.01f)
        assertEquals(original.pivotY, modified.pivotY, 0.01f)
    }
}
