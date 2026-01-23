package com.artboard.domain.sample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.artboard.data.model.*
import com.artboard.domain.brush.BrushEngine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates sample artwork to demonstrate Artboard features
 * Creates a pre-drawn project with multiple layers, brushes, and blend modes
 */
class SampleArtworkGenerator {
    
    private val brushEngine = BrushEngine()
    
    /**
     * Create a sample project with pre-drawn content
     * Demonstrates layers, brushes, colors, and blend modes
     * 
     * Project structure:
     * - Layer 0: Background (gradient)
     * - Layer 1: Sketch (pencil strokes)
     * - Layer 2: Color (colored brush strokes with multiply blend)
     * - Layer 3: Highlights (bright accents)
     */
    fun generateSampleProject(): Project {
        val width = 1024
        val height = 1024
        
        // Create background layer with gradient
        val backgroundLayer = Layer.create(width, height, "Background")
        drawGradientBackground(backgroundLayer.bitmap)
        
        // Create sketch layer with pencil strokes
        val sketchLayer = Layer.create(width, height, "Sketch").copy(
            opacity = 0.7f,
            blendMode = BlendMode.NORMAL
        )
        drawSketchStrokes(sketchLayer.bitmap)
        
        // Create color layer with painted strokes
        val colorLayer = Layer.create(width, height, "Color").copy(
            opacity = 0.8f,
            blendMode = BlendMode.MULTIPLY
        )
        drawColorStrokes(colorLayer.bitmap)
        
        // Create highlights layer
        val highlightsLayer = Layer.create(width, height, "Highlights").copy(
            opacity = 0.9f,
            blendMode = BlendMode.ADD
        )
        drawHighlights(highlightsLayer.bitmap)
        
        return Project(
            name = "Welcome Sample",
            width = width,
            height = height,
            layers = listOf(backgroundLayer, sketchLayer, colorLayer, highlightsLayer),
            backgroundColor = Color.WHITE
        )
    }
    
    /**
     * Draw a gradient background
     */
    private fun drawGradientBackground(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        
        // Subtle gradient from light gray to white
        val gradient = LinearGradient(
            0f, 0f,
            0f, bitmap.height.toFloat(),
            Color.rgb(245, 245, 250),
            Color.rgb(255, 255, 255),
            Shader.TileMode.CLAMP
        )
        
        val paint = Paint().apply {
            shader = gradient
        }
        
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
    }
    
    /**
     * Draw sketch strokes using pencil brush
     */
    private fun drawSketchStrokes(bitmap: Bitmap) {
        val pencil = Brush.pencil().copy(size = 3f)
        val color = Color.rgb(60, 60, 60)
        
        // Create artistic curved strokes
        val strokes = listOf(
            // Flowing curve across canvas
            createCurveStroke(
                pencil, 
                color,
                startX = 200f,
                startY = 400f,
                endX = 800f,
                endY = 600f,
                curves = 3,
                id = "sketch_curve_1"
            ),
            
            // Spiral shape
            createSpiralStroke(
                pencil,
                color,
                centerX = 512f,
                centerY = 512f,
                radius = 200f,
                turns = 2.5f,
                id = "sketch_spiral"
            ),
            
            // Additional flowing lines
            createCurveStroke(
                pencil,
                color,
                startX = 300f,
                startY = 300f,
                endX = 700f,
                endY = 300f,
                curves = 2,
                id = "sketch_curve_2"
            )
        )
        
        strokes.forEach { stroke ->
            brushEngine.renderStroke(stroke, bitmap)
        }
    }
    
    /**
     * Draw colored strokes using various brushes
     */
    private fun drawColorStrokes(bitmap: Bitmap) {
        val brush = Brush.pen().copy(size = 20f, opacity = 0.6f)
        
        // Vibrant colors
        val colors = listOf(
            Color.rgb(230, 115, 115), // Soft red
            Color.rgb(100, 181, 246), // Soft blue
            Color.rgb(129, 199, 132), // Soft green
            Color.rgb(255, 167, 38)   // Soft orange
        )
        
        colors.forEachIndexed { index, color ->
            val angle = (index * 90f) * (PI / 180f).toFloat()
            val startX = 512f + cos(angle) * 150f
            val startY = 512f + sin(angle) * 150f
            val endX = 512f + cos(angle + PI.toFloat()) * 150f
            val endY = 512f + sin(angle + PI.toFloat()) * 150f
            
            val stroke = createCurveStroke(
                brush,
                color,
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                curves = 1,
                id = "color_stroke_$index"
            )
            
            brushEngine.renderStroke(stroke, bitmap)
        }
    }
    
