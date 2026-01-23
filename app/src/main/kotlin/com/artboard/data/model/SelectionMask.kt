package com.artboard.data.model

import android.content.Context
import android.graphics.*
import androidx.core.graphics.alpha
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Represents a selection region as an alpha mask
 * Uses ALPHA_8 format for memory efficiency (1 byte per pixel)
 * 
 * For 2048x2048 canvas: 4MB memory footprint
 */
class SelectionMask(
    val width: Int,
    val height: Int
) {
    private var mask: Bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ALPHA_8  // 1 byte per pixel
    )
    
    init {
        // Initialize to transparent (no selection)
        clear()
    }
    
    /**
     * Create selection from path (lasso tool)
     */
    fun setFromPath(path: Path, featherRadius: Float = 0f) {
        val canvas = Canvas(mask)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        
        val paint = Paint().apply {
            color = Color.WHITE  // 255 = fully selected
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawPath(path, paint)
        
        if (featherRadius > 0f) {
            applyFeather(featherRadius)
        }
    }
    
    /**
     * Create selection from rectangle
     */
    fun setFromRect(rect: RectF, featherRadius: Float = 0f) {
        val canvas = Canvas(mask)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawRect(rect, paint)
        
        if (featherRadius > 0f) {
            applyFeather(featherRadius)
        }
    }
    
    /**
     * Create selection from ellipse
     */
    fun setFromEllipse(rect: RectF, featherRadius: Float = 0f) {
        val canvas = Canvas(mask)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawOval(rect, paint)
        
        if (featherRadius > 0f) {
            applyFeather(featherRadius)
        }
    }
    
    /**
     * Create selection from color tolerance (magic wand)
     * Uses flood fill algorithm
     */
    fun setFromColor(
        sourceBitmap: Bitmap,
        seedX: Int,
        seedY: Int,
        tolerance: Int = 32
    ) {
        if (seedX !in 0 until width || seedY !in 0 until height) {
            return
        }
        
        val canvas = Canvas(mask)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        
        // Flood fill algorithm
        val seedColor = sourceBitmap.getPixel(seedX, seedY)
        val selected = floodFill(sourceBitmap, seedX, seedY, seedColor, tolerance)
        
        // Draw selected pixels to mask
        val paint = Paint().apply {
            color = Color.WHITE
            strokeWidth = 1f
        }
        
        // Batch draw for performance
        val pixelArray = IntArray(width * height)
        selected.forEach { (x, y) ->
            val index = y * width + x
            pixelArray[index] = Color.WHITE
        }
        
        mask.setPixels(pixelArray, 0, width, 0, 0, width, height)
    }
    
    /**
     * Flood fill algorithm for magic wand selection
     * Returns set of selected pixel coordinates
     */
    private fun floodFill(
        bitmap: Bitmap,
        startX: Int,
        startY: Int,
        targetColor: Int,
        tolerance: Int
    ): Set<Pair<Int, Int>> {
        val selected = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        val visited = mutableSetOf<Pair<Int, Int>>()
        
        queue.add(Pair(startX, startY))
        
        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            
            // Bounds check
            if (x !in 0 until bitmap.width || y !in 0 until bitmap.height) {
                continue
            }
            
            // Already processed
            if (Pair(x, y) in visited) {
                continue
            }
            
            visited.add(Pair(x, y))
            
            val pixelColor = bitmap.getPixel(x, y)
            
            // Check color similarity
            if (colorDistance(pixelColor, targetColor) <= tolerance) {
                selected.add(Pair(x, y))
                
                // Add 4-connected neighbors
                queue.add(Pair(x + 1, y))
                queue.add(Pair(x - 1, y))
                queue.add(Pair(x, y + 1))
                queue.add(Pair(x, y - 1))
            }
        }
        
        return selected
    }
    
    /**
     * Calculate color distance (Euclidean distance in RGB space)
     */
    private fun colorDistance(color1: Int, color2: Int): Int {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)
        
        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2
        
        return sqrt((dr * dr + dg * dg + db * db).toDouble()).toInt()
    }
    
    /**
     * Invert selection
     */
    fun invert() {
        val inverted = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(inverted)
        
        // Invert using color matrix
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,  // Invert alpha channel
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
        }
        
        canvas.drawBitmap(mask, 0f, 0f, paint)
        
        // Replace mask
        mask.recycle()
        mask = inverted
    }
    
    /**
     * Select all (fill entire mask)
     */
    fun selectAll() {
        val canvas = Canvas(mask)
        canvas.drawColor(Color.WHITE)
    }
    
    /**
     * Clear selection
     */
    fun clear() {
        val canvas = Canvas(mask)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
    }
    
    /**
     * Apply feathering (soft edge blur)
     * Uses simple box blur for performance (can upgrade to Gaussian if needed)
     */
    private fun applyFeather(radius: Float) {
        if (radius <= 0f) return
        
        // Create blurred copy
        val blurred = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        
        // Simple box blur (fast approximation of Gaussian)
        val r = radius.toInt().coerceIn(1, 25)
        val pixels = IntArray(width * height)
        val blurredPixels = IntArray(width * height)
        
        mask.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Horizontal pass
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0
                
                for (dx in -r..r) {
                    val nx = x + dx
                    if (nx in 0 until width) {
                        sum += pixels[y * width + nx].alpha
                        count++
                    }
                }
                
                blurredPixels[y * width + x] = Color.argb(sum / count, 255, 255, 255)
            }
        }
        
        // Vertical pass
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0
                
                for (dy in -r..r) {
                    val ny = y + dy
                    if (ny in 0 until height) {
                        sum += blurredPixels[ny * width + x].alpha
                        count++
                    }
                }
                
                pixels[y * width + x] = Color.argb(sum / count, 255, 255, 255)
            }
        }
        
        blurred.setPixels(pixels, 0, width, 0, 0, width, height)
        
        // Replace mask
        val canvas = Canvas(mask)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(blurred, 0f, 0f, null)
        blurred.recycle()
    }
    
    /**
     * Get selection bounds (smallest rectangle containing selection)
     */
    fun getBounds(): Rect {
        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0
        var hasSelection = false
        
        val pixels = IntArray(width * height)
        mask.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = pixels[y * width + x].alpha
                if (alpha > 0) {
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                    hasSelection = true
                }
            }
        }
        
        return if (hasSelection) {
            Rect(minX.toFloat(), minY.toFloat(), (maxX + 1).toFloat(), (maxY + 1).toFloat())
        } else {
            Rect(0f, 0f, 0f, 0f)
        }
    }
    
    /**
     * Check if a point is selected
     */
    fun isSelected(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) {
            return false
        }
        return mask.getPixel(x, y).alpha > 127  // 50% threshold
    }
    
    /**
     * Get selection alpha at a point (0-255)
     */
    fun getAlpha(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) {
            return 0
        }
        return mask.getPixel(x, y).alpha
    }
    
    /**
     * Check if selection is empty
     */
    fun isEmpty(): Boolean {
        return getBounds().isEmpty()
    }
    
    /**
     * Extract selected pixels from a source bitmap
     * Returns a new bitmap with only the selected area
     */
    fun extractFromLayer(sourceBitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val sourcePixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        val resultPixels = IntArray(width * height)
        
        sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)
        
        for (i in sourcePixels.indices) {
            val sourcePixel = sourcePixels[i]
            val maskAlpha = maskPixels[i].alpha
            
            if (maskAlpha > 0) {
                // Apply mask alpha to source pixel
                val sourceAlpha = Color.alpha(sourcePixel)
                val finalAlpha = (sourceAlpha * maskAlpha) / 255
                
                resultPixels[i] = Color.argb(
                    finalAlpha,
                    Color.red(sourcePixel),
                    Color.green(sourcePixel),
                    Color.blue(sourcePixel)
                )
            } else {
                resultPixels[i] = Color.TRANSPARENT
            }
        }
        
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Clear selected area in a bitmap
     */
    fun clearInBitmap(targetBitmap: Bitmap) {
        val canvas = Canvas(targetBitmap)
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        
        // Draw mask to clear selected areas
        canvas.drawBitmap(mask, 0f, 0f, paint)
    }
    
    /**
     * Get the mask bitmap (for rendering marching ants)
     */
    fun getMaskBitmap(): Bitmap = mask
    
    /**
     * Create a copy of this selection mask
     */
    fun copy(): SelectionMask {
        val copy = SelectionMask(width, height)
        copy.mask = mask.copy(Bitmap.Config.ALPHA_8, true) ?: mask
        return copy
    }
    
    /**
     * Recycle the mask bitmap to free memory
     */
    fun recycle() {
        if (!mask.isRecycled) {
            mask.recycle()
        }
    }
    
    companion object {
        /**
         * Create a selection mask from an existing bitmap
         */
        fun fromBitmap(bitmap: Bitmap): SelectionMask {
            val mask = SelectionMask(bitmap.width, bitmap.height)
            val canvas = Canvas(mask.mask)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            return mask
        }
    }
}
