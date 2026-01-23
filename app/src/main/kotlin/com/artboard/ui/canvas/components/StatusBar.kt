package com.artboard.ui.canvas.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.data.model.Layer

/**
 * Bottom status bar showing layer info, canvas dimensions, and performance
 * Optional component that auto-hides with the toolbar
 */
@Composable
fun StatusBar(
    activeLayer: Layer?,
    canvasInfo: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    fps: Float = 0f
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ),
        exit = fadeOut(tween(300)) + slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(32.dp),
            color = Color(0xDD000000), // 87% opacity black
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Layer info
                Text(
                    text = activeLayer?.name ?: "No Layer",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color.White
                    )
                )
                
                // Canvas info
                Text(
                    text = canvasInfo,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                )
                
                // FPS indicator
                if (fps > 0) {
                    val fpsColor = when {
                        fps >= 55 -> Color(0xFF4CAF50) // Green for good FPS
                        fps >= 30 -> Color(0xFFFF9800) // Yellow for medium
                        else -> Color(0xFFF44336) // Red for bad
                    }
                    
                    Text(
                        text = "${fps.toInt()} FPS",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = fpsColor
                        )
                    )
                }
            }
        }
    }
}
