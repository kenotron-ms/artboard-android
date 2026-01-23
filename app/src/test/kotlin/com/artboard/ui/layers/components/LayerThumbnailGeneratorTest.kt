package com.artboard.ui.layers.components

import android.graphics.Bitmap
import com.artboard.data.model.Layer
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LayerThumbnailGenerator
 * Tests thumbnail generation and caching
 */
class LayerThumbnailGeneratorTest {
    
    private lateinit var generator: LayerThumbnailGenerator
    
    @Before
    fun setup() {
        generator = LayerThumbnailGenerator(thumbnailSize = 56)
    }
    
    @Test
    fun `generateThumbnail creates correct size bitmap`() {
        // Given
        val sourceBitmap = createMockBitmap(2048, 2048)
        val layer = createMockLayer("Test", sourceBitmap)
        
        // When
        val thumbnail = generator.generateThumbnail(layer)
        
        // Then
        assertEquals(112, thumbnail.width) // 2x for Retina
        assertEquals(112, thumbnail.height)
    }
    
    @Test
    fun `generateThumbnail caches result`() {
        // Given
        val sourceBitmap = createMockBitmap(1024, 1024)
        val layer = createMockLayer("Test", sourceBitmap)
        
        // When
        val thumbnail1 = generator.generateThumbnail(layer)
        val thumbnail2 = generator.generateThumbnail(layer)
        
        // Then
        assertSame(thumbnail1, thumbnail2) // Should be same cached instance
    }
    
    @Test
    fun `invalidate removes cached thumbnail`() {
        // Given
        val sourceBitmap = createMockBitmap(1024, 1024)
        val layer = createMockLayer("Test", sourceBitmap)
        
        val thumbnail1 = generator.generateThumbnail(layer)
        
        // When
        generator.invalidate(layer.id)
        val thumbnail2 = generator.generateThumbnail(layer)
        
        // Then
        assertNotSame(thumbnail1, thumbnail2) // Should be newly generated
    }
    
    @Test
    fun `clearCache evicts all thumbnails`() {
        // Given
        val layer1 = createMockLayer("Layer1", createMockBitmap(512, 512))
        val layer2 = createMockLayer("Layer2", createMockBitmap(512, 512))
        
        generator.generateThumbnail(layer1)
        generator.generateThumbnail(layer2)
        
        val statsBefore = generator.getCacheStats()
        assertTrue(statsBefore.size > 0)
        
        // When
        generator.clearCache()
        
        // Then
        val statsAfter = generator.getCacheStats()
        assertEquals(0, statsAfter.size)
    }
    
    @Test
    fun `getCacheStats returns accurate statistics`() {
        // Given
        val layer = createMockLayer("Test", createMockBitmap(1024, 1024))
        
        // When - generate and retrieve multiple times
        generator.generateThumbnail(layer) // Miss
        generator.generateThumbnail(layer) // Hit
        generator.generateThumbnail(layer) // Hit
        
        // Then
        val stats = generator.getCacheStats()
        assertEquals(1, stats.size)
        assertEquals(2, stats.hitCount)
        assertEquals(1, stats.missCount)
        assertEquals(0.67f, stats.hitRate, 0.01f)
    }
    
    @Test
    fun `preGenerateThumbnails generates multiple thumbnails`() {
        // Given
        val layers = listOf(
            createMockLayer("Layer1", createMockBitmap(512, 512)),
            createMockLayer("Layer2", createMockBitmap(512, 512)),
            createMockLayer("Layer3", createMockBitmap(512, 512))
        )
        
        // When
        generator.preGenerateThumbnails(layers)
        
        // Then
        val stats = generator.getCacheStats()
        assertEquals(3, stats.size)
    }
    
    @Test
    fun `invalidateAll removes multiple thumbnails`() {
        // Given
        val layer1 = createMockLayer("Layer1", createMockBitmap(512, 512))
        val layer2 = createMockLayer("Layer2", createMockBitmap(512, 512))
        
        generator.generateThumbnail(layer1)
        generator.generateThumbnail(layer2)
        
        // When
        generator.invalidateAll(listOf(layer1.id, layer2.id))
        
        // Then
        val stats = generator.getCacheStats()
        assertEquals(0, stats.size)
    }
    
    // Helper functions
    private fun createMockBitmap(width: Int, height: Int): Bitmap {
        return mockk<Bitmap>(relaxed = true).apply {
            every { getWidth() } returns width
            every { getHeight() } returns height
            every { this@apply.width } returns width
            every { this@apply.height } returns height
        }
    }
    
    private fun createMockLayer(name: String, bitmap: Bitmap): Layer {
        return Layer(
            name = name,
            bitmap = bitmap,
            position = 0
        )
    }
}
