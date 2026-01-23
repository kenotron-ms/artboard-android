package com.artboard.ui.animation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated button with press scale effect
 * 
 * Provides smooth spring-based scale animation when pressed:
 * - Scales down to 0.95 on press
 * - Springs back to 1.0 on release
 * - Includes haptic feedback
 * - Reduces elevation when pressed for depth effect
 * 
 * Performance: 60 FPS, GPU-accelerated scale transform
 * 
 * @param onClick Action to perform when button is clicked
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 * @param shape Shape of the button (default: rounded corners)
 * @param color Background color of the button
 * @param disabledColor Background color when disabled
 * @param elevation Shadow elevation when not pressed
 * @param pressedElevation Shadow elevation when pressed
 * @param content Button content (text, icon, etc.)
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    color: Color = Color(0xFF4A90E2),
    disabledColor: Color = Color(0xFF444444),
    elevation: Dp = 4.dp,
    pressedElevation: Dp = 2.dp,
    content: @Composable RowScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) AnimationScale.BUTTON_PRESS else 1f,
        animationSpec = ArtboardAnimations.buttonPress(),
        label = "button_scale"
    )
    
    Surface(
        modifier = modifier
            .scale(scale)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            pressed = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            tryAwaitRelease()
                            pressed = false
                        }
                    },
                    onTap = {
                        if (enabled) {
                            onClick()
                        }
                    }
                )
            },
        shape = shape,
        color = if (enabled) color else disabledColor,
        shadowElevation = if (pressed) pressedElevation else elevation
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Animated icon button with scale effect
 * 
 * Smaller, circular button designed for toolbar icons:
 * - Scales down to 0.9 on press (more pronounced than regular buttons)
 * - Smooth spring animation
 * - Circular shape
 * - Haptic feedback
 * 
 * Performance: 60 FPS, GPU-accelerated
 * 
 * @param onClick Action to perform when button is clicked
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 * @param size Size of the icon button (default: 48.dp)
 * @param content Icon content
 */
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) AnimationScale.ICON_BUTTON_PRESS else 1f,
        animationSpec = ArtboardAnimations.Springs.SMOOTH,
        label = "icon_button_scale"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        }
                    },
                    onTap = {
                        if (enabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Animated floating action button (FAB)
 * 
 * Large circular button with pronounced animation:
 * - Scale animation on press
 * - Higher elevation for floating effect
 * - Suitable for primary actions
 * 
 * Performance: 60 FPS, GPU-accelerated
 * 
 * @param onClick Action to perform when FAB is clicked
 * @param modifier Modifier for the FAB
 * @param enabled Whether the FAB is enabled
 * @param size Size of the FAB (default: 56.dp)
 * @param color Background color of the FAB
 * @param elevation Shadow elevation when not pressed
 * @param content FAB content (usually an icon)
 */
@Composable
fun AnimatedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 56.dp,
    color: Color = Color(0xFF4A90E2),
    elevation: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    val scale by animateFloatAsState(
        targetValue = if (pressed) AnimationScale.BUTTON_PRESS else 1f,
        animationSpec = ArtboardAnimations.Springs.BOUNCY,
        label = "fab_scale"
    )
    
    Surface(
        modifier = modifier
            .size(size)
            .scale(scale)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            pressed = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            tryAwaitRelease()
                            pressed = false
                        }
                    },
                    onTap = {
                        if (enabled) {
                            onClick()
                        }
                    }
                )
            },
        shape = CircleShape,
        color = color,
        shadowElevation = if (pressed) elevation - 2.dp else elevation
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Animated toggle button with scale and color transitions
 * 
 * Button that shows selected/unselected state:
 * - Scales slightly when selected (1.0 to 1.05)
 * - Smooth color transition
 * - Press animation
 * 
 * Performance: 60 FPS, uses animateColorAsState for smooth color blend
 * 
 * @param selected Whether the button is currently selected
 * @param onToggle Action to perform when button is toggled
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 * @param selectedColor Background color when selected
 * @param unselectedColor Background color when not selected
 * @param content Button content
 */
@Composable
fun AnimatedToggleButton(
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedColor: Color = Color(0xFF4A90E2),
    unselectedColor: Color = Color(0xFF444444),
    content: @Composable RowScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> AnimationScale.BUTTON_PRESS
            selected -> AnimationScale.CARD_SELECTED
            else -> 1f
        },
        animationSpec = ArtboardAnimations.Springs.SMOOTH,
        label = "toggle_button_scale"
    )
    
    val color by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = ArtboardAnimations.colorBlend(),
        label = "toggle_button_color"
    )
    
    Surface(
        modifier = modifier
            .scale(scale)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            pressed = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            tryAwaitRelease()
                            pressed = false
                        }
                    },
                    onTap = {
                        if (enabled) {
                            onToggle()
                        }
                    }
                )
            },
        shape = RoundedCornerShape(24.dp),
        color = color,
        shadowElevation = if (selected) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
