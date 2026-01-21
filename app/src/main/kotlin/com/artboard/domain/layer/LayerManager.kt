package com.artboard.domain.layer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.artboard.data.model.BlendMode
import com.artboard.data.model.Layer

/**
 * Manages layer operations and compositing
 */
class LayerManager(
    private val canvasWidth: Int,
    private val canvasHeight: Int
) {
    private val compositePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    /**
     * Composite all visible layers into a single bitmap
     */
    fun composite(layers: List<Layer>): Bitmap {
        val result = Bitmap.createBitmap(
            canvasWidth,
            canvasHeight,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(result)
        
        // Clear to transparent
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        
        // Composite each visible layer
        layers.forEach { layer ->
            if (layer.isVisible) {
                compositeLayer(canvas, layer)
            }
        }
        
        return result
    }
    
    /**
     * Composite a single layer onto the canvas
     */
    private fun compositeLayer(canvas: Canvas, layer: Layer) {
        compositePaint.alpha = (layer.opacity * 255).toInt()
        
        // Set blend mode
        compositePaint.xfermode = when (layer.blendMode) {
            BlendMode.NORMAL -> null
            BlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            BlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            BlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            BlendMode.ADD -> PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
        
        canvas.drawBitmap(layer.bitmap, 0f, 0f, compositePaint)
    }
    
    /**
     * Create a new blank layer
     */
    fun createLayer(name: String): Layer {
        return Layer.create(canvasWidth, canvasHeight, name)
    }
    
    /**
     * Merge two layers together
     */
    fun mergeLayers(bottom: Layer, top: Layer): Layer {
        val mergedBitmap = Bitmap.createBitmap(
            canvasWidth,
            canvasHeight,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(mergedBitmap)
        
        // Draw bottom layer
        compositePaint.alpha = (bottom.opacity * 255).toInt()
        compositePaint.xfermode = null
        canvas.drawBitmap(bottom.bitmap, 0f, 0f, compositePaint)
        
        // Draw top layer with its blend mode
        compositeLayer(canvas, top)
        
        return Layer(
            name = "${bottom.name} + ${top.name}",
            bitmap = mergedBitmap,
            opacity = 1f,
            blendMode = BlendMode.NORMAL,
            isVisible = true
        )
    }
    
    /**
     * Flatten all layers into a single layer
     */
    fun flattenLayers(layers: List<Layer>): Layer {
        val flattenedBitmap = composite(layers)
        
        return Layer(
            name = "Flattened",
            bitmap = flattenedBitmap,
            opacity = 1f,
            blendMode = BlendMode.NORMAL,
            isVisible = true
        )
    }
    
    /**
     * Duplicate a layer
     */
    fun duplicateLayer(layer: Layer): Layer {
        val duplicateBitmap = layer.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        return layer.copy(
            bitmap = duplicateBitmap,
            name = "${layer.name} copy"
        )
    }
    
    /**
     * Clear a layer (fill with transparent)
     */
    fun clearLayer(layer: Layer): Layer {
        val canvas = Canvas(layer.bitmap)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        return layer
    }
    
    /**
     * Fill a layer with a color
     */
    fun fillLayer(layer: Layer, color: Int): Layer {
        val canvas = Canvas(layer.bitmap)
        canvas.drawColor(color)
        return layer
    }
}
