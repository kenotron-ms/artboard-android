package com.artboard.ui.color

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import android.view.HapticFeedbackConstants
import com.artboard.ui.color.components.*

/**
 * Full-screen color picker panel with circular HSB picker.
 * Procreate-inspired design with smooth gradients and intuitive controls.
 * 
 * @param currentColor Initial color to display
 * @param onColorSelected Callback when color is confirmed
 * @param onDismiss Callback when picker is dismissed
 */
@Composable
fun ColorPickerPanel(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val viewModel: ColorPickerViewModel = viewModel()
    val view = LocalView.current
    
    // Initialize with current color
    LaunchedEffect(Unit) {
        viewModel.initialize(currentColor)
    }
    
    // Collect state
    val hue by viewModel.hue.collectAsState()
    val saturation by viewModel.saturation.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val alpha by viewModel.alpha.collectAsState()
    val color by viewModel.currentColor.collectAsState()
    val previousColor by viewModel.previousColor.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()
    val inputMode by viewModel.inputMode.collectAsState()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xF0000000) // 94% opacity black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xFF1A1A1A))
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Colors",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    
                    // Done button
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0x1A4A90E2),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.confirmColor()
                                onColorSelected(color)
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Done",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4A90E2)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Main picker area - Hue ring with sat/bri square inside
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(356.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Hue ring
                    HueRing(
                        selectedHue = hue,
                        onHueSelected = viewModel::setHue
                    )
                    
                    // Sat/Bri square (centered inside ring)
                    SaturationBrightnessSquare(
                        hue = hue,
                        saturation = saturation,
                        brightness = brightness,
                        onValueSelected = { sat, bri ->
                            viewModel.setSaturation(sat)
                            viewModel.setBrightness(bri)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Recent colors bar
                RecentColorsBar(
                    recentColors = recentColors,
                    onColorSelected = { argb ->
                        viewModel.setColor(Color(argb))
                    }
                )
                
                // Current/Previous color swatches
                ColorSwatches(
                    currentColor = color,
                    previousColor = previousColor,
                    onSwapClick = viewModel::swapCurrentPrevious
                )
                
                // Input mode tabs
                InputModeTabs(
                    selectedMode = inputMode,
                    onModeSelected = viewModel::setInputMode
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Value sliders based on input mode
                when (inputMode) {
                    InputMode.HSB -> {
                        HSBSliders(
                            hue = hue,
                            saturation = saturation,
                            brightness = brightness,
                            alpha = alpha,
                            onHueChange = viewModel::setHue,
                            onSaturationChange = viewModel::setSaturation,
                            onBrightnessChange = viewModel::setBrightness,
                            onAlphaChange = viewModel::setAlpha
                        )
                    }
                    
                    InputMode.RGB -> {
                        RGBSliders(
                            color = color,
                            alpha = alpha,
                            onColorChange = viewModel::setColor,
                            onAlphaChange = viewModel::setAlpha
                        )
                    }
                    
                    InputMode.HEX -> {
                        HexInput(
                            color = color,
                            onColorChange = viewModel::setColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * HSB sliders (Hue, Saturation, Brightness, Alpha)
 */
@Composable
private fun HSBSliders(
    hue: Float,
    saturation: Float,
    brightness: Float,
    alpha: Float,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hue slider (0-360 degrees)
        ColorSlider(
            label = "H",
            value = hue,
            valueRange = 0f..360f,
            onValueChange = onHueChange,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color.hsv(0f, 1f, 1f),      // Red
                    Color.hsv(60f, 1f, 1f),     // Yellow
                    Color.hsv(120f, 1f, 1f),    // Green
                    Color.hsv(180f, 1f, 1f),    // Cyan
                    Color.hsv(240f, 1f, 1f),    // Blue
                    Color.hsv(300f, 1f, 1f),    // Magenta
                    Color.hsv(360f, 1f, 1f)     // Red
                )
            )
        )
        
        // Saturation slider (0-100%)
        ColorSlider(
            label = "S",
            value = saturation,
            onValueChange = onSaturationChange,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color.hsv(hue, 0f, brightness),  // White/gray
                    Color.hsv(hue, 1f, brightness)   // Pure hue
                )
            )
        )
        
        // Brightness slider (0-100%)
        ColorSlider(
            label = "B",
            value = brightness,
            onValueChange = onBrightnessChange,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color.Black,                        // Black
                    Color.hsv(hue, saturation, 1f)     // Full brightness
                )
            )
        )
        
        // Alpha slider (0-100%)
        ColorSlider(
            label = "A",
            value = alpha,
            onValueChange = onAlphaChange,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color.hsv(hue, saturation, brightness, 0f),   // Transparent
                    Color.hsv(hue, saturation, brightness, 1f)    // Opaque
                )
            ),
            showCheckerboard = true
        )
    }
}

/**
 * RGB sliders (Red, Green, Blue, Alpha)
 */
@Composable
private fun RGBSliders(
    color: Color,
    alpha: Float,
    onColorChange: (Color) -> Unit,
    onAlphaChange: (Float) -> Unit
) {
    val (r, g, b) = ColorUtils.getRgbComponents(color)
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Red slider
        ColorSlider(
            label = "R",
            value = color.red,
            onValueChange = { newRed ->
                onColorChange(color.copy(red = newRed))
            },
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(0f, color.green, color.blue),
                    Color(1f, color.green, color.blue)
                )
            )
        )
        
        // Green slider
        ColorSlider(
            label = "G",
            value = color.green,
            onValueChange = { newGreen ->
                onColorChange(color.copy(green = newGreen))
            },
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(color.red, 0f, color.blue),
                    Color(color.red, 1f, color.blue)
                )
            )
        )
        
        // Blue slider
        ColorSlider(
            label = "B",
            value = color.blue,
            onValueChange = { newBlue ->
                onColorChange(color.copy(blue = newBlue))
            },
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(color.red, color.green, 0f),
                    Color(color.red, color.green, 1f)
                )
            )
        )
        
        // Alpha slider
        ColorSlider(
            label = "A",
            value = alpha,
            onValueChange = onAlphaChange,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    color.copy(alpha = 0f),
                    color.copy(alpha = 1f)
                )
            ),
            showCheckerboard = true
        )
    }
}
