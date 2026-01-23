package com.artboard.ui.layers.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Layer
import com.artboard.haptics.HapticCategory
import com.artboard.haptics.HapticIntensity
import com.artboard.haptics.rememberHapticFeedback

/**
 * ENHANCED LayerCard with integrated haptic feedback
 * 
 * This is an example of how to integrate haptics into the existing LayerCard.
 * 
 * Haptic additions:
 * - Medium haptic on layer selection (onClick)
 * - Heavy haptic on long press
 * - Light haptic on visibility toggle
 * - Medium haptic on lock toggle
 * - Light haptic on blend mode click
 * - Medium haptic on swipe actions (delete/duplicate)
 * 
 * To integrate into LayerCard.kt:
 * 1. Add rememberHapticFeedback() at top of composable
 * 2. Add haptics.perform() calls before each action
 * 3. Test with different haptic settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerCardWithHaptics(
    layer: Layer,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlendModeClick: () -> Unit,
    onSwipeDelete: () -> Unit,
    onSwipeDuplicate: () -> Unit,
    modifier: Modifier = Modifier
) {
    // NEW: Haptic feedback manager
    val haptics = rememberHapticFeedback()
    
    var showOpacitySlider by remember { mutableStateOf(false) }
    var swipeOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Background actions revealed by swipe
        SwipeActionsBackground(
            swipeOffset = swipeOffset,
            onDeleteClick = {
                // NEW: Warning haptic for destructive action
                haptics.perform(HapticIntensity.WARNING, HapticCategory.LAYER)
                onSwipeDelete()
            },
            onDuplicateClick = {
                // NEW: Medium haptic for duplicate
                haptics.perform(HapticIntensity.MEDIUM, HapticCategory.LAYER)
                onSwipeDuplicate()
            }
        )
        
        // Main card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = with(density) { swipeOffset.toDp() })
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // NEW: Light haptic on drag start
                            haptics.perform(HapticIntensity.LIGHT, HapticCategory.LAYER)
                        },
                        onDragEnd = {
                            // Check if threshold reached
                            when {
                                swipeOffset <= -100.dp.toPx() -> {
                                    haptics.perform(HapticIntensity.WARNING, HapticCategory.LAYER)
                                    onSwipeDelete()
                                }
                                swipeOffset >= 100.dp.toPx() -> {
                                    haptics.perform(HapticIntensity.MEDIUM, HapticCategory.LAYER)
                                    onSwipeDuplicate()
                                }
                                else -> {
                                    swipeOffset = 0f // Spring back
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        swipeOffset = (swipeOffset + dragAmount).coerceIn(
                            -120.dp.toPx(),
                            120.dp.toPx()
                        )
                    }
                }
                .combinedClickable(
                    onClick = {
                        // NEW: Medium haptic on layer select
                        haptics.perform(HapticIntensity.MEDIUM, HapticCategory.LAYER)
                        onClick()
                    },
                    onLongClick = {
                        // NEW: Heavy haptic on long press
                        haptics.perform(HapticIntensity.HEAVY, HapticCategory.LAYER)
                        onLongPress()
                    }
                ),
            shape = RoundedCornerShape(8.dp),
            color = if (isActive) Color(0xFF2A4A6A) else Color(0xFF242424),
            border = if (isActive) BorderStroke(2.dp, Color(0xFF4A90E2)) else null,
            shadowElevation = if (isActive) 2.dp else 0.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail
                    LayerThumbnail(layer = layer)
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Layer info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = layer.name,
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Blend mode chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF333333),
                                modifier = Modifier.clickable {
                                    // NEW: Light haptic on blend mode click
                                    haptics.perform(HapticIntensity.LIGHT, HapticCategory.LAYER)
                                    onBlendModeClick()
                                }
                            ) {
                                Text(
                                    text = layer.blendMode.displayName(),
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = Color(0xFF888888)
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            
                            Text(text = "·", style = TextStyle(fontSize = 10.sp, color = Color(0xFF666666)))
                            
                            // Opacity indicator
                            Text(
                                text = "${(layer.opacity * 100).toInt()}%",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = Color(0xFFAAAAAA)
                                ),
                                modifier = Modifier.clickable {
                                    // NEW: Light haptic on opacity slider toggle
                                    haptics.perform(HapticIntensity.LIGHT, HapticCategory.LAYER)
                                    showOpacitySlider = !showOpacitySlider
                                }
                            )
                        }
                    }
                    
                    // Controls
                    LayerControlsWithHaptics(
                        layer = layer,
                        onVisibilityToggle = onVisibilityToggle,
                        onLockToggle = onLockToggle,
                        onMoreClick = onLongPress,
                        haptics = haptics
                    )
                }
                
                // Expandable opacity slider
                AnimatedVisibility(
                    visible = showOpacitySlider,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    OpacitySliderWithHaptics(
                        opacity = layer.opacity,
                        onOpacityChange = onOpacityChange,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        haptics = haptics
                    )
                }
            }
        }
    }
}

/**
 * Layer thumbnail with checkerboard background
 */
