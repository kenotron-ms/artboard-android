package com.artboard.ui.color.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Horizontal scrollable bar of recent colors.
 * Displays up to 20 most recently used colors.
 * 
 * @param recentColors List of recent color ARGB values
 * @param onColorSelected Callback when a color is selected
 * @param modifier Modifier for the component
 */
@Composable
fun RecentColorsBar(
    recentColors: List<Int>,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentColors.isEmpty()) {
        // Don't show anything if no recent colors
        return
    }
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(recentColors) { colorArgb ->
            RecentColorSwatch(
                color = Color(colorArgb),
                onClick = {
                    onColorSelected(colorArgb)
                }
            )
        }
    }
}

/**
 * Individual recent color swatch
 */
@Composable
private fun RecentColorSwatch(
    color: Color,
    onClick: () -> Unit
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.White, CircleShape)
            .clickable {
                isPressed = true
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
                
                // Reset pressed state after animation
                kotlinx.coroutines.GlobalScope.launch {
                    kotlinx.coroutines.delay(150)
                    isPressed = false
                }
            }
    )
}
