package com.artboard.ui.layers

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Layer
import kotlin.math.abs

/**
 * Compact layer row for the layer panel popover.
 * 
 * Design spec:
 * - Height: 56dp (compact)
 * - Thumbnail: 48×48dp (live preview)
 * - Layer name: truncated with ellipsis
 * - Visibility toggle (eye icon)
 * - Blend mode indicator (small badge)
 * - Selected state: Blue left border (2dp)
 * 
 * Gestures:
 * - Tap thumbnail: Select layer
 * - Tap eye: Toggle visibility
 * - Swipe left: Delete action (red background)
 * - Swipe right: Duplicate action (blue background)
 * - Long-press: Show options menu
 * - Long-press + drag: Reorder (handled by parent)
 * 
 * Animations:
 * - Selection: Blue border fade in (100ms)
 * - Swipe: Spring-back or commit at threshold
 * - Delete: Slide out left (200ms) + collapse
 * - Drag: Lift with shadow elevation
 * 
 * @param layer Layer data to display
 * @param isSelected Whether this layer is currently selected
 * @param isDragging Whether this layer is being dragged for reorder
 * @param elevation Current elevation (animated during drag)
 * @param onTap Callback when layer is tapped (select)
 * @param onVisibilityToggle Callback when eye icon is tapped
 * @param onSwipeDelete Callback when swipe-left threshold reached
 * @param onSwipeDuplicate Callback when swipe-right threshold reached
 * @param onLongPress Callback when layer is long-pressed (show options)
 * @param onBlendModeClick Callback when blend mode badge is tapped
 * @param dragModifier Modifier for drag-to-reorder detection
 */
@Composable
fun CompactLayerRow(
    layer: Layer,
    isSelected: Boolean,
    isDragging: Boolean,
    elevation: Dp,
    onTap: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onSwipeDelete: () -> Unit,
    onSwipeDuplicate: () -> Unit,
    onLongPress: () -> Unit,
    onBlendModeClick: () -> Unit,
    dragModifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    
    // Swipe state
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = with(density) { 80.dp.toPx() }
    
    // Animate swipe offset for spring-back
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "swipeOffset"
    )
    
    // Selection border color animation
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF4A90E2) else Color.Transparent,
        animationSpec = tween(durationMillis = 100),
        label = "borderColor"
    )
    
    // Background color based on selection and drag state
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isDragging -> Color(0xFF3A3A3E)
            isSelected -> Color(0xFF2A3A4A)
            else -> Color(0xFF2C2C2E)
        },
        animationSpec = tween(durationMillis = 100),
        label = "backgroundColor"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        // Swipe action backgrounds (revealed when swiping)
        SwipeActionBackgrounds(
            swipeOffset = animatedSwipeOffset,
            swipeThreshold = swipeThreshold
        )
        
        // Main row content
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = with(density) { animatedSwipeOffset.toDp() })
                .shadow(elevation, RoundedCornerShape(8.dp))
                .then(dragModifier)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // Haptic on start
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        },
                        onDragEnd = {
                            when {
                                swipeOffset <= -swipeThreshold -> {
                                    // Delete threshold reached
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    onSwipeDelete()
                                }
                                swipeOffset >= swipeThreshold -> {
                                    // Duplicate threshold reached
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    onSwipeDuplicate()
                                }
                            }
                            // Spring back
                            swipeOffset = 0f
                        },
                        onDragCancel = {
                            swipeOffset = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        swipeOffset = (swipeOffset + dragAmount).coerceIn(
                            -swipeThreshold * 1.2f,
                            swipeThreshold * 1.2f
                        )
                        
                        // Haptic at threshold crossings
                        if (abs(swipeOffset) >= swipeThreshold && 
                            abs(swipeOffset - dragAmount) < swipeThreshold) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { 
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onTap() 
                        },
                        onLongPress = { 
                            onLongPress() 
                        }
                    )
                },
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor,
            border = if (isSelected) BorderStroke(2.dp, borderColor) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selected indicator (blue left bar)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(40.dp)
                            .background(
                                Color(0xFF4A90E2),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // Thumbnail (48×48dp)
                LayerThumbnail(
                    layer = layer,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Layer info (name + blend mode)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Layer name
                    Text(
                        text = layer.name,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Blend mode badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF3A3A3C),
                        modifier = Modifier.clickable(onClick = onBlendModeClick)
                    ) {
                        Text(
                            text = layer.blendMode.displayName(),
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = Color(0xFF8E8E93)
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Visibility toggle (eye icon) - 48dp touch target
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onVisibilityToggle()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (layer.isVisible) 
                            Icons.Default.Visibility 
                        else 
                            Icons.Default.VisibilityOff,
                        contentDescription = if (layer.isVisible) "Hide layer" else "Show layer",
                        tint = if (layer.isVisible) Color.White else Color(0xFF666666),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Layer thumbnail with checkerboard background for transparency
 */
@Composable
private fun LayerThumbnail(
    layer: Layer,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
    ) {
        // Checkerboard pattern for transparency
        // (simplified - just white background)
        
        layer.thumbnail?.let { thumb ->
            Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = "Layer preview",
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
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Swipe action backgrounds revealed during horizontal swipe
 */
@Composable
private fun SwipeActionBackgrounds(
    swipeOffset: Float,
    swipeThreshold: Float
) {
    val density = LocalDensity.current
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Delete action (red) - revealed on swipe left
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (swipeOffset <= -swipeThreshold) 
                        Color(0xFFFF3B30) // Bright red when threshold reached
                    else 
                        Color(0xFFCC0000) // Darker red otherwise
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Duplicate action (blue) - revealed on swipe right
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (swipeOffset >= swipeThreshold) 
                        Color(0xFF5AC8FA) // Bright blue when threshold reached
                    else 
                        Color(0xFF4A90E2) // Standard blue otherwise
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Duplicate",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
