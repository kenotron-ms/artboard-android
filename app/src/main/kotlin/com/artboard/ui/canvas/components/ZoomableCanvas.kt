package com.artboard.ui.canvas.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Wrapper that adds pinch-zoom and pan support to canvas
 * Uses hardware acceleration for smooth zooming
 */
@Composable
fun ZoomableCanvas(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val view = LocalView.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Update scale with bounds
                    val newScale = (scale * zoom).coerceIn(0.1f, 10f)
                    
                    // Haptic feedback at snap points (1x, 2x, 4x)
                    val snapPoints = listOf(1f, 2f, 4f)
                    snapPoints.forEach { snap ->
                        if (abs(scale - snap) > 0.05f && abs(newScale - snap) < 0.05f) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                    
                    scale = newScale
                    
                    // Apply pan
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            content()
        }
        
        // Zoom indicator (shows current zoom %)
        if (abs(scale - 1f) > 0.01f) {
            ZoomIndicator(
                zoom = scale,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/**
 * Small indicator showing current zoom level
 */
@Composable
fun ZoomIndicator(
    zoom: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xCC000000),
        shadowElevation = 2.dp
    ) {
        Text(
            text = "${(zoom * 100).toInt()}%",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
