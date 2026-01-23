package com.artboard.ui.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artboard.ui.theme.ArtboardTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for OnboardingFlow
 * Tests user interactions and flow navigation
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun onboarding_showsWelcomeScreenFirst() {
        // Given: Onboarding flow
        composeTestRule.setContent {
            ArtboardTheme {
                OnboardingFlow(
                    onComplete = {},
                    onSkip = {}
                )
            }
        }
        
        // Then: Welcome screen is displayed
        composeTestRule
            .onNodeWithText("Welcome to Artboard")
            .assertIsDisplayed()
    }
    
    @Test
    fun onboarding_skipButtonDismisses() {
        var skipped = false
        
        // Given: Onboarding flow
        composeTestRule.setContent {
            ArtboardTheme {
                OnboardingFlow(
                    onComplete = {},
                    onSkip = { skipped = true }
                )
            }
        }
        
        // When: Click skip button
        composeTestRule
            .onNodeWithText("Skip")
            .performClick()
        
        // Then: Skip callback is invoked
        assertTrue(skipped)
    }
    
    @Test
    fun onboarding_skipButtonHiddenOnLastPage() {
        // Given: Onboarding flow
        composeTestRule.setContent {
            ArtboardTheme {
                OnboardingFlow(
                    onComplete = {},
                    onSkip = {}
                )
            }
        }
        
        // When: Swipe to last page
        repeat(3) {
            composeTestRule
                .onRoot()
                .performTouchInput { swipeLeft() }
        }
        
        // Then: Skip button is not visible
        composeTestRule
            .onNodeWithText("Skip")
            .assertDoesNotExist()
    }
    
    @Test
    fun onboarding_pagerSwipesThrough4Screens() {
        // Given: Onboarding flow
        composeTestRule.setContent {
            ArtboardTheme {
                OnboardingFlow(
                    onComplete = {},
                    onSkip = {}
                )
            }
        }
        
        // When: Swipe through all screens
        composeTestRule.waitForIdle()
        
        // Screen 1: Welcome
        composeTestRule
            .onNodeWithText("Welcome to Artboard")
            .assertIsDisplayed()
        
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        
        // Screen 2: Gestures
        composeTestRule
            .onNodeWithText("Essential Gestures")
            .assertIsDisplayed()
        
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        
        // Screen 3: Canvas Tour
        composeTestRule
            .onNodeWithText("Your Workspace")
            .assertIsDisplayed()
        
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        
        // Screen 4: Create Prompt
        composeTestRule
            .onNodeWithText("You're All Set!")
            .assertIsDisplayed()
    }
    
    @Test
    fun onboarding_startCreatingButtonCompletes() {
        var completed = false
        
        // Given: Onboarding flow on last page
        composeTestRule.setContent {
            ArtboardTheme {
                OnboardingFlow(
                    onComplete = { completed = true },
                    onSkip = {}
                )
            }
        }
        
        // When: Navigate to last screen
        repeat(3) {
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()
        }
        
        // When: Click "Start Creating" button
        composeTestRule
            .onNodeWithText("Start Creating")
            .performClick()
        
        // Then: Complete callback is invoked
        assertTrue(completed)
    }
    
    @Test
    fun onboarding_pageIndicatorUpdates() {
        // Given: Onboarding flow
        composeTestRule.setContent {
            ArtboardTheme {
                OnboardingFlow(
                    onComplete = {},
                    onSkip = {}
                )
            }
        }
        
        // Then: Page indicator is visible
        // Note: We can't easily test the exact visual state of the indicator,
        // but we can verify the component exists through the flow
        composeTestRule.waitForIdle()
        
        // Swipe and verify we can navigate
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        
        // Should now be on page 2
        composeTestRule
            .onNodeWithText("Essential Gestures")
            .assertIsDisplayed()
    }
    
    @Test
    fun onboarding_dontShowAgainCheckbox() {
        // Given: Onboarding flow
        composeTestRule.setContent {
            ArtboardTheme {
                OnboardingFlow(
                    onComplete = {},
                    onSkip = {}
                )
            }
        }
        
        // Then: "Don't show again" checkbox is visible
        composeTestRule
            .onNodeWithText("Don't show again")
            .assertIsDisplayed()
    }
    
    @Test
    fun onboarding_canSwipeBackward() {
        // Given: Onboarding flow on second screen
        composeTestRule.setContent {
            ArtboardTheme {
                OnboardingFlow(
                    onComplete = {},
                    onSkip = {}
                )
            }
        }
        
        // When: Swipe forward
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithText("Essential Gestures")
            .assertIsDisplayed()
        
        // When: Swipe backward
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        
        // Then: Back on first screen
        composeTestRule
            .onNodeWithText("Welcome to Artboard")
            .assertIsDisplayed()
    }
}
