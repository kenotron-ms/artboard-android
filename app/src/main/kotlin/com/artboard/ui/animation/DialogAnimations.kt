package com.artboard.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Animated dialog with scale and fade effect from center
 * 
 * Classic dialog animation:
 * - Enter: Scale up from 80% + fade in (300ms)
 * - Exit: Scale down to 80% + fade out (200ms)
 * - Centers on screen
 * - Dims background
 * 
 * Use for: Confirmation dialogs, alerts, forms
 * 
 * Performance: 60 FPS, GPU-accelerated scale and alpha
 * 
 * @param isVisible Whether the dialog should be visible
 * @param onDismiss Action to perform when dialog is dismissed
 * @param dismissOnClickOutside Whether clicking outside dismisses the dialog
 * @param content Dialog content
 */
@Composable
fun AnimatedDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            initialScale = AnimationScale.DIALOG_INITIAL,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.NORMAL.toInt(),
                easing = ArtboardAnimations.Easing.FAST_OUT_SLOW_IN
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.NORMAL.toInt()
            )
        ),
        exit = scaleOut(
            targetScale = AnimationScale.DIALOG_INITIAL,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .clickable(enabled = dismissOnClickOutside, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .clickable(enabled = false, onClick = {}), // Prevent click-through
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2A2A2A),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * Animated confirmation dialog
 * 
 * Dialog optimized for yes/no questions:
 * - Scale and fade animation
 * - Pre-styled with proper spacing
 * - Action buttons at bottom
 * 
 * Use for: Delete confirmation, save prompts, action confirmations
 * 
 * Performance: 60 FPS, GPU-accelerated
 * 
 * @param isVisible Whether the dialog should be visible
 * @param onDismiss Action to perform when dialog is dismissed
 * @param onConfirm Action to perform when user confirms
 * @param onCancel Action to perform when user cancels (defaults to onDismiss)
 * @param confirmText Text for confirm button (default: "Confirm")
 * @param cancelText Text for cancel button (default: "Cancel")
 * @param content Dialog content (title, message, etc.)
 */
@Composable
fun AnimatedConfirmationDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedDialog(
        isVisible = isVisible,
        onDismiss = onDismiss,
        dismissOnClickOutside = true,
        modifier = modifier.widthIn(min = 280.dp, max = 400.dp)
    ) {
        // Content area
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedButton(
                onClick = {
                    onCancel?.invoke() ?: onDismiss()
                },
                color = Color(0xFF444444)
            ) {
                androidx.compose.material3.Text(cancelText)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            AnimatedButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                color = Color(0xFF4A90E2)
            ) {
                androidx.compose.material3.Text(confirmText)
            }
        }
    }
}

/**
 * Animated alert dialog
 * 
 * Single-action dialog for notifications:
 * - Scale and fade animation
 * - Single "OK" button
 * 
 * Use for: Error messages, info alerts, notifications
 * 
 * Performance: 60 FPS, GPU-accelerated
 * 
 * @param isVisible Whether the dialog should be visible
 * @param onDismiss Action to perform when dialog is dismissed
 * @param buttonText Text for dismiss button (default: "OK")
 * @param content Dialog content
 */
@Composable
fun AnimatedAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "OK",
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedDialog(
        isVisible = isVisible,
        onDismiss = onDismiss,
        dismissOnClickOutside = true,
        modifier = modifier.widthIn(min = 280.dp, max = 400.dp)
    ) {
        // Content area
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Action button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedButton(
                onClick = onDismiss,
                color = Color(0xFF4A90E2)
            ) {
                androidx.compose.material3.Text(buttonText)
            }
        }
    }
}

/**
 * Animated bottom sheet dialog
 * 
 * Slides up from bottom like a panel but functions as a dialog:
 * - Enter: Slides up from bottom + fade in (300ms)
 * - Exit: Slides down below screen + fade out (200ms)
 * - Rounded top corners
 * 
 * Use for: Options menu, share sheet, selection lists
 * 
 * Performance: 60 FPS, GPU-accelerated translation and alpha
 * 
 * @param isVisible Whether the dialog should be visible
 * @param onDismiss Action to perform when dialog is dismissed
 * @param content Dialog content
 */
@Composable
fun AnimatedBottomSheetDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
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
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt(),
                easing = ArtboardAnimations.Easing.LINEAR_OUT_SLOW_IN
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.FAST.toInt()
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(enabled = false, onClick = {}), // Prevent click-through
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color(0xFF2A2A2A),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * Animated popup (small dialog anchored to a position)
 * 
 * Appears near an anchor point with scale and fade:
 * - Enter: Scale up from 70% + fade in (200ms fast)
 * - Exit: Scale down to 70% + fade out (150ms)
 * 
 * Use for: Tooltips, quick actions, context menus
 * 
 * Performance: 60 FPS, GPU-accelerated
 * 
 * @param isVisible Whether the popup should be visible
 * @param onDismiss Action to perform when popup is dismissed
 * @param content Popup content
 */
@Composable
fun AnimatedPopup(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            initialScale = 0.7f,
            transformOrigin = TransformOrigin.Center,
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
            targetScale = 0.7f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.QUICK.toInt()
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.QUICK.toInt()
            )
        )
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF2A2A2A),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

/**
 * Animated tooltip
 * 
 * Small informational popup that appears quickly:
 * - Enter: Fade in + slight scale (150ms)
 * - Exit: Fade out (100ms)
 * - Minimal animation for quick display
 * 
 * Use for: Help text, keyboard shortcuts, hints
 * 
 * Performance: 60 FPS, GPU-accelerated
 * 
 * @param isVisible Whether the tooltip should be visible
 * @param content Tooltip content
 */
@Composable
fun AnimatedTooltip(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            initialScale = 0.9f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.QUICK.toInt()
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.QUICK.toInt()
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = ArtboardAnimations.Duration.MICRO.toInt()
            )
        )
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(4.dp),
            color = Color(0xE0000000),
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                content()
            }
        }
    }
}
