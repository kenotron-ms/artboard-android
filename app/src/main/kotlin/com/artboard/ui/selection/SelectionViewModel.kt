package com.artboard.ui.selection

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artboard.data.model.Layer
import com.artboard.data.model.SelectionMask
import com.artboard.domain.history.*
import com.artboard.domain.layer.LayerManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for selection mode
 * Manages selection state, tools, and operations
 */
class SelectionViewModel(
    private val historyManager: HistoryManager,
    private val layerManager: LayerManager
) : ViewModel() {
    
    // Selection mode state
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    
    // Current selection tool
    private val _selectionTool = MutableStateFlow(SelectionToolType.LASSO)
    val selectionTool: StateFlow<SelectionToolType> = _selectionTool.asStateFlow()
    
    // Current selection mask
    private val _selectionMask = MutableStateFlow<SelectionMask?>(null)
    val selectionMask: StateFlow<SelectionMask?> = _selectionMask.asStateFlow()
    
    // Feather radius (0-50px)
    private val _featherRadius = MutableStateFlow(0f)
    val featherRadius: StateFlow<Float> = _featherRadius.asStateFlow()
    
    // Magic wand tolerance (0-255)
    private val _tolerance = MutableStateFlow(32)
    val tolerance: StateFlow<Int> = _tolerance.asStateFlow()
    
    // Selection operation mode (new, add, subtract, intersect)
    private val _operationMode = MutableStateFlow(SelectionOperationMode.NEW)
    val operationMode: StateFlow<SelectionOperationMode> = _operationMode.asStateFlow()
    
    // Active layer for selection operations
    private val _activeLayer = MutableStateFlow<Layer?>(null)
    val activeLayer: StateFlow<Layer?> = _activeLayer.asStateFlow()
    
    // Selection in progress (for preview)
    private val _isSelecting = MutableStateFlow(false)
    val isSelecting: StateFlow<Boolean> = _isSelecting.asStateFlow()
    
    /**
     * Enter selection mode
     */
    fun enterSelectionMode(layer: Layer) {
        _isSelectionMode.value = true
        _activeLayer.value = layer
    }
    
    /**
     * Exit selection mode and clear selection
     */
    fun exitSelectionMode() {
        clearSelection()
        _isSelectionMode.value = false
        _activeLayer.value = null
    }
    
    /**
     * Set active selection tool
     */
    fun setSelectionTool(tool: SelectionToolType) {
        _selectionTool.value = tool
    }
    
    /**
     * Set selection mask
     */
    fun setSelection(mask: SelectionMask) {
        // Recycle old mask
        _selectionMask.value?.recycle()
        _selectionMask.value = mask
    }
    
    /**
     * Clear current selection
     */
    fun clearSelection() {
        _selectionMask.value?.recycle()
        _selectionMask.value = null
    }
    
    /**
     * Invert selection
     */
    fun invertSelection() {
        val currentMask = _selectionMask.value ?: return
        currentMask.invert()
        // Trigger recomposition
        _selectionMask.value = currentMask
    }
    
    /**
     * Select all
     */
    fun selectAll(width: Int, height: Int) {
        val mask = SelectionMask(width, height)
        mask.selectAll()
        setSelection(mask)
    }
    
    /**
     * Set feather radius
     */
    fun setFeatherRadius(radius: Float) {
        _featherRadius.value = radius.coerceIn(0f, 50f)
    }
    
    /**
     * Set magic wand tolerance
     */
    fun setTolerance(tolerance: Int) {
        _tolerance.value = tolerance.coerceIn(0, 255)
    }
    
    /**
     * Set selection operation mode
     */
    fun setOperationMode(mode: SelectionOperationMode) {
        _operationMode.value = mode
    }
    
    /**
     * Copy selection to new layer
     */
    fun copySelection() {
        viewModelScope.launch {
            val mask = _selectionMask.value ?: return@launch
            val layer = _activeLayer.value ?: return@launch
            
            val command = CopySelectionCommand(
                layerManager = layerManager,
                sourceLayer = layer,
                selectionMask = mask
            )
            
            historyManager.execute(command)
        }
    }
    
    /**
     * Cut selection (copy then clear)
     */
    fun cutSelection() {
        viewModelScope.launch {
            val mask = _selectionMask.value ?: return@launch
            val layer = _activeLayer.value ?: return@launch
            
            val cutCommand = CutSelectionCommand(
                layerManager = layerManager,
                sourceLayer = layer,
                selectionMask = mask
            )
            
            historyManager.execute(cutCommand)
        }
    }
    
    /**
     * Clear selected area (delete pixels)
     */
    fun clearSelectedArea() {
        viewModelScope.launch {
            val mask = _selectionMask.value ?: return@launch
            val layer = _activeLayer.value ?: return@launch
            
            val command = ClearSelectionCommand(
                layer = layer,
                selectionMask = mask
            )
            
            historyManager.execute(command)
        }
    }
    
    /**
     * Fill selection with color
     */
    fun fillSelection(color: Int) {
        viewModelScope.launch {
            val mask = _selectionMask.value ?: return@launch
            val layer = _activeLayer.value ?: return@launch
            
            val command = FillSelectionCommand(
                layer = layer,
                selectionMask = mask,
                fillColor = color
            )
            
            historyManager.execute(command)
        }
    }
    
    /**
     * Deselect (tap outside selection)
     */
    fun deselect() {
        clearSelection()
    }
    
    /**
     * Check if point is inside current selection
     */
    fun isPointInSelection(x: Int, y: Int): Boolean {
        return _selectionMask.value?.isSelected(x, y) ?: false
    }
    
    /**
     * Set selection in progress state
     */
    fun setIsSelecting(selecting: Boolean) {
        _isSelecting.value = selecting
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up selection mask
        _selectionMask.value?.recycle()
    }
}

/**
 * Selection tool types
 */
enum class SelectionToolType {
    LASSO,      // Freehand path
    RECTANGLE,  // Rectangular selection
    ELLIPSE,    // Elliptical selection
    MAGIC_WAND  // Color-based flood fill
}

/**
 * Selection operation modes
 */
enum class SelectionOperationMode {
    NEW,        // Replace existing selection
    ADD,        // Add to existing selection
    SUBTRACT,   // Remove from existing selection
    INTERSECT   // Intersect with existing selection
}
