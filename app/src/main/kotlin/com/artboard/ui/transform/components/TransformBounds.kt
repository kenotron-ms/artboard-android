package com.artboard.ui.transform.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.artboard.data.model.Handle
import com.artboard.data.model.Transform
import com.artboard.data.model.TransformType
import com.artboard.ui.transform.TransformGestureHandler
import kotlin.math.abs

/**
 * Visual bounds overlay with corner/edge handles for transform operations
 * Shows dashed outline, handles, and rotation indicator
 */
@Composable
fun TransformBounds(
    bounds: Rect,
    transform: Transform,
    transformType: TransformType,
    onTransformChange: (Transform) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeHandle by remember { mutableStateOf(Handle.NONE) }
    val view = LocalView.current
    val gestureHandler = remember { TransformGestureHandler(view) }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(transformType) {
                with(gestureHandler) {
                    detectTransformGestures(
                        transformType = transformType,
                        currentTransform = transform,
                        onTransformChange = onTransformChange,
                        isInsideBounds = { position ->
                            isPointInsideBounds(position, bounds, transform)
                        },
                        getActiveHandle = { position ->
                            detectHandle(position, bounds, transform)
                        }
                    )
                }
            }
    ) {
        // Draw dimmed background (outside bounds)
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )
        
        // Calculate transformed bounds
        val transformedBounds = applyTransformToBounds(bounds, transform)
        
        // Clear bounds area (show content)
        drawRect(
            color = Color.Transparent,
            topLeft = transformedBounds.topLeft,
            size = Size(transformedBounds.width, transformedBounds.height),
            blendMode = BlendMode.Clear
        )
        
        // Draw dashed outline
        drawDashedBorder(transformedBounds, transform)
        
        // Draw corner and edge handles
        drawHandles(transformedBounds, transformType)
        
        // Draw rotation indicator
        if (transformType == TransformType.FREE && abs(transform.rotation) > 0.1f) {
            drawRotationIndicator(transformedBounds, transform.rotation)
        }
    }
}

/**
 * Draw dashed border around bounds
 */
private fun DrawScope.drawDashedBorder(bounds: Rect, transform: Transform) {
    val path = Path().apply {
        moveTo(bounds.left, bounds.top)
        lineTo(bounds.right, bounds.top)
        lineTo(bounds.right, bounds.bottom)
        lineTo(bounds.left, bounds.bottom)
        close()
    }
    
    rotate(transform.rotation, pivot = bounds.center) {
        drawPath(
            path = path,
            color = Color(0xFF4A90E2), // Accent blue
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(10f, 10f),
                    0f
                ),
                cap = StrokeCap.Round
            )
        )
    }
}

/**
 * Draw corner and edge handles
 */
private fun DrawScope.drawHandles(bounds: Rect, transformType: TransformType) {
    val handleSize = 24.dp.toPx()
    val handleRadius = handleSize / 2
    val borderWidth = 3.dp.toPx()
    
    val handles = when (transformType) {
        TransformType.FREE, TransformType.UNIFORM -> listOf(
            Handle.TOP_LEFT to bounds.topLeft,
            Handle.TOP_RIGHT to Offset(bounds.right, bounds.top),
            Handle.BOTTOM_LEFT to Offset(bounds.left, bounds.bottom),
            Handle.BOTTOM_RIGHT to Offset(bounds.right, bounds.bottom)
        )
        TransformType.DISTORT -> listOf(
            // Include all 8 handles for distort mode
            Handle.TOP_LEFT to bounds.topLeft,
            Handle.TOP to Offset(bounds.center.x, bounds.top),
            Handle.TOP_RIGHT to Offset(bounds.right, bounds.top),
            Handle.RIGHT to Offset(bounds.right, bounds.center.y),
            Handle.BOTTOM_RIGHT to Offset(bounds.right, bounds.bottom),
            Handle.BOTTOM to Offset(bounds.center.x, bounds.bottom),
            Handle.BOTTOM_LEFT to Offset(bounds.left, bounds.bottom),
            Handle.LEFT to Offset(bounds.left, bounds.center.y)
        )
        else -> emptyList()
    }
    
    handles.forEach { (handle, position) ->
        // Draw white border
        drawCircle(
            color = Color.White,
            radius = handleRadius,
            center = position,
            style = Stroke(width = borderWidth)
        )
        
        // Draw blue fill
        drawCircle(
            color = Color(0xFF4A90E2),
            radius = handleRadius - borderWidth,
            center = position
        )
    }
}

/**
 * Draw rotation indicator (curved arrow showing angle)
 */
private fun DrawScope.drawRotationIndicator(bounds: Rect, rotation: Float) {
    val center = bounds.center
    val radius = 40.dp.toPx()
    
    // Draw arc showing rotation angle
    val startAngle = -90f
    val sweepAngle = rotation
    
    val path = Path().apply {
        arcTo(
            rect = Rect(
                center.x - radius,
                center.y - radius,
                center.x + radius,
                center.y + radius
            ),
            startAngleDegrees = startAngle,
            sweepAngleDegrees = sweepAngle,
            forceMoveTo = true
        )
    }
    
    drawPath(
        path = path,
        color = Color(0xFF4A90E2),
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
    
    // Draw rotation angle text (optional)
    // Could use drawIntoCanvas with Paint and TextPaint here
}

/**
 * Apply transform to bounds rectangle
 */
private fun applyTransformToBounds(bounds: Rect, transform: Transform): Rect {
    // For now, return bounds with translation applied
    // Full rotation/scale would require matrix transformation of all corners
    return bounds.translate(transform.translation)
}

/**
 * Check if point is inside transformed bounds
 */
private fun isPointInsideBounds(point: Offset, bounds: Rect, transform: Transform): Boolean {
    val transformedBounds = applyTransformToBounds(bounds, transform)
    return transformedBounds.contains(point)
}

/**
 * Detect which handle (if any) is at the given position
 */
private fun detectHandle(point: Offset, bounds: Rect, transform: Transform): Handle {
    val transformedBounds = applyTransformToBounds(bounds, transform)
    val handleTouchRadius = 32.dp.value // Touch area slightly larger than visual
    
    val handles = listOf(
        Handle.TOP_LEFT to transformedBounds.topLeft,
        Handle.TOP_RIGHT to Offset(transformedBounds.right, transformedBounds.top),
        Handle.BOTTOM_LEFT to Offset(transformedBounds.left, transformedBounds.bottom),
        Handle.BOTTOM_RIGHT to Offset(transformedBounds.right, transformedBounds.bottom),
        Handle.TOP to Offset(transformedBounds.center.x, transformedBounds.top),
        Handle.BOTTOM to Offset(transformedBounds.center.x, transformedBounds.bottom),
        Handle.LEFT to Offset(transformedBounds.left, transformedBounds.center.y),
        Handle.RIGHT to Offset(transformedBounds.right, transformedBounds.center.y)
    )
    
    // Find closest handle within touch radius
    return handles.firstOrNull { (_, position) ->
        val distance = (point - position).getDistance()
        distance <= handleTouchRadius
    }?.first ?: Handle.NONE
}

/**
 * Calculate scale percentage for display
 */
fun calculateScalePercentage(transform: Transform): Int {
    val effectiveScale = if (transform.scale != 1f) {
        transform.scale
    } else {
        (transform.scaleX + transform.scaleY) / 2f
    }
    return (effectiveScale * 100).toInt()
}

/**
 * Format rotation angle for display
 */
fun formatRotationAngle(rotation: Float): String {
    val normalized = rotation % 360f
    return "${normalized.toInt()}°"
}
