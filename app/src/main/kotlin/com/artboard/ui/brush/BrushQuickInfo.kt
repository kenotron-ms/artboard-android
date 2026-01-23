package com.artboard.ui.brush

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Brush
import com.artboard.ui.brush.components.BrushCategory

/**
 * Quick info bar showing currently selected brush details
 * 
 * Per design spec:
 * - Shows current brush name
 * - Category badge
 * - Tilt indicator (if tilt-enabled brush)
 * 
 * Layout (at bottom of popover):
 * ┌─────────────────────────────┐
 * │ Current: Pencil (Sketch) ✏️  │
 * └─────────────────────────────┘
 *         48dp height
 * 
 * Visual specifications:
 * - Height: 48dp
 * - Background: #2C2C2E (slightly lighter than popover)
 * - Top border: 1dp separator line #38383A
 * - Text: White, 13sp
 * - Category badge: Pill shape, accent color
 * - Tilt icon: Only shown for tilt-enabled brushes
 * 
 * @param brush Currently selected brush
 * @param category Current brush category
 */
@Composable
fun BrushQuickInfo(
    brush: Brush,
    category: BrushCategory,
    modifier: Modifier = Modifier
) {
    // Animate brush name changes
    var previousBrush by remember { mutableStateOf(brush) }
    val brushChanged = brush != previousBrush
    
    LaunchedEffect(brush) {
        previousBrush = brush
    }
    
    Column(
        modifier = modifier
            .height(48.dp)
            .background(Color(0xFF2C2C2E))
    ) {
        // Top separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF38383A))
        )
        
        // Content row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "Current:" label
            Text(
                text = "Current:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF8E8E93)
            )
            
            // Animated brush name
            AnimatedContent(
                targetState = brush,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(150)) + 
                        slideInVertically(animationSpec = tween(150)) { -it / 2 })
                        .togetherWith(
                            fadeOut(animationSpec = tween(100)) +
                                slideOutVertically(animationSpec = tween(100)) { it / 2 }
                        )
                },
                label = "brush_name_transition"
            ) { currentBrush ->
                Text(
                    text = currentBrush.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            
            // Category badge
            CategoryBadge(category = category)
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Tilt indicator (only for tilt-enabled brushes)
            if (brush.tiltSizeEnabled || brush.tiltAngleEnabled) {
                TiltIndicator()
            }
            
            // Brush icon
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Category badge pill
 */
@Composable
private fun CategoryBadge(
    category: BrushCategory
) {
    val badgeColor = when (category) {
        BrushCategory.SKETCH -> Color(0xFF5856D6)  // Purple
        BrushCategory.PAINT -> Color(0xFFFF9500)   // Orange
        BrushCategory.TEXTURE -> Color(0xFF34C759) // Green
        BrushCategory.EFFECTS -> Color(0xFFFF2D55) // Pink
        BrushCategory.FAVORITES -> Color(0xFFFFD700) // Gold
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeColor.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = category.displayName,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = badgeColor
        )
    }
}

/**
 * Tilt indicator icon with subtle animation
 * Shows that the brush responds to stylus tilt
 */
@Composable
private fun TiltIndicator() {
    // Subtle bobbing animation to indicate tilt responsiveness
    val infiniteTransition = rememberInfiniteTransition(label = "tilt_animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt_rotation"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = "Tilt-enabled brush",
            tint = Color(0xFF007AFF),
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
        )
        Text(
            text = "Tilt",
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF007AFF)
        )
    }
}

/**
 * Display name for brush
 */
private val Brush.displayName: String
    get() = type.name
        .lowercase()
        .replaceFirstChar { it.uppercase() }
