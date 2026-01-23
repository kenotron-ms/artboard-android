package com.artboard.ui.color.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.artboard.ui.color.ColorUtils

/**
 * Current and previous color swatches with swap functionality.
 * Displays two large color circles with hex values and swap icon.
 * 
 * @param currentColor Currently selected color
 * @param previousColor Previously selected color
 * @param onSwapClick Callback when swap is triggered
 * @param modifier Modifier for the component
 */
@Composable
fun ColorSwatches(
    currentColor: Color,
    previousColor: Color,
    onSwapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    
    // Animation for swap
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Current color swatch
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable {
                isPressed = true
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onSwapClick()
                isPressed = false
            }
        ) {
            Text(
                text = "Current",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(2.dp, Color.White, CircleShape)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = ColorUtils.toHex(currentColor),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            )
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // Swap icon
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = "Swap colors",
            tint = Color(0xFF4A90E2),
            modifier = Modifier
                .size(32.dp)
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onSwapClick()
                }
        )
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // Previous color swatch
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable {
                isPressed = true
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onSwapClick()
                isPressed = false
            }
        ) {
            Text(
                text = "Previous",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(previousColor)
                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = ColorUtils.toHex(previousColor),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFAAAAAA)
                )
            )
        }
    }
}
