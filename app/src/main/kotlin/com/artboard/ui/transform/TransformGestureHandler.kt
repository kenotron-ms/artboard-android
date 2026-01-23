package com.artboard.ui.transform

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import com.artboard.data.model.Handle
import com.artboard.data.model.Transform
import com.artboard.data.model.TransformType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Gesture handler for transform operations
 * Handles drag, pinch, and rotate gestures with snapping
 */
class TransformGestureHandler(
    private val view: View? = null // For haptic feedback
) {
    
    private var lastRotation = 0f
    private var lastSnapAngle: Float? = null
    
    /**
     * Detect transform gestures (drag, pinch, rotate)
     */
    suspend fun PointerInputScope.detectTransformGestures(
        transformType: TransformType,
        currentTransform: Transform,
        onTransformChange: (Transform) -> Unit,
        isInsideBounds: (Offset) -> Boolean,
        getActiveHandle: (Offset) -> Handle
    ) {
        awaitEachGesture {
            val firstPointer = awaitFirstDown(requireUnconsumed = false)
            val position = firstPointer.position
            
            // Check if touch is inside bounds
            if (!isInsideBounds(position)) {
                return@awaitEachGesture
            }
            
            // Detect which handle (if any) was touched
            val activeHandle = getActiveHandle(position)
            
            do {
                val event = awaitPointerEvent()
                val pointers = event.changes.filter { !it.isConsumed }
                
                if (pointers.isEmpty()) break
                
                when (pointers.size) {
                    1 -> {
                        // Single finger drag (move or scale from handle)
                        val pointer = pointers.first()
                        
                        if (pointer.positionChanged()) {
                            val pan = pointer.position - pointer.previousPosition
                            
                            if (activeHandle == Handle.NONE) {
                                // Move content
                                onTransformChange(currentTransform.withTranslation(pan))
                            } else {
                                // Scale from handle
                                handleCornerDrag(
                                    activeHandle,
                                    pan,
                                    transformType,
                                    currentTransform,
                                    onTransformChange
                                )
                            }
                            
                            pointer.consume()
                        }
                    }
                    
                    2 -> {
                        // Two-finger pinch/rotate
                        val zoom = event.calculateZoom()
                        val rotation = event.calculateRotation()
                        val pan = event.calculatePan()
                        
                        // Apply scale
                        if (zoom != 1f) {
                            val newTransform = when (transformType) {
                                TransformType.UNIFORM -> {
                                    val newScale = currentTransform.scale * zoom
                                    currentTransform.withUniformScale(newScale)
                                }
                                TransformType.FREE -> {
                                    val newScaleX = currentTransform.scaleX * zoom
                                    val newScaleY = currentTransform.scaleY * zoom
                                    currentTransform.withFreeScale(newScaleX, newScaleY)
                                }
                                else -> currentTransform
                            }
                            
                            onTransformChange(newTransform)
                        }
                        
                        // Apply rotation
                        if (rotation != 0f) {
                            val newRotation = currentTransform.rotation + rotation
                            val snappedTransform = currentTransform
                                .withRotation(newRotation)
                                .withSnappedRotation(threshold = 5f)
                            
                            // Trigger haptic feedback on snap
                            if (snappedTransform.rotation != currentTransform.rotation) {
                                val snapAngle = snappedTransform.rotation
                                if (snapAngle != lastSnapAngle) {
                                    triggerHaptic(
                                        if (Transform.STRONG_SNAP_ANGLES.contains(snapAngle)) {
                                            HapticFeedbackConstants.CONTEXT_CLICK
                                        } else {
                                            HapticFeedbackConstants.CLOCK_TICK
                                        }
                                    )
                                    lastSnapAngle = snapAngle
                                }
                            }
                            
                            onTransformChange(snappedTransform)
                        }
                        
                        // Apply translation
                        if (pan != Offset.Zero) {
                            onTransformChange(currentTransform.withTranslation(pan))
                        }
                        
                        pointers.forEach { it.consume() }
                    }
                }
            } while (pointers.any { it.pressed })
        }
    }
    
    /**
     * Handle corner/edge handle dragging for scaling
     */
    private fun handleCornerDrag(
        handle: Handle,
        pan: Offset,
        transformType: TransformType,
        currentTransform: Transform,
        onTransformChange: (Transform) -> Unit
    ) {
        when (transformType) {
            TransformType.UNIFORM -> {
                // Maintain aspect ratio
                val scaleDelta = calculateUniformScale(handle, pan)
                val newScale = (currentTransform.scale * (1f + scaleDelta)).coerceIn(0.1f, 10f)
                onTransformChange(currentTransform.withUniformScale(newScale))
            }
            
            TransformType.FREE -> {
                // Independent X/Y scaling
                val (scaleXDelta, scaleYDelta) = calculateFreeScale(handle, pan)
                val newScaleX = (currentTransform.scaleX * (1f + scaleXDelta)).coerceIn(0.1f, 10f)
                val newScaleY = (currentTransform.scaleY * (1f + scaleYDelta)).coerceIn(0.1f, 10f)
                onTransformChange(currentTransform.withFreeScale(newScaleX, newScaleY))
            }
            
            else -> {
                // No scaling for other modes
            }
        }
    }
    
    /**
     * Calculate uniform scale factor from handle drag
     */
    private fun calculateUniformScale(handle: Handle, pan: Offset): Float {
        val distance = sqrt(pan.x * pan.x + pan.y * pan.y)
        val sign = when (handle) {
            Handle.TOP_LEFT -> if (pan.x < 0 || pan.y < 0) -1f else 1f
            Handle.TOP_RIGHT -> if (pan.x > 0 || pan.y < 0) 1f else -1f
            Handle.BOTTOM_LEFT -> if (pan.x < 0 || pan.y > 0) 1f else -1f
            Handle.BOTTOM_RIGHT -> if (pan.x > 0 || pan.y > 0) 1f else -1f
            else -> 1f
        }
        
        return sign * distance * 0.01f // Scale factor
    }
    
    /**
     * Calculate independent X/Y scale factors from handle drag
     */
    private fun calculateFreeScale(handle: Handle, pan: Offset): Pair<Float, Float> {
        val scaleXDelta = when (handle) {
            Handle.LEFT, Handle.TOP_LEFT, Handle.BOTTOM_LEFT -> -pan.x * 0.01f
            Handle.RIGHT, Handle.TOP_RIGHT, Handle.BOTTOM_RIGHT -> pan.x * 0.01f
            Handle.TOP, Handle.BOTTOM -> 0f
            else -> 0f
        }
        
        val scaleYDelta = when (handle) {
            Handle.TOP, Handle.TOP_LEFT, Handle.TOP_RIGHT -> -pan.y * 0.01f
            Handle.BOTTOM, Handle.BOTTOM_LEFT, Handle.BOTTOM_RIGHT -> pan.y * 0.01f
            Handle.LEFT, Handle.RIGHT -> 0f
            else -> 0f
        }
        
        return Pair(scaleXDelta, scaleYDelta)
    }
    
    /**
     * Trigger haptic feedback
     */
    private fun triggerHaptic(feedbackConstant: Int) {
        view?.performHapticFeedback(
            feedbackConstant,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
    }
    
    /**
     * Calculate angle between two points
     */
    private fun calculateAngle(center: Offset, point: Offset): Float {
        val dx = point.x - center.x
        val dy = point.y - center.y
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(p1: Offset, p2: Offset): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Check if rotation angle is close to snap angle
     */
    private fun isCloseToSnapAngle(angle: Float, threshold: Float = 5f): Float? {
        val normalizedAngle = angle % 360f
        return Transform.SNAP_ANGLES.firstOrNull { snapAngle ->
            abs(normalizedAngle - snapAngle) < threshold
        }
    }
}
