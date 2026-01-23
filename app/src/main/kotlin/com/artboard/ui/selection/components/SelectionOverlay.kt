package com.artboard.ui.selection.components

import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.artboard.data.model.SelectionMask

/**
 * Selection overlay with animated marching ants
 * Renders the selection boundary with a continuously animating dashed line
 * 
 * Performance target: 60 FPS
 * Animation: 8px dash, 8px gap, moving 2px per frame at 60 FPS (200ms cycle)
 */
@Composable
fun SelectionOverlay(
    selectionMask: SelectionMask?,
    modifier: Modifier = Modifier,
    showOverlay: Boolean = true,
    overlayColor: Color = Color(0x404A90E2)  // 25% blue tint
) {
    if (selectionMask == null || selectionMask.isEmpty()) return
    
    // Marching ants animation phase
    // Animate from 0 to 16 (dash + gap = 8 + 8 = 16)
    // Duration: 200ms for full cycle at 60 FPS
    val infiniteTransition = rememberInfiniteTransition(label = "marching_ants")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 200,  // 200ms cycle = smooth 60 FPS
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "marching_ants_phase"
    )
    
    val bounds = remember(selectionMask) {
        selectionMask?.getBounds()
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        if (bounds == null || bounds.isEmpty()) return@Canvas
        
        // Convert Android Rect to Compose Rect
        val rect = Rect(
            left = bounds.left.toFloat(),
            top = bounds.top.toFloat(),
            right = bounds.right.toFloat(),
            bottom = bounds.bottom.toFloat()
        )
        
        // Draw semi-transparent overlay on selected area (optional)
        if (showOverlay) {
            drawRect(
                color = overlayColor,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                blendMode = BlendMode.SrcOver
            )
        }
        
        // Draw marching ants border
        drawRect(
            color = Color(0xFF4A90E2),  // Blue accent color
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(8f, 8f),  // 8px dash, 8px gap
                    phase = phase  // Animated offset
                )
            )
        )
    }
}

/**
 * Lasso tool preview overlay
 * Shows the path being drawn in real-time
 */
@Composable
fun LassoPreviewOverlay(
    path: Path,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4A90E2)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawPath(
            path = path.asComposePath(),
            color = color,
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8f, 4f),
                    0f
                )
            )
        )
    }
}

/**
 * Rectangle tool preview overlay
 * Shows the rectangle being drawn
 */
@Composable
fun RectanglePreviewOverlay(
    rect: RectF,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4A90E2)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            color = color,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width(), rect.height()),
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8f, 4f),
                    0f
                )
            )
        )
    }
}

/**
 * Ellipse tool preview overlay
 * Shows the ellipse being drawn
 */
@Composable
fun EllipsePreviewOverlay(
    rect: RectF,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4A90E2)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawOval(
            color = color,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width(), rect.height()),
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8f, 4f),
                    0f
                )
            )
        )
    }
}

/**
 * Convert Android Path to Compose Path
 */
private fun Path.asComposePath(): androidx.compose.ui.graphics.Path {
    return androidx.compose.ui.graphics.Path().apply {
        // This is a simplified conversion
        // For production, you might want to use AndroidPath or a proper conversion
        val pathIterator = this@asComposePath.approximate(0.5f)
        
        var i = 0
        while (i < pathIterator.size) {
            val x = pathIterator[i + 1]
            val y = pathIterator[i + 2]
            
            if (i == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
            
            i += 3  // Each point is [fraction, x, y]
        }
    }
}

/**
 * Selection mode indicator
 * Shows current selection tool and settings
 */
@Composable
fun SelectionModeIndicator(
    toolName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = Color(0xE0000000),  // 88% black
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        androidx.compose.material3.Text(
            text = "Selection: $toolName",
            color = Color.White,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Selection bounds indicator with dimensions
 * Shows width x height of selection
 */
@Composable
fun SelectionBoundsIndicator(
    bounds: android.graphics.Rect,
    modifier: Modifier = Modifier
) {
    if (bounds.isEmpty) return
    
    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = Color(0xE0000000),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        androidx.compose.material3.Text(
            text = "${bounds.width()} × ${bounds.height()}",
            color = Color.White,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// Extension to convert dp to px in Compose
private val Float.dp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(this)
