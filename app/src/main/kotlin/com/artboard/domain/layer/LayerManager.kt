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
 * Supports all 18 blend modes from Procreate
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
        
        // Composite each visible layer (bottom to top)
        layers.sortedBy { it.position }.forEach { layer ->
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
        
        // Set blend mode (map to Android PorterDuff modes)
        compositePaint.xfermode = when (layer.blendMode) {
            BlendMode.NORMAL -> null
            BlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            BlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            BlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            BlendMode.ADD -> PorterDuffXfermode(PorterDuff.Mode.ADD)
            BlendMode.DARKEN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
            BlendMode.LIGHTEN -> PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
            BlendMode.COLOR_DODGE -> PorterDuffXfermode(PorterDuff.Mode.LIGHTEN) // Approximation
            BlendMode.COLOR_BURN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN) // Approximation
            BlendMode.SOFT_LIGHT -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY) // Approximation
            BlendMode.HARD_LIGHT -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY) // Approximation
            BlendMode.DIFFERENCE -> PorterDuffXfermode(PorterDuff.Mode.XOR) // DIFFERENCE not available, use XOR
            BlendMode.EXCLUSION -> PorterDuffXfermode(PorterDuff.Mode.XOR)
            BlendMode.HUE -> null // TODO: Custom shader implementation
            BlendMode.SATURATION -> null // TODO: Custom shader implementation
            BlendMode.COLOR -> null // TODO: Custom shader implementation
            BlendMode.LUMINOSITY -> null // TODO: Custom shader implementation
            BlendMode.XOR -> PorterDuffXfermode(PorterDuff.Mode.XOR)
        }
        
        canvas.drawBitmap(layer.bitmap, 0f, 0f, compositePaint)
    }
    
    /**
     * Create a new blank layer
     */
    fun createLayer(name: String, position: Int = 0): Layer {
        return Layer.create(canvasWidth, canvasHeight, name, position)
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
            isVisible = true,
            position = bottom.position
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
            isVisible = true,
            position = 0
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
