package com.artboard.ui.gallery.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Large, inspiring "Create New" button for gallery
 * Size: 256×256dp (same as project cards)
 * NOT a small FAB - this is prominent and inviting
 */
@Composable
fun CreateNewButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track press state for animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animate scale with spring physics (dampingRatio 0.75 as per spec)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessHigh
        ),
        label = "create_button_scale"
    )
    
    // Animate elevation
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 4f else 8f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessHigh
        ),
        label = "create_button_elevation"
    )
    
    Surface(
        modifier = modifier
            .width(256.dp)
            .height(256.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom animation instead of ripple
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF4A90E2), // Vibrant blue accent from spec
        shadowElevation = elevation.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large plus icon (72dp as per spec)
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new project",
                    modifier = Modifier.size(72.dp),
                    tint = Color.White
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // "Create New" label
                Text(
                    text = "Create New",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
