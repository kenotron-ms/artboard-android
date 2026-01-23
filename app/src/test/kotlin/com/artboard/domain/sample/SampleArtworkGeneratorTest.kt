package com.artboard.domain.sample

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SampleArtworkGenerator
 */
class SampleArtworkGeneratorTest {
    
    private lateinit var generator: SampleArtworkGenerator
    
    @Before
    fun setup() {
        // Mock Bitmap.createBitmap since it's an Android framework class
        mockkStatic(Bitmap::class)
        every { 
            Bitmap.createBitmap(any<Int>(), any<Int>(), any()) 
        } returns mockk(relaxed = true)
        
        generator = SampleArtworkGenerator()
    }
    
    @Test
    fun `generateSampleProject creates project with correct dimensions`() {
        // When: Generate sample project
        val project = generator.generateSampleProject()
        
        // Then: Project has expected dimensions
        assertEquals(1024, project.width)
        assertEquals(1024, project.height)
        assertEquals("Welcome Sample", project.name)
    }
    
    @Test
    fun `generateSampleProject creates 4 layers`() {
        // When: Generate sample project
        val project = generator.generateSampleProject()
        
        // Then: Project has 4 layers
        assertEquals(4, project.layers.size)
    }
    
    @Test
    fun `generateSampleProject layers have correct names`() {
        // When: Generate sample project
        val project = generator.generateSampleProject()
        
        // Then: Layers have expected names
        assertEquals("Background", project.layers[0].name)
        assertEquals("Sketch", project.layers[1].name)
        assertEquals("Color", project.layers[2].name)
        assertEquals("Highlights", project.layers[3].name)
    }
    
    @Test
    fun `generateSampleProject layers have correct blend modes`() {
        // When: Generate sample project
        val project = generator.generateSampleProject()
        
        // Then: Layers have expected blend modes
        assertEquals(com.artboard.data.model.BlendMode.NORMAL, project.layers[0].blendMode)
        assertEquals(com.artboard.data.model.BlendMode.NORMAL, project.layers[1].blendMode)
        assertEquals(com.artboard.data.model.BlendMode.MULTIPLY, project.layers[2].blendMode)
        assertEquals(com.artboard.data.model.BlendMode.ADD, project.layers[3].blendMode)
    }
    
    @Test
    fun `generateSampleProject layers have correct opacity`() {
        // When: Generate sample project
        val project = generator.generateSampleProject()
        
        // Then: Layers have expected opacity
        assertEquals(1.0f, project.layers[0].opacity, 0.01f) // Background full opacity
        assertEquals(0.7f, project.layers[1].opacity, 0.01f) // Sketch slightly transparent
        assertEquals(0.8f, project.layers[2].opacity, 0.01f) // Color mostly opaque
        assertEquals(0.9f, project.layers[3].opacity, 0.01f) // Highlights mostly opaque
    }
    
    @Test
    fun `generateSimpleSample creates smaller project`() {
        // When: Generate simple sample
        val project = generator.generateSimpleSample()
        
        // Then: Project is smaller for performance
        assertEquals(512, project.width)
        assertEquals(512, project.height)
        assertEquals("Simple Sample", project.name)
    }
    
    @Test
    fun `generateSimpleSample creates 2 layers`() {
        // When: Generate simple sample
        val project = generator.generateSimpleSample()
        
        // Then: Project has fewer layers
        assertEquals(2, project.layers.size)
        assertEquals("Background", project.layers[0].name)
        assertEquals("Drawing", project.layers[1].name)
    }
    
    @Test
    fun `generated projects have valid timestamps`() {
        // When: Generate sample project
        val project = generator.generateSampleProject()
        
        // Then: Timestamps are valid
        assertTrue(project.createdAt > 0)
        assertTrue(project.modifiedAt > 0)
        assertTrue(project.modifiedAt >= project.createdAt)
    }
    
    @Test
    fun `generated projects have all layers visible`() {
        // When: Generate sample project
        val project = generator.generateSampleProject()
        
        // Then: All layers are visible
        project.layers.forEach { layer ->
            assertTrue(layer.isVisible)
        }
    }
    
    @Test
    fun `generated projects have no locked layers`() {
        // When: Generate sample project
        val project = generator.generateSampleProject()
        
        // Then: No layers are locked
        project.layers.forEach { layer ->
            assertFalse(layer.isLocked)
        }
    }
}
