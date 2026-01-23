package com.artboard.haptics

import android.content.Context
import android.content.SharedPreferences
import android.os.Vibrator
import android.provider.Settings
import android.view.View
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HapticFeedbackManager
 * 
 * Tests:
 * - Haptic triggering with different intensities
 * - User preference filtering
 * - System settings check
 * - Category-based filtering
 * - Settings caching and reload
 * - Device compatibility
 * 
 * Based on HAPTIC_FEEDBACK.md specification
 */
class HapticFeedbackManagerTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockVibrator: Vibrator
    private lateinit var mockSharedPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var hapticManager: HapticFeedbackManager
    
    @Before
    fun setup() {
        // Mock Android components
        mockContext = mockk(relaxed = true)
        mockVibrator = mockk(relaxed = true)
        mockSharedPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        
        // Setup SharedPreferences mocking
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPrefs
        every { mockSharedPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        
        // Setup default settings (all enabled)
        every { mockSharedPrefs.getBoolean("enabled", true) } returns true
        every { mockSharedPrefs.getBoolean("buttons", true) } returns true
        every { mockSharedPrefs.getBoolean("sliders", true) } returns true
        every { mockSharedPrefs.getBoolean("gestures", true) } returns true
        every { mockSharedPrefs.getBoolean("feedback", true) } returns true
        every { mockSharedPrefs.getBoolean("transform", true) } returns true
        every { mockSharedPrefs.getBoolean("layer", true) } returns true
        every { mockSharedPrefs.getBoolean("file", true) } returns true
        
        // Setup vibrator
        every { mockContext.getSystemService(Context.VIBRATOR_SERVICE) } returns mockVibrator
        every { mockVibrator.hasVibrator() } returns true
        
        // Setup system settings (haptics enabled)
        mockkStatic(Settings.System::class)
        every { 
            Settings.System.getInt(
                any(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            )
        } returns 1
        
        hapticManager = HapticFeedbackManager(mockContext)
    }
    
    @After
    fun teardown() {
        unmockkAll()
    }
    
    // ========================================================================
    // Basic Haptic Triggering Tests
    // ========================================================================
    
    @Test
    fun `perform triggers vibration when all settings enabled`() {
        // Act
        hapticManager.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        
        // Assert
        verify { mockVibrator.vibrate(any<Long>()) }
    }
    
    @Test
    fun `perform uses correct duration for each intensity`() {
        // Test each intensity
        val intensities = mapOf(
            HapticIntensity.LIGHT to 10L,
            HapticIntensity.MEDIUM to 20L,
            HapticIntensity.HEAVY to 40L,
            HapticIntensity.SUCCESS to 25L,
            HapticIntensity.WARNING to 50L
        )
        
        intensities.forEach { (intensity, expectedDuration) ->
            clearMocks(mockVibrator)
            
            hapticManager.perform(intensity, HapticCategory.BUTTON)
            
            verify { mockVibrator.vibrate(expectedDuration) }
        }
    }
    
    // ========================================================================
    // User Preference Tests
    // ========================================================================
    
    @Test
    fun `perform skips when haptics disabled globally`() {
        // Arrange
        every { mockSharedPrefs.getBoolean("enabled", true) } returns false
        hapticManager.reloadSettings()
        
        // Act
        hapticManager.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        
        // Assert
        verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
    }
    
    @Test
    fun `perform skips when category disabled`() {
        // Arrange - disable button haptics
        every { mockSharedPrefs.getBoolean("buttons", true) } returns false
        hapticManager.reloadSettings()
        
        // Act - try button haptic
        hapticManager.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        
        // Assert - should not vibrate
        verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
        
        // But other categories should still work
        clearMocks(mockVibrator)
        hapticManager.perform(HapticIntensity.MEDIUM, HapticCategory.SLIDER)
        verify { mockVibrator.vibrate(any<Long>()) }
    }
    
    @Test
    fun `perform respects all category settings`() {
        val categories = listOf(
            "buttons" to HapticCategory.BUTTON,
            "sliders" to HapticCategory.SLIDER,
            "gestures" to HapticCategory.GESTURE,
            "feedback" to HapticCategory.FEEDBACK,
            "transform" to HapticCategory.TRANSFORM,
            "layer" to HapticCategory.LAYER,
            "file" to HapticCategory.FILE
        )
        
        categories.forEach { (settingKey, category) ->
            // Disable this category
            every { mockSharedPrefs.getBoolean(settingKey, true) } returns false
            hapticManager.reloadSettings()
            clearMocks(mockVibrator)
            
            // Should not vibrate
            hapticManager.perform(HapticIntensity.MEDIUM, category)
            verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
            
            // Re-enable
            every { mockSharedPrefs.getBoolean(settingKey, true) } returns true
            hapticManager.reloadSettings()
        }
    }
    
    // ========================================================================
    // System Settings Tests
    // ========================================================================
    
    @Test
    fun `perform skips when system haptics disabled`() {
        // Arrange - system haptics disabled
        every { 
            Settings.System.getInt(
                any(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            )
        } returns 0
        
        // Act
        hapticManager.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        
        // Assert
        verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
    }
    
    @Test
    fun `perform handles system settings read exception gracefully`() {
        // Arrange - system settings throws exception
        every { 
            Settings.System.getInt(
                any(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            )
        } throws SecurityException("Cannot read system settings")
        
        // Act - should not crash, assumes enabled
        hapticManager.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        
        // Assert - should still attempt vibration (fallback to enabled)
        verify { mockVibrator.vibrate(any<Long>()) }
    }
    
    // ========================================================================
    // Device Compatibility Tests
    // ========================================================================
    
    @Test
    fun `perform skips when device has no vibrator`() {
        // Arrange
        every { mockVibrator.hasVibrator() } returns false
        
        // Act
        hapticManager.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        
        // Assert
        verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
    }
    
    @Test
    fun `isHapticAvailable returns false when no vibrator`() {
        // Arrange
        every { mockVibrator.hasVibrator() } returns false
        
        // Act & Assert
        assertFalse(hapticManager.isHapticAvailable())
    }
    
    @Test
    fun `isHapticAvailable returns true when vibrator present`() {
        // Arrange
        every { mockVibrator.hasVibrator() } returns true
        
        // Act & Assert
        assertTrue(hapticManager.isHapticAvailable())
    }
    
    // ========================================================================
    // Slider Haptic Tests
    // ========================================================================
    
    @Test
    fun `performIfChanged only triggers on threshold`() {
        // Small change (< 10%)
        hapticManager.performIfChanged(0.51f, 0.50f, threshold = 0.1f)
        verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
        
        // Large change (>= 10%)
        clearMocks(mockVibrator)
        hapticManager.performIfChanged(0.60f, 0.50f, threshold = 0.1f)
        verify(exactly = 1) { mockVibrator.vibrate(any<Long>()) }
    }
    
    @Test
    fun `performIfChanged respects slider category setting`() {
        // Disable slider haptics
        every { mockSharedPrefs.getBoolean("sliders", true) } returns false
        hapticManager.reloadSettings()
        
        // Large change (should trigger if enabled)
        hapticManager.performIfChanged(0.60f, 0.50f, threshold = 0.1f)
        
        // Should not vibrate (category disabled)
        verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
    }
    
    // ========================================================================
    // Pattern Haptic Tests
    // ========================================================================
    
    @Test
    fun `performPattern triggers with correct pattern`() {
        // Arrange
        val pattern = longArrayOf(0, 20, 10, 20)
        val amplitudes = intArrayOf(0, 150, 0, 150)
        
        // Act
        hapticManager.performPattern(pattern, amplitudes, HapticCategory.FEEDBACK)
        
        // Assert
        verify { mockVibrator.vibrate(pattern, -1) }
    }
    
    @Test
    fun `performPattern respects category setting`() {
        // Disable feedback haptics
        every { mockSharedPrefs.getBoolean("feedback", true) } returns false
        hapticManager.reloadSettings()
        
        // Act
        val pattern = longArrayOf(0, 20)
        val amplitudes = intArrayOf(0, 150)
        hapticManager.performPattern(pattern, amplitudes, HapticCategory.FEEDBACK)
        
        // Assert
        verify(exactly = 0) { mockVibrator.vibrate(any<LongArray>(), any()) }
    }
    
    // ========================================================================
    // Convenience Method Tests
    // ========================================================================
    
    @Test
    fun `performSnapPoint triggers light transform haptic`() {
        // Act
        hapticManager.performSnapPoint()
        
        // Assert
        verify { mockVibrator.vibrate(HapticIntensity.LIGHT.getDuration()) }
    }
    
    @Test
    fun `performGestureComplete triggers medium gesture haptic`() {
        // Act
        hapticManager.performGestureComplete()
        
        // Assert
        verify { mockVibrator.vibrate(HapticIntensity.MEDIUM.getDuration()) }
    }
    
    @Test
    fun `performSuccess triggers success pattern`() {
        // Act
        hapticManager.performSuccess()
        
        // Assert
        verify { mockVibrator.vibrate(HapticPatterns.SUCCESS.first, -1) }
    }
    
    @Test
    fun `performWarning triggers warning intensity`() {
        // Act
        hapticManager.performWarning()
        
        // Assert
        verify { mockVibrator.vibrate(HapticIntensity.WARNING.getDuration()) }
    }
    
    @Test
    fun `performLayerOperation triggers with correct category`() {
        // Disable layer haptics
        every { mockSharedPrefs.getBoolean("layer", true) } returns false
        hapticManager.reloadSettings()
        
        // Act
        hapticManager.performLayerOperation()
        
        // Assert - should not trigger
        verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
    }
    
    @Test
    fun `performFileOperation triggers success on success`() {
        // Act - success
        hapticManager.performFileOperation(success = true)
        
        // Assert
        verify { mockVibrator.vibrate(HapticIntensity.SUCCESS.getDuration()) }
    }
    
    @Test
    fun `performFileOperation triggers warning on failure`() {
        // Act - failure
        hapticManager.performFileOperation(success = false)
        
        // Assert
        verify { mockVibrator.vibrate(HapticIntensity.WARNING.getDuration()) }
    }
    
    // ========================================================================
    // Settings Cache Tests
    // ========================================================================
    
    @Test
    fun `settings are cached for performance`() {
        // Perform multiple haptics
        repeat(10) {
            hapticManager.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        }
        
        // Settings should only be loaded once (cached)
        verify(atMost = 2) { mockSharedPrefs.getBoolean(any(), any()) }
    }
    
    @Test
    fun `reloadSettings forces settings refresh`() {
        // Initial load
        hapticManager.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        
        // Change settings
        every { mockSharedPrefs.getBoolean("enabled", true) } returns false
        
        // Reload
        hapticManager.reloadSettings()
        clearMocks(mockVibrator)
        
        // Should now respect new settings
        hapticManager.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
    }
    
    // ========================================================================
    // View-Based Haptic Tests
    // ========================================================================
    
    @Test
    fun `performView uses View performHapticFeedback`() {
        // Arrange
        val mockView = mockk<View>(relaxed = true)
        
        // Act
        hapticManager.performView(mockView, HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        
        // Assert
        verify { 
            mockView.performHapticFeedback(
                HapticIntensity.MEDIUM.toHapticFeedbackConstant(),
                any()
            )
        }
    }
    
    @Test
    fun `performView respects settings`() {
        // Arrange
        val mockView = mockk<View>(relaxed = true)
        every { mockSharedPrefs.getBoolean("enabled", true) } returns false
        hapticManager.reloadSettings()
        
        // Act
        hapticManager.performView(mockView, HapticIntensity.MEDIUM, HapticCategory.BUTTON)
        
        // Assert - should not call view haptic
        verify(exactly = 0) { 
            mockView.performHapticFeedback(any(), any())
        }
    }
    
    // ========================================================================
    // Cancel Tests
    // ========================================================================
    
    @Test
    fun `cancel stops vibration`() {
        // Act
        hapticManager.cancel()
        
        // Assert
        verify { mockVibrator.cancel() }
    }
    
    // ========================================================================
    // Singleton Tests
    // ========================================================================
    
    @Test
    fun `getInstance returns singleton instance`() {
        // Act
        val instance1 = HapticFeedbackManager.getInstance(mockContext)
        val instance2 = HapticFeedbackManager.getInstance(mockContext)
        
        // Assert
        assertSame(instance1, instance2)
    }
}
