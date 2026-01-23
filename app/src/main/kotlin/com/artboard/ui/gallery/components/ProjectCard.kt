package com.artboard.ui.gallery.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.artboard.data.model.ProjectSummary

/**
 * Custom project card for gallery (NOT Material Card)
 * Size: 256×256dp thumbnail + 42dp info = 298dp total height
 * Design: Deep charcoal card (#242424) with rounded corners
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(
    project: ProjectSummary,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
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
        label = "card_scale"
    )
    
    // Animate elevation
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 4f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_elevation"
    )
    
    Surface(
        modifier = modifier
            .width(256.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null, // Custom animation instead of ripple
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF242424), // Card background from spec
        shadowElevation = elevation.dp
    ) {
        Column {
            // Thumbnail with 1:1 aspect ratio (256×256dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF1A1A1A)) // Placeholder background
            ) {
                AsyncImage(
                    model = project.thumbnailPath,
                    contentDescription = project.name,
                    modifier = Modifier.fillMaxSize()
                    // contentScale = ContentScale.Crop,
                    // placeholder = painterResource(R.drawable.placeholder_thumbnail),
                    // error = painterResource(R.drawable.error_thumbnail)
                )
            }
            
            // Project info (42dp height)
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                // Project name (16sp Medium, max 2 lines)
                Text(
                    text = project.name,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        lineHeight = 20.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Metadata: date • dimensions
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Date
                    Text(
                        text = project.formattedDate(),
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color(0xFFAAAAAA) // Secondary text color
                        )
                    )
                    
                    // Dimensions
                    Text(
                        text = "${project.width}×${project.height}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    )
                }
            }
        }
    }
}
