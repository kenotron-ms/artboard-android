package com.artboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.artboard.ui.canvas.CanvasScreenV2
import com.artboard.ui.canvas.CanvasViewModel
import com.artboard.ui.theme.ArtboardTheme

/**
 * Main Activity for Artboard
 * 
 * Uses the new edge-based UI (CanvasScreenV2) which provides:
 * - Full-screen immersive canvas
 * - Left edge: Size/Opacity sliders + Eyedropper
 * - Right edge: Category buttons (Brush, Color, Layers, Transform, Settings)
 * - Compact popovers instead of full-screen dialogs
 * - Auto-hide during drawing (2s delay, fade to 30%)
 * - Four-finger tap to toggle UI completely
 * 
 * Based on:
 * - design/components/EdgeControlBar.md
 * - docs/TOOL_ORGANIZATION_PHILOSOPHY.md
 */
class MainActivity : ComponentActivity() {
    
    private val viewModel: CanvasViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display for immersive canvas experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Hide system bars for full immersion
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        setContent {
            ArtboardTheme {
                // Use the new edge-based canvas screen
                CanvasScreenV2(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-hide system bars when regaining focus
            val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
