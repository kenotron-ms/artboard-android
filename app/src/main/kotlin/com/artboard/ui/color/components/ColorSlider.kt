package com.artboard.ui.color.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import kotlin.math.abs

/**
 * Custom slider with live gradient background.
 * Shows the full range of values with a gradient track.
 * 
 * @param label Label text (e.g., "H", "S", "B", "R", "G", "B", "A")
 * @param value Current value (0-1 for most, 0-360 for hue)
 * @param valueRange Range of values
 * @param onValueChange Callback when value changes
 * @param gradient Brush for the gradient background
 * @param showCheckerboard Whether to show checkerboard background (for alpha)
 * @param modifier Modifier for the component
 */
@Composable
fun ColorSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
    gradient: Brush,
    showCheckerboard: Boolean = false,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var lastHapticValue = value
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 24.dp)
    ) {
        // Label
        Text(
            text = label,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFAAAAAA)
            ),
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Slider track with gradient
        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
        ) {
            // Checkerboard for alpha slider
            if (showCheckerboard) {
                CheckerboardBackground(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            
            // Gradient track
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = gradient,
                    size = size,
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            
            // Slider (transparent track, white thumb)
            Slider(
                value = value,
                onValueChange = { newValue ->
                    onValueChange(newValue)
                    
                    // Haptic feedback every 5%
                    if (abs(newValue - lastHapticValue) >= 0.05f) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        lastHapticValue = newValue
                    }
                },
                valueRange = valueRange,
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Value display
        Text(
            text = formatValue(label, value, valueRange),
            style = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFFAAAAAA)
            ),
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Format value based on label type
 */
private fun formatValue(label: String, value: Float, range: ClosedFloatingPointRange<Float>): String {
    return when (label) {
        "H" -> "${value.toInt()}°"
        "R", "G", "B" -> "${(value * 255).toInt()}"
        else -> "${(value * 100).toInt()}%"
    }
}

/**
 * Checkerboard background for alpha channel visualization
 */
@Composable
fun CheckerboardBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val squareSize = 8.dp.toPx()
        val cols = (size.width / squareSize).toInt() + 1
        val rows = (size.height / squareSize).toInt() + 1
        
        for (row in 0..rows) {
            for (col in 0..cols) {
                val isWhite = (row + col) % 2 == 0
                drawRect(
                    color = if (isWhite) Color(0xFFFFFFFF) else Color(0xFFCCCCCC),
                    topLeft = Offset(col * squareSize, row * squareSize),
                    size = Size(squareSize, squareSize)
                )
            }
        }
    }
}
