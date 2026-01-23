package com.artboard.ui.edge

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Category button for the right edge control bar.
 * 
 * A 48x48dp touch target button that displays a category icon with multiple states:
 * - Inactive: Gray icon, transparent background
 * - Active: Blue icon (panel is open), light blue background
 * - Selected: Blue icon with orange indicator dot (tool from this category is selected)
 * - Pressed: Scale to 0.92 with blue background
 * 
 * Animation Specifications (from design doc):
 * - Press: Scale 1.0 → 0.92 over 80ms with ease-out
 * - Release: Spring animation (damping: 0.7, stiffness: 300)
 * - Icon transition: Outlined → Filled when active (80ms)
 * - Selection dot: Scale 0 → 1.2 → 1.0 over 250ms (bouncy spring)
 * 
 * Accessibility:
 * - 48x48dp touch target (exceeds 44dp minimum)
 * - ContentDescription for screen readers
 * - Haptic feedback on press
 * 
 * @param category The tool category this button represents
 * @param isActive Whether this category's panel is currently open
 * @param hasSelectedTool Whether a tool from this category is currently selected
 * @param onClick Callback when button is tapped
 * @param modifier Modifier for the button
 */
@Composable
fun CategoryButton(
    category: ToolCategory,
    isActive: Boolean,
    hasSelectedTool: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    // Press scale animation - 80ms ease-out for press, spring for release
    val scale by animateFloatAsState(
        targetValue = if (isPressed) EdgeBarAnimations.PRESS_SCALE else 1f,
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
        label = "category_button_scale"
    )

    // Background color animation
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color(EdgeBarColors.BG_PRESSED)
            isActive -> Color(EdgeBarColors.BG_ACTIVE)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = EdgeBarAnimations.PRESS_DURATION_MS),
        label = "category_button_bg"
    )

    // Icon color animation
    val iconColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color(EdgeBarColors.ICON_PRESSED)
            isActive || hasSelectedTool -> Color(EdgeBarColors.ICON_ACTIVE)
            else -> Color(EdgeBarColors.ICON_INACTIVE)
        },
        animationSpec = tween(durationMillis = EdgeBarAnimations.PRESS_DURATION_MS),
        label = "category_button_icon_color"
    )

    // Selection dot scale animation (bouncy)
    val dotScale by animateFloatAsState(
        targetValue = if (hasSelectedTool) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selection_dot_scale"
    )

    Box(
        modifier = modifier
            .size(EdgeBarDimensions.BUTTON_SIZE.dp)
            .scale(scale)
            .clip(RoundedCornerShape(EdgeBarDimensions.BUTTON_CORNER_RADIUS.dp))
            .background(backgroundColor)
            .indication(
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = true,
                    color = Color(EdgeBarColors.ICON_ACTIVE)
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isPressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        
                        // Create press interaction for ripple
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
            .semantics {
                contentDescription = "${category.contentDescription}. ${category.accessibilityHint}"
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        // Category icon (outlined when inactive, filled when active)
        Icon(
            imageVector = CategoryIcons.getIcon(category, isActive || hasSelectedTool),
            contentDescription = null, // Handled by parent semantics
            tint = iconColor,
            modifier = Modifier.size(EdgeBarDimensions.ICON_SIZE.dp)
        )

        // Selection indicator dot (bottom center, outside icon bounds)
        if (dotScale > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp) // Slightly below the icon
                    .scale(dotScale)
                    .size(EdgeBarDimensions.INDICATOR_DOT_SIZE.dp)
                    .clip(CircleShape)
                    .background(Color(EdgeBarColors.INDICATOR_DOT))
            )
        }
    }
}

/**
 * Preview states for CategoryButton
 */
enum class CategoryButtonState {
    /** Default inactive state */
    INACTIVE,
    /** Mouse/finger hovering over button */
    HOVER,
    /** Button is being pressed */
    PRESSED,
    /** Category panel is open */
    ACTIVE,
    /** Tool from this category is selected (shows orange dot) */
    SELECTED,
    /** Button is disabled (non-interactive) */
    DISABLED
}

/**
 * Disabled variant of CategoryButton.
 * 
 * Shows grayed out icon with no interaction capability.
 * Used when a category is temporarily unavailable.
 */
@Composable
fun CategoryButtonDisabled(
    category: ToolCategory,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(EdgeBarDimensions.BUTTON_SIZE.dp)
            .clip(RoundedCornerShape(EdgeBarDimensions.BUTTON_CORNER_RADIUS.dp))
            .semantics {
                contentDescription = "${category.contentDescription}. Disabled."
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryIcons.getOutlinedIcon(category),
            contentDescription = null,
            tint = Color(EdgeBarColors.ICON_DISABLED),
            modifier = Modifier.size(EdgeBarDimensions.ICON_SIZE.dp)
        )
    }
}
