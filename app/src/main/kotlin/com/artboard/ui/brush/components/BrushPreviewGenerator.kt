package com.artboard.ui.brush.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.LruCache
import com.artboard.data.model.Brush
import com.artboard.data.model.Point
import com.artboard.data.model.Stroke
import com.artboard.domain.brush.BrushEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.math.PI

/**
 * Generates and caches live brush preview bitmaps
 * Uses actual BrushEngine to render realistic stroke previews
 */
class BrushPreviewGenerator {
    
    private val cache = LruCache<String, Bitmap>(50) // Cache 50 previews (~10 MB)
    private val brushEngine = BrushEngine()
    
    /**
     * Generate preview bitmap showing brush stroke with S-curve path
     * Uses pressure variation (0.3 → 0.8 → 0.3) for realistic preview
     * 
     * @param brush The brush to preview
     * @param size Preview bitmap size in pixels (default 100dp)
     * @param backgroundColor Background color (default white)
     * @param strokeColor Stroke color (default black)
     * @return Cached or newly generated preview bitmap
     */
    suspend fun generatePreview(
        brush: Brush,
        size: Int = 100,
        backgroundColor: Int = Color.WHITE,
        strokeColor: Int = Color.BLACK
    ): Bitmap = withContext(Dispatchers.Default) {
        // Check cache first
        val cacheKey = "${brush.hashCode()}-$size"
        cache.get(cacheKey)?.let { return@withContext it }
        
        // Generate new preview
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // White background
        canvas.drawColor(backgroundColor)
        
        // Create sample stroke (S-curve with pressure variation)
        val points = createPreviewStrokePath(size)
        val stroke = Stroke(
            id = "preview-${brush.hashCode()}",
            points = points,
            brush = brush,
            color = strokeColor,
            layerId = "preview",

        )
        
        // Render stroke using actual brush engine
        brushEngine.renderStroke(stroke, bitmap)
        
        // Cache and return
        cache.put(cacheKey, bitmap)
        return@withContext bitmap
    }
    
    /**
     * Generate preview synchronously (for immediate display)
     * Use sparingly - prefer async generatePreview() for better performance
     */
    fun generatePreviewSync(
        brush: Brush,
        size: Int = 100,
        backgroundColor: Int = Color.WHITE,
        strokeColor: Int = Color.BLACK
    ): Bitmap {
        val cacheKey = "${brush.hashCode()}-$size"
        cache.get(cacheKey)?.let { return it }
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)
        
        val points = createPreviewStrokePath(size)
        val stroke = Stroke(
            id = "preview-${brush.hashCode()}",
            points = points,
            brush = brush,
            color = strokeColor,
            layerId = "preview",

        )
        
        brushEngine.renderStroke(stroke, bitmap)
        cache.put(cacheKey, bitmap)
        return bitmap
    }
    
    /**
     * Create S-curve path with pressure variation
     * This produces a natural-looking preview stroke that shows brush characteristics
     */
    private fun createPreviewStrokePath(size: Int): List<Point> {
        val points = mutableListOf<Point>()
        val numPoints = 20
        val margin = size * 0.15f // 15% margin on each side
        val drawableWidth = size - (margin * 2)
        val drawableHeight = size - (margin * 2)
        
        for (i in 0..numPoints) {
            val t = i.toFloat() / numPoints
            
            // S-curve path (horizontal with vertical sine wave)
            val x = margin + (t * drawableWidth)
            val y = margin + (drawableHeight / 2) + 
                    sin(t * PI * 2).toFloat() * (drawableHeight / 3)
            
            // Pressure varies along stroke (thin → thick → thin)
            // Creates beautiful preview showing pressure sensitivity
            val pressure = 0.3f + sin(t * PI).toFloat() * 0.5f // 0.3 → 0.8 → 0.3
            
            // Add slight tilt variation for tilt-enabled brushes
            val tiltX = sin(t * PI * 4).toFloat() * 15f // -15° to +15°
            val tiltY = sin(t * PI * 3).toFloat() * 20f // -20° to +20°
            
            points.add(
                Point(
                    x = x,
                    y = y,
                    pressure = pressure,
                    tiltX = tiltX,
                    tiltY = tiltY,
                    timestamp = System.currentTimeMillis() + (i * 10L)
                )
            )
        }
        
        return points
    }
    
    /**
     * Clear preview cache (call on memory pressure)
     */
    fun clearCache() {
        cache.evictAll()
    }
    
    /**
     * Get current cache size
     */
    fun getCacheSize(): Int = cache.size()
    
    /**
     * Pre-generate previews for a list of brushes
     * Useful for warming the cache before displaying the brush selector
     */
    suspend fun preGeneratePreviews(
        brushes: List<Brush>,
        size: Int = 100
    ) = withContext(Dispatchers.Default) {
        brushes.forEach { brush ->
            generatePreview(brush, size)
        }
    }
}
