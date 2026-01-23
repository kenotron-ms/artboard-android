package com.artboard.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for OnboardingViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    
    private lateinit var viewModel: OnboardingViewModel
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    
    @Before
    fun setup() {
        // Mock Context and SharedPreferences
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        
        every { mockContext.getSharedPreferences("onboarding", Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        
        viewModel = OnboardingViewModel()
    }
    
    @Test
    fun `shouldShowOnboarding returns true on first launch`() = runTest {
        // Given: User has not seen onboarding
        every { mockPrefs.getBoolean("completed", false) } returns false
        
        // When: Check onboarding status
        viewModel.checkOnboardingStatus(mockContext)
        
        // Then: Should show onboarding
        assertTrue(viewModel.shouldShowOnboarding.value)
    }
    
    @Test
    fun `shouldShowOnboarding returns false after completion`() = runTest {
        // Given: User has completed onboarding
        every { mockPrefs.getBoolean("completed", false) } returns true
        
        // When: Check onboarding status
        viewModel.checkOnboardingStatus(mockContext)
        
        // Then: Should not show onboarding
        assertFalse(viewModel.shouldShowOnboarding.value)
    }
    
    @Test
    fun `completeOnboarding saves preference when dontShowAgain is true`() = runTest {
        // When: Complete onboarding with "don't show again"
        viewModel.completeOnboarding(mockContext, dontShowAgain = true)
        
        // Then: Preference is saved
        verify { mockEditor.putBoolean("completed", true) }
        verify { mockEditor.apply() }
        assertFalse(viewModel.shouldShowOnboarding.value)
    }
    
    @Test
    fun `completeOnboarding does not save preference when dontShowAgain is false`() = runTest {
        // When: Complete onboarding without "don't show again"
        viewModel.completeOnboarding(mockContext, dontShowAgain = false)
        
        // Then: Preference is not saved
        verify(exactly = 0) { mockEditor.putBoolean("completed", true) }
        assertFalse(viewModel.shouldShowOnboarding.value)
    }
    
    @Test
    fun `resetOnboarding clears completion status`() = runTest {
        // When: Reset onboarding
        viewModel.resetOnboarding(mockContext)
        
        // Then: Preference is cleared
        verify { mockEditor.putBoolean("completed", false) }
        verify { mockEditor.apply() }
        assertTrue(viewModel.shouldShowOnboarding.value)
    }
    
    @Test
    fun `setCurrentPage updates currentPage state`() = runTest {
        // When: Set page to 2
        viewModel.setCurrentPage(2)
        
        // Then: Current page is updated
        assertEquals(2, viewModel.currentPage.value)
    }
    
    @Test
    fun `setDontShowAgain updates state`() = runTest {
        // When: Set don't show again to true
        viewModel.setDontShowAgain(true)
        
        // Then: State is updated
        assertTrue(viewModel.dontShowAgain.value)
        
        // When: Set don't show again to false
        viewModel.setDontShowAgain(false)
        
        // Then: State is updated
        assertFalse(viewModel.dontShowAgain.value)
    }
}
