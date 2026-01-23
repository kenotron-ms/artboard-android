package com.artboard.ui.transform

import android.graphics.Bitmap
import com.artboard.data.model.Layer
import com.artboard.data.model.Transform
import com.artboard.data.model.TransformType
import com.artboard.domain.history.HistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for TransformViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TransformViewModelTest {
    
    private lateinit var viewModel: TransformViewModel
    private lateinit var historyManager: HistoryManager
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        historyManager = HistoryManager()
        viewModel = TransformViewModel(historyManager)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is not in transform mode`() = runTest {
        assertFalse(viewModel.isTransformMode.first())
    }
    
    @Test
    fun `enterTransformMode activates transform mode`() = runTest {
        val layers = createTestLayers(1)
        
        viewModel.enterTransformMode(0, layers)
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.isTransformMode.first())
    }
    
    @Test
    fun `enterTransformMode resets transform to identity`() = runTest {
        val layers = createTestLayers(1)
        
        viewModel.enterTransformMode(0, layers)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val transform = viewModel.currentTransform.first()
        assertTrue(transform.isIdentity())
    }
    
    @Test
    fun `exitTransformMode deactivates transform mode`() = runTest {
        val layers = createTestLayers(1)
        viewModel.enterTransformMode(0, layers)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.exitTransformMode()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.isTransformMode.first())
    }
    
    @Test
    fun `setTransformType changes transform type`() = runTest {
        viewModel.setTransformType(TransformType.UNIFORM)
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(TransformType.UNIFORM, viewModel.transformType.first())
    }
    
    @Test
    fun `setTransformType to UNIFORM normalizes scale`() = runTest {
        val layers = createTestLayers(1)
        viewModel.enterTransformMode(0, layers)
        
        // Set non-uniform scale
        viewModel.updateTransform(Transform(scaleX = 2f, scaleY = 3f))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Switch to uniform
        viewModel.setTransformType(TransformType.UNIFORM)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val transform = viewModel.currentTransform.first()
        assertEquals(2.5f, transform.scale, 0.1f) // Average of 2 and 3
    }
    
    @Test
    fun `updateTransform updates current transform`() = runTest {
        val layers = createTestLayers(1)
        viewModel.enterTransformMode(0, layers)
        
        val newTransform = Transform(rotation = 45f)
        viewModel.updateTransform(newTransform)
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(45f, viewModel.currentTransform.first().rotation, 0.01f)
    }
    
    @Test
    fun `rotate90Clockwise adds 90 degrees`() = runTest {
        val layers = createTestLayers(1)
        viewModel.enterTransformMode(0, layers)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.rotate90Clockwise()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(90f, viewModel.currentTransform.first().rotation, 0.01f)
    }
    
    @Test
    fun `rotate90CounterClockwise subtracts 90 degrees`() = runTest {
        val layers = createTestLayers(1)
        viewModel.enterTransformMode(0, layers)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.rotate90CounterClockwise()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val rotation = viewModel.currentTransform.first().rotation
        assertTrue(rotation >= 270f || rotation <= -270f) // -90 or 270
    }
    
    @Test
    fun `resetTransform resets to identity`() = runTest {
        val layers = createTestLayers(1)
        viewModel.enterTransformMode(0, layers)
        
        viewModel.updateTransform(Transform(rotation = 45f, scale = 2f))
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.resetTransform()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.currentTransform.first().isIdentity())
    }
    
    @Test
    fun `canApply returns false for identity transform`() = runTest {
        val layers = createTestLayers(1)
        viewModel.enterTransformMode(0, layers)
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.canApply())
    }
    
    @Test
    fun `canApply returns true for non-identity transform`() = runTest {
        val layers = createTestLayers(1)
        viewModel.enterTransformMode(0, layers)
        
        viewModel.updateTransform(Transform(rotation = 45f))
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.canApply())
    }
    
    @Test
    fun `applyTransform exits transform mode`() = runTest {
        val layers = createTestLayers(1)
        viewModel.enterTransformMode(0, layers)
        
        viewModel.updateTransform(Transform(rotation = 45f))
        testDispatcher.scheduler.advanceUntilIdle()
        
        var completed = false
        viewModel.applyTransform { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(completed)
        assertFalse(viewModel.isTransformMode.first())
    }
    
    @Test
    fun `cancelTransform exits transform mode`() = runTest {
        val layers = createTestLayers(1)
        viewModel.enterTransformMode(0, layers)
        testDispatcher.scheduler.advanceUntilIdle()
        
        var completed = false
        viewModel.cancelTransform { completed = true }
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(completed)
        assertFalse(viewModel.isTransformMode.first())
    }
    
    @Test
    fun `isApplying is false initially`() = runTest {
        assertFalse(viewModel.isApplying.first())
    }
    
    // Helper methods
    
    private fun createTestLayers(count: Int): List<Layer> {
        return List(count) { index ->
            Layer(
                name = "Layer $index",
                bitmap = createTestBitmap(),
                position = index
            )
        }
    }
    
    private fun createTestBitmap(): Bitmap {
        return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    }
}
