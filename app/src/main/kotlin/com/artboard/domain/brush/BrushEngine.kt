package com.artboard.domain.brush

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.artboard.data.model.Brush
import com.artboard.data.model.BrushType
import com.artboard.data.model.Point
import com.artboard.data.model.Stroke
import kotlin.math.min

/**
 * Renders brush strokes to bitmaps using stamp-based rendering
 */
class BrushEngine {
    
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        isFilterBitmap = true
    }
    
    private val interpolator = StrokeInterpolator()
    
    /**
     * Render a complete stroke to a bitmap
     */
    fun renderStroke(stroke: Stroke, targetBitmap: Bitmap) {
        if (stroke.points.isEmpty()) return
        
        val canvas = Canvas(targetBitmap)
        
        // Interpolate points based on brush spacing
        val spacing = stroke.brush.getSpacingDistance()
        val interpolatedPoints = interpolator.interpolate(stroke.points, spacing)
        
        // Render each point as a stamp
        interpolatedPoints.forEach { point ->
            renderStamp(canvas, point, stroke.brush, stroke.color)
        }
    }
    
    /**
     * Render a single brush stamp at the given point
     * Supports tilt-based size and rotation dynamics
     */
    private fun renderStamp(
        canvas: Canvas,
        point: Point,
        brush: Brush,
        color: Int
    ) {
        val size = calculateEffectiveSize(point, brush)
        val opacity = brush.getEffectiveOpacity(point.pressure)
        val rotation = calculateStampRotation(point, brush)
        
        // Set up paint based on brush type
        when (brush.type) {
            BrushType.ERASER -> {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                paint.alpha = (opacity * 255).toInt()
                paint.shader = null
            }
            else -> {
                paint.xfermode = null
                paint.color = color
                paint.alpha = (opacity * 255).toInt()
                
                // Apply softness via radial gradient
                if (brush.hardness < 1f) {
                    val gradient = createBrushGradient(
                        point.x,
                        point.y,
                        size / 2f,
                        brush.hardness,
                        color,
                        opacity
                    )
                    paint.shader = gradient
                } else {
                    paint.shader = null
                }
            }
        }
        
        // Apply flow (for airbrush effect)
        val flowAlpha = (opacity * brush.flow * 255).toInt()
        paint.alpha = min(paint.alpha, flowAlpha)
        
        // Draw the stamp with rotation and tilt
        val radius = size / 2f
        
        if (rotation != 0f || (brush.tiltSizeEnabled && point.hasTilt())) {
            canvas.save()
            
            // Apply rotation if tilt angle is enabled
            if (rotation != 0f) {
                canvas.rotate(rotation, point.x, point.y)
            }
            
            // Apply scaling for tilt (flatten vertically)
            if (brush.tiltSizeEnabled && point.hasTilt()) {
                val tiltMag = point.tiltMagnitude() / 90f
                val scaleY = 1f - (tiltMag * 0.7f) // Flatten up to 70%
                canvas.scale(1f, scaleY, point.x, point.y)
            }
            
            canvas.drawCircle(point.x, point.y, radius, paint)
            canvas.restore()
        } else {
            // No tilt or rotation - simple draw
            canvas.drawCircle(point.x, point.y, radius, paint)
        }
        
        // Reset shader for next stamp
        paint.shader = null
    }
    
    /**
     * Calculate effective brush size with pressure and tilt dynamics
     */
    private fun calculateEffectiveSize(point: Point, brush: Brush): Float {
        var size = brush.size
        
        // Pressure dynamics
        if (brush.pressureSizeEnabled) {
            val factor = brush.minPressureSize + (1f - brush.minPressureSize) * point.pressure
            size *= factor
        }
        
        // Tilt dynamics (brush widens when tilted, like real pencil)
        if (brush.tiltSizeEnabled && point.hasTilt()) {
            val tiltMag = point.tiltMagnitude() / 90f // Normalize to 0-1
            val factor = lerp(brush.tiltSizeMin, brush.tiltSizeMax, tiltMag)
            size *= factor
        }
        
        return size.coerceIn(0.5f, 1000f)
    }
    
    /**
     * Calculate stamp rotation based on tilt direction
     */
    private fun calculateStampRotation(point: Point, brush: Brush): Float {
        if (!brush.tiltAngleEnabled || !point.hasTilt()) {
            return 0f
        }
        
        return point.tiltDirection()
    }
    
    /**
     * Linear interpolation helper
     */
    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }
    
    /**
     * Create a radial gradient for soft brushes
     */
    private fun createBrushGradient(
        centerX: Float,
        centerY: Float,
        radius: Float,
        hardness: Float,
        color: Int,
        opacity: Float
    ): RadialGradient {
        // Hardness affects where the gradient starts fading
        val fadeStart = hardness.coerceIn(0.1f, 1f)
        
        val colors = intArrayOf(
            applyAlpha(color, opacity),
            applyAlpha(color, opacity),
            applyAlpha(color, 0f)
        )
        
        val positions = floatArrayOf(
            0f,
            fadeStart,
            1f
        )
        
        return RadialGradient(
            centerX,
            centerY,
            radius,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
    }
    
    /**
     * Apply alpha to a color
     */
    private fun applyAlpha(color: Int, alpha: Float): Int {
        val a = ((alpha * 255).toInt() and 0xFF) shl 24
        val rgb = color and 0x00FFFFFF
        return a or rgb
    }
    
    /**
     * Render a stroke incrementally (for real-time drawing)
     * Only renders new points since last call
     */
    fun renderStrokeIncremental(
        stroke: Stroke,
        targetBitmap: Bitmap,
        startIndex: Int = 0
    ): Int {
        if (stroke.points.isEmpty() || startIndex >= stroke.points.size) {
            return startIndex
        }
        
        val canvas = Canvas(targetBitmap)
        val spacing = stroke.brush.getSpacingDistance()
        
        // Get points to render (from startIndex onward)
        val pointsToRender = stroke.points.subList(startIndex, stroke.points.size)
        
        if (pointsToRender.isEmpty()) return startIndex
        
        // For incremental rendering, we need to include the previous point
        // for smooth interpolation
        val previousPoint = if (startIndex > 0) {
            listOf(stroke.points[startIndex - 1])
        } else {
            emptyList()
        }
        
        val allPoints = previousPoint + pointsToRender
        val interpolatedPoints = interpolator.interpolate(allPoints, spacing)
        
        // Render the interpolated points
        interpolatedPoints.forEach { point ->
            renderStamp(canvas, point, stroke.brush, stroke.color)
        }
        
        return stroke.points.size
    }
    
    /**
     * Create a preview bitmap of what the brush looks like
     */
    fun createBrushPreview(brush: Brush, color: Int, size: Int = 100): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw brush at center with full pressure
        val center = size / 2f
        val point = Point(center, center, 1f)
        
        renderStamp(canvas, point, brush, color)
        
        return bitmap
    }
}
