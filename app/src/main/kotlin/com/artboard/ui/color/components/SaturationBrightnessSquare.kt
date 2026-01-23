package com.artboard.ui.color.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants

/**
 * 2D gradient square for saturation and brightness selection.
 * Displays a gradient from white (left) to current hue (right),
 * with black overlay from top to bottom.
 * 
 * @param hue Current hue (0-360 degrees)
 * @param saturation Current saturation (0-1)
 * @param brightness Current brightness (0-1)
 * @param onValueSelected Callback when saturation/brightness is selected
 * @param modifier Modifier for the component
 */
@Composable
fun SaturationBrightnessSquare(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onValueSelected: (sat: Float, bri: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isDragging by remember { mutableStateOf(false) }
    
    Canvas(
        modifier = modifier
            .size(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        
                        val sat = (offset.x / size.width).coerceIn(0f, 1f)
                        val bri = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        onValueSelected(sat, bri)
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        val sat = (change.position.x / size.width).coerceIn(0f, 1f)
                        val bri = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        onValueSelected(sat, bri)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val sat = (offset.x / size.width).coerceIn(0f, 1f)
                    val bri = (1f - offset.y / size.height).coerceIn(0f, 1f)
                    
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onValueSelected(sat, bri)
                }
            }
    ) {
        // Draw horizontal saturation gradient (white to pure hue)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White,
                    Color.hsv(hue, 1f, 1f)
                )
            ),
            size = size
        )
        
        // Draw vertical brightness gradient overlay (transparent to black)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black
                )
            ),
            size = size
        )
        
        // Calculate indicator position
        val x = saturation * size.width
        val y = (1f - brightness) * size.height
        val indicatorCenter = Offset(x, y)
        
        // Draw selection indicator (crosshair)
        // Outer white circle
        drawCircle(
            color = Color.White,
            radius = 12.dp.toPx(),
            center = indicatorCenter,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Inner black circle (for contrast)
        drawCircle(
            color = Color.Black.copy(alpha = 0.6f),
            radius = 9.dp.toPx(),
            center = indicatorCenter,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Optional: Add glow effect when dragging
        if (isDragging) {
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = 14.dp.toPx(),
                center = indicatorCenter
            )
        }
        
        // Draw rounded corner border for polish
        drawRoundRect(
            color = Color.White.copy(alpha = 0.1f),
            size = size,
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
