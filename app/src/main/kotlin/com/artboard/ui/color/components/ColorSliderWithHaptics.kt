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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.haptics.HapticCategory
import com.artboard.haptics.HapticIntensity
import com.artboard.haptics.rememberHapticFeedback
import com.artboard.haptics.rememberSliderHaptic

/**
 * ENHANCED ColorSlider with integrated haptic feedback
 * 
 * This is an example of how to integrate haptics into the existing ColorSlider.
 * 
 * Changes from original:
 * - Uses rememberSliderHaptic() for automatic tick feedback
 * - Haptic every 5% change (threshold = 0.05f)
 * - Respects user haptic preferences via HapticSettings
 * 
 * To integrate into ColorSlider.kt:
 * 1. Import haptic functions
 * 2. Replace manual haptic code (lines 104-107) with sliderHaptic.onValueChange()
 * 3. Remove view and lastHapticValue variables
 */
@Composable
fun ColorSliderWithHaptics(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
    gradient: Brush,
    showCheckerboard: Boolean = false,
    modifier: Modifier = Modifier
) {
    // NEW: Use haptic helper instead of manual tracking
    val sliderHaptic = rememberSliderHaptic(
        initialValue = value,
        threshold = 0.05f, // 5% threshold for color sliders
        intensity = HapticIntensity.LIGHT,
        category = HapticCategory.SLIDER
    )
    
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
                    // NEW: Automatic haptic with threshold check
                    sliderHaptic.onValueChange(newValue)
                    onValueChange(newValue)
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