    /**
     * Draw highlight strokes
     */
    private fun drawHighlights(bitmap: Bitmap) {
        val brush = Brush.airbrush().copy(size = 40f, opacity = 0.4f)
        val color = Color.rgb(255, 235, 59) // Bright yellow
        
        // Create soft highlight dots
        val highlights = listOf(
            Point(400f, 400f, 0.7f),
            Point(600f, 400f, 0.8f),
            Point(400f, 600f, 0.6f),
            Point(600f, 600f, 0.9f)
        )
        
        highlights.forEachIndexed { index, point ->
            val stroke = Stroke(
                id = "highlight_$index",
                points = listOf(point),
                brush = brush,
                color = color,
                layerId = "highlights"
            )
            brushEngine.renderStroke(stroke, bitmap)
        }
    }
    
    /**
     * Create a curved stroke between two points
     */
    private fun createCurveStroke(
        brush: Brush,
        color: Int,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        curves: Int = 1,
        id: String
    ): Stroke {
        val points = mutableListOf<Point>()
        val steps = 50
        
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            
            // Base line interpolation
            var x = startX + (endX - startX) * t
            var y = startY + (endY - startY) * t
            
            // Add wave curves
            val waveOffset = sin(t * PI.toFloat() * curves) * 50f
            val perpX = -(endY - startY)
            val perpY = (endX - startX)
            val perpLength = kotlin.math.sqrt(perpX * perpX + perpY * perpY)
            
            if (perpLength > 0) {
                x += (perpX / perpLength) * waveOffset
                y += (perpY / perpLength) * waveOffset
            }
            
            // Pressure varies along the stroke
            val pressure = 0.3f + sin(t * PI.toFloat()).toFloat() * 0.5f
            
            points.add(Point(x, y, pressure.coerceIn(0.2f, 1f), 0f, 0f, 0f, i * 10L))
        }
        
        return Stroke(
            id = id,
            points = points,
            brush = brush,
            color = color,
            layerId = "sample"
        )
    }
    
    /**
     * Create a spiral stroke
     */
    private fun createSpiralStroke(
        brush: Brush,
        color: Int,
        centerX: Float,
        centerY: Float,
        radius: Float,
        turns: Float,
        id: String
    ): Stroke {
        val points = mutableListOf<Point>()
        val steps = 100
        
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val angle = t * turns * 2 * PI.toFloat()
            val currentRadius = radius * t
            
            val x = centerX + cos(angle) * currentRadius
            val y = centerY + sin(angle) * currentRadius
            
            // Pressure increases as spiral grows
            val pressure = 0.2f + t * 0.6f
            
            points.add(Point(x, y, pressure, 0f, 0f, 0f, i * 10L))
        }
        
        return Stroke(
            id = id,
            points = points,
            brush = brush,
            color = color,
            layerId = "sample"
        )
    }
    
    /**
     * Create a simple straight stroke between two points
     */
    private fun createStraightStroke(
        brush: Brush,
        color: Int,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        id: String
    ): Stroke {
        val points = mutableListOf<Point>()
        val steps = 30
        
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = startX + (endX - startX) * t
            val y = startY + (endY - startY) * t
            val pressure = 0.5f + sin(t * PI.toFloat()).toFloat() * 0.3f
            
            points.add(Point(x, y, pressure.coerceIn(0.3f, 1f), 0f, 0f, 0f, i * 10L))
        }
        
        return Stroke(
            id = id,
            points = points,
            brush = brush,
            color = color,
            layerId = "sample"
        )
    }
    
    /**
     * Generate a simpler sample for performance testing
     */
    fun generateSimpleSample(): Project {
        val width = 512
        val height = 512
        
        val backgroundLayer = Layer.create(width, height, "Background")
        drawGradientBackground(backgroundLayer.bitmap)
        
        val drawingLayer = Layer.create(width, height, "Drawing")
        drawSimpleShape(drawingLayer.bitmap)
        
        return Project(
            name = "Simple Sample",
            width = width,
            height = height,
            layers = listOf(backgroundLayer, drawingLayer),
            backgroundColor = Color.WHITE
        )
    }
    
    /**
     * Draw a simple shape for testing
     */
    private fun drawSimpleShape(bitmap: Bitmap) {
        val brush = Brush.pen().copy(size = 10f)
        val color = Color.rgb(66, 133, 244) // Blue
        
        // Draw a simple circle
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        val radius = 150f
        
        val points = mutableListOf<Point>()
        val steps = 60
        
        for (i in 0..steps) {
            val angle = (i / steps.toFloat()) * 2 * PI.toFloat()
            val x = centerX + cos(angle) * radius
            val y = centerY + sin(angle) * radius
            
            points.add(Point(x, y, 0.8f, 0f, 0f, 0f, i * 10L))
        }
        
        val stroke = Stroke(
            id = "simple_circle",
            points = points,
            brush = brush,
            color = color,
            layerId = "drawing"
        )
        
        brushEngine.renderStroke(stroke, bitmap)
    }
}
