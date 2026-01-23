package com.artboard.haptics

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HapticSettings
 * 
 * Tests:
 * - Settings load/save
 * - Category filtering
 * - Reset to defaults
 * - SharedPreferences integration
 */
class HapticSettingsTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockSharedPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockSharedPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPrefs
        every { mockSharedPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        
        // Default: all enabled
        every { mockSharedPrefs.getBoolean(any(), any()) } returns true
    }
    
    @After
    fun teardown() {
        unmockkAll()
    }
    
    // ========================================================================
    // Load Tests
    // ========================================================================
    
    @Test
    fun `load returns default settings when no saved preferences`() {
        // Arrange - all defaults
        every { mockSharedPrefs.getBoolean(any(), true) } returns true
        
        // Act
        val settings = HapticSettings.load(mockContext)
        
        // Assert
        assertTrue(settings.hapticsEnabled)
        assertTrue(settings.buttonHaptics)
        assertTrue(settings.sliderHaptics)
        assertTrue(settings.gestureHaptics)
        assertTrue(settings.feedbackHaptics)
        assertTrue(settings.transformHaptics)
        assertTrue(settings.layerHaptics)
        assertTrue(settings.fileHaptics)
    }
    
    @Test
    fun `load returns saved preferences`() {
        // Arrange - custom settings
        every { mockSharedPrefs.getBoolean("enabled", true) } returns true
        every { mockSharedPrefs.getBoolean("buttons", true) } returns false
        every { mockSharedPrefs.getBoolean("sliders", true) } returns true
        every { mockSharedPrefs.getBoolean("gestures", true) } returns false
        every { mockSharedPrefs.getBoolean("feedback", true) } returns true
        every { mockSharedPrefs.getBoolean("transform", true) } returns false
        every { mockSharedPrefs.getBoolean("layer", true) } returns true
        every { mockSharedPrefs.getBoolean("file", true) } returns false
        
        // Act
        val settings = HapticSettings.load(mockContext)
        
        // Assert
        assertTrue(settings.hapticsEnabled)
        assertFalse(settings.buttonHaptics)
        assertTrue(settings.sliderHaptics)
        assertFalse(settings.gestureHaptics)
        assertTrue(settings.feedbackHaptics)
        assertFalse(settings.transformHaptics)
        assertTrue(settings.layerHaptics)
        assertFalse(settings.fileHaptics)
    }
    
    // ========================================================================
    // Save Tests
    // ========================================================================
    
    @Test
    fun `save writes all settings to SharedPreferences`() {
        // Arrange
        val settings = HapticSettings(
            hapticsEnabled = false,
            buttonHaptics = true,
            sliderHaptics = false,
            gestureHaptics = true,
            feedbackHaptics = false,
            transformHaptics = true,
            layerHaptics = false,
            fileHaptics = true
        )
        
        // Act
        HapticSettings.save(mockContext, settings)
        
        // Assert
        verify { mockEditor.putBoolean("enabled", false) }
        verify { mockEditor.putBoolean("buttons", true) }
        verify { mockEditor.putBoolean("sliders", false) }
        verify { mockEditor.putBoolean("gestures", true) }
        verify { mockEditor.putBoolean("feedback", false) }
        verify { mockEditor.putBoolean("transform", true) }
        verify { mockEditor.putBoolean("layer", false) }
        verify { mockEditor.putBoolean("file", true) }
        verify { mockEditor.apply() }
    }
    
    // ========================================================================
    // Category Filtering Tests
    // ========================================================================
    
    @Test
    fun `isEnabled returns false when master toggle disabled`() {
        // Arrange
        val settings = HapticSettings(hapticsEnabled = false)
        
        // Act & Assert - all categories should be disabled
        assertFalse(settings.isEnabled(HapticCategory.BUTTON))
        assertFalse(settings.isEnabled(HapticCategory.SLIDER))
        assertFalse(settings.isEnabled(HapticCategory.GESTURE))
        assertFalse(settings.isEnabled(HapticCategory.FEEDBACK))
        assertFalse(settings.isEnabled(HapticCategory.TRANSFORM))
        assertFalse(settings.isEnabled(HapticCategory.LAYER))
        assertFalse(settings.isEnabled(HapticCategory.FILE))
    }
    
    @Test
    fun `isEnabled respects category-specific settings`() {
        // Arrange
        val settings = HapticSettings(
            hapticsEnabled = true,
            buttonHaptics = true,
            sliderHaptics = false,
            gestureHaptics = true,
            feedbackHaptics = false,
            transformHaptics = true,
            layerHaptics = false,
            fileHaptics = true
        )
        
        // Act & Assert
        assertTrue(settings.isEnabled(HapticCategory.BUTTON))
        assertFalse(settings.isEnabled(HapticCategory.SLIDER))
        assertTrue(settings.isEnabled(HapticCategory.GESTURE))
        assertFalse(settings.isEnabled(HapticCategory.FEEDBACK))
        assertTrue(settings.isEnabled(HapticCategory.TRANSFORM))
        assertFalse(settings.isEnabled(HapticCategory.LAYER))
        assertTrue(settings.isEnabled(HapticCategory.FILE))
    }
    
    // ========================================================================
    // Reset Tests
    // ========================================================================
    
    @Test
    fun `reset saves default settings`() {
        // Act
        HapticSettings.reset(mockContext)
        
        // Assert - all should be true (defaults)
        verify { mockEditor.putBoolean("enabled", true) }
        verify { mockEditor.putBoolean("buttons", true) }
        verify { mockEditor.putBoolean("sliders", true) }
        verify { mockEditor.putBoolean("gestures", true) }
        verify { mockEditor.putBoolean("feedback", true) }
        verify { mockEditor.putBoolean("transform", true) }
        verify { mockEditor.putBoolean("layer", true) }
        verify { mockEditor.putBoolean("file", true) }
        verify { mockEditor.apply() }
    }
    
    // ========================================================================
    // Copy Tests
    // ========================================================================
    
    @Test
    fun `copy creates independent instance`() {
        // Arrange
        val original = HapticSettings(
            hapticsEnabled = true,
            buttonHaptics = false
        )
        
        // Act
        val copy = original.copy(buttonHaptics = true)
        
        // Assert
        assertNotSame(original, copy)
        assertFalse(original.buttonHaptics)
        assertTrue(copy.buttonHaptics)
    }
}
