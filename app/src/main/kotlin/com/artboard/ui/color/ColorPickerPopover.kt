package com.artboard.ui.color

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artboard.ui.color.components.ColorQuickActions
import com.artboard.ui.color.components.CompactColorDisc
import com.artboard.ui.color.components.RecentColorsRow

/**
 * Compact color picker popover for Artboard.
 * 
 * Design spec (from EdgeControlBar.md):
 * - Width: 280dp (fixed)
 * - Height: auto (content-based, max 400dp)
 * - Position: Slides from right edge, below Color category button
 * - Background: #1C1C1E with 16dp corner radius
 * - Shadow: Elevation 8dp
 * 
 * Key changes from full-screen ColorPickerPanel:
 * - NO full-screen overlay
 * - Canvas visible (70%+ of screen)
 * - Compact layout (280dp vs 100%)
 * - Auto-close on tap outside
 * - Live preview on canvas while adjusting
 * - No "Apply" button - changes are immediate
 * 
 * Layout:
 * ```
 * ┌─────────────────────────────┐
 * │         Color Disc          │
 * │     ┌─────────────────┐     │
 * │     │   ┌───────┐     │     │
 * │  H  │   │ Sat/  │     │  H  │
 * │  U  │   │ Bri   │     │  U  │
 * │  E  │   │       │     │  E  │
 * │     │   └───────┘     │     │
 * │     └─────────────────┘     │
 * ├─────────────────────────────┤
 * │  [■][■][■][■][■][■][■][■]   │  Recent colors
 * ├─────────────────────────────┤
 * │  [Current] ⇄ [Previous] [◉] │  Actions
 * └─────────────────────────────┘
 *         280dp wide
 * ```
 * 
 * Animations:
 * - Open: Slide from right + fade, 200ms spring
 * - Close: Slide to right + fade, 150ms ease-out
 * - Color change: Smooth transition, 100ms
 * 
 * @param isVisible Whether the popover is visible
 * @param currentColor Initial color to display
 * @param anchorOffset Vertical offset from top (to position below Color button)
 * @param onColorChange Live callback as user drags (for canvas preview)
 * @param onDismiss Callback when popover is dismissed
 * @param onEyedropperClick Callback when eyedropper button is tapped
 */
@Composable
fun ColorPickerPopover(
    isVisible: Boolean,
    currentColor: Color,
    anchorOffset: Int = 0,
    onColorChange: (Color) -> Unit,
    onDismiss: () -> Unit,
    onEyedropperClick: () -> Unit = {}
) {
    val viewModel: ColorPickerViewModel = viewModel()
    val view = LocalView.current
    
    // Initialize with current color when popover becomes visible
    LaunchedEffect(isVisible, currentColor) {
        if (isVisible) {
            viewModel.initialize(currentColor)
        }
    }
    
    // Collect state from ViewModel
    val hue by viewModel.hue.collectAsState()
    val saturation by viewModel.saturation.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val color by viewModel.currentColor.collectAsState()
    val previousColor by viewModel.previousColor.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()
    
    // Live update callback - notify parent as user drags
    LaunchedEffect(color) {
        onColorChange(color)
    }
    
    // Animated visibility with slide from right + fade
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth }, // Start from right edge
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(
            animationSpec = tween(durationMillis = 200)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth }, // Exit to right edge
            animationSpec = tween(durationMillis = 150)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 150)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Semi-transparent scrim for tap-outside-to-dismiss
            // Keep it light to maintain canvas visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, // No ripple
                        onClick = {
                            viewModel.confirmColor() // Save to recent colors
                            onDismiss()
                        }
                    )
            )
            
            // Popover panel positioned at right edge
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .wrapContentHeight()
                    .align(Alignment.TopEnd)
                    .padding(end = 56.dp, top = anchorOffset.dp) // 56dp clears edge button bar
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevent click-through to scrim
                    ),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1C1C1E) // Dark background per spec
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(max = 400.dp), // Max height constraint
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Compact Color Disc (200×200dp)
                    CompactColorDisc(
                        hue = hue,
                        saturation = saturation,
                        brightness = brightness,
                        onHueChange = { newHue ->
                            viewModel.setHue(newHue)
                        },
                        onSatBriChange = { sat, bri ->
                            viewModel.setSaturation(sat)
                            viewModel.setBrightness(bri)
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // Divider
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    
                    // Recent Colors Row (8 swatches)
                    RecentColorsRow(
                        recentColors = recentColors,
                        currentColorArgb = color.toArgb(),
                        onColorSelected = { argb ->
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.setColor(Color(argb))
                        }
                    )
                    
                    // Divider
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    
                    // Quick Actions Row
                    ColorQuickActions(
                        currentColor = color,
                        previousColor = previousColor,
                        onSwapColors = {
                            viewModel.swapCurrentPrevious()
                        },
                        onEyedropperClick = {
                            viewModel.confirmColor()
                            onDismiss()
                            onEyedropperClick()
                        },
                        showHex = true
                    )
                }
            }
        }
    }
}

/**
 * Simplified version that auto-manages its visibility state.
 * Use this when the parent just needs to know about color changes.
 * 
 * @param initialColor Starting color
 * @param onColorChange Live callback as user adjusts color
 * @param onEyedropperClick Callback when eyedropper is tapped
 * @param content Content to wrap (clicking this toggles the popover)
 */
@Composable
fun ColorPickerPopoverHost(
    initialColor: Color,
    onColorChange: (Color) -> Unit,
    onEyedropperClick: () -> Unit = {},
    content: @Composable (isOpen: Boolean, toggle: () -> Unit) -> Unit
) {
    var isOpen by remember { mutableStateOf(false) }
    var currentColor by remember { mutableStateOf(initialColor) }
    
    // Update current color when initial changes
    LaunchedEffect(initialColor) {
        currentColor = initialColor
    }
    
    Box {
        content(isOpen) { isOpen = !isOpen }
        
        ColorPickerPopover(
            isVisible = isOpen,
            currentColor = currentColor,
            onColorChange = { color ->
                currentColor = color
                onColorChange(color)
            },
            onDismiss = { isOpen = false },
            onEyedropperClick = onEyedropperClick
        )
    }
}
