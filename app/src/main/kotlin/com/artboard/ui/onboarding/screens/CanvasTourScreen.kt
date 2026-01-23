package com.artboard.ui.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Canvas Tour screen - Third screen of onboarding
 * Quick overview of UI elements and how to access them
 */
@Composable
fun CanvasTourScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Workspace",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Everything you need is here",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color(0xFFAAAAAA)
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // UI element overview
            UIElement(
                icon = Icons.Default.Brush,
                title = "Toolbar",
                description = "Access brushes, colors, and tools at the top",
                position = "Top"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            UIElement(
                icon = Icons.Default.Layers,
                title = "Layers Panel",
                description = "Swipe up from bottom to manage layers",
                position = "Bottom"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            UIElement(
                icon = Icons.Default.Palette,
                title = "Color Picker",
                description = "Tap color swatch for advanced color selection",
                position = "Toolbar"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            UIElement(
                icon = Icons.Default.Settings,
                title = "Brush Settings",
                description = "Long press brush to adjust size and properties",
                position = "Toolbar"
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Tip box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF242424), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF4A90E2).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFF4A90E2),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "Tip: Four-finger tap hides all UI for distraction-free drawing",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Individual UI element description
 */
@Composable
fun UIElement(
    icon: ImageVector,
    title: String,
    description: String,
    position: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF242424), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF4A90E2),
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Position badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4A90E2).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = position,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = Color(0xFF4A90E2)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFFAAAAAA)
                )
            )
        }
    }
}
