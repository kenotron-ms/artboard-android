package com.artboard.domain.history

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.artboard.data.model.Layer
import com.artboard.data.model.SelectionMask
import com.artboard.domain.history.Command
import com.artboard.domain.history.CommandResult
import com.artboard.domain.layer.LayerManager

/**
 * Command to copy selection to a new layer
 */
class CopySelectionCommand(
    private val layerManager: LayerManager,
    private val sourceLayer: Layer,
    private val selectionMask: SelectionMask
) : Command() {
    
    private var createdLayerId: String? = null
    private val maskCopy = selectionMask.copy()
    
    override fun execute(): CommandResult {
        // Extract selected pixels
        val selectedBitmap = maskCopy.extractFromLayer(sourceLayer.bitmap)
        
        // Create new layer with extracted content
        val newLayer = Layer(
            name = "${sourceLayer.name} Copy",
            bitmap = selectedBitmap,
            opacity = sourceLayer.opacity,
            blendMode = sourceLayer.blendMode,
            position = sourceLayer.position + 1
        )
        
        createdLayerId = newLayer.id
        
        // Add to layer manager (assuming it has an addLayer method)
        // This would need to be implemented in LayerManager
        
        return CommandResult(
            message = "Selection copied to new layer"
        )
    }
    
    override fun undo(): CommandResult {
        // Remove the created layer
        // This would need LayerManager.removeLayer(layerId)
        
        return CommandResult(
            message = "Copy undone"
        )
    }
    
    fun cleanup() {
        maskCopy.recycle()
    }
}

/**
 * Command to cut selection (copy then clear)
 */
class CutSelectionCommand(
    private val layerManager: LayerManager,
    private val sourceLayer: Layer,
    private val selectionMask: SelectionMask
) : Command() {
    
    private var createdLayerId: String? = null
    private val maskCopy = selectionMask.copy()
    private val originalBitmap: Bitmap = sourceLayer.bitmap.copy(
        sourceLayer.bitmap.config,
        true
    ) ?: sourceLayer.bitmap
    
    override fun execute(): CommandResult {
        // Extract selected pixels to new layer
        val selectedBitmap = maskCopy.extractFromLayer(sourceLayer.bitmap)
        
        val newLayer = Layer(
            name = "${sourceLayer.name} Cut",
            bitmap = selectedBitmap,
            opacity = sourceLayer.opacity,
            blendMode = sourceLayer.blendMode,
            position = sourceLayer.position + 1
        )
        
        createdLayerId = newLayer.id
        
        // Clear selected area from source layer
        maskCopy.clearInBitmap(sourceLayer.bitmap)
        
        return CommandResult(
            message = "Selection cut to new layer"
        )
    }
    
    override fun undo(): CommandResult {
        // Restore original bitmap
        val canvas = Canvas(sourceLayer.bitmap)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        
        // Remove created layer
        // LayerManager.removeLayer(createdLayerId)
        
        return CommandResult(
            message = "Cut undone"
        )
    }
    
    fun cleanup() {
        maskCopy.recycle()
        originalBitmap.recycle()
    }
}

/**
 * Command to clear selection (delete selected pixels)
 */
class ClearSelectionCommand(
    private val layer: Layer,
    private val selectionMask: SelectionMask
) : Command() {
    
    private val maskCopy = selectionMask.copy()
    private val originalBitmap: Bitmap = layer.bitmap.copy(
        layer.bitmap.config,
        true
    ) ?: layer.bitmap
    
    override fun execute(): CommandResult {
        // Clear selected pixels
        maskCopy.clearInBitmap(layer.bitmap)
        
        return CommandResult(
            message = "Selection cleared"
        )
    }
    
    override fun undo(): CommandResult {
        // Restore original bitmap
        val canvas = Canvas(layer.bitmap)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        
        return CommandResult(
            message = "Clear undone"
        )
    }
    
    fun cleanup() {
        maskCopy.recycle()
        originalBitmap.recycle()
    }
}

/**
 * Command to fill selection with a color
 */
class FillSelectionCommand(
    private val layer: Layer,
    private val selectionMask: SelectionMask,
    private val fillColor: Int
) : Command() {
    
    private val maskCopy = selectionMask.copy()
    private val originalBitmap: Bitmap = layer.bitmap.copy(
        layer.bitmap.config,
        true
    ) ?: layer.bitmap
    
    override fun execute(): CommandResult {
        val canvas = Canvas(layer.bitmap)
        val paint = Paint().apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        
        // Fill selected area with color
        // This is a simplified approach - for production, you'd want to respect the mask alpha
        val bounds = maskCopy.getBounds()
        
        for (y in bounds.top.toInt() until bounds.bottom.toInt()) {
            for (x in bounds.left.toInt() until bounds.right.toInt()) {
                val maskAlpha = maskCopy.getAlpha(x, y)
                if (maskAlpha > 0) {
                    // Apply fill color with mask alpha
                    val pixel = layer.bitmap.getPixel(x, y)
                    val finalColor = android.graphics.Color.argb(
                        maskAlpha,
                        android.graphics.Color.red(fillColor),
                        android.graphics.Color.green(fillColor),
                        android.graphics.Color.blue(fillColor)
                    )
                    layer.bitmap.setPixel(x, y, finalColor)
                }
            }
        }
        
        return CommandResult(
            message = "Selection filled"
        )
    }
    
    override fun undo(): CommandResult {
        // Restore original bitmap
        val canvas = Canvas(layer.bitmap)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        
        return CommandResult(
            message = "Fill undone"
        )
    }
    
    fun cleanup() {
        maskCopy.recycle()
        originalBitmap.recycle()
    }
}

