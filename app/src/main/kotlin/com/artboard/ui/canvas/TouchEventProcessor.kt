package com.artboard.ui.canvas

import android.view.MotionEvent
import com.artboard.data.model.Point

/**
 * Processes touch events and extracts drawing points with full stylus data
 * including pressure, tilt, and orientation
 */
class TouchEventProcessor {
    
    /**
     * Extract all points from a motion event, including historical data
     * This ensures we don't miss any points for smooth drawing
     */
    fun extractPoints(event: MotionEvent): List<Point> {
        val points = mutableListOf<Point>()
        
        // Process historical events first (batched events between frames)
        for (i in 0 until event.historySize) {
            val point = Point(
                x = event.getHistoricalX(i),
                y = event.getHistoricalY(i),
                pressure = normalizePressure(event.getHistoricalPressure(i)),
                tiltX = getHistoricalTiltX(event, i),
                tiltY = getHistoricalTiltY(event, i),
                orientation = getHistoricalOrientation(event, i),
                timestamp = event.getHistoricalEventTime(i)
            )
            points.add(point)
        }
        
        // Process current event
        val point = Point(
            x = event.x,
            y = event.y,
            pressure = normalizePressure(event.pressure),
            tiltX = getTiltX(event),
            tiltY = getTiltY(event),
            orientation = getOrientation(event),
            timestamp = event.eventTime
        )
        points.add(point)
        
        return points
    }
    
    /**
     * Check if the event is from a stylus
     */
    fun isStylus(event: MotionEvent): Boolean {
        return event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
    }
    
    /**
     * Get tilt X angle in degrees (-90 to +90)
     */
    fun getTiltX(event: MotionEvent): Float {
        val tiltRad = event.getAxisValue(MotionEvent.AXIS_TILT)
        return (tiltRad * 90f).coerceIn(-90f, 90f)
    }
    
    /**
     * Get tilt Y angle in degrees (-90 to +90)
     */
    fun getTiltY(event: MotionEvent): Float {
        // Some devices provide AXIS_TILT_Y, others compute from AXIS_TILT
        val tiltRad = event.getAxisValue(MotionEvent.AXIS_TILT)
        return (tiltRad * 90f).coerceIn(-90f, 90f)
    }
    
    /**
     * Get historical tilt X angle
     */
    private fun getHistoricalTiltX(event: MotionEvent, index: Int): Float {
        val tiltRad = event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, index)
        return (tiltRad * 90f).coerceIn(-90f, 90f)
    }
    
    /**
     * Get historical tilt Y angle
     */
    private fun getHistoricalTiltY(event: MotionEvent, index: Int): Float {
        val tiltRad = event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, index)
        return (tiltRad * 90f).coerceIn(-90f, 90f)
    }
    
    /**
     * Get stylus orientation/rotation in degrees (0-360)
     */
    fun getOrientation(event: MotionEvent): Float {
        val orientationRad = event.orientation
        val orientationDeg = Math.toDegrees(orientationRad.toDouble()).toFloat()
        return (orientationDeg + 360f) % 360f
    }
    
    /**
     * Get historical orientation
     */
    private fun getHistoricalOrientation(event: MotionEvent, index: Int): Float {
        val orientationRad = event.getHistoricalOrientation(index)
        val orientationDeg = Math.toDegrees(orientationRad.toDouble()).toFloat()
        return (orientationDeg + 360f) % 360f
    }
    
    /**
     * Normalize pressure values (some devices report different ranges)
     */
    fun normalizePressure(pressure: Float): Float {
        return pressure.coerceIn(0f, 1f)
    }
}
