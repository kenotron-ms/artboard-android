package com.artboard.ui.color.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants
import kotlin.math.*

/**
 * Circular hue ring selector with smooth 360° spectrum.
 * Displays full HSB spectrum as a ring with white indicator.
 * 
 * @param selectedHue Currently selected hue (0-360 degrees)
 * @param onHueSelected Callback when hue is selected
 * @param modifier Modifier for the component
 */
@Composable
fun HueRing(
    selectedHue: Float,
    onHueSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isDragging by remember { mutableStateOf(false) }
    
    Canvas(
        modifier = modifier
            .size(300.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        
                        // Calculate hue from touch position
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val hue = calculateHueFromPosition(offset, center)
                        onHueSelected(hue)
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val hue = calculateHueFromPosition(change.position, center)
                        onHueSelected(hue)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val hue = calculateHueFromPosition(offset, center)
                    
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onHueSelected(hue)
                }
            }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2
        val innerRadius = outerRadius * 0.8f
        val ringWidth = outerRadius - innerRadius
        
        // Draw hue spectrum (360 degrees with smooth gradient)
        // Using 360 individual arcs to prevent banding
        for (angleDeg in 0..359) {
            val hue = angleDeg.toFloat()
            val color = Color.hsv(hue, 1f, 1f)
            
            drawArc(
                color = color,
                startAngle = angleDeg.toFloat() - 90f, // Offset by 90° to start at top
                sweepAngle = 1f,
                useCenter = false,
                style = Stroke(width = ringWidth, cap = StrokeCap.Butt),
                topLeft = Offset(
                    center.x - outerRadius,
                    center.y - outerRadius
                ),
                size = Size(outerRadius * 2, outerRadius * 2)
            )
        }
        
        // Draw selection indicator
        val selectedAngleRad = Math.toRadians((selectedHue - 90f).toDouble()) // Offset by 90° to match ring
        val indicatorRadius = (outerRadius + innerRadius) / 2
        val indicatorX = center.x + cos(selectedAngleRad).toFloat() * indicatorRadius
        val indicatorY = center.y + sin(selectedAngleRad).toFloat() * indicatorRadius
        
        val indicatorCenter = Offset(indicatorX, indicatorY)
        
        // Outer white ring (for visibility on all colors)
        drawCircle(
            color = Color.White,
            radius = 14.dp.toPx(),
            center = indicatorCenter,
            style = Stroke(width = 3.dp.toPx())
        )
        
        // Inner black ring (for contrast)
        drawCircle(
            color = Color.Black.copy(alpha = 0.6f),
            radius = 11.dp.toPx(),
            center = indicatorCenter,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Optional: Add shadow effect for depth
        if (isDragging) {
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = 16.dp.toPx(),
                center = indicatorCenter
            )
        }
    }
}

/**
 * Calculate hue from touch position
 * @param position Touch position
 * @param center Center of the ring
 * @return Hue in degrees (0-360)
 */
private fun calculateHueFromPosition(position: Offset, center: Offset): Float {
    val dx = position.x - center.x
    val dy = position.y - center.y
    
    // Calculate angle in radians
    val angleRad = atan2(dy, dx)
    
    // Convert to degrees and normalize to 0-360
    // Add 90° to offset the coordinate system (0° at top instead of right)
    val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
    
    // Ensure positive angle
    return (angleDeg + 360f) % 360f
}
