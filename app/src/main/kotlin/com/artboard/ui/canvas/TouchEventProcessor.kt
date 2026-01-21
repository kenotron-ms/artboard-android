package com.artboard.ui.canvas

import android.view.MotionEvent
import com.artboard.data.model.Point

/**
 * Processes touch events and extracts drawing points with pressure
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
                pressure = event.getHistoricalPressure(i),
                timestamp = event.getHistoricalEventTime(i)
            )
            points.add(point)
        }
        
        // Process current event
        val point = Point(
            x = event.x,
            y = event.y,
            pressure = event.pressure,
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
     * Get the tilt angle if available (for advanced stylus)
     */
    fun getTilt(event: MotionEvent): Float {
        return event.getAxisValue(MotionEvent.AXIS_TILT)
    }
    
    /**
     * Get the orientation/rotation of the stylus
     */
    fun getOrientation(event: MotionEvent): Float {
        return event.orientation
    }
    
    /**
     * Normalize pressure values (some devices report different ranges)
     */
    fun normalizePressure(pressure: Float): Float {
        return pressure.coerceIn(0f, 1f)
    }
}
