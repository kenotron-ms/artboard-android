package com.artboard.ui.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ColorPickerViewModel
 * Tests state management, color conversions, and recent colors
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ColorPickerViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ColorPickerViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ColorPickerViewModel()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `setHue updates hue state`() = runTest {
        viewModel.setHue(180f)
        advanceUntilIdle()
        
        assertEquals(180f, viewModel.hue.value, 0.01f)
    }
    
    @Test
    fun `setHue clamps to valid range`() = runTest {
        viewModel.setHue(400f)
        advanceUntilIdle()
        assertEquals(360f, viewModel.hue.value, 0.01f)
        
        viewModel.setHue(-10f)
        advanceUntilIdle()
        assertEquals(0f, viewModel.hue.value, 0.01f)
    }
    
    @Test
    fun `setSaturation updates saturation state`() = runTest {
        viewModel.setSaturation(0.75f)
        advanceUntilIdle()
        
        assertEquals(0.75f, viewModel.saturation.value, 0.01f)
    }
    
    @Test
    fun `setSaturation clamps to valid range`() = runTest {
        viewModel.setSaturation(1.5f)
        advanceUntilIdle()
        assertEquals(1f, viewModel.saturation.value, 0.01f)
        
        viewModel.setSaturation(-0.5f)
        advanceUntilIdle()
        assertEquals(0f, viewModel.saturation.value, 0.01f)
    }
    
    @Test
    fun `setBrightness updates brightness state`() = runTest {
        viewModel.setBrightness(0.5f)
        advanceUntilIdle()
        
        assertEquals(0.5f, viewModel.brightness.value, 0.01f)
    }
    
    @Test
    fun `setAlpha updates alpha state`() = runTest {
        viewModel.setAlpha(0.8f)
        advanceUntilIdle()
        
        assertEquals(0.8f, viewModel.alpha.value, 0.01f)
    }
    
    @Test
    fun `currentColor reflects HSB values`() = runTest {
        viewModel.setHue(120f)      // Green
        viewModel.setSaturation(1f)
        viewModel.setBrightness(1f)
        advanceUntilIdle()
        
        val color = viewModel.currentColor.value
        
        // Verify color is bright green
        assertTrue(color.green > 0.9f)
        assertTrue(color.red < 0.1f)
        assertTrue(color.blue < 0.1f)
    }
    
    @Test
    fun `setColor updates HSB values`() = runTest {
        viewModel.setColor(Color.Red)
        advanceUntilIdle()
        
        assertEquals(0f, viewModel.hue.value, 1f)
        assertEquals(1f, viewModel.saturation.value, 0.01f)
        assertEquals(1f, viewModel.brightness.value, 0.01f)
    }
    
    @Test
    fun `confirmColor saves to previous color`() = runTest {
        viewModel.setColor(Color.Red)
        advanceUntilIdle()
        
        viewModel.confirmColor()
        advanceUntilIdle()
        
        assertEquals(Color.Red, viewModel.previousColor.value)
    }
    
    @Test
    fun `confirmColor adds to recent colors`() = runTest {
        viewModel.setColor(Color.Red)
        advanceUntilIdle()
        
        viewModel.confirmColor()
        advanceUntilIdle()
        
        assertEquals(1, viewModel.recentColors.value.size)
        assertEquals(Color.Red.toArgb(), viewModel.recentColors.value[0])
    }
    
    @Test
    fun `confirmColor moves existing color to front`() = runTest {
        // Add red
        viewModel.setColor(Color.Red)
        advanceUntilIdle()
        viewModel.confirmColor()
        advanceUntilIdle()
        
        // Add blue
        viewModel.setColor(Color.Blue)
        advanceUntilIdle()
        viewModel.confirmColor()
        advanceUntilIdle()
        
        // Add red again
        viewModel.setColor(Color.Red)
        advanceUntilIdle()
        viewModel.confirmColor()
        advanceUntilIdle()
        
        // Red should be first, blue second, no duplicate
        assertEquals(2, viewModel.recentColors.value.size)
        assertEquals(Color.Red.toArgb(), viewModel.recentColors.value[0])
        assertEquals(Color.Blue.toArgb(), viewModel.recentColors.value[1])
    }
    
    @Test
    fun `confirmColor limits recent colors to 10`() = runTest {
        // Add 15 different colors
        for (i in 0..14) {
            val color = Color(i / 15f, 0.5f, 0.5f)
            viewModel.setColor(color)
            advanceUntilIdle()
            viewModel.confirmColor()
            advanceUntilIdle()
        }
        
        // Should only keep 10
        assertEquals(10, viewModel.recentColors.value.size)
    }
    
    @Test
    fun `swapCurrentPrevious swaps colors`() = runTest {
        viewModel.setColor(Color.Red)
        advanceUntilIdle()
        viewModel.confirmColor()
        advanceUntilIdle()
        
        viewModel.setColor(Color.Blue)
        advanceUntilIdle()
        
        val currentBefore = viewModel.currentColor.value
        val previousBefore = viewModel.previousColor.value
        
        viewModel.swapCurrentPrevious()
        advanceUntilIdle()
        
        // Current should become previous and vice versa
        assertEquals(previousBefore, viewModel.currentColor.value)
        assertEquals(currentBefore, viewModel.previousColor.value)
    }
    
    @Test
    fun `setInputMode updates input mode`() = runTest {
        viewModel.setInputMode(InputMode.RGB)
        advanceUntilIdle()
        
        assertEquals(InputMode.RGB, viewModel.inputMode.value)
        
        viewModel.setInputMode(InputMode.HEX)
        advanceUntilIdle()
        
        assertEquals(InputMode.HEX, viewModel.inputMode.value)
    }
    
    @Test
    fun `initialize sets color and previous`() = runTest {
        viewModel.initialize(Color.Cyan)
        advanceUntilIdle()
        
        assertEquals(Color.Cyan, viewModel.currentColor.value)
        assertEquals(Color.Cyan, viewModel.previousColor.value)
    }
    
    @Test
    fun `loadRecentColors populates recent colors`() = runTest {
        val colors = listOf(
            Color.Red.toArgb(),
            Color.Green.toArgb(),
            Color.Blue.toArgb()
        )
        
        viewModel.loadRecentColors(colors)
        advanceUntilIdle()
        
        assertEquals(3, viewModel.recentColors.value.size)
        assertEquals(colors, viewModel.recentColors.value)
    }
    
    @Test
    fun `loadRecentColors limits to 10 colors`() = runTest {
        val colors = (0..14).map { Color(it / 15f, 0.5f, 0.5f).toArgb() }
        
        viewModel.loadRecentColors(colors)
        advanceUntilIdle()
        
        assertEquals(10, viewModel.recentColors.value.size)
    }
    
    @Test
    fun `default input mode is HSB`() = runTest {
        advanceUntilIdle()
        assertEquals(InputMode.HSB, viewModel.inputMode.value)
    }
}
