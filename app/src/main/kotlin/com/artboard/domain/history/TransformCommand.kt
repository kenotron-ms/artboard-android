package com.artboard.domain.history

import android.graphics.Bitmap
import com.artboard.data.model.Layer
import com.artboard.data.model.Transform
import com.artboard.domain.history.Command
import com.artboard.domain.history.CommandResult
import com.artboard.domain.transform.TransformEngine

/**
 * Command to transform a layer (scale, rotate, translate)
 * Supports undo/redo
 */
class TransformLayerCommand(
    private val layerIndex: Int,
    private val transform: Transform,
    private val layers: List<Layer>
) : Command() {
    
    private var originalBitmap: Bitmap? = null
    private var transformedBitmap: Bitmap? = null
    private val transformEngine = TransformEngine()
    
    override fun execute(): CommandResult {
        val layer = layers[layerIndex]
        
        // Save original on first execution
        if (originalBitmap == null) {
            originalBitmap = layer.bitmap.copy(layer.bitmap.config, true)
        }
        
        // Apply transformation
        transformedBitmap = transformEngine.applyTransform(
            source = originalBitmap!!,
            transform = transform
        )
        
        // Replace layer bitmap
        val oldBitmap = layer.bitmap
        layer.bitmap = transformedBitmap!!
        
        // Recycle old bitmap if it's not the original
        if (oldBitmap != originalBitmap) {
            oldBitmap.recycle()
        }
        
        return CommandResult(updatedLayers = layers, message = "Transform applied")
    }
    
    override fun undo(): CommandResult {
        val layer = layers[layerIndex]
        
        // Restore original
        val oldBitmap = layer.bitmap
        layer.bitmap = originalBitmap!!.copy(originalBitmap!!.config, true)
        
        // Recycle transformed bitmap
        if (oldBitmap != originalBitmap) {
            oldBitmap.recycle()
        }
        
        return CommandResult(updatedLayers = layers, message = "Transform undone")
    }
    
    /**
     * Cleanup bitmaps when command is removed from history
     */
    fun cleanup() {
        originalBitmap?.recycle()
        transformedBitmap?.recycle()
        originalBitmap = null
        transformedBitmap = null
    }
}

/**
 * Command to flip a layer horizontally
 */
class FlipLayerHorizontalCommand(
    private val layerIndex: Int,
    private val layers: List<Layer>
) : Command() {
    
    private val transformEngine = TransformEngine()
    
    override fun execute(): CommandResult {
        val layer = layers[layerIndex]
        
        val flipped = transformEngine.flipHorizontal(layer.bitmap)
        val old = layer.bitmap
        layer.bitmap = flipped
        old.recycle()
        
        return CommandResult(updatedLayers = layers, message = "Flipped horizontally")
    }
    
    override fun undo(): CommandResult {
        // Flip again to reverse
        return execute()
    }
}

/**
 * Command to flip a layer vertically
 */
class FlipLayerVerticalCommand(
    private val layerIndex: Int,
    private val layers: List<Layer>
) : Command() {
    
    private val transformEngine = TransformEngine()
    
    override fun execute(): CommandResult {
        val layer = layers[layerIndex]
        
        val flipped = transformEngine.flipVertical(layer.bitmap)
        val old = layer.bitmap
        layer.bitmap = flipped
        old.recycle()
        
        return CommandResult(updatedLayers = layers, message = "Flipped vertically")
    }
    
    override fun undo(): CommandResult {
        // Flip again to reverse
        return execute()
    }
}

/**
 * Command to rotate a layer 90 degrees clockwise
 */
class Rotate90ClockwiseCommand(
    private val layerIndex: Int,
    private val layers: List<Layer>
) : Command() {
    
    private val transformEngine = TransformEngine()
    
    override fun execute(): CommandResult {
        val layer = layers[layerIndex]
        
        val rotated = transformEngine.rotate90Clockwise(layer.bitmap)
        val old = layer.bitmap
        layer.bitmap = rotated
        old.recycle()
        
        return CommandResult(updatedLayers = layers, message = "Rotated 90° CW")
    }
    
    override fun undo(): CommandResult {
        val layer = layers[layerIndex]
        
        // Undo with 90° counter-clockwise
        val rotated = transformEngine.rotate90CounterClockwise(layer.bitmap)
        val old = layer.bitmap
        layer.bitmap = rotated
        old.recycle()
        
        return CommandResult(updatedLayers = layers, message = "Rotation undone")
    }
}

/**
 * Command to rotate a layer 90 degrees counter-clockwise
 */
class Rotate90CounterClockwiseCommand(
    private val layerIndex: Int,
    private val layers: List<Layer>
) : Command() {
    
    private val transformEngine = TransformEngine()
    
    override fun execute(): CommandResult {
        val layer = layers[layerIndex]
        
        val rotated = transformEngine.rotate90CounterClockwise(layer.bitmap)
        val old = layer.bitmap
        layer.bitmap = rotated
        old.recycle()
        
        return CommandResult(updatedLayers = layers, message = "Rotated 90° CCW")
    }
    
    override fun undo(): CommandResult {
        val layer = layers[layerIndex]
        
        // Undo with 90° clockwise
        val rotated = transformEngine.rotate90Clockwise(layer.bitmap)
        val old = layer.bitmap
        layer.bitmap = rotated
        old.recycle()
        
        return CommandResult(updatedLayers = layers, message = "Rotation undone")
    }
}

/**
 * Command to rotate a layer 180 degrees
 */
class Rotate180Command(
    private val layerIndex: Int,
    private val layers: List<Layer>
) : Command() {
    
    private val transformEngine = TransformEngine()
    
    override fun execute(): CommandResult {
        val layer = layers[layerIndex]
        
        val rotated = transformEngine.rotate180(layer.bitmap)
        val old = layer.bitmap
        layer.bitmap = rotated
        old.recycle()
        
        return CommandResult(updatedLayers = layers, message = "Rotated 180°")
    }
    
    override fun undo(): CommandResult {
        // Rotate 180 again to reverse
        return execute()
    }
}