@Composable
private fun LayerThumbnail(layer: Layer) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
    ) {
        layer.thumbnail?.let { thumb ->
            Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Layer control buttons with haptic feedback
 */
@Composable
private fun LayerControlsWithHaptics(
    layer: Layer,
    onVisibilityToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onMoreClick: () -> Unit,
    haptics: com.artboard.haptics.HapticFeedbackManager
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Visibility toggle
        IconButton(
            onClick = {
                // NEW: Light haptic for visibility toggle
                haptics.perform(HapticIntensity.LIGHT, HapticCategory.LAYER)
                onVisibilityToggle()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (layer.isVisible) "Hide" else "Show",
                tint = if (layer.isVisible) Color.White else Color(0xFF666666),
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Lock toggle
        IconButton(
            onClick = {
                // NEW: Medium haptic for lock toggle (important action)
                haptics.perform(HapticIntensity.MEDIUM, HapticCategory.LAYER)
                onLockToggle()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (layer.isLocked) "Unlock" else "Lock",
                tint = if (layer.isLocked) Color(0xFFFF9800) else Color(0xFF666666),
                modifier = Modifier.size(20.dp)
            )
        }
        
        // More options
        IconButton(
            onClick = {
                // NEW: Medium haptic for more menu
                haptics.perform(HapticIntensity.MEDIUM, HapticCategory.LAYER)
                onMoreClick()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Swipe actions background (revealed when swiping)
 */
@Composable
private fun SwipeActionsBackground(
    swipeOffset: Float,
    onDeleteClick: () -> Unit,
    onDuplicateClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Delete (red)
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFCC0000))
                .clickable(onClick = onDeleteClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Right: Duplicate (blue)
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF4A90E2))
                .clickable(onClick = onDuplicateClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Duplicate",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Opacity slider with haptic ticks
 */
@Composable
fun OpacitySliderWithHaptics(
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    haptics: com.artboard.haptics.HapticFeedbackManager
) {
    // NEW: Slider haptic with 10% threshold
    val sliderHaptic = com.artboard.haptics.rememberSliderHaptic(
        initialValue = opacity,
        threshold = 0.1f,
        intensity = HapticIntensity.LIGHT,
        category = HapticCategory.LAYER
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.Opacity,
            contentDescription = null,
            tint = Color(0xFFAAAAAA),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Slider(
            value = opacity,
            onValueChange = { newValue ->
                // NEW: Automatic haptic with threshold
                sliderHaptic.onValueChange(newValue)
                onOpacityChange(newValue)
            },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4A90E2),
                activeTrackColor = Color(0xFF4A90E2),
                inactiveTrackColor = Color(0xFF444444)
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "${(opacity * 100).toInt()}%",
            style = TextStyle(
                fontSize = 12.sp,
                color = Color(0xFFAAAAAA)
            ),
            modifier = Modifier.width(40.dp)
        )
    }
}
