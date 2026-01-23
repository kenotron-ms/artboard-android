package com.artboard.ui.transform

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artboard.data.model.Layer
import com.artboard.data.model.Transform
import com.artboard.data.model.TransformType
import com.artboard.domain.history.*
import com.artboard.domain.transform.TransformEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for transform mode
 * Manages transform state, preview, and command execution
 */
class TransformViewModel(
    private val historyManager: HistoryManager
) : ViewModel() {
    
    private val _isTransformMode = MutableStateFlow(false)
    val isTransformMode: StateFlow<Boolean> = _isTransformMode.asStateFlow()
    
    private val _transformType = MutableStateFlow(TransformType.FREE)
    val transformType: StateFlow<TransformType> = _transformType.asStateFlow()
    
    private val _currentTransform = MutableStateFlow(Transform.identity())
    val currentTransform: StateFlow<Transform> = _currentTransform.asStateFlow()
    
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()
    
    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()
    
    private var originalLayer: Layer? = null
    private var originalBitmap: Bitmap? = null
    private var currentLayerIndex: Int = -1
    private var currentLayers: List<Layer> = emptyList()
    
    private val transformEngine = TransformEngine()
    
    /**
     * Enter transform mode with the active layer
     */
    fun enterTransformMode(layerIndex: Int, layers: List<Layer>) {
        if (layerIndex < 0 || layerIndex >= layers.size) return
        
        val layer = layers[layerIndex]
        
        // Save original state
        originalLayer = layer
        originalBitmap = layer.bitmap.copy(layer.bitmap.config, true)
        currentLayerIndex = layerIndex
        currentLayers = layers
        
        // Reset transform
        _currentTransform.value = Transform.identity()
        _transformType.value = TransformType.FREE
        _isTransformMode.value = true
        
        // Generate initial preview
        updatePreview()
    }
    
    /**
     * Exit transform mode (cleanup)
     */
    fun exitTransformMode() {
        // Cleanup
        originalBitmap?.recycle()
        _previewBitmap.value?.recycle()
        
        originalLayer = null
        originalBitmap = null
        currentLayerIndex = -1
        currentLayers = emptyList()
        
        _isTransformMode.value = false
        _currentTransform.value = Transform.identity()
        _previewBitmap.value = null
    }
    
    /**
     * Set transform type (FREE, UNIFORM, DISTORT)
     */
    fun setTransformType(type: TransformType) {
        _transformType.value = type
        
        // Reset certain transform properties based on type
        when (type) {
            TransformType.UNIFORM -> {
                // Reset to uniform scale
                val avgScale = (_currentTransform.value.scaleX + _currentTransform.value.scaleY) / 2f
                _currentTransform.value = _currentTransform.value.copy(
                    scale = avgScale,
                    scaleX = 1f,
                    scaleY = 1f
                )
            }
            TransformType.FREE -> {
                // Allow free scaling
                if (_currentTransform.value.scale != 1f) {
                    _currentTransform.value = _currentTransform.value.copy(
                        scaleX = _currentTransform.value.scale,
                        scaleY = _currentTransform.value.scale,
                        scale = 1f
                    )
                }
            }
            else -> {}
        }
        
        updatePreview()
    }
    
    /**
     * Update current transform
     */
    fun updateTransform(transform: Transform) {
        _currentTransform.value = transform
        updatePreview()
    }
    
    /**
     * Update preview bitmap with current transform
     */
    private fun updatePreview() {
        viewModelScope.launch {
            val bitmap = originalBitmap ?: return@launch
            val transform = _currentTransform.value
            
            if (transform.isIdentity()) {
                _previewBitmap.value = bitmap.copy(bitmap.config, true)
                return@launch
            }
            
            // Generate preview on background thread
            val preview = withContext(Dispatchers.Default) {
                transformEngine.applyTransform(bitmap, transform)
            }
            
            // Update preview
            _previewBitmap.value?.recycle()
            _previewBitmap.value = preview
        }
    }
    
    /**
     * Apply current transform and commit to history
     */
    fun applyTransform(onComplete: (List<Layer>) -> Unit) {
        val transform = _currentTransform.value
        
        // Don't apply if no changes
        if (transform.isIdentity()) {
            exitTransformMode()
            onComplete(currentLayers)
            return
        }
        
        _isApplying.value = true
        
        viewModelScope.launch {
            try {
                // Create and execute command
                val command = TransformLayerCommand(
                    layerIndex = currentLayerIndex,
                    transform = transform,
                    layers = currentLayers
                )
                
                val result = withContext(Dispatchers.Default) {
                    historyManager.execute(command)
                }
                
                // Update layers
                result.updatedLayers?.let { updatedLayers ->
                    currentLayers = updatedLayers
                    onComplete(updatedLayers)
                }
                
                exitTransformMode()
            } finally {
                _isApplying.value = false
            }
        }
    }
    
    /**
     * Cancel transform and restore original
     */
    fun cancelTransform(onComplete: (List<Layer>) -> Unit) {
        // Restore original bitmap
        originalBitmap?.let { original ->
            val layer = currentLayers[currentLayerIndex]
            layer.bitmap.recycle()
            layer.bitmap = original.copy(original.config, true)
        }
        
        exitTransformMode()
        onComplete(currentLayers)
    }
    
    /**
     * Flip horizontally
     */
    fun flipHorizontal() {
        viewModelScope.launch {
            val command = FlipLayerHorizontalCommand(
                layerIndex = currentLayerIndex,
                layers = currentLayers
            )
            
            val result = withContext(Dispatchers.Default) {
                historyManager.execute(command)
            }
            
            result.updatedLayers?.let { updatedLayers ->
                currentLayers = updatedLayers
                
                // Update original bitmap reference
                originalBitmap?.recycle()
                originalBitmap = updatedLayers[currentLayerIndex].bitmap.copy(
                    updatedLayers[currentLayerIndex].bitmap.config,
                    true
                )
                
                updatePreview()
            }
        }
    }
    
    /**
     * Flip vertically
     */
    fun flipVertical() {
        viewModelScope.launch {
            val command = FlipLayerVerticalCommand(
                layerIndex = currentLayerIndex,
                layers = currentLayers
            )
            
            val result = withContext(Dispatchers.Default) {
                historyManager.execute(command)
            }
            
            result.updatedLayers?.let { updatedLayers ->
                currentLayers = updatedLayers
                
                // Update original bitmap reference
                originalBitmap?.recycle()
                originalBitmap = updatedLayers[currentLayerIndex].bitmap.copy(
                    updatedLayers[currentLayerIndex].bitmap.config,
                    true
                )
                
                updatePreview()
            }
        }
    }
    
    /**
     * Rotate 90 degrees clockwise
     */
    fun rotate90Clockwise() {
        val currentRotation = _currentTransform.value.rotation
        val newRotation = (currentRotation + 90f) % 360f
        _currentTransform.value = _currentTransform.value.withRotation(newRotation)
        updatePreview()
    }
    
    /**
     * Rotate 90 degrees counter-clockwise
     */
    fun rotate90CounterClockwise() {
        val currentRotation = _currentTransform.value.rotation
        val newRotation = (currentRotation - 90f) % 360f
        _currentTransform.value = _currentTransform.value.withRotation(newRotation)
        updatePreview()
    }
    
    /**
     * Reset transform to identity
     */
    fun resetTransform() {
        _currentTransform.value = Transform.identity()
        updatePreview()
    }
    
    /**
     * Check if transform can be applied (has changes)
     */
    fun canApply(): Boolean {
        return !_currentTransform.value.isIdentity()
    }
    
    override fun onCleared() {
        super.onCleared()
        exitTransformMode()
    }
}
