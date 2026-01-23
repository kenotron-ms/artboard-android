package com.artboard.ui.color.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.ui.color.ColorUtils

/**
 * Bottom action row for the color picker popover.
 * 
 * Design spec:
 * - Current color preview (48×48dp)
 * - Previous color (tap to swap)
 * - Eyedropper button
 * - Optional: Hex display
 * 
 * Layout:
 * ```
 * [Current] ⇄ [Previous] [◉]   Actions
 * ```
 * 
 * @param currentColor Currently selected color
 * @param previousColor Previously selected color
 * @param onSwapColors Callback to swap current and previous colors
 * @param onEyedropperClick Callback when eyedropper is tapped
 * @param showHex Whether to show hex value below current color
 * @param modifier Modifier for the component
 */
@Composable
fun ColorQuickActions(
    currentColor: Color,
    previousColor: Color,
    onSwapColors: () -> Unit,
    onEyedropperClick: () -> Unit,
    showHex: Boolean = true,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Current + Swap + Previous
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current color swatch (larger, primary)
            CurrentColorSwatch(
                color = currentColor,
                showHex = showHex
            )
            
            // Swap button
            SwapButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onSwapColors()
                }
            )
            
            // Previous color swatch (smaller, secondary)
            PreviousColorSwatch(
                color = previousColor,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onSwapColors()
                }
            )
        }
        
        // Right side: Eyedropper
        EyedropperButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onEyedropperClick()
            }
        )
    }
}

/**
 * Current color swatch with hex value (48×48dp)
 */
@Composable
private fun CurrentColorSwatch(
    color: Color,
    showHex: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .border(
                    width = 2.dp,
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp)
                )
        )
        
        if (showHex) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ColorUtils.toHex(color),
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
        }
    }
}

/**
 * Previous color swatch (32×32dp, tappable to swap)
 */
@Composable
private fun PreviousColorSwatch(
    color: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "previous_scale"
    )
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable {
                isPressed = true
                onClick()
                isPressed = false
            }
    )
}

/**
 * Swap button between current and previous colors
 */
@Composable
private fun SwapButton(
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swap_scale"
    )
    
    Box(
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .clickable {
                isPressed = true
                onClick()
                isPressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = "Swap colors",
            tint = Color(0xFF4A90E2),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Eyedropper button for picking colors from canvas
 */
@Composable
private fun EyedropperButton(
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "eyedropper_scale"
    )
    
    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3A3A3C),
                        Color(0xFF2C2C2E)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .clickable {
                isPressed = true
                onClick()
                isPressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Colorize,
            contentDescription = "Pick color from canvas",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
