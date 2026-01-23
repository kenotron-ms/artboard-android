package com.artboard.haptics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlin.math.abs

/**
 * Composable helper to remember HapticFeedbackManager instance
 * 
 * Usage:
 * ```kotlin
 * val haptics = rememberHapticFeedback()
 * Button(onClick = { 
 *     haptics.perform(HapticIntensity.MEDIUM)
 *     // ... action
 * })
 * ```
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackManager {
    val context = LocalContext.current
    return remember(context) {
        HapticFeedbackManager.getInstance(context)
    }
}

/**
 * Perform haptic feedback in a Composable context
 * 
 * Usage:
 * ```kotlin
 * Button(onClick = {
 *     performHaptic(HapticIntensity.MEDIUM)
 *     // ... action
 * })
 * ```
 */
@Composable
fun performHaptic(
    intensity: HapticIntensity,
    category: HapticCategory = HapticCategory.BUTTON
) {
    val haptics = rememberHapticFeedback()
    LaunchedEffect(Unit) {
        haptics.perform(intensity, category)
    }
}

/**
 * Modifier extension for clickable with haptic feedback
 * 
 * Usage:
 * ```kotlin
 * Box(
 *     modifier = Modifier.hapticClickable { /* action */ }
 * )
 * ```
 */
fun Modifier.hapticClickable(
    enabled: Boolean = true,
    intensity: HapticIntensity = HapticIntensity.MEDIUM,
    category: HapticCategory = HapticCategory.BUTTON,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = composed {
    val haptics = rememberHapticFeedback()
    
    this.clickable(
        enabled = enabled,
        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        indication = null
    ) {
        haptics.perform(intensity, category)
        onClick()
    }
}

/**
 * Modifier extension for tap gestures with haptic feedback
 * 
 * Usage:
 * ```kotlin
 * Box(
 *     modifier = Modifier.hapticTap(
 *         onTap = { /* action */ },
 *         onLongPress = { /* action */ }
 *     )
 * )
 * ```
 */
fun Modifier.hapticTap(
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    tapIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    longPressIntensity: HapticIntensity = HapticIntensity.HEAVY,
    category: HapticCategory = HapticCategory.BUTTON
): Modifier = composed {
    val haptics = rememberHapticFeedback()
    
    this.pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                onTap?.let {
                    haptics.perform(tapIntensity, category)
                    it()
                }
            },
            onLongPress = {
                onLongPress?.let {
                    haptics.perform(longPressIntensity, category)
                    it()
                }
            },
            onDoubleTap = {
                onDoubleTap?.let {
                    haptics.perform(tapIntensity, category)
                    it()
                }
            }
        )
    }
}

/**
 * State holder for slider haptic feedback
 * Tracks value changes and triggers haptic at threshold
 * 
 * Usage:
 * ```kotlin
 * val sliderHaptic = rememberSliderHaptic(threshold = 0.1f)
 * Slider(
 *     value = value,
 *     onValueChange = { newValue ->
 *         sliderHaptic.onValueChange(newValue)
 *         setValue(newValue)
 *     }
 * )
 * ```
 */
@Composable
fun rememberSliderHaptic(
    initialValue: Float = 0f,
    threshold: Float = 0.1f,
    intensity: HapticIntensity = HapticIntensity.LIGHT,
    category: HapticCategory = HapticCategory.SLIDER
): SliderHapticState {
    val haptics = rememberHapticFeedback()
    
    return remember(threshold) {
        SliderHapticState(
            initialValue = initialValue,
            threshold = threshold,
            intensity = intensity,
            category = category,
            haptics = haptics
        )
    }
}

/**
 * State class for slider haptic feedback
 */
class SliderHapticState(
    initialValue: Float,
    private val threshold: Float,
    private val intensity: HapticIntensity,
    private val category: HapticCategory,
    private val haptics: HapticFeedbackManager
) {
    private var lastHapticValue by mutableStateOf(initialValue)
    
    /**
     * Call this in onValueChange to trigger haptic when threshold crossed
     */
    fun onValueChange(newValue: Float) {
        if (abs(newValue - lastHapticValue) >= threshold) {
            haptics.perform(intensity, category)
            lastHapticValue = newValue
        }
    }
    
    /**
     * Reset the last haptic value (e.g., when slider is released)
     */
    fun reset(value: Float) {
        lastHapticValue = value
    }
}

/**
 * Haptic extension for success feedback
 * 
 * Usage:
 * ```kotlin
 * LaunchedEffect(saveSuccess) {
 *     if (saveSuccess) {
 *         performSuccessHaptic()
 *     }
 * }
 * ```
 */
@Composable
fun performSuccessHaptic() {
    val haptics = rememberHapticFeedback()
    LaunchedEffect(Unit) {
        haptics.performSuccess()
    }
}

/**
 * Haptic extension for warning feedback
 * 
 * Usage:
 * ```kotlin
 * LaunchedEffect(error) {
 *     if (error != null) {
 *         performWarningHaptic()
 *     }
 * }
 * ```
 */
@Composable
fun performWarningHaptic() {
    val haptics = rememberHapticFeedback()
    LaunchedEffect(Unit) {
        haptics.performWarning()
    }
}

/**
 * Haptic extension for snap point feedback
 * 
 * Usage:
 * ```kotlin
 * LaunchedEffect(isAtSnapPoint) {
 *     if (isAtSnapPoint) {
 *         performSnapPointHaptic()
 *     }
 * }
 * ```
 */
@Composable
fun performSnapPointHaptic() {
    val haptics = rememberHapticFeedback()
    LaunchedEffect(Unit) {
        haptics.performSnapPoint()
    }
}

/**
 * Observe snap points and trigger haptic when reached
 * 
 * Usage:
 * ```kotlin
 * val snapHaptic = rememberSnapPointHaptic(
 *     snapPoints = listOf(0f, 0.5f, 1f),
 *     tolerance = 0.05f
 * )
 * 
 * // In zoom/transform handler
 * snapHaptic.checkSnapPoint(currentValue)
 * ```
 */
@Composable
fun rememberSnapPointHaptic(
    snapPoints: List<Float>,
    tolerance: Float = 0.05f
): SnapPointHapticState {
    val haptics = rememberHapticFeedback()
    
    return remember(snapPoints, tolerance) {
        SnapPointHapticState(
            snapPoints = snapPoints,
            tolerance = tolerance,
            haptics = haptics
        )
    }
}

/**
 * State class for snap point haptic feedback
 */
class SnapPointHapticState(
    private val snapPoints: List<Float>,
    private val tolerance: Float,
    private val haptics: HapticFeedbackManager
) {
    private var lastSnapPoint: Float? = null
    
    /**
     * Check if value is at a snap point and trigger haptic
     * 
     * @param value Current value to check
     * @return The snap point if snapped, null otherwise
     */
    fun checkSnapPoint(value: Float): Float? {
        val nearestSnap = snapPoints.firstOrNull { snapPoint ->
            abs(value - snapPoint) < tolerance
        }
        
        if (nearestSnap != null && nearestSnap != lastSnapPoint) {
            haptics.performSnapPoint()
            lastSnapPoint = nearestSnap
        } else if (nearestSnap == null) {
            lastSnapPoint = null
        }
        
        return nearestSnap
    }
    
    /**
     * Reset snap point tracking
     */
    fun reset() {
        lastSnapPoint = null
    }
}

/**
 * Haptic feedback for layer operations
 * 
 * Usage:
 * ```kotlin
 * IconButton(
 *     onClick = {
 *         performLayerHaptic()
 *         onToggleVisibility()
 *     }
 * )
 * ```
 */
@Composable
fun performLayerHaptic(intensity: HapticIntensity = HapticIntensity.MEDIUM) {
    val haptics = rememberHapticFeedback()
    LaunchedEffect(Unit) {
        haptics.performLayerOperation(intensity)
    }
}

/**
 * Haptic feedback for file operations
 * 
 * Usage:
 * ```kotlin
 * LaunchedEffect(saveResult) {
 *     performFileHaptic(success = saveResult.isSuccess)
 * }
 * ```
 */
@Composable
fun performFileHaptic(success: Boolean) {
    val haptics = rememberHapticFeedback()
    LaunchedEffect(Unit) {
        haptics.performFileOperation(success)
    }
}
