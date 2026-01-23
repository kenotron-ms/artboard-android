package com.artboard.ui.layers

import android.graphics.Bitmap
import com.artboard.data.model.BlendMode
import com.artboard.data.model.Layer
import com.artboard.domain.layer.LayerManager
import com.artboard.ui.layers.components.LayerThumbnailGenerator
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LayerPanelViewModel
 * Tests layer management operations and state changes
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LayerPanelViewModelTest {
    
    private lateinit var viewModel: LayerPanelViewModel
    private lateinit var layerManager: LayerManager
    private lateinit var thumbnailGenerator: LayerThumbnailGenerator
    private lateinit var testDispatcher: TestDispatcher
    
    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        // Mock dependencies
        layerManager = mockk(relaxed = true)
        thumbnailGenerator = mockk(relaxed = true)
        
        viewModel = LayerPanelViewModel(layerManager, thumbnailGenerator)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `setLayers updates layer list`() = runTest {
        // Given
        val testLayers = listOf(
            createTestLayer("Layer 1", position = 0),
            createTestLayer("Layer 2", position = 1),
            createTestLayer("Layer 3", position = 2)
        )
        
        // When
        viewModel.setLayers(testLayers)
        
        // Then
        val layers = viewModel.layers.first()
        assertEquals(3, layers.size)
        assertEquals("Layer 1", layers[0].name)
        assertEquals("Layer 3", layers[2].name)
    }
    
    @Test
    fun `setActiveLayer updates active layer ID`() = runTest {
        // Given
        val layer = createTestLayer("Test Layer")
        viewModel.setLayers(listOf(layer))
        
        // When
        viewModel.setActiveLayer(layer.id)
        
        // Then
        assertEquals(layer.id, viewModel.activeLayerId.first())
    }
    
    @Test
    fun `addLayer creates new layer and sets it active`() = runTest {
        // Given
        val newLayer = createTestLayer("New Layer", position = 0)
        every { layerManager.createLayer(any()) } returns newLayer
        
        // When
        viewModel.addLayer("New Layer")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val layers = viewModel.layers.first()
        assertTrue(layers.any { it.name == "New Layer" })
        assertEquals(newLayer.id, viewModel.activeLayerId.first())
    }
    
    @Test
    fun `deleteLayer removes layer from list`() = runTest {
        // Given
        val layer1 = createTestLayer("Layer 1", position = 0)
        val layer2 = createTestLayer("Layer 2", position = 1)
        viewModel.setLayers(listOf(layer1, layer2))
        
        // When
        viewModel.deleteLayer(layer1.id)
        
        // Then
        val layers = viewModel.layers.first()
        assertEquals(1, layers.size)
        assertEquals("Layer 2", layers[0].name)
    }
    
    @Test
    fun `deleteLayer does not delete last layer`() = runTest {
        // Given
        val layer = createTestLayer("Only Layer")
        viewModel.setLayers(listOf(layer))
        
        // When
        viewModel.deleteLayer(layer.id)
        
        // Then
        val layers = viewModel.layers.first()
        assertEquals(1, layers.size) // Layer should still exist
    }
    
    @Test
    fun `duplicateLayer creates copy with incremented position`() = runTest {
        // Given
        val originalLayer = createTestLayer("Original", position = 0)
        val duplicatedLayer = createTestLayer("Original copy", position = 1)
        every { layerManager.duplicateLayer(originalLayer) } returns duplicatedLayer
        
        viewModel.setLayers(listOf(originalLayer))
        
        // When
        viewModel.duplicateLayer(originalLayer.id)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val layers = viewModel.layers.first()
        assertEquals(2, layers.size)
        assertTrue(layers.any { it.name.contains("copy") })
    }
    
    @Test
    fun `toggleVisibility changes layer visibility`() = runTest {
        // Given
        val layer = createTestLayer("Layer", position = 0, isVisible = true)
        viewModel.setLayers(listOf(layer))
        
        // When
        viewModel.toggleVisibility(layer.id)
        
        // Then
        val updatedLayer = viewModel.layers.first().find { it.id == layer.id }
        assertFalse(updatedLayer?.isVisible ?: true)
    }
    
    @Test
    fun `toggleLock changes layer lock state`() = runTest {
        // Given
        val layer = createTestLayer("Layer", position = 0, isLocked = false)
        viewModel.setLayers(listOf(layer))
        
        // When
        viewModel.toggleLock(layer.id)
        
        // Then
        val updatedLayer = viewModel.layers.first().find { it.id == layer.id }
        assertTrue(updatedLayer?.isLocked ?: false)
    }
    
    @Test
    fun `changeOpacity updates layer opacity`() = runTest {
        // Given
        val layer = createTestLayer("Layer", position = 0, opacity = 1f)
        viewModel.setLayers(listOf(layer))
        
        // When
        viewModel.changeOpacity(layer.id, 0.5f)
        
        // Then
        val updatedLayer = viewModel.layers.first().find { it.id == layer.id }
        assertEquals(0.5f, updatedLayer?.opacity ?: 0f, 0.01f)
    }
    
    @Test
    fun `changeBlendMode updates layer blend mode`() = runTest {
        // Given
        val layer = createTestLayer("Layer", position = 0)
        viewModel.setLayers(listOf(layer))
        
        // When
        viewModel.changeBlendMode(layer.id, BlendMode.MULTIPLY)
        
        // Then
        val updatedLayer = viewModel.layers.first().find { it.id == layer.id }
        assertEquals(BlendMode.MULTIPLY, updatedLayer?.blendMode)
    }
    
    @Test
    fun `renameLayer updates layer name`() = runTest {
        // Given
        val layer = createTestLayer("Old Name", position = 0)
        viewModel.setLayers(listOf(layer))
        
        // When
        viewModel.renameLayer(layer.id, "New Name")
        
        // Then
        val updatedLayer = viewModel.layers.first().find { it.id == layer.id }
        assertEquals("New Name", updatedLayer?.name)
    }
    
    @Test
    fun `reorderLayer updates layer positions`() = runTest {
        // Given
        val layer1 = createTestLayer("Layer 1", position = 0)
        val layer2 = createTestLayer("Layer 2", position = 1)
        val layer3 = createTestLayer("Layer 3", position = 2)
        viewModel.setLayers(listOf(layer1, layer2, layer3))
        
        // When - move layer 1 to position 2
        viewModel.reorderLayer(0, 2)
        
        // Then
        val layers = viewModel.layers.first()
        assertEquals(2, layers.find { it.id == layer1.id }?.position)
    }
    
    @Test
    fun `mergeLayerDown combines two layers`() = runTest {
        // Given
        val layer1 = createTestLayer("Layer 1", position = 0)
        val layer2 = createTestLayer("Layer 2", position = 1)
        val mergedLayer = createTestLayer("Layer 1 + Layer 2", position = 0)
        
        every { layerManager.mergeLayers(layer1, layer2) } returns mergedLayer
        
        viewModel.setLayers(listOf(layer1, layer2))
        
        // When
        viewModel.mergeLayerDown(layer2.id)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val layers = viewModel.layers.first()
        assertEquals(1, layers.size)
        assertTrue(layers[0].name.contains("+"))
    }
    
    @Test
    fun `clearLayer removes layer content`() = runTest {
        // Given
        val layer = createTestLayer("Layer", position = 0)
        val clearedLayer = layer.copy()
        
        every { layerManager.clearLayer(layer) } returns clearedLayer
        
        viewModel.setLayers(listOf(layer))
        
        // When
        viewModel.clearLayer(layer.id)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify { layerManager.clearLayer(layer) }
        verify { thumbnailGenerator.invalidate(layer.id) }
    }
    
    @Test
    fun `getActiveLayer returns current active layer`() = runTest {
        // Given
        val layer = createTestLayer("Active Layer")
        viewModel.setLayers(listOf(layer))
        viewModel.setActiveLayer(layer.id)
        
        // When
        val activeLayer = viewModel.getActiveLayer()
        
        // Then
        assertNotNull(activeLayer)
        assertEquals(layer.id, activeLayer?.id)
    }
    
    // Helper function to create test layers
    private fun createTestLayer(
        name: String,
        position: Int = 0,
        opacity: Float = 1f,
        isVisible: Boolean = true,
        isLocked: Boolean = false
    ): Layer {
        val bitmap = mockk<Bitmap>(relaxed = true)
        return Layer(
            name = name,
            bitmap = bitmap,
            opacity = opacity,
            isVisible = isVisible,
            isLocked = isLocked,
            position = position
        )
    }
}
