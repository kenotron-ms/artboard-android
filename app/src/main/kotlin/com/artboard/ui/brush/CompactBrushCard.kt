package com.artboard.ui.brush

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Brush
import com.artboard.ui.brush.components.BrushPreviewGenerator
import kotlinx.coroutines.launch

/**
 * Compact brush card for the 2-column popover grid
 * 
 * Per design spec:
 * - Card size: 120×80dp each
 * - Live stroke preview in each card
 * - Brush name below preview
 * - Favorite star (gold) in corner
 * 
 * Visual specifications:
 * - Background: #242424 (normal), #2A4A6A (selected)
 * - Corner radius: 8dp
 * - Selected: Blue border (#007AFF), subtle scale pulse
 * - Preview area: ~60dp height
 * - Name area: ~20dp height
 * 
 * Animations:
 * - Press: Scale to 0.95
 * - Select: Scale pulse (1.0 -> 1.05 -> 1.0)
 * - Favorite toggle: Star scale bounce
 * 
 * @param brush The brush to display
 * @param isSelected Whether this brush is currently selected
 * @param isFavorite Whether this brush is marked as favorite
 * @param onClick Callback when card is tapped
 * @param onFavoriteClick Callback when favorite star is tapped
 */
@Composable
fun CompactBrushCard(
    brush: Brush,
    isSelected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    // Scale animation for press and selection
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_scale"
    )
    
    // Selection pulse animation
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 1.0f,
        animationSpec = if (isSelected) {
            keyframes {
                durationMillis = 300
                1.0f at 0
                1.05f at 150
                1.0f at 300
            }
        } else {
            tween(0)
        },
        label = "selection_pulse"
    )
    
    Surface(
        modifier = modifier
            .size(width = 120.dp, height = 80.dp)
            .scale(scale * selectionScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            onClick()
                        }
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF2A4A6A) else Color(0xFF242424),
        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF007AFF)) else null,
        shadowElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live brush preview (compact)
                CompactBrushPreview(
                    brush = brush,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Brush name
                Text(
                    text = brush.displayName,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Favorite star in top-right corner
            FavoriteStarButton(
                isFavorite = isFavorite,
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            )
        }
    }
}

/**
 * Compact brush stroke preview
 * Uses BrushPreviewGenerator for realistic rendering
 */
@Composable
private fun CompactBrushPreview(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    val previewGenerator = remember { BrushPreviewGenerator() }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()
    
    // Generate preview asynchronously
    LaunchedEffect(brush) {
        scope.launch {
            val bitmap = previewGenerator.generatePreview(
                brush = brush,
                size = 80 // Compact preview size
            )
            previewBitmap = bitmap
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        previewBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "${brush.displayName} preview",
                modifier = Modifier.fillMaxSize()
            )
        } ?: run {
            // Loading placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "...",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Animated favorite star button
 */
@Composable
private fun FavoriteStarButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Bounce animation when toggled
    var wasJustToggled by remember { mutableStateOf(false) }
    val starScale by animateFloatAsState(
        targetValue = if (wasJustToggled) 1.3f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        finishedListener = { wasJustToggled = false },
        label = "star_bounce"
    )
    
    Icon(
        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
        tint = if (isFavorite) Color(0xFFFFD700) else Color(0xFF8E8E93), // Gold or gray
        modifier = modifier
            .size(16.dp)
            .scale(starScale)
            .clickable {
                wasJustToggled = true
                onClick()
            }
    )
}

/**
 * Display name for brush (humanized type name)
 */
private val Brush.displayName: String
    get() = type.name
        .lowercase()
        .replaceFirstChar { it.uppercase() }
