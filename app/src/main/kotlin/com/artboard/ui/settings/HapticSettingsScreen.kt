package com.artboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.haptics.HapticFeedbackManager
import com.artboard.haptics.HapticIntensity
import com.artboard.haptics.HapticSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Haptic feedback settings screen
 * 
 * Allows users to:
 * - Enable/disable haptics globally
 * - Toggle individual haptic categories
 * - Test haptic feedback
 * - Reset to defaults
 * 
 * Based on HAPTIC_FEEDBACK.md specification
 */
@Composable
fun HapticSettingsScreen(
    onBackPressed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = remember { HapticFeedbackManager.getInstance(context) }
    
    var settings by remember { mutableStateOf(HapticSettings.load(context)) }
    
    // Save settings whenever they change
    LaunchedEffect(settings) {
        HapticSettings.save(context, settings)
        haptics.reloadSettings()
    }
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Haptic Feedback",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Customize vibration feedback for interactions",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Master toggle
            item {
                HapticSettingCard {
                    SwitchPreference(
                        title = "Enable Haptics",
                        description = "Master toggle for all haptic feedback",
                        checked = settings.hapticsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                haptics.perform(HapticIntensity.MEDIUM)
                            }
                            settings = settings.copy(hapticsEnabled = enabled)
                        }
                    )
                }
            }
            
            // Device compatibility info
            if (!haptics.isHapticAvailable()) {
                item {
                    InfoCard(
                        text = "⚠️ This device does not have a vibration motor. Haptic feedback will not work.",
                        color = Color(0xFFFF9800)
                    )
                }
            }
            
            // Category toggles (only shown if haptics enabled)
            if (settings.hapticsEnabled) {
                item {
                    Text(
                        text = "Haptic Categories",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                
                item {
                    HapticSettingCard {
                        SwitchPreference(
                            title = "Button Haptics",
                            description = "Feedback when tapping buttons and controls",
                            checked = settings.buttonHaptics,
                            onCheckedChange = {
                                haptics.perform(HapticIntensity.LIGHT)
                                settings = settings.copy(buttonHaptics = it)
                            }
                        )
                    }
                }
                
                item {
                    HapticSettingCard {
                        SwitchPreference(
                            title = "Slider Haptics",
                            description = "Ticks when adjusting sliders (color, brush size)",
                            checked = settings.sliderHaptics,
                            onCheckedChange = {
                                haptics.perform(HapticIntensity.LIGHT)
                                settings = settings.copy(sliderHaptics = it)
                            }
                        )
                    }
                }
                
                item {
                    HapticSettingCard {
                        SwitchPreference(
                            title = "Gesture Haptics",
                            description = "Feedback for multi-finger gestures (undo, redo)",
                            checked = settings.gestureHaptics,
                            onCheckedChange = {
                                haptics.perform(HapticIntensity.MEDIUM)
                                settings = settings.copy(gestureHaptics = it)
                            }
                        )
                    }
                }
                
                item {
                    HapticSettingCard {
                        SwitchPreference(
                            title = "Transform Haptics",
                            description = "Snap points for zoom, rotation, and transforms",
                            checked = settings.transformHaptics,
                            onCheckedChange = {
                                haptics.perform(HapticIntensity.LIGHT)
                                settings = settings.copy(transformHaptics = it)
                            }
                        )
                    }
                }
                
                item {
                    HapticSettingCard {
                        SwitchPreference(
                            title = "Layer Haptics",
                            description = "Feedback for layer operations",
                            checked = settings.layerHaptics,
                            onCheckedChange = {
                                haptics.perform(HapticIntensity.MEDIUM)
                                settings = settings.copy(layerHaptics = it)
                            }
                        )
                    }
                }
                
                item {
                    HapticSettingCard {
                        SwitchPreference(
                            title = "File Operation Haptics",
                            description = "Success and error feedback for saves/exports",
                            checked = settings.fileHaptics,
                            onCheckedChange = {
                                haptics.perform(HapticIntensity.MEDIUM)
                                settings = settings.copy(fileHaptics = it)
                            }
                        )
                    }
                }
                
                item {
                    HapticSettingCard {
                        SwitchPreference(
                            title = "Action Feedback",
                            description = "Success/warning/error vibrations",
                            checked = settings.feedbackHaptics,
                            onCheckedChange = {
                                if (it) {
                                    haptics.performSuccess()
                                } else {
                                    haptics.perform(HapticIntensity.LIGHT)
                                }
                                settings = settings.copy(feedbackHaptics = it)
                            }
                        )
                    }
                }
            }
            
            // Test button
            if (settings.hapticsEnabled) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                // Test each intensity level
                                haptics.perform(HapticIntensity.LIGHT)
                                delay(300)
                                haptics.perform(HapticIntensity.MEDIUM)
                                delay(300)
                                haptics.perform(HapticIntensity.HEAVY)
                                delay(300)
                                haptics.performSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🎵 Test Haptic Feedback",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        text = "Plays light, medium, heavy, and success haptics",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // Reset button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        haptics.perform(HapticIntensity.MEDIUM)
                        settings = HapticSettings()
                        HapticSettings.reset(context)
                        haptics.reloadSettings()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Reset to Defaults",
                        fontSize = 14.sp
                    )
                }
            }
            
            // Info section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                InfoCard(
                    text = "💡 Haptic feedback adds a tactile dimension to your interactions. " +
                            "You can disable specific categories while keeping others enabled.",
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

/**
 * Card container for haptic settings
 */
@Composable
private fun HapticSettingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2A2A2A),
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/**
 * Switch preference row
 */
@Composable
private fun SwitchPreference(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF424242)
            )
        )
    }
}

/**
 * Info card with colored background
 */
@Composable
private fun InfoCard(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color.White,
            lineHeight = 18.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}
