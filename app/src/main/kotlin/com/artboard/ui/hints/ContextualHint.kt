package com.artboard.ui.hints

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Contextual hint that shows once per feature
 * Automatically dismisses after 5 seconds or on user tap
 * 
 * Usage:
 * ```
 * ContextualHint(
 *     hintId = "first_brush_use",
 *     message = "Tap any brush to see a preview",
 *     onDismiss = { }
 * )
 * ```
 */
@Composable
fun ContextualHint(
    hintId: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissDelay: Long = 5000L
) {
    val context = LocalContext.current
    val hintManager = remember { HintManager(context) }
    var isVisible by remember { mutableStateOf(!hintManager.hasShown(hintId)) }
    
    // Auto-dismiss after delay
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(autoDismissDelay)
            hintManager.markShown(hintId)
            isVisible = false
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF4A90E2),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .clickable {
                        hintManager.markShown(hintId)
                        isVisible = false
                        onDismiss()
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss hint",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Contextual hint with arrow pointing to target element
 */
@Composable
fun ContextualHintWithArrow(
    hintId: String,
    message: String,
    targetOffset: IntOffset,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissDelay: Long = 5000L
) {
    Column(
        modifier = modifier.offset { targetOffset }
    ) {
        // Arrow pointing up
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(0.dp, 8.dp)
                .background(Color(0xFF4A90E2))
        )
        
        ContextualHint(
            hintId = hintId,
            message = message,
            onDismiss = onDismiss,
            autoDismissDelay = autoDismissDelay
        )
    }
}

/**
 * Manager for tracking which hints have been shown
 * Persists state to SharedPreferences
 */
class HintManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "hints"
        
        // Predefined hint IDs
        const val HINT_BRUSH_SELECTOR = "brush_selector_intro"
        const val HINT_LAYERS_PANEL = "layers_panel_intro"
        const val HINT_COLOR_PICKER = "color_picker_intro"
        const val HINT_UNDO_GESTURE = "undo_gesture_intro"
        const val HINT_UI_TOGGLE = "ui_toggle_intro"
        const val HINT_TRANSFORM_TOOLS = "transform_tools_intro"
        const val HINT_BLEND_MODES = "blend_modes_intro"
        const val HINT_PRESSURE_SETTINGS = "pressure_settings_intro"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if a hint has been shown
     */
    fun hasShown(hintId: String): Boolean {
        return prefs.getBoolean(hintId, false)
    }
    
    /**
     * Mark a hint as shown
     */
    fun markShown(hintId: String) {
        prefs.edit().putBoolean(hintId, true).apply()
    }
    
    /**
     * Reset a specific hint
     */
    fun resetHint(hintId: String) {
        prefs.edit().putBoolean(hintId, false).apply()
    }
    
    /**
     * Reset all hints (for testing or "View Tutorial Again")
     */
    fun resetAll() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Get count of hints shown
     */
    fun getShownCount(): Int {
        return prefs.all.count { it.value == true }
    }
}

/**
 * Hook to use hint manager in Composables
 */
@Composable
fun rememberHintManager(): HintManager {
    val context = LocalContext.current
    return remember { HintManager(context) }
}

/**
 * Show hint if not previously shown
 */
@Composable
fun ShowHintOnce(
    hintId: String,
    message: String,
    condition: Boolean = true,
    modifier: Modifier = Modifier
) {
    val hintManager = rememberHintManager()
    var shouldShow by remember { mutableStateOf(false) }
    
    LaunchedEffect(condition) {
        if (condition && !hintManager.hasShown(hintId)) {
            shouldShow = true
        }
    }
    
    if (shouldShow) {
        ContextualHint(
            hintId = hintId,
            message = message,
            onDismiss = { shouldShow = false },
            modifier = modifier
        )
    }
}
