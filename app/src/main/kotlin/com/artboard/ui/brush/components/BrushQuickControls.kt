package com.artboard.ui.brush.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Brush
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.abs

/**
 * Quick adjustment controls for Size, Opacity, and Flow
 * 
 * Visual specifications:
 * - Background: #1A1A1A, 4dp elevation
 * - Padding: 16dp
 * - Slider spacing: 12dp
 * - Labels: 14sp Medium #FFFFFF, 80dp width
 * - Values: 14sp Regular #AAAAAA, right-aligned
 * - Slider track: 32dp height (easy touch target)
 * - Active color: #4A90E2
 * - Inactive color: #444444
 */
@Composable
fun BrushQuickControls(
    brush: Brush,
    onBrushChanged: (Brush) -> Unit,
    onAdvancedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1A1A1A),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Size slider (1-500px, logarithmic scale)
            QuickSlider(
                label = "Size",
                value = brush.size,
                valueRange = 1f..500f,
                onValueChange = { onBrushChanged(brush.copy(size = it)) },
                valueDisplay = "${brush.size.toInt()}px",
                logarithmic = true
            )
            
            // Opacity slider (0-100%, linear)
            QuickSlider(
                label = "Opacity",
                value = brush.opacity,
                valueRange = 0f..1f,
                onValueChange = { onBrushChanged(brush.copy(opacity = it)) },
                valueDisplay = "${(brush.opacity * 100).toInt()}%",
                logarithmic = false
            )
            
            // Flow slider (0-100%, linear)
            QuickSlider(
                label = "Flow",
                value = brush.flow,
                valueRange = 0f..1f,
                onValueChange = { onBrushChanged(brush.copy(flow = it)) },
                valueDisplay = "${(brush.flow * 100).toInt()}%",
                logarithmic = false
            )
            
            // Advanced settings button
            Button(
                onClick = onAdvancedClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF242424),
                    contentColor = Color(0xFF4A90E2)
                )
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Advanced Settings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Custom slider with label and value display
 * Supports logarithmic scaling for size control
 */
@Composable
fun QuickSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: String,
    logarithmic: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        // Label
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.width(80.dp)
        )
        
        // Slider
        Slider(
            value = if (logarithmic) {
                // Convert value to logarithmic slider position (0-1)
                val logMin = log10(valueRange.start.coerceAtLeast(1f))
                val logMax = log10(valueRange.endInclusive)
                val logValue = log10(value.coerceAtLeast(1f))
                ((logValue - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
            } else {
                // Linear mapping
                ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
                    .coerceIn(0f, 1f)
            },
            onValueChange = { sliderValue ->
                val actualValue = if (logarithmic) {
                    // Convert slider position back to actual value
                    val logMin = log10(valueRange.start.coerceAtLeast(1f))
                    val logMax = log10(valueRange.endInclusive)
                    10f.pow(logMin + sliderValue * (logMax - logMin))
                } else {
                    // Linear mapping
                    valueRange.start + sliderValue * (valueRange.endInclusive - valueRange.start)
                }
                onValueChange(actualValue.coerceIn(valueRange))
            },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4A90E2),
                activeTrackColor = Color(0xFF4A90E2),
                inactiveTrackColor = Color(0xFF444444)
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Value display
        Text(
            text = valueDisplay,
            fontSize = 14.sp,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Range slider for min/max pressure dynamics
 * Used in advanced settings
 */
@Composable
fun RangeSliderControl(
    label: String,
    valueRange: ClosedFloatingPointRange<Float>,
    currentRange: ClosedFloatingPointRange<Float>,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Min: ${(currentRange.start * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
                Text(
                    text = "Max: ${(currentRange.endInclusive * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            }
        }
        
        // RangeSlider would be used here
        // Note: Material3 RangeSlider implementation
        Text(
            text = "Range slider implementation",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(8.dp)
        )
    }
}
