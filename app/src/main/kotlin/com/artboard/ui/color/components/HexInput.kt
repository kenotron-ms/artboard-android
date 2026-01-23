package com.artboard.ui.color.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.artboard.ui.color.ColorUtils

/**
 * Hex color input field with RGB display.
 * Allows entering color as hex (#RRGGBB format).
 * 
 * @param color Current color
 * @param onColorChange Callback when color is changed via hex input
 * @param modifier Modifier for the component
 */
@Composable
fun HexInput(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var hexText by remember { mutableStateOf(ColorUtils.toHex(color)) }
    var isEditing by remember { mutableStateOf(false) }
    
    // Update hex text when color changes externally (but not while editing)
    LaunchedEffect(color) {
        if (!isEditing) {
            hexText = ColorUtils.toHex(color)
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .background(Color(0xFF242424), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hex input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        ) {
            BasicTextField(
                value = hexText,
                onValueChange = { newText ->
                    // Allow # and hex characters only
                    val filtered = newText.filter { it == '#' || it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                    hexText = filtered.take(7) // Max 7 characters (#RRGGBB)
                    
                    // Try to parse and update color
                    if (filtered.length == 7) {
                        ColorUtils.parseHex(filtered)?.let { newColor ->
                            onColorChange(newColor)
                        }
                    }
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4A90E2)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                cursorBrush = SolidColor(Color(0xFF4A90E2)),
                modifier = Modifier.width(100.dp),
                onTextLayout = { isEditing = true },
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (hexText.isEmpty()) {
                            Text(
                                text = "#000000",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF666666)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        
        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(Color(0xFF333333))
        )
        
        // RGB display
        val (r, g, b) = ColorUtils.getRgbComponents(color)
        Text(
            text = "RGB: $r, $g, $b",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFFAAAAAA)
            )
        )
    }
}

/**
 * Alternative: Simple HSB/RGB mode tabs
 * This component allows switching between HSB and RGB sliders
 */
@Composable
fun InputModeTabs(
    selectedMode: com.artboard.ui.color.InputMode,
    onModeSelected: (com.artboard.ui.color.InputMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // HSB tab
        ModeTab(
            label = "HSB",
            isSelected = selectedMode == com.artboard.ui.color.InputMode.HSB,
            onClick = { onModeSelected(com.artboard.ui.color.InputMode.HSB) },
            modifier = Modifier.weight(1f)
        )
        
        // RGB tab
        ModeTab(
            label = "RGB",
            isSelected = selectedMode == com.artboard.ui.color.InputMode.RGB,
            onClick = { onModeSelected(com.artboard.ui.color.InputMode.RGB) },
            modifier = Modifier.weight(1f)
        )
        
        // HEX tab
        ModeTab(
            label = "HEX",
            isSelected = selectedMode == com.artboard.ui.color.InputMode.HEX,
            onClick = { onModeSelected(com.artboard.ui.color.InputMode.HEX) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    
    Box(
        modifier = modifier
            .height(36.dp)
            .background(
                color = if (isSelected) Color(0xFF4A90E2) else Color(0xFF333333),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFFAAAAAA)
            )
        )
    }
}
