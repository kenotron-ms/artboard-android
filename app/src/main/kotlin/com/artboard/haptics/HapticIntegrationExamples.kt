package com.artboard.haptics

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Integration examples for Haptic Feedback system
 * 
 * Demonstrates proper usage across different UI patterns:
 * - Buttons with haptic feedback
 * - Sliders with tick feedback
 * - Gestures with confirmation haptics
 * - Layer operations
 * - File operations
 * - Transform snap points
 * 
 * Based on HAPTIC_FEEDBACK.md specification
 */

// ============================================================================
// EXAMPLE 1: Button with Haptic Feedback
// ============================================================================

/**
 * Standard button with medium haptic feedback on tap
 * 
 * Usage: All primary action buttons in the app
 */
@Composable
fun HapticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val haptics = rememberHapticFeedback()
    
    Button(
        onClick = {
            haptics.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2196F3)
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text)
    }
}

// ============================================================================
// EXAMPLE 2: Icon Button with Haptic
// ============================================================================

/**
 * Icon button with light haptic feedback
 * 
 * Usage: Toolbar icons, layer visibility toggle, etc.
 */
@Composable
fun HapticIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    intensity: HapticIntensity = HapticIntensity.LIGHT,
    enabled: Boolean = true
) {
    val haptics = rememberHapticFeedback()
    
    IconButton(
        onClick = {
            haptics.perform(intensity, HapticCategory.BUTTON)
            onClick()
        },
        modifier = modifier,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.Gray
        )
    }
}

// ============================================================================
// EXAMPLE 3: Slider with Haptic Ticks
// ============================================================================

/**
 * Slider with haptic tick every 10% change
 * 
 * Usage: Brush size, opacity, color channels
 */
@Composable
fun HapticSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    threshold: Float = 0.1f
) {
    val sliderHaptic = rememberSliderHaptic(
        initialValue = value,
        threshold = threshold,
        intensity = HapticIntensity.LIGHT,
        category = HapticCategory.SLIDER
    )
    
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Slider(
                value = value,
                onValueChange = { newValue ->
                    sliderHaptic.onValueChange(newValue)
                    onValueChange(newValue)
                },
                modifier = Modifier.weight(1f),
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2196F3),
                    activeTrackColor = Color(0xFF2196F3),
                    inactiveTrackColor = Color(0xFF424242)
                )
            )
            
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.width(48.dp)
            )
        }
    }
}

// ============================================================================
// EXAMPLE 4: Color Slider with Fine Ticks (5% threshold)
// ============================================================================

/**
 * Color channel slider with finer haptic ticks
 * 
 * Usage: RGB/HSV color sliders
 */
@Composable
fun ColorChannelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    color: Color
) {
    val sliderHaptic = rememberSliderHaptic(
        initialValue = value,
        threshold = 0.05f, // 5% threshold for finer control
        intensity = HapticIntensity.LIGHT
    )
    
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Slider(
            value = value,
            onValueChange = { newValue ->
                sliderHaptic.onValueChange(newValue)
                onValueChange(newValue)
            },
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0xFF424242)
            )
        )
    }
}

// ============================================================================
// EXAMPLE 5: Layer Visibility Toggle
// ============================================================================

/**
 * Layer visibility toggle with light haptic
 * 
 * Usage: Layer panel visibility icon
 */
@Composable
fun LayerVisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticFeedback()
    
    IconButton(
        onClick = {
            haptics.perform(HapticIntensity.LIGHT, HapticCategory.LAYER)
            onToggle()
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = if (visible) "Hide layer" else "Show layer",
            tint = if (visible) Color.White else Color.Gray
        )
    }
}

// ============================================================================
// EXAMPLE 6: Layer Lock Toggle
// ============================================================================

/**
 * Layer lock toggle with medium haptic (important action)
 * 
 * Usage: Layer panel lock icon
 */
@Composable
fun LayerLockToggle(
    locked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticFeedback()
    
    IconButton(
        onClick = {
            haptics.perform(HapticIntensity.MEDIUM, HapticCategory.LAYER)
            onToggle()
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = if (locked) "Unlock layer" else "Lock layer",
            tint = if (locked) Color(0xFFFF9800) else Color.Gray
        )
    }
}

// ============================================================================
// EXAMPLE 7: Delete Button (Destructive Action)
// ============================================================================

/**
 * Delete button with warning haptic
 * 
 * Usage: Delete layer, delete project, clear canvas
 */
