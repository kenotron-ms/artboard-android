package com.artboard.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Undo/Redo action trigger
 */
enum class UndoRedoTrigger {
    UNDO,
    REDO
}

/**
 * Undo/Redo visual feedback overlay
 * 
 * Shows a brief animated icon in center of screen:
 * - Scale in from 50% to 100%
 * - Fade in quickly
 * - Hold for 300ms
 * - Scale out to 150% while fading
 * - Semi-transparent dark circle background
 * 
 * Use for: Undo/redo actions, giving visual confirmation
 * 
 * Performance: 60 FPS, GPU-accelerated scale and alpha
 * 
 * @param trigger Current undo/redo trigger (null = not showing)
 * @param modifier Modifier for positioning
 */
@Composable
fun UndoRedoFeedback(
    trigger: UndoRedoTrigger?,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(trigger) {
        if (trigger != null) {
            isVisible = true
            delay(300) // Show for 300ms
            isVisible = false
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            initialScale = 0.5f,
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt(),
                easing = ArtboardAnimations.Easing.FAST_OUT_SLOW_IN
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ),
        exit = scaleOut(
            targetScale = 1.5f,
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0x80000000),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (trigger) {
                            UndoRedoTrigger.UNDO -> Icons.Default.Undo
                            UndoRedoTrigger.REDO -> Icons.Default.Redo
                            null -> Icons.Default.Check
                        },
                        contentDescription = when (trigger) {
                            UndoRedoTrigger.UNDO -> "Undo"
                            UndoRedoTrigger.REDO -> "Redo"
                            null -> null
                        },
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
    }
}

/**
 * Success feedback (e.g., "Saved", "Exported")
 * 
 * Slides in from top, holds briefly, slides out:
 * - Slide in from top + fade in
 * - Display for configured duration
 * - Slide out to top + fade out
 * - Green background for success
 * 
 * Use for: Save confirmation, export success, operation complete
 * 
 * Performance: 60 FPS, GPU-accelerated translation and alpha
 * 
 * @param isVisible Whether the feedback should be visible
 * @param message Success message to display
 * @param icon Icon to show (default: checkmark)
 * @param modifier Modifier for positioning
 */
@Composable
fun SaveSuccessFeedback(
    isVisible: Boolean,
    message: String = "Saved",
    icon: ImageVector = Icons.Default.CheckCircle,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.NORMAL.toInt(),
                easing = ArtboardAnimations.Easing.FAST_OUT_SLOW_IN
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt(),
                easing = ArtboardAnimations.Easing.LINEAR_OUT_SLOW_IN
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.8f),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF4CAF50),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Error feedback (e.g., "Failed to save", "Invalid input")
 * 
 * Similar to success feedback but with red background:
 * - Slide in from top + fade in
 * - Display error message
 * - Slide out to top + fade out
 * - Red background for error
 * 
 * Use for: Operation failures, validation errors
 * 
 * Performance: 60 FPS, GPU-accelerated
 * 
 * @param isVisible Whether the feedback should be visible
 * @param message Error message to display
 * @param icon Icon to show (default: error icon)
 * @param modifier Modifier for positioning
 */
@Composable
fun ErrorFeedback(
    isVisible: Boolean,
    message: String = "Error",
    icon: ImageVector = Icons.Default.Error,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.NORMAL.toInt(),
                easing = ArtboardAnimations.Easing.FAST_OUT_SLOW_IN
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt(),
                easing = ArtboardAnimations.Easing.LINEAR_OUT_SLOW_IN
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.8f),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF44336),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Generic toast notification
 * 
 * Flexible notification with customizable color and icon:
 * - Slide in from bottom + fade in
 * - Display message
 * - Slide out to bottom + fade out
 * - Customizable appearance
 * 
 * Use for: General notifications, info messages
 * 
 * Performance: 60 FPS, GPU-accelerated
 * 
 * @param isVisible Whether the toast should be visible
 * @param message Message to display
 * @param icon Optional icon
 * @param backgroundColor Background color
 * @param modifier Modifier for positioning
 */
@Composable
fun ToastNotification(
    isVisible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    backgroundColor: Color = Color(0xFF323232)
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.NORMAL.toInt(),
                easing = ArtboardAnimations.Easing.FAST_OUT_SLOW_IN
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt(),
                easing = ArtboardAnimations.Easing.LINEAR_OUT_SLOW_IN
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.9f),
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = if (icon != null) Arrangement.Start else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * File operation feedback (e.g., "Exporting...", "Export complete")
 * 
 * Shows operation progress with icon and message:
 * - Fade in when operation starts
 * - Update message during operation
 * - Fade out when complete
 * 
 * Use for: File save, export, import operations
 * 
 * Performance: 60 FPS
 * 
 * @param isVisible Whether the feedback should be visible
 * @param message Operation message
 * @param isComplete Whether operation is complete (changes icon)
 * @param modifier Modifier for positioning
 */
@Composable
fun FileOperationFeedback(
    isVisible: Boolean,
    message: String,
    isComplete: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt(),
                easing = ArtboardAnimations.Easing.FAST_OUT_SLOW_IN
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ) + scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xE0000000),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isComplete) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    // Rotating icon for in-progress
                    val infiniteTransition = rememberInfiniteTransition(label = "file_operation")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 1000,
                                easing = ArtboardAnimations.Easing.LINEAR
                            ),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )
                    
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF4A90E2),
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Transform preview feedback (e.g., "Rotating", "Scaling")
 * 
 * Shows current transformation with value:
 * - Fade in when transform starts
 * - Update value in real-time
 * - Fade out when released
 * - Semi-transparent, non-intrusive
 * 
 * Use for: Transform tool, showing rotation angle, scale factor
 * 
 * Performance: 60 FPS, instant updates during gesture
 * 
 * @param isVisible Whether the feedback should be visible
 * @param label Transform label (e.g., "Rotation")
 * @param value Current value (e.g., "45°")
 * @param modifier Modifier for positioning
 */
@Composable
fun TransformFeedback(
    isVisible: Boolean,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.QUICK.toInt()
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xCC000000),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                )
                Text(
                    text = value,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Quick action feedback flash
 * 
 * Brief full-screen flash for quick actions:
 * - Fade in very quickly
 * - Hold for very short duration
 * - Fade out quickly
 * - Very subtle, non-intrusive
 * 
 * Use for: Screenshot taken, layer merged, quick actions
 * 
 * Performance: 60 FPS, GPU-accelerated alpha
 * 
 * @param trigger Trigger to show flash (increment to trigger)
 * @param color Flash color
 * @param modifier Modifier
 */
@Composable
fun QuickActionFlash(
    trigger: Int,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            isVisible = true
            delay(100) // Very brief flash
            isVisible = false
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.MICRO.toInt()
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.QUICK.toInt()
            )
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.3f }
                .background(color)
        )
    }
}
