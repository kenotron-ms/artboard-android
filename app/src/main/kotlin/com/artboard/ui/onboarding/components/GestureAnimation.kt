package com.artboard.ui.onboarding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Container for gesture demonstration with title and description
 */
@Composable
fun GestureDemo(
    title: String,
    description: String,
    animation: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animation preview (80×80 box)
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFF242424), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            animation()
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFFAAAAAA)
                )
            )
        }
    }
}

/**
 * Animated demonstration of two-finger tap gesture (for undo)
 */
@Composable
fun TwoFingerTapAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "two_finger_tap")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "two_finger_alpha"
    )
    
    // Show two finger tap gesture
    Canvas(modifier = Modifier.size(60.dp)) {
        val fingerRadius = 12.dp.toPx()
        val spacing = size.width * 0.2f
        
        drawCircle(
            color = Color(0xFF4A90E2).copy(alpha = alpha),
            radius = fingerRadius,
            center = Offset(size.width * 0.4f, size.height * 0.5f)
        )
        
        drawCircle(
            color = Color(0xFF4A90E2).copy(alpha = alpha),
            radius = fingerRadius,
            center = Offset(size.width * 0.6f, size.height * 0.5f)
        )
    }
}

/**
 * Animated demonstration of four-finger tap gesture (for UI toggle)
 */
@Composable
fun FourFingerTapAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "four_finger_tap")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "four_finger_alpha"
    )
    
    // Show four finger tap gesture in a square pattern
    Canvas(modifier = Modifier.size(60.dp)) {
        val fingerRadius = 10.dp.toPx()
        val offset = size.width * 0.15f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        
        // Top-left
        drawCircle(
            color = Color(0xFF4A90E2).copy(alpha = alpha),
            radius = fingerRadius,
            center = Offset(centerX - offset, centerY - offset)
        )
        
        // Top-right
        drawCircle(
            color = Color(0xFF4A90E2).copy(alpha = alpha),
            radius = fingerRadius,
            center = Offset(centerX + offset, centerY - offset)
        )
        
        // Bottom-left
        drawCircle(
            color = Color(0xFF4A90E2).copy(alpha = alpha),
            radius = fingerRadius,
            center = Offset(centerX - offset, centerY + offset)
        )
        
        // Bottom-right
        drawCircle(
            color = Color(0xFF4A90E2).copy(alpha = alpha),
            radius = fingerRadius,
            center = Offset(centerX + offset, centerY + offset)
        )
    }
}

/**
 * Animated demonstration of pinch-to-zoom gesture
 */
@Composable
fun PinchZoomAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pinch_zoom")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pinch_scale"
    )
    
    Canvas(modifier = Modifier.size(60.dp)) {
        val fingerRadius = 12.dp.toPx()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseDistance = size.width * 0.15f
        val distance = baseDistance * scale
        
        // Left finger
        drawCircle(
            color = Color(0xFF4A90E2),
            radius = fingerRadius,
            center = Offset(centerX - distance, centerY)
        )
        
        // Right finger
        drawCircle(
            color = Color(0xFF4A90E2),
            radius = fingerRadius,
            center = Offset(centerX + distance, centerY)
        )
        
        // Small square in center showing zoom effect
        val squareSize = 10.dp.toPx() * scale
        drawRect(
            color = Color(0xFF4A90E2).copy(alpha = 0.3f),
            topLeft = Offset(centerX - squareSize / 2, centerY - squareSize / 2),
            size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
        )
    }
}

/**
 * Animated demonstration of long-press gesture (for eyedropper)
 */
@Composable
fun LongPressAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "long_press")
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "long_press_progress"
    )
    
    Canvas(modifier = Modifier.size(60.dp)) {
        val fingerRadius = 14.dp.toPx()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        
        // Expanding ring showing hold duration
        val ringAlpha = if (progress < 0.8f) progress / 0.8f else 1f - ((progress - 0.8f) / 0.2f)
        val ringRadius = fingerRadius + (20.dp.toPx() * progress)
        
        drawCircle(
            color = Color(0xFF4A90E2).copy(alpha = ringAlpha * 0.3f),
            radius = ringRadius,
            center = Offset(centerX, centerY)
        )
        
        // Finger
        drawCircle(
            color = Color(0xFF4A90E2),
            radius = fingerRadius,
            center = Offset(centerX, centerY)
        )
        
        // Small circle in center (eyedropper indicator)
        if (progress > 0.8f) {
            drawCircle(
                color = Color(0xFFFF5722),
                radius = 4.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }
    }
}

/**
 * Animated demonstration of pan/drag gesture
 */
@Composable
fun PanGestureAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pan_gesture")
    
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pan_offset"
    )
    
    Canvas(modifier = Modifier.size(60.dp)) {
        val fingerRadius = 12.dp.toPx()
        val startX = size.width * 0.3f
        val endX = size.width * 0.7f
        val y = size.height * 0.5f
        
        val currentX = startX + (endX - startX) * offset
        
        // Trail
        drawLine(
            color = Color(0xFF4A90E2).copy(alpha = 0.3f),
            start = Offset(startX, y),
            end = Offset(currentX, y),
            strokeWidth = 4.dp.toPx()
        )
        
        // Finger
        drawCircle(
            color = Color(0xFF4A90E2),
            radius = fingerRadius,
            center = Offset(currentX, y)
        )
    }
}

/**
 * Animated demonstration of rotation gesture
 */
@Composable
fun RotationGestureAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation_gesture")
    
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )
    
    Canvas(modifier = Modifier.size(60.dp)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.width * 0.3f
        val fingerRadius = 10.dp.toPx()
        
        // Two fingers rotating around center
        val angle1Rad = Math.toRadians(angle.toDouble())
        val angle2Rad = Math.toRadians((angle + 180).toDouble())
        
        val x1 = centerX + (radius * cos(angle1Rad)).toFloat()
        val y1 = centerY + (radius * sin(angle1Rad)).toFloat()
        
        val x2 = centerX + (radius * cos(angle2Rad)).toFloat()
        val y2 = centerY + (radius * sin(angle2Rad)).toFloat()
        
        // Center square
        val squareSize = 15.dp.toPx()
        drawRect(
            color = Color(0xFF4A90E2).copy(alpha = 0.3f),
            topLeft = Offset(centerX - squareSize / 2, centerY - squareSize / 2),
            size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
        )
        
        // Fingers
        drawCircle(
            color = Color(0xFF4A90E2),
            radius = fingerRadius,
            center = Offset(x1, y1)
        )
        
        drawCircle(
            color = Color(0xFF4A90E2),
            radius = fingerRadius,
            center = Offset(x2, y2)
        )
    }
}
