package com.artboard.ui.canvas

import com.artboard.data.model.Brush
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CanvasViewModel UI state management
 */
class CanvasViewModelUITest {
    
    private lateinit var viewModel: CanvasViewModel
    
    @Before
    fun setup() {
        viewModel = CanvasViewModel()
    }
    
    @Test
    fun `toolbar initially visible`() {
        assertTrue("Toolbar should be visible initially", viewModel.toolbarVisible.value)
    }
    
    @Test
    fun `status bar initially visible`() {
        assertTrue("Status bar should be visible initially", viewModel.statusBarVisible.value)
    }
    
    @Test
    fun `showUI makes both toolbar and status bar visible`() {
        // Hide first
        viewModel.hideUI()
        
        // Then show
        viewModel.showUI()
        
        assertTrue("Toolbar should be visible", viewModel.toolbarVisible.value)
        assertTrue("Status bar should be visible", viewModel.statusBarVisible.value)
    }
    
    @Test
    fun `hideUI hides both toolbar and status bar when not drawing`() {
        viewModel.hideUI()
        
        assertFalse("Toolbar should be hidden", viewModel.toolbarVisible.value)
        assertFalse("Status bar should be hidden", viewModel.statusBarVisible.value)
    }
    
    @Test
    fun `hideUI does not hide UI while drawing`() {
        // Start drawing
        viewModel.onDrawingStarted()
        
        // Try to hide
        viewModel.hideUI()
        
        // UI should still be visible
        assertTrue("Toolbar should remain visible while drawing", viewModel.toolbarVisible.value)
        assertTrue("Status bar should remain visible while drawing", viewModel.statusBarVisible.value)
    }
    
    @Test
    fun `toggleUI switches visibility state`() {
        // Initially visible
        assertTrue(viewModel.toolbarVisible.value)
        
        // Toggle off
        viewModel.toggleUI()
        assertFalse("Toolbar should be hidden after toggle", viewModel.toolbarVisible.value)
        
        // Toggle on
        viewModel.toggleUI()
        assertTrue("Toolbar should be visible after second toggle", viewModel.toolbarVisible.value)
    }
    
    @Test
    fun `onInteraction shows UI`() {
        // Hide first
        viewModel.hideUI()
        
        // Interaction should show UI
        viewModel.onInteraction()
        
        assertTrue("Toolbar should be visible after interaction", viewModel.toolbarVisible.value)
    }
    
    @Test
    fun `drawing state tracked correctly`() {
        assertFalse("Should not be drawing initially", viewModel.isDrawing.value)
        
        viewModel.onDrawingStarted()
        assertTrue("Should be drawing after start", viewModel.isDrawing.value)
        
        viewModel.onDrawingEnded()
        assertFalse("Should not be drawing after end", viewModel.isDrawing.value)
    }
    
    @Test
    fun `undo and redo state initially false`() {
        assertFalse("Cannot undo initially", viewModel.canUndo.value)
        assertFalse("Cannot redo initially", viewModel.canRedo.value)
    }
}

/**
 * Integration tests for auto-hide behavior
 * Note: These test the logic, not the actual timing (which requires instrumented tests)
 */
class AutoHideControllerTest {
    
    @Test
    fun `auto hide controller calls onHide callback`() {
        var hideCalled = false
        val controller = com.artboard.ui.common.AutoHideController(
            hideDelay = 100L,
            onHide = { hideCalled = true }
        )
        
        controller.start()
        
        // Wait for delay
        Thread.sleep(150)
        
        assertTrue("onHide should have been called", hideCalled)
        
        controller.cleanup()
    }
    
    @Test
    fun `stop prevents auto hide`() {
        var hideCalled = false
        val controller = com.artboard.ui.common.AutoHideController(
            hideDelay = 100L,
            onHide = { hideCalled = true }
        )
        
        controller.start()
        controller.stop()
        
        // Wait longer than delay
        Thread.sleep(150)
        
        assertFalse("onHide should not have been called", hideCalled)
        
        controller.cleanup()
    }
    
    @Test
    fun `reset restarts timer`() {
        var hideCount = 0
        val controller = com.artboard.ui.common.AutoHideController(
            hideDelay = 100L,
            onHide = { hideCount++ }
        )
        
        controller.start()
        Thread.sleep(50) // Wait half the delay
        controller.reset() // Restart timer
        Thread.sleep(80) // Wait most of new delay
        
        assertEquals("Should not have hidden yet", 0, hideCount)
        
        Thread.sleep(30) // Complete the new delay
        
        assertEquals("Should have hidden once", 1, hideCount)
        
        controller.cleanup()
    }
}
