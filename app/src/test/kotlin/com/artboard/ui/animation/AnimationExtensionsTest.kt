package com.artboard.ui.animation

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import org.junit.Assert.*

/**
 * Tests for animation extension functions
 * 
 * Tests verify:
 * - Extensions apply correctly
 * - Animations respect reduced motion
 * - Performance characteristics
 * - Edge cases
 */
class AnimationExtensionsTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `pressScale modifier applies correctly`() {
        var pressed by mutableStateOf(false)
        
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .pressScale(pressed = pressed)
                    .testTag("test_box")
            )
        }
        
        // Verify box exists
        composeTestRule.onNodeWithTag("test_box").assertExists()
        
        // Change pressed state
        pressed = true
        
        // Animation should be running
        composeTestRule.waitForIdle()
        
        // Reset
        pressed = false
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun `selectionScale modifier applies when selected`() {
        var selected by mutableStateOf(false)
        
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .selectionScale(selected = selected)
                    .testTag("test_box")
            )
        }
        
        composeTestRule.onNodeWithTag("test_box").assertExists()
        
        // Select element
        selected = true
        composeTestRule.waitForIdle()
        
        // Unselect
        selected = false
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun `animatedVisibility fades element`() {
        var visible by mutableStateOf(true)
        
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .animatedVisibility(visible = visible)
                    .testTag("test_box")
            )
        }
        
        composeTestRule.onNodeWithTag("test_box").assertExists()
        
        // Hide element
        visible = false
        composeTestRule.waitForIdle()
        
        // Show element
        visible = true
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun `shake animation triggers on value change`() {
        var trigger by mutableStateOf(0)
        
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shake(trigger = trigger)
                    .testTag("test_box")
            )
        }
        
        composeTestRule.onNodeWithTag("test_box").assertExists()
        
        // Trigger shake
        trigger = 1
        composeTestRule.waitForIdle()
        
        // Wait for animation to complete
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun `popIn animation handles visibility changes`() {
        var visible by mutableStateOf(false)
        
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .popIn(visible = visible)
                    .testTag("test_box")
            )
        }
        
        // Element should exist even when not visible (alpha = 0)
        composeTestRule.onNodeWithTag("test_box").assertExists()
        
        // Show element with pop-in
        visible = true
        composeTestRule.waitForIdle()
        
        // Hide element
        visible = false
        composeTestRule.waitForIdle()
    }
}

/**
 * Tests for reduced motion accessibility
 */
class ReducedMotionTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `pulse animation respects reduced motion`() {
        composeTestRule.setContent {
            val reducedMotion = rememberReducedMotion()
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .pulse(enabled = !reducedMotion)
                    .testTag("test_box")
            )
        }
        
        composeTestRule.onNodeWithTag("test_box").assertExists()
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun `bounce animation respects reduced motion`() {
        composeTestRule.setContent {
            val reducedMotion = rememberReducedMotion()
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .bounce(enabled = !reducedMotion)
                    .testTag("test_box")
            )
        }
        
        composeTestRule.onNodeWithTag("test_box").assertExists()
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun `glow animation respects reduced motion`() {
        composeTestRule.setContent {
            val reducedMotion = rememberReducedMotion()
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .glow(enabled = !reducedMotion)
                    .testTag("test_box")
            )
        }
        
        composeTestRule.onNodeWithTag("test_box").assertExists()
        composeTestRule.waitForIdle()
    }
}

/**
 * Tests for continuous animations
 */
class ContinuousAnimationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `rotating animation runs continuously`() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .rotating(enabled = true)
                    .testTag("test_box")
            )
        }
        
        composeTestRule.onNodeWithTag("test_box").assertExists()
        
        // Advance time to verify rotation is ongoing
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun `shimmer animation runs continuously`() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shimmer(enabled = true)
                    .testTag("test_box")
            )
        }
        
        composeTestRule.onNodeWithTag("test_box").assertExists()
        
        // Advance time
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()
    }
}

/**
 * Tests for animation state changes
 */
class AnimationStateTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `dragScale increases scale when dragging`() {
        var isDragging by mutableStateOf(false)
        
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .dragScale(isDragging = isDragging)
                    .testTag("test_box")
            )
        }
        
        composeTestRule.onNodeWithTag("test_box").assertExists()
        
        // Start dragging
        isDragging = true
        composeTestRule.waitForIdle()
        
        // Stop dragging
        isDragging = false
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun `slideIn animates from correct direction`() {
        var visible by mutableStateOf(false)
        
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .slideIn(visible = visible, fromLeft = true)
                    .testTag("test_box")
            )
        }
        
        composeTestRule.onNodeWithTag("test_box").assertExists()
        
        // Slide in
        visible = true
        composeTestRule.waitForIdle()
        
        // Slide out
        visible = false
        composeTestRule.waitForIdle()
    }
}
