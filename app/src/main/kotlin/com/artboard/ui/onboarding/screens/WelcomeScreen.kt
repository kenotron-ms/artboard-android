package com.artboard.ui.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Welcome screen - First screen of onboarding
 * Shows brand identity and key features
 */
@Composable
fun WelcomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // App logo/icon (large)
            Icon(
                imageVector = Icons.Default.Brush,
                contentDescription = null,
                modifier = Modifier.size(128.dp),
                tint = Color(0xFF4A90E2)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Welcome to Artboard",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Professional digital art\nfor Android tablets",
                style = TextStyle(
                    fontSize = 18.sp,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Feature highlights
            FeatureHighlight(
                icon = Icons.Default.Brush,
                text = "Full stylus pressure and tilt support"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FeatureHighlight(
                icon = Icons.Default.Layers,
                text = "Unlimited layers with blend modes"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FeatureHighlight(
                icon = Icons.Default.Speed,
                text = "60+ FPS drawing with zero lag"
            )
        }
    }
}

/**
 * Individual feature highlight row
 */
@Composable
fun FeatureHighlight(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(280.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4A90E2),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White
            )
        )
    }
}
