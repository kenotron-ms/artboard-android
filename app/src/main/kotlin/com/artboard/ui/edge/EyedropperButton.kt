package com.artboard.ui.edge

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants

/**
 * Eyedropper/Color picker shortcut button.
 * 
 * Features:
 * - 48×48dp touch target (Material Design minimum)
 * - Circular button positioned between size and opacity sliders
 * - Eyedropper/pipette icon (24×24dp)
 * - Tap: Activates eyedropper mode
 * - Shows current color as background tint
 * - Press animation with haptic feedback
 * 
 * Based on design/components/EdgeControlBar.md and TOOL_ORGANIZATION_PHILOSOPHY.md
 * 
 * @param currentColor The current brush color (shown as background tint)
 * @param isActive Whether eyedropper mode is currently active
 * @param onClick Callback when button is tapped
 * @param modifier Modifier for the component
 * @param enabled Whether the button is interactive
 */
@Composable
fun EyedropperButton(
    currentColor: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animated scale for press effect
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.9f
            isActive -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "buttonScale"
    )
    
    // Animated background color
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive -> EdgeControlColors.Accent
            else -> calculateTintedBackground(currentColor)
        },
        label = "buttonBackground"
    )
    
    // Animated icon color
    val iconColor by animateColorAsState(
        targetValue = when {
            isActive -> Color.White
            isLightColor(currentColor) -> Color(0xFF2D2D2D)
            else -> Color.White
        },
        label = "iconColor"
    )
    
    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) EdgeControlColors.Accent else Color(0x33FFFFFF),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Eyedropper icon
        EyedropperIcon(
            color = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Eyedropper/pipette icon drawn with Canvas.
 * 
 * The icon depicts a pipette tool commonly used for color picking.
 */
@Composable
private fun EyedropperIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val iconSize = size.minDimension
        
        // Rotate the icon 45 degrees for a more natural pipette angle
        rotate(degrees = -45f, pivot = Offset(iconSize / 2, iconSize / 2)) {
            // Pipette body (elongated shape)
            val bodyPath = Path().apply {
                // Top bulb
                moveTo(iconSize * 0.5f, iconSize * 0.15f)
                lineTo(iconSize * 0.35f, iconSize * 0.3f)
                lineTo(iconSize * 0.35f, iconSize * 0.5f)
                
                // Tip
                lineTo(iconSize * 0.5f, iconSize * 0.85f)
                
                // Right side
                lineTo(iconSize * 0.65f, iconSize * 0.5f)
                lineTo(iconSize * 0.65f, iconSize * 0.3f)
                close()
            }
            
            drawPath(
                path = bodyPath,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Dropper tip detail
            drawLine(
                color = color,
                start = Offset(iconSize * 0.5f, iconSize * 0.7f),
                end = Offset(iconSize * 0.5f, iconSize * 0.85f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            
            // Bulb top
            drawLine(
                color = color,
                start = Offset(iconSize * 0.4f, iconSize * 0.15f),
                end = Offset(iconSize * 0.6f, iconSize * 0.15f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Calculate a tinted background color based on the current brush color.
 * The background shows a subtle hint of the current color.
 */
private fun calculateTintedBackground(color: Color): Color {
    return color.copy(alpha = 0.3f)
}

/**
 * Determine if a color is light (for contrast calculations).
 * Uses relative luminance formula.
 */
private fun isLightColor(color: Color): Boolean {
    // Calculate relative luminance
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance > 0.5f
}

/**
 * Eyedropper button with color swatch preview.
 * Extended version that shows a larger color preview below the button.
 * 
 * @param currentColor The current brush color
 * @param isActive Whether eyedropper mode is active
 * @param onClick Callback when button is tapped
 * @param showColorPreview Whether to show the color preview swatch
 * @param modifier Modifier for the component
 */
@Composable
fun EyedropperButtonWithPreview(
    currentColor: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    showColorPreview: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EyedropperButton(
            currentColor = currentColor,
            isActive = isActive,
            onClick = onClick
        )
        
        if (showColorPreview) {
            // Color swatch preview
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(2.dp, CircleShape),
                shape = CircleShape,
                color = currentColor
            ) {
                // Checkerboard background for transparency
                if (currentColor.alpha < 1f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val checkerSize = 4.dp.toPx()
                        val cols = (size.width / checkerSize).toInt() + 1
                        val rows = (size.height / checkerSize).toInt() + 1
                        
                        for (row in 0 until rows) {
                            for (col in 0 until cols) {
                                val isLight = (row + col) % 2 == 0
                                drawRect(
                                    color = if (isLight) Color.White else Color(0xFFCCCCCC),
                                    topLeft = Offset(col * checkerSize, row * checkerSize),
                                    size = androidx.compose.ui.geometry.Size(checkerSize, checkerSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
