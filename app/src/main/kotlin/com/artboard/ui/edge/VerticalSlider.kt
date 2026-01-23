package com.artboard.ui.edge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import kotlin.math.roundToInt

/**
 * Reusable vertical slider component for edge controls.
 * 
 * Features:
 * - 48dp wide, configurable height (~200dp default)
 * - 24dp circular thumb with shadow
 * - 4dp wide track with rounded ends
 * - Real-time value callback (no "apply" needed)
 * - Haptic tick every 10% of the range
 * - Shows value tooltip while dragging
 * - Smooth 60 FPS animations
 * 
 * Based on design/components/EdgeControlBar.md specification.
 * 
 * @param value Current value (0.0 to 1.0)
 * @param onValueChange Callback when value changes (real-time)
 * @param modifier Modifier for the component
 * @param height Total height of the slider
 * @param enabled Whether the slider is interactive
 * @param topIcon Composable for icon at top of slider
 * @param bottomIcon Composable for icon at bottom of slider
 * @param formatValue Function to format value for tooltip display
 */
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    enabled: Boolean = true,
    topIcon: @Composable (() -> Unit)? = null,
    bottomIcon: @Composable (() -> Unit)? = null,
    formatValue: (Float) -> String = { "${(it * 100).roundToInt()}%" }
) {
    val view = LocalView.current
    val density = LocalDensity.current
    
    // Track dimensions
    val sliderWidth = 48.dp
    val trackWidth = 4.dp
    val thumbSize = 24.dp
    val iconSize = 20.dp
    val iconPadding = 8.dp
    
    // Calculate usable track height (excluding icons and padding)
    val trackPadding = if (topIcon != null || bottomIcon != null) {
        iconSize + iconPadding * 2
    } else {
        thumbSize / 2
    }
    
    // State for drag handling
    var isDragging by remember { mutableStateOf(false) }
    var trackHeightPx by remember { mutableStateOf(0f) }
    var lastHapticStep by remember { mutableStateOf((value * 10).toInt()) }
    
    // Animated thumb position for smooth movement
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "sliderValue"
    )
    
    // Haptic feedback helper
    fun triggerHapticIfNeeded(newValue: Float) {
        val newStep = (newValue * 10).toInt()
        if (newStep != lastHapticStep) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            lastHapticStep = newStep
        }
    }
    
    Box(
        modifier = modifier
            .width(sliderWidth)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top icon (small size indicator)
            if (topIcon != null) {
                Box(
                    modifier = Modifier
                        .size(iconSize + iconPadding * 2)
                        .padding(iconPadding),
                    contentAlignment = Alignment.Center
                ) {
                    topIcon()
                }
            }
            
            // Main slider track area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(sliderWidth)
                    .onGloballyPositioned { coordinates ->
                        trackHeightPx = coordinates.size.height.toFloat()
                    }
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        
                        detectTapGestures { offset ->
                            // Calculate value from tap position (inverted: top = 1, bottom = 0)
                            val thumbRadiusPx = with(density) { thumbSize.toPx() / 2 }
                            val usableHeight = trackHeightPx - thumbRadiusPx * 2
                            val normalizedY = ((trackHeightPx - thumbRadiusPx - offset.y) / usableHeight)
                                .coerceIn(0f, 1f)
                            
                            onValueChange(normalizedY)
                            triggerHapticIfNeeded(normalizedY)
                        }
                    }
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        
                        detectDragGestures(
                            onDragStart = { 
                                isDragging = true 
                            },
                            onDragEnd = { 
                                isDragging = false 
                            },
                            onDragCancel = { 
                                isDragging = false 
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                
                                // Calculate value from position (inverted: top = 1, bottom = 0)
                                val thumbRadiusPx = with(density) { thumbSize.toPx() / 2 }
                                val usableHeight = trackHeightPx - thumbRadiusPx * 2
                                val normalizedY = ((trackHeightPx - thumbRadiusPx - change.position.y) / usableHeight)
                                    .coerceIn(0f, 1f)
                                
                                onValueChange(normalizedY)
                                triggerHapticIfNeeded(normalizedY)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Track background
                Canvas(
                    modifier = Modifier
                        .width(trackWidth)
                        .fillMaxHeight()
                        .padding(vertical = thumbSize / 2)
                ) {
                    // Inactive track (full)
                    drawRoundRect(
                        color = EdgeControlColors.TrackInactive,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(size.width / 2)
                    )
                    
                    // Active track (filled portion)
                    val filledHeight = size.height * animatedValue
                    drawRoundRect(
                        color = EdgeControlColors.TrackActive,
                        topLeft = Offset(0f, size.height - filledHeight),
                        size = Size(size.width, filledHeight),
                        cornerRadius = CornerRadius(size.width / 2)
                    )
                }
                
                // Thumb
                val thumbOffsetY = with(density) {
                    val usableHeight = trackHeightPx - thumbSize.toPx()
                    (usableHeight * (1 - animatedValue)).roundToInt()
                }
                
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, thumbOffsetY) }
                        .align(Alignment.TopCenter)
                ) {
                    // Thumb circle with shadow
                    Surface(
                        modifier = Modifier
                            .size(thumbSize)
                            .shadow(
                                elevation = if (isDragging) 8.dp else 4.dp,
                                shape = CircleShape
                            ),
                        shape = CircleShape,
                        color = EdgeControlColors.Thumb
                    ) {}
                    
                    // Value tooltip (shows while dragging)
                    if (isDragging) {
                        ValueTooltip(
                            value = formatValue(value),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = thumbSize + 8.dp)
                        )
                    }
                }
            }
            
            // Bottom icon (large size indicator)
            if (bottomIcon != null) {
                Box(
                    modifier = Modifier
                        .size(iconSize + iconPadding * 2)
                        .padding(iconPadding),
                    contentAlignment = Alignment.Center
                ) {
                    bottomIcon()
                }
            }
        }
    }
}

/**
 * Value tooltip that appears while dragging.
 */
@Composable
private fun ValueTooltip(
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = EdgeControlColors.TooltipBackground,
        shadowElevation = 4.dp
    ) {
        Text(
            text = value,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = EdgeControlColors.TooltipText,
                textAlign = TextAlign.Center
            )
        )
    }
}

/**
 * Color constants for edge controls.
 * Based on design spec and existing theme colors.
 */
object EdgeControlColors {
    // Track colors
    val TrackInactive = Color(0xFF3A3A3C)
    val TrackActive = Color(0xFFFFFFFF)
    
    // Thumb colors
    val Thumb = Color(0xFFFFFFFF)
    val ThumbBorder = Color(0x33000000)
    
    // Accent color (for active states)
    val Accent = Color(0xFF007AFF)
    
    // Tooltip colors
    val TooltipBackground = Color(0xE6000000)
    val TooltipText = Color(0xFFFFFFFF)
    
    // Icon colors
    val IconInactive = Color(0xCC8E8E93)
    val IconActive = Color(0xFFFFFFFF)
    
    // Background colors
    val EdgeBackground = Color(0x00000000) // Transparent for canvas-first
    val EdgeBackgroundHover = Color(0x14000000)
    
    // Button colors
    val ButtonBackground = Color(0x33FFFFFF)
    val ButtonBackgroundPressed = Color(0x4DFFFFFF)
}
