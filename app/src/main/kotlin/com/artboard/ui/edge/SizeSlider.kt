package com.artboard.ui.edge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Brush size slider control with logarithmic scaling.
 * 
 * Features:
 * - Range: 1px to 500px (logarithmic scale for better control at small sizes)
 * - Icon at top: small circle (represents minimum size)
 * - Icon at bottom: large circle (represents maximum size)
 * - Live preview of brush size near thumb
 * - Real-time size updates
 * 
 * Based on design/components/EdgeControlBar.md and TOOL_ORGANIZATION_PHILOSOPHY.md
 * 
 * @param sizePx Current brush size in pixels (1-500)
 * @param onSizeChange Callback when size changes (real-time)
 * @param modifier Modifier for the component
 * @param height Total height of the slider
 * @param minSize Minimum brush size in pixels
 * @param maxSize Maximum brush size in pixels
 * @param enabled Whether the slider is interactive
 */
@Composable
fun SizeSlider(
    sizePx: Float,
    onSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    minSize: Float = 1f,
    maxSize: Float = 500f,
    enabled: Boolean = true
) {
    // Convert pixel size to normalized value (0-1) using logarithmic scale
    val normalizedValue = sizeToNormalized(sizePx, minSize, maxSize)
    
    VerticalSlider(
        value = normalizedValue,
        onValueChange = { normalized ->
            val newSize = normalizedToSize(normalized, minSize, maxSize)
            onSizeChange(newSize)
        },
        modifier = modifier,
        height = height,
        enabled = enabled,
        topIcon = {
            SizeIcon(
                sizeFraction = 0.3f,
                color = EdgeControlColors.IconInactive
            )
        },
        bottomIcon = {
            SizeIcon(
                sizeFraction = 1f,
                color = EdgeControlColors.IconInactive
            )
        },
        formatValue = { normalized ->
            val size = normalizedToSize(normalized, minSize, maxSize)
            "${size.roundToInt()}px"
        }
    )
}

/**
 * Size indicator icon - a circle that represents brush size.
 * 
 * @param sizeFraction Fraction of max size (0.0 to 1.0)
 * @param color Circle color
 */
@Composable
private fun SizeIcon(
    sizeFraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(20.dp)
    ) {
        val maxRadius = size.minDimension / 2 - 2.dp.toPx()
        val minRadius = 2.dp.toPx()
        val radius = minRadius + (maxRadius - minRadius) * sizeFraction
        
        // Draw filled circle
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(size.width / 2, size.height / 2)
        )
    }
}

/**
 * Brush size preview circle that appears near the slider thumb.
 * Shows a visual representation of the current brush size.
 * 
 * @param sizePx Current brush size in pixels
 * @param maxDisplaySize Maximum display size for the preview
 * @param color Preview circle color
 */
@Composable
fun BrushSizePreview(
    sizePx: Float,
    modifier: Modifier = Modifier,
    maxDisplaySize: Dp = 40.dp,
    color: Color = EdgeControlColors.Accent.copy(alpha = 0.5f)
) {
    Canvas(modifier = modifier.size(maxDisplaySize)) {
        // Scale the size for display (cap at maxDisplaySize)
        val displayRadius = (sizePx / 2).coerceAtMost(maxDisplaySize.toPx() / 2 - 2.dp.toPx())
        
        // Draw the preview circle
        if (sizePx <= 1f) {
            // Very small - draw as a dot
            drawCircle(
                color = color,
                radius = 1.dp.toPx(),
                center = Offset(size.width / 2, size.height / 2)
            )
        } else if (displayRadius > 0) {
            // Draw filled circle with stroke
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = displayRadius,
                center = Offset(size.width / 2, size.height / 2)
            )
            drawCircle(
                color = color,
                radius = displayRadius,
                center = Offset(size.width / 2, size.height / 2),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

/**
 * Convert brush size in pixels to normalized value (0-1) using logarithmic scale.
 * 
 * Logarithmic scale provides better control at small brush sizes where
 * precision is more important, while still allowing large sizes.
 * 
 * @param sizePx Size in pixels
 * @param minSize Minimum size in pixels
 * @param maxSize Maximum size in pixels
 * @return Normalized value between 0 and 1
 */
private fun sizeToNormalized(sizePx: Float, minSize: Float, maxSize: Float): Float {
    // Clamp to valid range
    val clampedSize = sizePx.coerceIn(minSize, maxSize)
    
    // Use logarithmic scale: normalized = log(size - min + 1) / log(max - min + 1)
    val logMin = ln(1f)  // ln(1) = 0
    val logMax = ln(maxSize - minSize + 1)
    val logValue = ln(clampedSize - minSize + 1)
    
    return ((logValue - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
}

/**
 * Convert normalized value (0-1) to brush size in pixels using logarithmic scale.
 * 
 * @param normalized Normalized value between 0 and 1
 * @param minSize Minimum size in pixels
 * @param maxSize Maximum size in pixels
 * @return Size in pixels
 */
private fun normalizedToSize(normalized: Float, minSize: Float, maxSize: Float): Float {
    // Clamp normalized value
    val clampedNormalized = normalized.coerceIn(0f, 1f)
    
    // Inverse logarithmic scale: size = exp(normalized * log(max - min + 1)) + min - 1
    val logMax = ln(maxSize - minSize + 1)
    val logValue = clampedNormalized * logMax
    
    return (exp(logValue) + minSize - 1).coerceIn(minSize, maxSize)
}

/**
 * Preset brush sizes for quick selection.
 */
object BrushSizePresets {
    val TINY = 1f
    val SMALL = 5f
    val MEDIUM = 20f
    val LARGE = 50f
    val XLARGE = 100f
    val HUGE = 200f
    
    val ALL = listOf(TINY, SMALL, MEDIUM, LARGE, XLARGE, HUGE)
}
