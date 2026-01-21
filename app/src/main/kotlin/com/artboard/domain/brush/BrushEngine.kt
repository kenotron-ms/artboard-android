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
     */
    private fun renderStamp(
        canvas: Canvas,
        point: Point,
        brush: Brush,
        color: Int
    ) {
        val size = brush.getEffectiveSize(point.pressure)
        val opacity = brush.getEffectiveOpacity(point.pressure)
        
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
        
        // Draw the stamp
        val radius = size / 2f
        canvas.drawCircle(point.x, point.y, radius, paint)
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
