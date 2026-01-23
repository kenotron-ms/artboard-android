package com.artboard.ui.color.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Compact color disc for popover picker.
 * 
 * Design spec (from EdgeControlBar.md):
 * - 200×200dp total size (fits in 280dp popover with padding)
 * - Hue ring: 20dp thick (outer)
 * - Sat/Bri square: 140×140dp (inner)
 * - Current color indicator on ring
 * - Live update as user drags
 * 
 * Layout:
 * ```
 *     ┌─────────────────┐
 *     │   ┌───────┐     │
 *  H  │   │ Sat/  │     │  H
 *  U  │   │ Bri   │     │  U
 *  E  │   │       │     │  E
 *     │   └───────┘     │
 *     └─────────────────┘
 * ```
 * 
 * @param hue Current hue (0-360 degrees)
 * @param saturation Current saturation (0-1)
 * @param brightness Current brightness (0-1)
 * @param onHueChange Callback when hue changes (live during drag)
 * @param onSatBriChange Callback when saturation/brightness changes (live during drag)
 * @param modifier Modifier for the component
 */
@Composable
fun CompactColorDisc(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onHueChange: (Float) -> Unit,
    onSatBriChange: (saturation: Float, brightness: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer hue ring
        CompactHueRing(
            selectedHue = hue,
            onHueSelected = onHueChange,
            modifier = Modifier.size(200.dp)
        )
        
        // Inner saturation/brightness square
        CompactSatBriSquare(
            hue = hue,
            saturation = saturation,
            brightness = brightness,
            onValueSelected = onSatBriChange,
            modifier = Modifier.size(140.dp)
        )
    }
}

/**
 * Compact hue ring (20dp thick, 200dp diameter)
 */
@Composable
private fun CompactHueRing(
    selectedHue: Float,
    onHueSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isDragging by remember { mutableStateOf(false) }
    
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        
                        val center = Offset(size.width / 2f, size.height / 2f)
                        // Only respond to touches in the ring area
                        if (isInHueRing(offset, center, size.width.toFloat())) {
                            val hue = calculateHueFromPosition(offset, center)
                            onHueSelected(hue)
                        }
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
                    if (isInHueRing(offset, center, size.width.toFloat())) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val hue = calculateHueFromPosition(offset, center)
                        onHueSelected(hue)
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2
        val ringWidth = 20.dp.toPx() // 20dp thick ring
        val middleRadius = outerRadius - ringWidth / 2
        
        // Draw hue spectrum (360 degrees with smooth gradient)
        for (angleDeg in 0..359) {
            val hue = angleDeg.toFloat()
            val color = Color.hsv(hue, 1f, 1f)
            
            drawArc(
                color = color,
                startAngle = angleDeg.toFloat() - 90f, // Start at top
                sweepAngle = 1.5f, // Slight overlap to prevent gaps
                useCenter = false,
                style = Stroke(width = ringWidth, cap = StrokeCap.Butt),
                topLeft = Offset(
                    center.x - middleRadius - ringWidth / 2,
                    center.y - middleRadius - ringWidth / 2
                ),
                size = Size(
                    (middleRadius + ringWidth / 2) * 2,
                    (middleRadius + ringWidth / 2) * 2
                )
            )
        }
        
        // Draw selection indicator on ring
        val selectedAngleRad = Math.toRadians((selectedHue - 90f).toDouble())
        val indicatorX = center.x + cos(selectedAngleRad).toFloat() * middleRadius
        val indicatorY = center.y + sin(selectedAngleRad).toFloat() * middleRadius
        val indicatorCenter = Offset(indicatorX, indicatorY)
        
        // Outer white circle indicator
        drawCircle(
            color = Color.White,
            radius = 10.dp.toPx(),
            center = indicatorCenter,
            style = Stroke(width = 2.5.dp.toPx())
        )
        
        // Inner shadow for contrast
        drawCircle(
            color = Color.Black.copy(alpha = 0.5f),
            radius = 7.5.dp.toPx(),
            center = indicatorCenter,
            style = Stroke(width = 1.5.dp.toPx())
        )
        
        // Glow effect when dragging
        if (isDragging) {
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = 12.dp.toPx(),
                center = indicatorCenter
            )
        }
    }
}

/**
 * Compact saturation/brightness square (140×140dp)
 */
@Composable
private fun CompactSatBriSquare(
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
        val cornerRadius = 8.dp.toPx()
        
        // Draw horizontal saturation gradient (white to pure hue)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White,
                    Color.hsv(hue, 1f, 1f)
                )
            ),
            size = size,
            cornerRadius = CornerRadius(cornerRadius)
        )
        
        // Draw vertical brightness gradient overlay (transparent to black)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black
                )
            ),
            size = size,
            cornerRadius = CornerRadius(cornerRadius)
        )
        
        // Calculate indicator position
        val x = saturation * size.width
        val y = (1f - brightness) * size.height
        val indicatorCenter = Offset(x, y)
        
        // Outer white circle indicator
        drawCircle(
            color = Color.White,
            radius = 10.dp.toPx(),
            center = indicatorCenter,
            style = Stroke(width = 2.5.dp.toPx())
        )
        
        // Inner shadow for contrast
        drawCircle(
            color = Color.Black.copy(alpha = 0.5f),
            radius = 7.5.dp.toPx(),
            center = indicatorCenter,
            style = Stroke(width = 1.5.dp.toPx())
        )
        
        // Glow effect when dragging
        if (isDragging) {
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = 12.dp.toPx(),
                center = indicatorCenter
            )
        }
        
        // Subtle border for definition
        drawRoundRect(
            color = Color.White.copy(alpha = 0.1f),
            size = size,
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * Calculate hue from touch position relative to center
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

/**
 * Check if touch position is within the hue ring area
 */
private fun isInHueRing(position: Offset, center: Offset, totalSize: Float): Boolean {
    val dx = position.x - center.x
    val dy = position.y - center.y
    val distance = sqrt(dx * dx + dy * dy)
    
    val outerRadius = totalSize / 2
    val innerRadius = outerRadius - 30.dp.value // Ring width + tolerance
    
    return distance >= innerRadius && distance <= outerRadius
}
