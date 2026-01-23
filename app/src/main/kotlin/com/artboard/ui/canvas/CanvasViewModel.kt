package com.artboard.ui.canvas

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artboard.data.model.Brush
import com.artboard.data.model.Layer
import com.artboard.data.model.Project
import com.artboard.data.model.Stroke
import com.artboard.domain.history.AddStrokeCommand
import com.artboard.domain.history.ClearLayerCommand
import com.artboard.domain.history.DeleteLayerCommand
import com.artboard.domain.history.HistoryManager
import com.artboard.domain.layer.LayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for canvas state management
 */
class CanvasViewModel : ViewModel() {
    
    // State
    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()
    
    private val _currentBrush = MutableStateFlow(Brush.pen())
    val currentBrush: StateFlow<Brush> = _currentBrush.asStateFlow()
    
    private val _currentColor = MutableStateFlow(Color.BLACK)
    val currentColor: StateFlow<Int> = _currentColor.asStateFlow()
    
    private val _activeLayerIndex = MutableStateFlow(0)
    val activeLayerIndex: StateFlow<Int> = _activeLayerIndex.asStateFlow()
    
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    
    // UI visibility state
    private val _toolbarVisible = MutableStateFlow(true)
    val toolbarVisible: StateFlow<Boolean> = _toolbarVisible.asStateFlow()
    
    private val _statusBarVisible = MutableStateFlow(true)
    val statusBarVisible: StateFlow<Boolean> = _statusBarVisible.asStateFlow()
    
    private val _isDrawing = MutableStateFlow(false)
    val isDrawing: StateFlow<Boolean> = _isDrawing.asStateFlow()
    
    // History
    private val historyManager = HistoryManager()
    private var layerManager: LayerManager? = null
    
    init {
        // Create default project
        createNewProject("My Drawing", 2048, 2048)
    }
    
    /**
     * Create a new project
     */
    fun createNewProject(name: String, width: Int, height: Int) {
        val project = Project.create(name, width, height)
        _project.value = project
        _activeLayerIndex.value = 0
        layerManager = LayerManager(width, height)
        historyManager.clear()
        updateHistoryState()
    }
    
    /**
     * Load an existing project
     */
    fun loadProject(project: Project) {
        _project.value = project
        _activeLayerIndex.value = project.getActiveLayerIndex()
        layerManager = LayerManager(project.width, project.height)
        historyManager.clear()
        updateHistoryState()
    }
    
    /**
     * Set current brush
     */
    fun setBrush(brush: Brush) {
        _currentBrush.value = brush
    }
    
    /**
     * Set current color
     */
    fun setColor(color: Int) {
        _currentColor.value = color
    }
    
    /**
     * Add a stroke to the active layer
     */
    fun addStroke(stroke: Stroke) {
        val currentProject = _project.value ?: return
        
        viewModelScope.launch {
            val command = AddStrokeCommand(
                stroke,
                _activeLayerIndex.value,
                currentProject.layers
            )
            
            historyManager.execute(command)
            _project.value = currentProject.touch()
            updateHistoryState()
        }
    }
    
    /**
     * Add a new layer
     */
    fun addLayer() {
        val currentProject = _project.value ?: return
        val manager = layerManager ?: return
        
        val newLayer = manager.createLayer("Layer ${currentProject.layers.size + 1}")
        _project.value = currentProject.addLayer(newLayer)
        _activeLayerIndex.value = currentProject.layers.size
        updateHistoryState()
    }
    
    /**
     * Delete a layer
     */
    fun deleteLayer(index: Int) {
        val currentProject = _project.value ?: return
        if (currentProject.layers.size <= 1) return // Keep at least one layer
        
        viewModelScope.launch {
            val command = DeleteLayerCommand(index, currentProject.layers)
            val result = historyManager.execute(command)
            
            result.updatedLayers?.let { layers ->
                _project.value = currentProject.copy(layers = layers).touch()
                
                // Adjust active layer if needed
                if (_activeLayerIndex.value >= layers.size) {
                    _activeLayerIndex.value = layers.size - 1
                }
            }
            
            updateHistoryState()
        }
    }
    
    /**
     * Clear the active layer
     */
    fun clearActiveLayer() {
        val currentProject = _project.value ?: return
        
        viewModelScope.launch {
            val command = ClearLayerCommand(
                _activeLayerIndex.value,
                currentProject.layers
            )
            
            historyManager.execute(command)
            _project.value = currentProject.touch()
            updateHistoryState()
        }
    }
    
    /**
     * Set active layer
     */
    fun setActiveLayer(index: Int) {
        val currentProject = _project.value ?: return
        if (index in 0 until currentProject.layers.size) {
            _activeLayerIndex.value = index
        }
    }
    
    /**
     * Toggle layer visibility
     */
    fun toggleLayerVisibility(index: Int) {
        val currentProject = _project.value ?: return
        val layer = currentProject.layers.getOrNull(index) ?: return
        
        val updatedLayer = layer.toggleVisibility()
        _project.value = currentProject.updateLayer(index, updatedLayer)
    }
    
    /**
     * Update layer opacity
     */
    fun updateLayerOpacity(index: Int, opacity: Float) {
        val currentProject = _project.value ?: return
        val layer = currentProject.layers.getOrNull(index) ?: return
        
        val updatedLayer = layer.withOpacity(opacity)
        _project.value = currentProject.updateLayer(index, updatedLayer)
    }
    
    /**
     * Undo last action
     */
    fun undo() {
        val result = historyManager.undo()
        result?.updatedLayers?.let { layers ->
            val currentProject = _project.value ?: return
            _project.value = currentProject.copy(layers = layers)
        }
        updateHistoryState()
    }
    
    /**
     * Redo last undone action
     */
    fun redo() {
        val result = historyManager.redo()
        result?.updatedLayers?.let { layers ->
            val currentProject = _project.value ?: return
            _project.value = currentProject.copy(layers = layers)
        }
        updateHistoryState()
    }
    
    /**
     * Update history state
     */
    private fun updateHistoryState() {
        _canUndo.value = historyManager.canUndo()
        _canRedo.value = historyManager.canRedo()
    }
    
    /**
     * Get layers for the canvas
     */
    fun getLayers(): List<Layer> {
        return _project.value?.layers ?: emptyList()
    }
    
    /**
     * Show UI (toolbar and status bar)
     */
    fun showUI() {
        _toolbarVisible.value = true
        _statusBarVisible.value = true
    }
    
    /**
     * Hide UI (toolbar and status bar)
     */
    fun hideUI() {
        if (!_isDrawing.value) {
            _toolbarVisible.value = false
            _statusBarVisible.value = false
        }
    }
    
    /**
     * Toggle UI visibility
     */
    fun toggleUI() {
        if (_toolbarVisible.value) {
            hideUI()
        } else {
            showUI()
        }
    }
    
    /**
     * User interaction - reset auto-hide timer
     */
    fun onInteraction() {
        showUI()
    }
    
    /**
     * Drawing started - keep UI visible
     */
    fun onDrawingStarted() {
        _isDrawing.value = true
    }
    
    /**
     * Drawing ended - resume auto-hide
     */
    fun onDrawingEnded() {
        _isDrawing.value = false
    }
}
