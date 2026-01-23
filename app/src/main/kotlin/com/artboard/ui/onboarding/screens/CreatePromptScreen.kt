package com.artboard.ui.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Create Prompt screen - Final screen of onboarding
 * Inspiring call-to-action to start creating
 */
@Composable
fun CreatePromptScreen(
    onGetStarted: () -> Unit,
    onExploreSample: (() -> Unit)? = null
) {
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
            // Inspiring illustration
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = Color(0xFF4A90E2)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "You're All Set!",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Time to create something beautiful",
                style = TextStyle(
                    fontSize = 18.sp,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Large "Get Started" button
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Start Creating",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            
            // Optional: Explore sample artwork button
            if (onExploreSample != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(onClick = onExploreSample) {
                    Text(
                        text = "Explore sample artwork first",
                        color = Color(0xFFAAAAAA)
                    )
                }
            }
        }
    }
}
