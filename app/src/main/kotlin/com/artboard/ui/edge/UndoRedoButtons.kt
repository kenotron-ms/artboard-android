package com.artboard.ui.edge

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Undo/Redo button section for the right edge control bar.
 * 
 * Positioned at the bottom of the edge bar, separated from category
 * buttons by a subtle divider line. Provides quick access to history
 * navigation without opening any panels.
 * 
 * Design rationale:
 * - Undo is accessed very frequently (constant use tier)
 * - Redo is accessed frequently (frequent use tier)
 * - Both should be always visible and quickly accessible
 * - Separated from category buttons to avoid accidental taps
 * 
 * Accessibility:
 * - 48x48dp touch targets
 * - Disabled state clearly communicated visually and to screen readers
 * - Haptic feedback on successful action
 * 
 * @param canUndo Whether undo action is available
 * @param canRedo Whether redo action is available
 * @param onUndo Callback when undo button is tapped
 * @param onRedo Callback when redo button is tapped
 * @param modifier Modifier for the container
 */
@Composable
fun UndoRedoButtons(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EdgeBarDimensions.BUTTON_GAP.dp)
    ) {
        // Separator line above undo/redo
        Separator()
        
        Spacer(modifier = Modifier.height(EdgeBarDimensions.BUTTON_GAP.dp))
        
        // Undo button
        HistoryButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            enabled = canUndo,
            onClick = onUndo
        )
        
        // Redo button
        HistoryButton(
            icon = Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            enabled = canRedo,
            onClick = onRedo
        )
    }
}

/**
 * Separator line between category buttons and undo/redo section.
 * 
 * Specifications:
 * - Width: 32dp (centered in 48dp button width)
 * - Height: 1dp
 * - Color: #C6C6C8 (light mode) / #38383A (dark mode)
 * - Margin: 8dp above and below
 */
@Composable
private fun Separator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(EdgeBarDimensions.SEPARATOR_WIDTH.dp)
            .height(EdgeBarDimensions.SEPARATOR_HEIGHT.dp)
            .background(Color(EdgeBarColors.SEPARATOR))
    )
}

/**
 * Individual undo or redo button.
 * 
 * Same touch target and animation as CategoryButton but without
 * the selection dot indicator. Disabled state shows grayed out
 * icon when history is empty in that direction.
 * 
 * @param icon The icon to display (Undo or Redo)
 * @param contentDescription Accessibility description
 * @param enabled Whether the button is enabled (history available)
 * @param onClick Callback when button is tapped
 * @param modifier Modifier for the button
 */
@Composable
private fun HistoryButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    // Press scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) EdgeBarAnimations.PRESS_SCALE else 1f,
        animationSpec = if (isPressed) {
            tween(
                durationMillis = EdgeBarAnimations.PRESS_DURATION_MS,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        } else {
            spring(
                dampingRatio = EdgeBarAnimations.RELEASE_DAMPING,
                stiffness = EdgeBarAnimations.RELEASE_STIFFNESS
            )
        },
        label = "history_button_scale"
    )

    // Background color animation
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isPressed -> Color(EdgeBarColors.BG_PRESSED)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = EdgeBarAnimations.PRESS_DURATION_MS),
        label = "history_button_bg"
    )

    // Icon color animation
    val iconColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color(EdgeBarColors.ICON_DISABLED)
            isPressed -> Color(EdgeBarColors.ICON_PRESSED)
            else -> Color(EdgeBarColors.ICON_INACTIVE)
        },
        animationSpec = tween(durationMillis = EdgeBarAnimations.PRESS_DURATION_MS),
        label = "history_button_icon_color"
    )

    Box(
        modifier = modifier
            .size(EdgeBarDimensions.BUTTON_SIZE.dp)
            .scale(scale)
            .clip(RoundedCornerShape(EdgeBarDimensions.BUTTON_CORNER_RADIUS.dp))
            .background(backgroundColor)
            .then(
                if (enabled) {
                    Modifier.indication(
                        interactionSource = interactionSource,
                        indication = rememberRipple(
                            bounded = true,
                            color = Color(EdgeBarColors.ICON_ACTIVE)
                        )
                    )
                } else {
                    Modifier
                }
            )
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = { offset ->
                            isPressed = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            
                            val press = PressInteraction.Press(offset)
                            scope.launch {
                                interactionSource.emit(press)
                            }
                            
                            val released = tryAwaitRelease()
                            isPressed = false
                            
                            scope.launch {
                                interactionSource.emit(
                                    if (released) PressInteraction.Release(press)
                                    else PressInteraction.Cancel(press)
                                )
                            }
                        },
                        onTap = {
                            onClick()
                        }
                    )
                }
            }
            .semantics {
                this.contentDescription = if (enabled) {
                    "$contentDescription. Double tap to $contentDescription."
                } else {
                    "$contentDescription. Disabled. No actions to $contentDescription."
                }
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Handled by parent semantics
            tint = iconColor,
            modifier = Modifier.size(EdgeBarDimensions.ICON_SIZE.dp)
        )
    }
}

/**
 * Compact undo/redo row variant for alternative layouts.
 * 
 * Displays undo and redo buttons side-by-side instead of stacked.
 * Useful for horizontal toolbar layouts or when vertical space
 * is constrained.
 * 
 * @param canUndo Whether undo action is available
 * @param canRedo Whether redo action is available
 * @param onUndo Callback when undo button is tapped
 * @param onRedo Callback when redo button is tapped
 * @param modifier Modifier for the container
 */
@Composable
fun UndoRedoRow(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(EdgeBarDimensions.BUTTON_GAP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HistoryButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            enabled = canUndo,
            onClick = onUndo
        )
        
        HistoryButton(
            icon = Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            enabled = canRedo,
            onClick = onRedo
        )
    }
}
