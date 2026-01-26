package com.artboard.ui.color.components

import android.graphics.SweepGradient
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Circular hue ring selector with smooth 360° spectrum.
 * Displays full HSB spectrum as a ring with white indicator.
 * 
 * Performance optimized:
 * - Uses SweepGradient shader instead of 360 separate arcs
 * - Input smoothing to reduce jitter
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
    
    // Smoothing: track last emitted hue to prevent micro-jitter
    var lastEmittedHue by remember { mutableFloatStateOf(selectedHue) }
    val hueThreshold = 0.5f // Minimum change to emit
    
    // Create the hue colors for the sweep gradient (cached)
    val hueColors = remember {
        intArrayOf(
            android.graphics.Color.HSVToColor(floatArrayOf(0f, 1f, 1f)),
            android.graphics.Color.HSVToColor(floatArrayOf(60f, 1f, 1f)),
            android.graphics.Color.HSVToColor(floatArrayOf(120f, 1f, 1f)),
            android.graphics.Color.HSVToColor(floatArrayOf(180f, 1f, 1f)),
            android.graphics.Color.HSVToColor(floatArrayOf(240f, 1f, 1f)),
            android.graphics.Color.HSVToColor(floatArrayOf(300f, 1f, 1f)),
            android.graphics.Color.HSVToColor(floatArrayOf(360f, 1f, 1f))
        )
    }
    
    val huePositions = remember {
        floatArrayOf(0f, 0.166f, 0.333f, 0.5f, 0.666f, 0.833f, 1f)
    }
    
    fun emitHueIfChanged(newHue: Float) {
        if (abs(newHue - lastEmittedHue) >= hueThreshold || 
            // Handle wraparound at 0/360
            (lastEmittedHue > 350 && newHue < 10) ||
            (lastEmittedHue < 10 && newHue > 350)) {
            lastEmittedHue = newHue
            onHueSelected(newHue)
        }
    }
    
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
                        lastEmittedHue = hue
                        onHueSelected(hue)
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val hue = calculateHueFromPosition(change.position, center)
                        emitHueIfChanged(hue)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val hue = calculateHueFromPosition(offset, center)
                    
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    lastEmittedHue = hue
                    onHueSelected(hue)
                }
            }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2
        val innerRadius = outerRadius * 0.8f
        val ringWidth = outerRadius - innerRadius
        val middleRadius = (outerRadius + innerRadius) / 2
        
        // Draw hue ring using SweepGradient (MUCH faster than 360 arcs)
        val sweepShader = SweepGradient(
            center.x, center.y,
            hueColors,
            huePositions
        )
        
        // Rotate shader so red (0°) is at top instead of right
        val shaderMatrix = android.graphics.Matrix()
        shaderMatrix.setRotate(-90f, center.x, center.y)
        sweepShader.setLocalMatrix(shaderMatrix)
        
        val ringPaint = android.graphics.Paint().apply {
            shader = sweepShader
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = ringWidth
            isAntiAlias = true
        }
        
        drawContext.canvas.nativeCanvas.drawCircle(
            center.x, center.y, middleRadius, ringPaint
        )
        
        // Draw selection indicator
        val selectedAngleRad = Math.toRadians((selectedHue - 90.0)) // Offset by 90° to match ring
        val indicatorX = center.x + cos(selectedAngleRad).toFloat() * middleRadius
        val indicatorY = center.y + sin(selectedAngleRad).toFloat() * middleRadius
        
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
        
        // Add shadow effect for depth when dragging
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
