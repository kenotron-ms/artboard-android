package com.artboard.ui.canvas

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.composed

/**
 * Edge zones for tap detection
 */
enum class Edge { TOP, BOTTOM, LEFT, RIGHT }

/**
 * Detect four-finger tap gesture
 * Used for toggling UI visibility
 */
fun Modifier.detectFourFingerTap(
    onTap: () -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        
        // Wait for more fingers (within 100ms window)
        var pointerCount = 1
        val startTime = System.currentTimeMillis()
        
        while (pointerCount < 4 && System.currentTimeMillis() - startTime < 100) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            pointerCount = event.changes.count { it.pressed }
            
            if (pointerCount >= 4) {
                break
            }
        }
        
        if (pointerCount >= 4) {
            // Wait for quick release (within 300ms)
            val tapTime = System.currentTimeMillis()
            var allReleased = false
            
            while (System.currentTimeMillis() - tapTime < 300) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                allReleased = event.changes.all { !it.pressed }
                
                if (allReleased) {
                    onTap()
                    break
                }
            }
        }
    }
}

/**
 * Detect taps on screen edges
 * Used for revealing UI by tapping edge zones
 */
fun Modifier.detectEdgeTap(
    edgeMargin: Dp = 48.dp,
    onEdgeTap: (Edge) -> Unit
): Modifier = composed {
    this.pointerInput(Unit) {
        detectTapGestures { offset ->
            val marginPx = edgeMargin.toPx()
            
            val edge = when {
                offset.y < marginPx -> Edge.TOP
                offset.y > size.height - marginPx -> Edge.BOTTOM
                offset.x < marginPx -> Edge.LEFT
                offset.x > size.width - marginPx -> Edge.RIGHT
                else -> null
            }
            
            edge?.let { onEdgeTap(it) }
        }
    }
}
