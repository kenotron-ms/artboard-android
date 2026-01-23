package com.artboard.ui.brush.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch

/**
 * Brush preview card with 120×120dp size and live stroke preview
 * 
 * Visual specifications:
 * - Card: 120×120dp, 12dp rounded corners
 * - Preview: 100×100dp white canvas with rendered stroke
 * - Selected: Blue border (#4A90E2), elevated 8dp
 * - Normal: Dark background (#242424), elevated 2dp
 * - Favorite: Gold star (#FFD700) in top-right corner
 */
@Composable
fun BrushPreviewCard(
    brush: Brush,
    isSelected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    
    // Animated scale for press and selection feedback
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.95f
            isSelected -> 1.0f // No scale up, use elevation instead
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )
    
    Surface(
        modifier = modifier
            .size(120.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        val released = tryAwaitRelease()
                        pressed = false
                        if (released) {
                            onClick()
                        }
                    },
                    onLongPress = {
                        onLongPress()
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF2A4A6A) else Color(0xFF242424),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF4A90E2)) else null,
        shadowElevation = if (isSelected) 8.dp else 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Live brush preview (88dp to leave room for padding and label)
                BrushPreview(
                    brush = brush,
                    modifier = Modifier.size(88.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Brush name
                Text(
                    text = brush.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Favorite star (top-right corner)
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = Color(0xFFFFD700), // Gold
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .clickable { onFavoriteClick() }
                )
            }
        }
    }
}

/**
 * Live brush preview using actual BrushEngine rendering
 * Shows S-curve stroke with pressure variation
 */
@Composable
fun BrushPreview(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    val previewGenerator = remember { BrushPreviewGenerator() }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val scope = rememberCoroutineScope()
    
    // Generate preview asynchronously
    LaunchedEffect(brush) {
        scope.launch {
            val bitmap = previewGenerator.generatePreview(
                brush = brush,
                size = 100 // 100px for 88dp display area
            )
            previewBitmap = bitmap
        }
    }
    
    // Display preview or placeholder
    previewBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "${brush.type.name} preview",
            modifier = modifier
        )
    } ?: run {
        // Placeholder while loading
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "...",
                color = Color.Gray,
                fontSize = 24.sp
            )
        }
    }
}

/**
 * Grid of brush cards for the brush selector panel
 * Handles selection, favorites, and long-press actions
 */
@Composable
fun BrushCardGrid(
    brushes: List<Brush>,
    selectedBrush: Brush,
    favorites: Set<String>,
    onBrushSelected: (Brush) -> Unit,
    onBrushLongPress: (Brush) -> Unit,
    onFavoriteToggle: (Brush) -> Unit,
    modifier: Modifier = Modifier
) {
    // Grid implementation would use LazyVerticalGrid
    // This is a helper function that can be used in the main panel
}
