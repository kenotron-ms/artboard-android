package com.artboard.ui.gallery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
 * Beautiful, inspiring empty state for gallery
 * Shows when user has no projects yet
 * Design: Large icon, inspiring headline, inviting subtext, prominent Create button
 */
@Composable
fun EmptyGalleryState(
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // Deep charcoal background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(48.dp)
        ) {
            // Large inspiring icon (128dp as per spec)
            Icon(
                imageVector = Icons.Default.Create, // Brush icon
                contentDescription = null,
                modifier = Modifier.size(128.dp),
                tint = Color(0xFF444444) // Subtle gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Inspiring headline (32sp Bold)
            Text(
                text = "Create something beautiful",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp // Premium tight spacing
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Inviting subtext (16sp Regular)
            Text(
                text = "Tap below to start your first masterpiece",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color(0xFFAAAAAA), // Light gray
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Prominent Create New button
            CreateNewButton(onClick = onCreateNew)
        }
    }
}
