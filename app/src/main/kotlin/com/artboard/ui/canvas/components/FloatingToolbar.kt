package com.artboard.ui.canvas.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import com.artboard.data.model.Brush

/**
 * Custom floating toolbar with frosted glass effect
 * Translucent pill-shaped toolbar that auto-hides after 3 seconds
 * NOT using Material TopAppBar - completely custom
 */
@Composable
fun FloatingToolbar(
    currentBrush: Brush,
    currentColor: Color,
    canUndo: Boolean,
    canRedo: Boolean,
    isVisible: Boolean,
    onBrushClick: () -> Unit,
    onColorClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onLayersClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(
            animationSpec = tween(300)
        ) + slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xCC1A1A1A), // 80% opacity black (#CC = 204/255 = 80%)
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brush tool
                ToolButton(
                    icon = Icons.Default.Brush,
                    label = "${currentBrush.size.toInt()}px",
                    onClick = onBrushClick
                )
                
                // Color swatch
                ColorButton(
                    color = currentColor,
                    size = 40.dp,
                    onClick = onColorClick
                )
                
                // Undo
                ToolButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    enabled = canUndo,
                    onClick = onUndoClick
                )
                
                // Redo
                ToolButton(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    enabled = canRedo,
                    onClick = onRedoClick
                )
                
                // Layers
                ToolButton(
                    icon = Icons.Default.Layers,
                    onClick = onLayersClick
                )
                
                // Menu (settings, export, etc.)
                ToolButton(
                    icon = Icons.Default.MoreVert,
                    onClick = onMenuClick
                )
            }
        }
    }
}
