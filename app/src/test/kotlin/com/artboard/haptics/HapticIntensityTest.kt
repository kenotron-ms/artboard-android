package com.artboard.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HapticIntensity enum
 * 
 * Tests:
 * - Correct mapping to HapticFeedbackConstants
 * - Correct duration values
 * - Correct amplitude values
 * - Pattern definitions
 */
class HapticIntensityTest {
    
    @Test
    fun `HapticIntensity maps to correct HapticFeedbackConstants`() {
        assertEquals(
            HapticFeedbackConstants.CLOCK_TICK,
            HapticIntensity.LIGHT.toHapticFeedbackConstant()
        )
        
        assertEquals(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticIntensity.MEDIUM.toHapticFeedbackConstant()
        )
        
        assertEquals(
            HapticFeedbackConstants.LONG_PRESS,
            HapticIntensity.HEAVY.toHapticFeedbackConstant()
        )
        
        // SUCCESS and WARNING depend on API level
        // On older APIs, they fallback to basic constants
    }
    
    @Test
    fun `HapticIntensity has correct durations`() {
        assertEquals(10L, HapticIntensity.LIGHT.getDuration())
        assertEquals(20L, HapticIntensity.MEDIUM.getDuration())
        assertEquals(40L, HapticIntensity.HEAVY.getDuration())
        assertEquals(25L, HapticIntensity.SUCCESS.getDuration())
        assertEquals(50L, HapticIntensity.WARNING.getDuration())
    }
    
    @Test
    fun `HapticIntensity has correct amplitudes`() {
        assertEquals(80, HapticIntensity.LIGHT.getAmplitude())
        assertEquals(150, HapticIntensity.MEDIUM.getAmplitude())
        assertEquals(255, HapticIntensity.HEAVY.getAmplitude())
        assertEquals(180, HapticIntensity.SUCCESS.getAmplitude())
        assertEquals(255, HapticIntensity.WARNING.getAmplitude())
    }
    
    @Test
    fun `duration increases with intensity`() {
        assertTrue(HapticIntensity.LIGHT.getDuration() < HapticIntensity.MEDIUM.getDuration())
        assertTrue(HapticIntensity.MEDIUM.getDuration() < HapticIntensity.HEAVY.getDuration())
    }
    
    @Test
    fun `amplitude increases with intensity`() {
        assertTrue(HapticIntensity.LIGHT.getAmplitude() < HapticIntensity.MEDIUM.getAmplitude())
        assertTrue(HapticIntensity.MEDIUM.getAmplitude() < HapticIntensity.HEAVY.getAmplitude())
    }
    
    @Test
    fun `HapticPatterns are defined correctly`() {
        // SUCCESS pattern
        val success = HapticPatterns.SUCCESS
        assertEquals(4, success.first.size)
        assertEquals(4, success.second.size)
        assertEquals(0L, success.first[0]) // Starts immediately
        
        // WARNING pattern
        val warning = HapticPatterns.WARNING
        assertEquals(2, warning.first.size)
        assertEquals(2, warning.second.size)
        assertEquals(0L, warning.first[0])
        assertEquals(255, warning.second[1]) // Full intensity
        
        // TICK pattern
        val tick = HapticPatterns.TICK
        assertEquals(2, tick.first.size)
        assertEquals(8L, tick.first[1]) // Very short
        
        // SNAP pattern
        val snap = HapticPatterns.SNAP
        assertEquals(4, snap.first.size)
        assertTrue(snap.first[1] > 0) // Has duration
        
        // MODE_CHANGE pattern
        val modeChange = HapticPatterns.MODE_CHANGE
        assertEquals(4, modeChange.first.size)
        assertTrue(modeChange.first[2] > 0) // Has pause
    }
}