/**
 * Command to invert selection
 */
class InvertSelectionCommand(
    private val selectionMask: SelectionMask
) : Command() {
    
    override fun execute(): CommandResult {
        selectionMask.invert()
        return CommandResult(
            message = "Selection inverted"
        )
    }
    
    override fun undo(): CommandResult {
        // Invert again to restore original
        selectionMask.invert()
        return CommandResult(
            message = "Invert undone"
        )
    }
}

/**
 * Command to create a new selection
 * This allows undo to restore the previous selection state
 */
class CreateSelectionCommand(
    private val newMask: SelectionMask,
    private val previousMask: SelectionMask?
) : Command() {
    
    private var currentMask: SelectionMask = newMask.copy()
    private var savedPreviousMask: SelectionMask? = previousMask?.copy()
    
    override fun execute(): CommandResult {
        // The new selection is already set in the ViewModel
        // This command just tracks the change for undo
        return CommandResult(
            message = "Selection created"
        )
    }
    
    override fun undo(): CommandResult {
        // Restore previous selection state
        // This would need to update the ViewModel's selection
        return CommandResult(
            message = "Selection undone"
        )
    }
    
    fun cleanup() {
        currentMask.recycle()
        savedPreviousMask?.recycle()
    }
}

/**
 * Command to transform selection (move, scale, rotate)
 * Used in conjunction with transform tools
 */
class TransformSelectionCommand(
    private val layer: Layer,
    private val selectionMask: SelectionMask,
    private val transformMatrix: android.graphics.Matrix
) : Command() {
    
    private val maskCopy = selectionMask.copy()
    private val originalBitmap: Bitmap = layer.bitmap.copy(
        layer.bitmap.config,
        true
    ) ?: layer.bitmap
    
    override fun execute(): CommandResult {
        // Extract selected region
        val selectedBitmap = maskCopy.extractFromLayer(layer.bitmap)
        
        // Clear original selected area
        maskCopy.clearInBitmap(layer.bitmap)
        
        // Transform selected bitmap
        val transformedBitmap = Bitmap.createBitmap(
            selectedBitmap,
            0,
            0,
            selectedBitmap.width,
            selectedBitmap.height,
            transformMatrix,
            true
        )
        
        // Draw transformed bitmap back
        val canvas = Canvas(layer.bitmap)
        canvas.drawBitmap(transformedBitmap, 0f, 0f, null)
        
        transformedBitmap.recycle()
        selectedBitmap.recycle()
        
        return CommandResult(
            message = "Selection transformed"
        )
    }
    
    override fun undo(): CommandResult {
        // Restore original bitmap
        val canvas = Canvas(layer.bitmap)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        
        return CommandResult(
            message = "Transform undone"
        )
    }
    
    fun cleanup() {
        maskCopy.recycle()
        originalBitmap.recycle()
    }
}

/**
 * Command to apply an effect to selection
 * Generic command for blur, sharpen, etc.
 */
class ApplyEffectToSelectionCommand(
    private val layer: Layer,
    private val selectionMask: SelectionMask,
    private val effect: (Bitmap) -> Bitmap
) : Command() {
    
    private val maskCopy = selectionMask.copy()
    private val originalBitmap: Bitmap = layer.bitmap.copy(
        layer.bitmap.config,
        true
    ) ?: layer.bitmap
    
    override fun execute(): CommandResult {
        // Extract selected region
        val selectedBitmap = maskCopy.extractFromLayer(layer.bitmap)
        
        // Apply effect
        val effectBitmap = effect(selectedBitmap)
        
        // Composite back to layer
        val canvas = Canvas(layer.bitmap)
        
        // Clear selected area first
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        canvas.drawBitmap(maskCopy.getMaskBitmap(), 0f, 0f, paint)
        
        // Draw effect result
        paint.xfermode = null
        canvas.drawBitmap(effectBitmap, 0f, 0f, paint)
        
        effectBitmap.recycle()
        selectedBitmap.recycle()
        
        return CommandResult(
            message = "Effect applied to selection"
        )
    }
    
    override fun undo(): CommandResult {
        // Restore original bitmap
        val canvas = Canvas(layer.bitmap)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        
        return CommandResult(
            message = "Effect undone"
        )
    }
    
    fun cleanup() {
        maskCopy.recycle()
        originalBitmap.recycle()
    }
}
