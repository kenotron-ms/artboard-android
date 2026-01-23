package com.artboard.ui.hints

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for HintManager
 */
class HintManagerTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var hintManager: HintManager
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        
        every { mockContext.getSharedPreferences("hints", Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        
        hintManager = HintManager(mockContext)
    }
    
    @Test
    fun `hint not shown initially`() {
        // Given: No hints shown
        every { mockPrefs.getBoolean("test_hint", false) } returns false
        
        // Then: Hint should not be marked as shown
        assertFalse(hintManager.hasShown("test_hint"))
    }
    
    @Test
    fun `hint shown only once`() {
        // Given: Hint marked as shown
        every { mockPrefs.getBoolean("test_hint", false) } returns false andThen true
        
        // When: First check
        assertFalse(hintManager.hasShown("test_hint"))
        
        // When: Mark as shown
        hintManager.markShown("test_hint")
        
        // Then: Preference is saved
        verify { mockEditor.putBoolean("test_hint", true) }
        verify { mockEditor.apply() }
        
        // When: Check again
        assertTrue(hintManager.hasShown("test_hint"))
    }
    
    @Test
    fun `resetHint clears specific hint`() {
        // When: Reset a specific hint
        hintManager.resetHint("test_hint")
        
        // Then: Preference is set to false
        verify { mockEditor.putBoolean("test_hint", false) }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `resetAll clears all hints`() {
        // When: Reset all hints
        hintManager.resetAll()
        
        // Then: All preferences are cleared
        verify { mockEditor.clear() }
        verify { mockEditor.apply() }
    }
    
    @Test
    fun `getShownCount returns correct count`() {
        // Given: Some hints shown
        val shownHints = mapOf(
            "hint1" to true,
            "hint2" to true,
            "hint3" to false
        )
        every { mockPrefs.all } returns shownHints
        
        // When: Get count
        val count = hintManager.getShownCount()
        
        // Then: Returns count of true values
        assertEquals(2, count)
    }
    
    @Test
    fun `predefined hint IDs are accessible`() {
        // Verify predefined hint constants exist
        assertNotNull(HintManager.HINT_BRUSH_SELECTOR)
        assertNotNull(HintManager.HINT_LAYERS_PANEL)
        assertNotNull(HintManager.HINT_COLOR_PICKER)
        assertNotNull(HintManager.HINT_UNDO_GESTURE)
        assertNotNull(HintManager.HINT_UI_TOGGLE)
        assertNotNull(HintManager.HINT_TRANSFORM_TOOLS)
        assertNotNull(HintManager.HINT_BLEND_MODES)
        assertNotNull(HintManager.HINT_PRESSURE_SETTINGS)
    }
}
