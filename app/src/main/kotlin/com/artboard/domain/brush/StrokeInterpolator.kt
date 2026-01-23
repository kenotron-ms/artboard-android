package com.artboard.domain.brush

import com.artboard.data.model.Point
import kotlin.math.sqrt

/**
 * Interpolates stroke points using Catmull-Rom splines for smooth curves
 */
class StrokeInterpolator {
    
    /**
     * Interpolate between points to create a smooth stroke
     * Uses Catmull-Rom spline interpolation
     */
    fun interpolate(points: List<Point>, spacing: Float): List<Point> {
        if (points.size < 2) return points
        if (points.size == 2) return interpolateLinear(points[0], points[1], spacing)
        
        val result = mutableListOf<Point>()
        
        // Add first point
        result.add(points[0])
        
        // Interpolate between each pair of points
        for (i in 0 until points.size - 1) {
            val p0 = if (i > 0) points[i - 1] else points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]
            
            val interpolated = interpolateCatmullRom(p0, p1, p2, p3, spacing)
            result.addAll(interpolated)
        }
        
        return result
    }
    
    /**
     * Catmull-Rom spline interpolation between p1 and p2
     * Using p0 and p3 as control points
     */
    private fun interpolateCatmullRom(
        p0: Point,
        p1: Point,
        p2: Point,
        p3: Point,
        spacing: Float
    ): List<Point> {
        val result = mutableListOf<Point>()
        
        // Calculate distance between p1 and p2
        val distance = p1.distanceTo(p2)
        if (distance < 0.01f) return emptyList()
        
        // Calculate number of steps based on spacing
        val steps = (distance / spacing).toInt().coerceAtLeast(1)
        
        for (step in 1..steps) {
            val t = step.toFloat() / steps
            
            // Catmull-Rom basis functions
            val t2 = t * t
            val t3 = t2 * t
            
            val q0 = -t3 + 2f * t2 - t
            val q1 = 3f * t3 - 5f * t2 + 2f
            val q2 = -3f * t3 + 4f * t2 + t
            val q3 = t3 - t2
            
            val x = 0.5f * (p0.x * q0 + p1.x * q1 + p2.x * q2 + p3.x * q3)
            val y = 0.5f * (p0.y * q0 + p1.y * q1 + p2.y * q2 + p3.y * q3)
            val pressure = 0.5f * (p0.pressure * q0 + p1.pressure * q1 + p2.pressure * q2 + p3.pressure * q3)
            val tiltX = 0.5f * (p0.tiltX * q0 + p1.tiltX * q1 + p2.tiltX * q2 + p3.tiltX * q3)
            val tiltY = 0.5f * (p0.tiltY * q0 + p1.tiltY * q1 + p2.tiltY * q2 + p3.tiltY * q3)
            val orientation = 0.5f * (p0.orientation * q0 + p1.orientation * q1 + p2.orientation * q2 + p3.orientation * q3)
            
            result.add(Point(x, y, pressure.coerceIn(0f, 1f), tiltX, tiltY, orientation))
        }
        
        return result
    }
    
    /**
     * Linear interpolation between two points
     */
    private fun interpolateLinear(p1: Point, p2: Point, spacing: Float): List<Point> {
        val result = mutableListOf<Point>()
        result.add(p1)
        
        val distance = p1.distanceTo(p2)
        if (distance < 0.01f) return result
        
        val steps = (distance / spacing).toInt().coerceAtLeast(1)
        
        for (step in 1..steps) {
            val t = step.toFloat() / steps
            val x = p1.x + (p2.x - p1.x) * t
            val y = p1.y + (p2.y - p1.y) * t
            val pressure = p1.pressure + (p2.pressure - p1.pressure) * t
            val tiltX = p1.tiltX + (p2.tiltX - p1.tiltX) * t
            val tiltY = p1.tiltY + (p2.tiltY - p1.tiltY) * t
            val orientation = p1.orientation + (p2.orientation - p1.orientation) * t
            
            result.add(Point(x, y, pressure, tiltX, tiltY, orientation))
        }
        
        return result
    }
    
    /**
     * Simplify a stroke by removing redundant points
     * Uses Ramer-Douglas-Peucker algorithm
     */
    fun simplify(points: List<Point>, tolerance: Float = 2f): List<Point> {
        if (points.size < 3) return points
        
        return ramerDouglasPeucker(points, 0, points.size - 1, tolerance)
    }
    
    private fun ramerDouglasPeucker(
        points: List<Point>,
        start: Int,
        end: Int,
        tolerance: Float
    ): List<Point> {
        if (end <= start + 1) {
            return listOf(points[start], points[end])
        }
        
        // Find point with maximum distance from line
        var maxDistance = 0f
        var maxIndex = start
        
        for (i in start + 1 until end) {
            val distance = perpendicularDistance(
                points[i],
                points[start],
                points[end]
            )
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }
        
        // If max distance is greater than tolerance, recursively simplify
        if (maxDistance > tolerance) {
            val left = ramerDouglasPeucker(points, start, maxIndex, tolerance)
            val right = ramerDouglasPeucker(points, maxIndex, end, tolerance)
            
            // Combine results (remove duplicate point at junction)
            return left.dropLast(1) + right
        } else {
            // All points can be removed
            return listOf(points[start], points[end])
        }
    }
    
    private fun perpendicularDistance(point: Point, lineStart: Point, lineEnd: Point): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        
        val norm = sqrt(dx * dx + dy * dy)
        if (norm < 0.01f) return point.distanceTo(lineStart)
        
        val u = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / (norm * norm)
        
        val projX = lineStart.x + u * dx
        val projY = lineStart.y + u * dy
        
        val distX = point.x - projX
        val distY = point.y - projY
        
        return sqrt(distX * distX + distY * distY)
    }
}
