package com.artboard.domain.transform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.artboard.data.model.Transform

/**
 * Engine for applying transformations to bitmaps
 * Handles translate, scale, rotate operations with high quality
 */
class TransformEngine {
    
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }
    
    /**
     * Apply transformation to a bitmap
     * Returns new bitmap with transformed content
     */
    fun applyTransform(
        source: Bitmap,
        transform: Transform,
        backgroundColor: Int = android.graphics.Color.TRANSPARENT
    ): Bitmap {
        if (transform.isIdentity()) {
            return source.copy(source.config, true)
        }
        
        val width = source.width
        val height = source.height
        
        // Calculate output size after transformation
        val outputSize = calculateOutputSize(width.toFloat(), height.toFloat(), transform)
        
        val result = Bitmap.createBitmap(
            outputSize.first.toInt(),
            outputSize.second.toInt(),
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(result)
        
        // Fill background
        canvas.drawColor(backgroundColor)
        
        // Build transformation matrix
        val matrix = buildTransformMatrix(width, height, transform)
        
        // Draw transformed bitmap
        canvas.drawBitmap(source, matrix, paint)
        
        return result
    }
    
    /**
     * Apply transformation to bitmap in-place (overwrites original)
     * More efficient for real-time preview
     */
    fun applyTransformInPlace(
        destination: Bitmap,
        source: Bitmap,
        transform: Transform
    ) {
        val canvas = Canvas(destination)
        canvas.drawColor(android.graphics.Color.TRANSPARENT)
        
        val matrix = buildTransformMatrix(source.width, source.height, transform)
        canvas.drawBitmap(source, matrix, paint)
    }
    
    /**
     * Build Android Matrix from Transform object
     */
    private fun buildTransformMatrix(width: Int, height: Int, transform: Transform): Matrix {
        return Matrix().apply {
            // Calculate pivot point in pixels
            val pivotX = width * transform.pivotX
            val pivotY = height * transform.pivotY
            
            // Translate to pivot
            postTranslate(-pivotX, -pivotY)
            
            // Scale (use uniform scale if set, otherwise use X/Y scales)
            if (transform.scale != 1f) {
                postScale(transform.scale, transform.scale)
            } else {
                postScale(transform.scaleX, transform.scaleY)
            }
            
            // Rotate around origin (pivot)
            if (transform.rotation != 0f) {
                postRotate(transform.rotation)
            }
            
            // Translate back from pivot
            postTranslate(pivotX, pivotY)
            
            // Apply translation
            postTranslate(transform.translation.x, transform.translation.y)
        }
    }
    
    /**
     * Calculate bounding box of transformed content
     */
    fun calculateTransformedBounds(
        originalBounds: Rect,
        transform: Transform
    ): Rect {
        val rectF = RectF(
            originalBounds.left,
            originalBounds.top,
            originalBounds.right,
            originalBounds.bottom
        )
        
        val matrix = buildTransformMatrix(
            originalBounds.width.toInt(),
            originalBounds.height.toInt(),
            transform
        )
        
        val transformedRect = RectF()
        matrix.mapRect(transformedRect, rectF)
        
        return Rect(
            transformedRect.left,
            transformedRect.top,
            transformedRect.right,
            transformedRect.bottom
        )
    }
    
    /**
     * Calculate output size needed for transformed bitmap
     */
    private fun calculateOutputSize(width: Float, height: Float, transform: Transform): Pair<Float, Float> {
        // Create rectangle for original size
        val rectF = RectF(0f, 0f, width, height)
        
        // Build matrix
        val matrix = buildTransformMatrix(width.toInt(), height.toInt(), transform)
        
        // Map rectangle through matrix
        val transformed = RectF()
        matrix.mapRect(transformed, rectF)
        
        return Pair(transformed.width(), transformed.height())
    }
    
    /**
     * Flip bitmap horizontally
     */
    fun flipHorizontal(source: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postScale(-1f, 1f, source.width / 2f, source.height / 2f)
        }
        
        return Bitmap.createBitmap(
            source,
            0, 0,
            source.width, source.height,
            matrix,
            true
        )
    }
    
    /**
     * Flip bitmap vertically
     */
    fun flipVertical(source: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postScale(1f, -1f, source.width / 2f, source.height / 2f)
        }
        
        return Bitmap.createBitmap(
            source,
            0, 0,
            source.width, source.height,
            matrix,
            true
        )
    }
    
    /**
     * Rotate 90 degrees clockwise
     */
    fun rotate90Clockwise(source: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postRotate(90f)
            postTranslate(source.height.toFloat(), 0f)
        }
        
        return Bitmap.createBitmap(
            source.height,
            source.width,
            Bitmap.Config.ARGB_8888
        ).also { result ->
            val canvas = Canvas(result)
            canvas.drawBitmap(source, matrix, paint)
        }
    }
    
    /**
     * Rotate 90 degrees counter-clockwise
     */
    fun rotate90CounterClockwise(source: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postRotate(-90f)
            postTranslate(0f, source.width.toFloat())
        }
        
        return Bitmap.createBitmap(
            source.height,
            source.width,
            Bitmap.Config.ARGB_8888
        ).also { result ->
            val canvas = Canvas(result)
            canvas.drawBitmap(source, matrix, paint)
        }
    }
    
    /**
     * Rotate 180 degrees
     */
    fun rotate180(source: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postRotate(180f)
            postTranslate(source.width.toFloat(), source.height.toFloat())
        }
        
        return Bitmap.createBitmap(
            source,
            0, 0,
            source.width, source.height,
            matrix,
            true
        )
    }
    
    /**
     * Get effective scale (considers both uniform and non-uniform)
     */
    fun getEffectiveScale(transform: Transform): Pair<Float, Float> {
        val scaleX = if (transform.scale != 1f) transform.scale else transform.scaleX
        val scaleY = if (transform.scale != 1f) transform.scale else transform.scaleY
        return Pair(scaleX, scaleY)
    }
    
    /**
     * Check if transform requires resampling (quality loss)
     */
    fun requiresResampling(transform: Transform): Boolean {
        val (scaleX, scaleY) = getEffectiveScale(transform)
        return scaleX < 1f || scaleY < 1f || transform.rotation != 0f
    }
}
