package com.artboard.ui.layers.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import com.artboard.data.model.Layer

/**
 * Generates 56×56dp thumbnails for layer preview cards
 * Uses LRU cache for performance
 */
class LayerThumbnailGenerator(
    private val thumbnailSize: Int = 56
) {
    private val cache = LruCache<String, Bitmap>(20) // Cache up to 20 thumbnails
    
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true // Use bilinear filtering for quality
    }
    
    /**
     * Generate 56×56 thumbnail from layer bitmap
     * Returns cached version if available
     */
    fun generateThumbnail(layer: Layer): Bitmap {
        // Check cache first
        cache.get(layer.id)?.let { return it }
        
        // Generate new thumbnail
        val thumbnail = createThumbnail(layer.bitmap)
        
        // Cache it
        cache.put(layer.id, thumbnail)
        
        // Update layer with thumbnail reference
        layer.thumbnail = thumbnail
        
        return thumbnail
    }
    
    /**
     * Create scaled thumbnail bitmap
     */
    private fun createThumbnail(source: Bitmap): Bitmap {
        // Create thumbnail at 2× resolution for Retina displays
        val actualSize = thumbnailSize * 2 // 112×112px
        
        val thumbnail = Bitmap.createBitmap(
            actualSize,
            actualSize,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(thumbnail)
        
        // Draw checkerboard background for transparency
        drawCheckerboard(canvas, actualSize)
        
        // Scale and center the source bitmap
        val scaleFactor = minOf(
            actualSize.toFloat() / source.width,
            actualSize.toFloat() / source.height
        )
        
        val scaledWidth = (source.width * scaleFactor).toInt()
        val scaledHeight = (source.height * scaleFactor).toInt()
        
        val left = (actualSize - scaledWidth) / 2
        val top = (actualSize - scaledHeight) / 2
        
        val destRect = Rect(left, top, left + scaledWidth, top + scaledHeight)
        val srcRect = Rect(0, 0, source.width, source.height)
        
        canvas.drawBitmap(source, srcRect, destRect, paint)
        
        return thumbnail
    }
    
    /**
     * Draw checkerboard pattern to show transparency
     * 8dp squares alternating between #1A1A1A and #2A2A2A
     */
    private fun drawCheckerboard(canvas: Canvas, size: Int) {
        val squareSize = 16 // 8dp × 2 for Retina
        val darkPaint = Paint().apply { color = 0xFF1A1A1A.toInt() }
        val lightPaint = Paint().apply { color = 0xFF2A2A2A.toInt() }
        
        var y = 0
        var rowOffset = 0
        
        while (y < size) {
            var x = 0
            var useLight = (rowOffset % 2 == 0)
            
            while (x < size) {
                val paint = if (useLight) lightPaint else darkPaint
                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    (x + squareSize).toFloat(),
                    (y + squareSize).toFloat(),
                    paint
                )
                x += squareSize
                useLight = !useLight
            }
            
            y += squareSize
            rowOffset++
        }
    }
    
    /**
     * Invalidate cached thumbnail when layer content changes
     */
    fun invalidate(layerId: String) {
        cache.remove(layerId)
    }
    
    /**
     * Invalidate all thumbnails for a list of layers
     */
    fun invalidateAll(layerIds: List<String>) {
        layerIds.forEach { cache.remove(it) }
    }
    
    /**
     * Clear entire cache (e.g., on memory pressure)
     */
    fun clearCache() {
        cache.evictAll()
    }
    
    /**
     * Pre-generate thumbnails for multiple layers (background task)
     */
    fun preGenerateThumbnails(layers: List<Layer>) {
        layers.forEach { layer ->
            if (cache.get(layer.id) == null) {
                generateThumbnail(layer)
            }
        }
    }
    
    /**
     * Get cache statistics for debugging
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = cache.size(),
            maxSize = cache.maxSize(),
            hitCount = cache.hitCount(),
            missCount = cache.missCount()
        )
    }
    
    data class CacheStats(
        val size: Int,
        val maxSize: Int,
        val hitCount: Int,
        val missCount: Int
    ) {
        val hitRate: Float
            get() = if (hitCount + missCount > 0) {
                hitCount.toFloat() / (hitCount + missCount)
            } else 0f
    }
}
