package com.artboard.ui.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.ui.onboarding.components.*

/**
 * Gesture Guide screen - Second screen of onboarding
 * Teaches essential gestures with animated demonstrations
 */
@Composable
fun GestureGuideScreen() {
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
                text = "Essential Gestures",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Learn these time-savers",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color(0xFFAAAAAA)
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Gesture demonstrations
            GestureDemo(
                title = "Two-Finger Tap",
                description = "Undo your last action",
                animation = { TwoFingerTapAnimation() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GestureDemo(
                title = "Four-Finger Tap",
                description = "Toggle UI visibility",
                animation = { FourFingerTapAnimation() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GestureDemo(
                title = "Pinch to Zoom",
                description = "Zoom in and out of your canvas",
                animation = { PinchZoomAnimation() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GestureDemo(
                title = "Long Press",
                description = "Pick color from canvas",
                animation = { LongPressAnimation() }
            )
        }
    }
}
