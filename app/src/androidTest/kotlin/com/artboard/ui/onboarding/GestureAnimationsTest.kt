package com.artboard.ui.onboarding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artboard.ui.onboarding.components.*
import com.artboard.ui.theme.ArtboardTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for gesture animations
 * Verifies animations render without crashing
 */
@RunWith(AndroidJUnit4::class)
class GestureAnimationsTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun twoFingerTapAnimation_renders() {
        composeTestRule.setContent {
            ArtboardTheme {
                TwoFingerTapAnimation()
            }
        }
        
        // Wait for animation to render
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun fourFingerTapAnimation_renders() {
        composeTestRule.setContent {
            ArtboardTheme {
                FourFingerTapAnimation()
            }
        }
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun pinchZoomAnimation_renders() {
        composeTestRule.setContent {
            ArtboardTheme {
                PinchZoomAnimation()
            }
        }
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun longPressAnimation_renders() {
        composeTestRule.setContent {
            ArtboardTheme {
                LongPressAnimation()
            }
        }
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun gestureDemo_displaysAllComponents() {
        composeTestRule.setContent {
            ArtboardTheme {
                GestureDemo(
                    title = "Test Gesture",
                    description = "Test description",
                    animation = { TwoFingerTapAnimation() }
                )
            }
        }
        
        // Verify text is displayed
        composeTestRule
            .onNodeWithText("Test Gesture")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Test description")
            .assertIsDisplayed()
    }
}