@Composable
fun DeleteButton(
    text: String = "Delete",
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticFeedback()
    
    Button(
        onClick = {
            haptics.perform(HapticIntensity.WARNING, HapticCategory.FEEDBACK)
            onDelete()
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE53935)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

// ============================================================================
// EXAMPLE 8: Save Operation with Success Haptic
// ============================================================================

/**
 * Save operation example with success/error haptic
 * 
 * Usage: Save project, export image
 */
@Composable
fun SaveOperationExample(
    onSave: suspend () -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    Button(
        onClick = {
            scope.launch {
                isLoading = true
                val result = onSave()
                isLoading = false
                
                if (result.isSuccess) {
                    haptics.performFileOperation(success = true)
                } else {
                    haptics.performFileOperation(success = false)
                }
            }
        },
        modifier = modifier,
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save")
        }
    }
}

// ============================================================================
// EXAMPLE 9: Zoom Snap Points
// ============================================================================

/**
 * Zoom control with snap point haptics
 * 
 * Usage: Canvas zoom (25%, 50%, 100%, 200%)
 */
@Composable
fun ZoomControlExample(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val snapHaptic = rememberSnapPointHaptic(
        snapPoints = listOf(0.25f, 0.5f, 1f, 2f, 4f),
        tolerance = 0.05f
    )
    
    Column(modifier = modifier) {
        Text(
            text = "Zoom: ${(zoom * 100).toInt()}%",
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Slider(
            value = zoom,
            onValueChange = { newZoom ->
                val snappedZoom = snapHaptic.checkSnapPoint(newZoom) ?: newZoom
                onZoomChange(snappedZoom)
            },
            valueRange = 0.1f..10f
        )
        
        // Quick zoom buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            QuickZoomButton("25%", 0.25f, onZoomChange)
            QuickZoomButton("50%", 0.5f, onZoomChange)
            QuickZoomButton("100%", 1f, onZoomChange)
            QuickZoomButton("200%", 2f, onZoomChange)
        }
    }
}

@Composable
private fun QuickZoomButton(
    label: String,
    zoomValue: Float,
    onZoomChange: (Float) -> Unit
) {
    val haptics = rememberHapticFeedback()
    
    Button(
        onClick = {
            haptics.perform(HapticIntensity.LIGHT, HapticCategory.TRANSFORM)
            onZoomChange(zoomValue)
        },
        modifier = Modifier.size(60.dp, 36.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(label, fontSize = 12.sp)
    }
}

// ============================================================================
// EXAMPLE 10: Two-Finger Undo Gesture Integration
// ============================================================================

/**
 * Canvas area with gesture haptic integration
 * 
 * Usage: Main canvas with undo/redo gestures
 */
@Composable
fun CanvasWithGestureHaptics(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptics = rememberHapticFeedback()
    
    // This would be integrated with your actual GestureHandler
    // Shown here as an example of how to trigger haptics from gestures
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                // In actual implementation, this connects to GestureHandler
                // When gesture detected:
                // - Two-finger tap -> haptics.perform(MEDIUM, GESTURE) + onUndo()
                // - Three-finger tap -> haptics.perform(MEDIUM, GESTURE) + onRedo()
            }
    ) {
        content()
    }
}

// ============================================================================
// EXAMPLE 11: Layer Drag and Drop with Haptic
// ============================================================================

/**
 * Layer card with drag haptic feedback
 * 
 * Usage: Layer panel drag reordering
 */
@Composable
fun DraggableLayerCard(
    layerName: String,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticFeedback()
    var isDragging by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        haptics.perform(HapticIntensity.MEDIUM, HapticCategory.LAYER)
                        isDragging = true
                        onDragStart()
                    },
                    onDragEnd = {
                        haptics.perform(HapticIntensity.MEDIUM, HapticCategory.LAYER)
                        isDragging = false
                        onDragEnd()
                    },
                    onDrag = { _, _ ->
                        // Dragging (no haptic during drag)
                    }
                )
            },
        color = if (isDragging) Color(0xFF424242) else Color(0xFF2A2A2A),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DragHandle, contentDescription = "Drag to reorder", tint = Color.Gray)
            Spacer(modifier = Modifier.width(12.dp))
            Text(layerName, color = Color.White)
        }
    }
}

// ============================================================================
// EXAMPLE 12: Switch with Haptic
// ============================================================================

/**
 * Switch toggle with medium haptic
 * 
 * Usage: Settings toggles, feature flags
 */
@Composable
fun HapticSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticFeedback()
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White)
        
        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                haptics.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
                onCheckedChange(newValue)
            }
        )
    }
}

// ============================================================================
// EXAMPLE 13: Floating Action Button with Haptic
// ============================================================================

/**
 * FAB with medium haptic feedback
 * 
 * Usage: New layer, new project buttons
 */
@Composable
fun HapticFab(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val haptics = rememberHapticFeedback()
    
    FloatingActionButton(
        onClick = {
            haptics.perform(HapticIntensity.MEDIUM, HapticCategory.BUTTON)
            onClick()
        },
        modifier = modifier,
        containerColor = Color(0xFF2196F3)
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

// ============================================================================
// EXAMPLE 14: Long Press with Heavy Haptic
// ============================================================================

/**
 * Long press action with heavy haptic feedback
 * 
 * Usage: Long press to activate eyedropper, show context menu
 */
@Composable
fun LongPressArea(
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptics = rememberHapticFeedback()
    
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    haptics.perform(HapticIntensity.HEAVY, HapticCategory.GESTURE)
                    onLongPress()
                }
            )
        }
    ) {
        content()
    }
}
