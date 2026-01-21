package com.artboard.domain.history

import com.artboard.data.model.Layer
import com.artboard.data.model.Stroke

/**
 * Command pattern for undo/redo functionality
 */
sealed class Command {
    abstract fun execute(): CommandResult
    abstract fun undo(): CommandResult
}

/**
 * Result of command execution
 */
data class CommandResult(
    val updatedLayers: List<Layer>? = null,
    val message: String? = null
)

/**
 * Command to add a stroke to a layer
 */
class AddStrokeCommand(
    private val stroke: Stroke,
    private val layerIndex: Int,
    private val layers: List<Layer>
) : Command() {
    
    private val beforeBitmap = layers[layerIndex].bitmap.copy(
        layers[layerIndex].bitmap.config,
        true
    )
    
    override fun execute(): CommandResult {
        val layer = layers[layerIndex]
        val canvas = android.graphics.Canvas(layer.bitmap)
        
        // Render stroke to layer
        val brushEngine = com.artboard.domain.brush.BrushEngine()
        brushEngine.renderStroke(stroke, layer.bitmap)
        
        return CommandResult(updatedLayers = layers)
    }
    
    override fun undo(): CommandResult {
        val layer = layers[layerIndex]
        val canvas = android.graphics.Canvas(layer.bitmap)
        
        // Restore previous bitmap
        canvas.drawBitmap(beforeBitmap, 0f, 0f, null)
        
        return CommandResult(updatedLayers = layers)
    }
}

/**
 * Command to add a new layer
 */
class AddLayerCommand(
    private val layer: Layer,
    private val layers: List<Layer>
) : Command() {
    
    override fun execute(): CommandResult {
        val newLayers = layers + layer
        return CommandResult(updatedLayers = newLayers)
    }
    
    override fun undo(): CommandResult {
        val newLayers = layers.dropLast(1)
        return CommandResult(updatedLayers = newLayers)
    }
}

/**
 * Command to delete a layer
 */
class DeleteLayerCommand(
    private val layerIndex: Int,
    private val layers: List<Layer>
) : Command() {
    
    private val deletedLayer = layers[layerIndex]
    
    override fun execute(): CommandResult {
        val newLayers = layers.filterIndexed { i, _ -> i != layerIndex }
        return CommandResult(updatedLayers = newLayers)
    }
    
    override fun undo(): CommandResult {
        val newLayers = layers.toMutableList()
        newLayers.add(layerIndex, deletedLayer)
        return CommandResult(updatedLayers = newLayers)
    }
}

/**
 * Command to clear a layer
 */
class ClearLayerCommand(
    private val layerIndex: Int,
    private val layers: List<Layer>
) : Command() {
    
    private val beforeBitmap = layers[layerIndex].bitmap.copy(
        layers[layerIndex].bitmap.config,
        true
    )
    
    override fun execute(): CommandResult {
        val layer = layers[layerIndex]
        val canvas = android.graphics.Canvas(layer.bitmap)
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR)
        
        return CommandResult(updatedLayers = layers)
    }
    
    override fun undo(): CommandResult {
        val layer = layers[layerIndex]
        val canvas = android.graphics.Canvas(layer.bitmap)
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(beforeBitmap, 0f, 0f, null)
        
        return CommandResult(updatedLayers = layers)
    }
}

/**
 * Command to merge layers
 */
class MergeLayersCommand(
    private val layerIndex1: Int,
    private val layerIndex2: Int,
    private val layers: List<Layer>,
    private val layerManager: com.artboard.domain.layer.LayerManager
) : Command() {
    
    private val layer1 = layers[layerIndex1]
    private val layer2 = layers[layerIndex2]
    
    override fun execute(): CommandResult {
        val mergedLayer = layerManager.mergeLayers(layer1, layer2)
        
        val newLayers = layers.toMutableList()
        newLayers[layerIndex1] = mergedLayer
        newLayers.removeAt(layerIndex2)
        
        return CommandResult(updatedLayers = newLayers)
    }
    
    override fun undo(): CommandResult {
        val newLayers = layers.toMutableList()
        newLayers[layerIndex1] = layer1
        newLayers.add(layerIndex2, layer2)
        
        return CommandResult(updatedLayers = newLayers)
    }
}
