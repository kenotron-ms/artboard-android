package com.artboard.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.Color

/**
 * Centralized animation specifications for the Artboard app
 * 
 * Design Principles:
 * 1. Purposeful - Every animation communicates state or provides feedback
 * 2. Consistent - Similar actions use similar timing/easing
 * 3. Natural - Motion feels organic, not mechanical
 * 4. Fast - Never slow down workflow (200-400ms max)
 * 5. Respectful - 60 FPS always, respects reduced motion settings
 * 
 * Performance Requirements:
 * - All animations must maintain 60 FPS minimum
 * - Use GPU-accelerated properties (alpha, scale, translation, rotation)
 * - Avoid animating layout properties (width, height, padding)
 */
object ArtboardAnimations {
    
    /**
     * Standard animation durations
     * Based on Material Design motion principles
     */
    object Duration {
        /** No animation - immediate response */
        const val INSTANT = 0L
        
        /** Micro-interactions (100ms) - very quick feedback */
        const val MICRO = 100L
        
        /** Quick interactions (150ms) - button press, ripple */
        const val QUICK = 150L
        
        /** Fast transitions (200ms) - toggle, checkbox, quick state changes */
        const val FAST = 200L
        
        /** Normal transitions (300ms) - default for most UI elements */
        const val NORMAL = 300L
        
        /** Slow transitions (400ms) - emphasized actions, screen transitions */
        const val SLOW = 400L
        
        /** Deliberate transitions (600ms) - important state changes, onboarding */
        const val DELIBERATE = 600L
    }
    
    /**
     * Standard easing curves
     * Used to create natural-feeling motion
     */
    object Easing {
        /** Most natural feeling - quick start, slow end (Material Design standard) */
        val FAST_OUT_SLOW_IN = FastOutSlowInEasing
        
        /** Slow start, quick end - good for exits */
        val LINEAR_OUT_SLOW_IN = LinearOutSlowInEasing
        
        /** Decelerate to rest - natural for appearing elements */
        val EASE_OUT = CubicBezierEasing(0f, 0f, 0.58f, 1f)
        
        /** Accelerate from rest - natural for disappearing elements */
        val EASE_IN = CubicBezierEasing(0.42f, 0f, 1f, 1f)
        
        /** Smooth start and stop */
        val EASE_IN_OUT = EaseInOutCubic
        
        /** Constant speed - use for progress indicators */
        val LINEAR = LinearEasing
    }
    
    /**
     * Spring physics configurations
     * Create organic, bouncy motion for interactive elements
     */
    object Springs {
        /** Bouncy spring - playful button presses (dampingRatio ~0.5-0.6) */
        val BOUNCY = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
        
        /** Smooth spring - cards and panels (dampingRatio ~0.75) */
        val SMOOTH = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )
        
        /** Gentle spring - list reordering (dampingRatio ~0.8) */
        val GENTLE = spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
        
        /** Stiff spring - precise movements */
        val STIFF = spring<Float>(
            dampingRatio = 1f,
            stiffness = Spring.StiffnessHigh
        )
    }
    
    /**
     * Common animation specs for specific use cases
     */
    
    /** Button press scale animation (150ms with bouncy spring) */
    fun <T> buttonPress() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    /** Panel slide animation (300ms fast-out-slow-in) */
    fun <T> panelSlide() = tween<T>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.FAST_OUT_SLOW_IN
    )
    
    /** Dialog scale animation (300ms fast-out-slow-in) */
    fun <T> dialogScale() = tween<T>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.FAST_OUT_SLOW_IN
    )
    
    /** Color blend animation (300ms linear for smooth color transitions) */
    fun colorBlend() = tween<Color>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.LINEAR
    )
    
    /** Quick fade animation (200ms) */
    fun <T> quickFade() = tween<T>(
        durationMillis = Duration.FAST.toInt(),
        easing = Easing.LINEAR
    )
    
    /** Layer card reorder with spring physics */
    fun <T> layerReorder() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    /**
     * EnterTransition for fade in
     */
    fun fadeIn() = fadeIn(
        animationSpec = tween(
            durationMillis = Duration.NORMAL.toInt(),
            easing = Easing.EASE_OUT
        )
    )
    
    /**
     * ExitTransition for fade out
     */
    fun fadeOut() = fadeOut(
        animationSpec = tween(
            durationMillis = Duration.FAST.toInt(),
            easing = Easing.LINEAR_OUT_SLOW_IN
        )
    )
    
    /**
     * Toolbar auto-hide fade out with 3 second delay
     */
    fun toolbarAutoHideFadeOut() = fadeOut(
        animationSpec = tween(
            durationMillis = Duration.NORMAL.toInt(),
            delayMillis = 3000,
            easing = Easing.LINEAR
        )
    )
    
    /**
     * Toolbar show fade in (immediate, no delay)
     */
    fun toolbarShowFadeIn() = fadeIn(
        animationSpec = tween(
            durationMillis = Duration.NORMAL.toInt(),
            easing = Easing.LINEAR
        )
    )
}

/**
 * Animation scale factors
 */
object AnimationScale {
    /** Button press scale down */
    const val BUTTON_PRESS = 0.95f
    
    /** Icon button press scale down */
    const val ICON_BUTTON_PRESS = 0.9f
    
    /** Card selection scale up */
    const val CARD_SELECTED = 1.05f
    
    /** Dialog initial scale (appears from 80% to 100%) */
    const val DIALOG_INITIAL = 0.8f
    
    /** Panel initial scale (appears from 90% to 100%) */
    const val PANEL_INITIAL = 0.9f
    
    /** Pulse animation range */
    const val PULSE_MIN = 1f
    const val PULSE_MAX = 1.05f
}
