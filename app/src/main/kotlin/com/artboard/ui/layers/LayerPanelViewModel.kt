package com.artboard.ui.layers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artboard.data.model.BlendMode
import com.artboard.data.model.Layer
import com.artboard.domain.layer.LayerManager
import com.artboard.ui.layers.components.LayerThumbnailGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for Layer Panel UI
 * Manages layer list, active selection, and operations
 */
class LayerPanelViewModel(
    private val layerManager: LayerManager,
    private val thumbnailGenerator: LayerThumbnailGenerator
) : ViewModel() {
    
    private val _layers = MutableStateFlow<List<Layer>>(emptyList())
    val layers: StateFlow<List<Layer>> = _layers.asStateFlow()
    
    private val _activeLayerId = MutableStateFlow<String?>(null)
    val activeLayerId: StateFlow<String?> = _activeLayerId.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Initialize with existing layers
     */
    fun setLayers(layers: List<Layer>) {
        _layers.value = layers.sortedBy { it.position }
        
        // Generate thumbnails for all layers
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                thumbnailGenerator.preGenerateThumbnails(layers)
            }
        }
        
        // Set first layer as active if none selected
        if (_activeLayerId.value == null && layers.isNotEmpty()) {
            _activeLayerId.value = layers.first().id
        }
    }
    
    /**
     * Set active layer
     */
    fun setActiveLayer(layerId: String) {
        _activeLayerId.value = layerId
    }
    
    /**
     * Add new layer
     */
    fun addLayer(name: String = "Layer ${_layers.value.size + 1}") {
        val newPosition = _layers.value.maxOfOrNull { it.position }?.plus(1) ?: 0
        val newLayer = layerManager.createLayer(name).withPosition(newPosition)
        
        _layers.value = _layers.value + newLayer
        _activeLayerId.value = newLayer.id
        
        // Generate thumbnail
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                thumbnailGenerator.generateThumbnail(newLayer)
            }
        }
    }
    
    /**
     * Delete layer
     */
    fun deleteLayer(layerId: String) {
        val currentLayers = _layers.value
        val layerToDelete = currentLayers.find { it.id == layerId } ?: return
        
        // Can't delete if it's the only layer
        if (currentLayers.size <= 1) return
        
        // Remove from list
        _layers.value = currentLayers.filter { it.id != layerId }
        
        // Clear thumbnail cache
        thumbnailGenerator.invalidate(layerId)
        
        // If deleted layer was active, select another
        if (_activeLayerId.value == layerId) {
            val newActive = _layers.value.firstOrNull()
            _activeLayerId.value = newActive?.id
        }
    }
    
    /**
     * Duplicate layer
     */
    fun duplicateLayer(layerId: String) {
        val layer = _layers.value.find { it.id == layerId } ?: return
        
        val duplicated = layerManager.duplicateLayer(layer)
        val newPosition = layer.position + 1
        val duplicatedWithPosition = duplicated.withPosition(newPosition)
        
        // Update positions of layers above
        val updatedLayers = _layers.value.map { 
            if (it.position >= newPosition && it.id != layerId) {
                it.withPosition(it.position + 1)
            } else {
                it
            }
        }
        
        _layers.value = (updatedLayers + duplicatedWithPosition).sortedBy { it.position }
        
        // Generate thumbnail for duplicate
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                thumbnailGenerator.generateThumbnail(duplicatedWithPosition)
            }
        }
    }
    
    /**
     * Reorder layers (drag and drop)
     */
    fun reorderLayer(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        
        val currentLayers = _layers.value.toMutableList()
        val layer = currentLayers.removeAt(fromIndex)
        currentLayers.add(toIndex, layer)
        
        // Update positions
        _layers.value = currentLayers.mapIndexed { index, layer ->
            layer.withPosition(index)
        }
    }
    
    /**
     * Toggle layer visibility
     */
    fun toggleVisibility(layerId: String) {
        _layers.value = _layers.value.map { layer ->
            if (layer.id == layerId) {
                layer.toggleVisibility()
            } else {
                layer
            }
        }
    }
    
    /**
     * Toggle layer lock
     */
    fun toggleLock(layerId: String) {
        _layers.value = _layers.value.map { layer ->
            if (layer.id == layerId) {
                layer.toggleLock()
            } else {
                layer
            }
        }
    }
    
    /**
     * Change layer opacity
     */
    fun changeOpacity(layerId: String, opacity: Float) {
        _layers.value = _layers.value.map { layer ->
            if (layer.id == layerId) {
                layer.withOpacity(opacity)
            } else {
                layer
            }
        }
    }
    
    /**
     * Change layer blend mode
     */
    fun changeBlendMode(layerId: String, blendMode: BlendMode) {
        _layers.value = _layers.value.map { layer ->
            if (layer.id == layerId) {
                layer.withBlendMode(blendMode)
            } else {
                layer
            }
        }
    }
    
    /**
     * Rename layer
     */
    fun renameLayer(layerId: String, newName: String) {
        _layers.value = _layers.value.map { layer ->
            if (layer.id == layerId) {
                layer.withName(newName)
            } else {
                layer
            }
        }
    }
    
    /**
     * Merge layer with layer below
     */
    fun mergeLayerDown(layerId: String) {
        val currentLayers = _layers.value
        val layer = currentLayers.find { it.id == layerId } ?: return
        
        // Find layer below (lower position)
        val layerBelow = currentLayers
            .filter { it.position < layer.position }
            .maxByOrNull { it.position } ?: return
        
        // Merge layers
        val merged = layerManager.mergeLayers(layerBelow, layer)
            .withPosition(layerBelow.position)
        
        // Remove both layers and add merged
        _layers.value = currentLayers
            .filter { it.id != layerId && it.id != layerBelow.id }
            .plus(merged)
            .sortedBy { it.position }
        
        // Set merged layer as active
        _activeLayerId.value = merged.id
        
        // Generate thumbnail for merged layer
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                thumbnailGenerator.generateThumbnail(merged)
            }
        }
        
        // Clear old thumbnails
        thumbnailGenerator.invalidateAll(listOf(layerId, layerBelow.id))
    }
    
    /**
     * Clear layer content (fill with transparent)
     */
    fun clearLayer(layerId: String) {
        val layer = _layers.value.find { it.id == layerId } ?: return
        
        val cleared = layerManager.clearLayer(layer)
        
        _layers.value = _layers.value.map { 
            if (it.id == layerId) cleared else it
        }
        
        // Regenerate thumbnail
        thumbnailGenerator.invalidate(layerId)
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                thumbnailGenerator.generateThumbnail(cleared)
            }
        }
    }
    
    /**
     * Update thumbnail for a layer (after drawing)
     */
    fun updateThumbnail(layerId: String) {
        val layer = _layers.value.find { it.id == layerId } ?: return
        
        // Invalidate cached thumbnail
        thumbnailGenerator.invalidate(layerId)
        
        // Generate new thumbnail
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                thumbnailGenerator.generateThumbnail(layer)
            }
        }
    }
    
    /**
     * Get active layer
     */
    fun getActiveLayer(): Layer? {
        val id = _activeLayerId.value ?: return null
        return _layers.value.find { it.id == id }
    }
    
    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        thumbnailGenerator.clearCache()
    }
}
