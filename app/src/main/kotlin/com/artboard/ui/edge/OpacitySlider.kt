package com.artboard.ui.edge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Brush opacity slider control with linear scaling.
 * 
 * Features:
 * - Range: 0% to 100% (linear scale)
 * - Icon at top: solid circle (represents full opacity)
 * - Icon at bottom: faded circle (represents minimum opacity)
 * - Shows percentage while dragging
 * - Real-time opacity updates
 * 
 * Based on design/components/EdgeControlBar.md and TOOL_ORGANIZATION_PHILOSOPHY.md
 * 
 * @param opacity Current opacity (0.0 to 1.0)
 * @param onOpacityChange Callback when opacity changes (real-time)
 * @param modifier Modifier for the component
 * @param height Total height of the slider
 * @param enabled Whether the slider is interactive
 */
@Composable
fun OpacitySlider(
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    enabled: Boolean = true
) {
    VerticalSlider(
        value = opacity.coerceIn(0f, 1f),
        onValueChange = { newOpacity ->
            onOpacityChange(newOpacity.coerceIn(0f, 1f))
        },
        modifier = modifier,
        height = height,
        enabled = enabled,
        topIcon = {
            OpacityIcon(
                opacity = 1f,
                color = EdgeControlColors.IconInactive
            )
        },
        bottomIcon = {
            OpacityIcon(
                opacity = 0.2f,
                color = EdgeControlColors.IconInactive
            )
        },
        formatValue = { value ->
            "${(value * 100).roundToInt()}%"
        }
    )
}

/**
 * Opacity indicator icon - a circle with varying opacity.
 * 
 * @param opacity Visual opacity of the circle (0.0 to 1.0)
 * @param color Base circle color
 */
@Composable
private fun OpacityIcon(
    opacity: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(20.dp)
    ) {
        val radius = size.minDimension / 2 - 2.dp.toPx()
        
        // Draw checkerboard background to show transparency
        val checkerSize = 4.dp.toPx()
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        // Simple 2x2 checkerboard pattern visible through the circle
        for (row in 0..1) {
            for (col in 0..1) {
                val isLight = (row + col) % 2 == 0
                val checkerColor = if (isLight) Color(0xFFCCCCCC) else Color(0xFF999999)
                
                // Only draw checkerboard within the circle bounds (approximately)
                val x = centerX - checkerSize + col * checkerSize
                val y = centerY - checkerSize + row * checkerSize
                
                drawRect(
                    color = checkerColor.copy(alpha = 1f - opacity),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(checkerSize, checkerSize)
                )
            }
        }
        
        // Draw circle with specified opacity
        drawCircle(
            color = color.copy(alpha = opacity),
            radius = radius,
            center = Offset(centerX, centerY)
        )
    }
}

/**
 * Opacity preview overlay - shows the current opacity visually.
 * Useful for displaying alongside the slider thumb.
 * 
 * @param opacity Current opacity (0.0 to 1.0)
 * @param currentColor The current brush color to preview
 */
@Composable
fun OpacityPreview(
    opacity: Float,
    currentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val canvasSize = this.size
        val checkerSize = 4.dp.toPx()
        
        // Draw checkerboard background
        val cols = (canvasSize.width / checkerSize).toInt() + 1
        val rows = (canvasSize.height / checkerSize).toInt() + 1
        
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val isLight = (row + col) % 2 == 0
                val checkerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFFCCCCCC)
                
                drawRect(
                    color = checkerColor,
                    topLeft = Offset(col * checkerSize, row * checkerSize),
                    size = androidx.compose.ui.geometry.Size(checkerSize, checkerSize)
                )
            }
        }
        
        // Draw color with current opacity
        drawRect(
            color = currentColor.copy(alpha = opacity),
            size = canvasSize
        )
    }
}

/**
 * Preset opacity values for quick selection.
 */
object OpacityPresets {
    const val INVISIBLE = 0f
    const val VERY_LOW = 0.1f
    const val LOW = 0.25f
    const val MEDIUM = 0.5f
    const val HIGH = 0.75f
    const val FULL = 1f
    
    val ALL = listOf(INVISIBLE, VERY_LOW, LOW, MEDIUM, HIGH, FULL)
    
    /**
     * Common opacity values artists use frequently.
     */
    val COMMON = listOf(0.1f, 0.25f, 0.5f, 0.75f, 1f)
}
