package com.artboard.ui.brush

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artboard.data.model.Brush
import com.artboard.ui.brush.components.BrushCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Brush Selector panel
 * Manages brush library, categories, favorites, and temporary brush modifications
 */
class BrushSelectorViewModel : ViewModel() {
    
    // All available brushes
    private val _brushes = MutableStateFlow<List<Brush>>(emptyList())
    val brushes: StateFlow<List<Brush>> = _brushes.asStateFlow()
    
    // Currently selected category
    private val _selectedCategory = MutableStateFlow(BrushCategory.SKETCH)
    val selectedCategory: StateFlow<BrushCategory> = _selectedCategory.asStateFlow()
    
    // Favorite brush IDs
    private val _favorites = MutableStateFlow<Set<Int>>(emptySet())
    val favorites: StateFlow<Set<Int>> = _favorites.asStateFlow()
    
    // Temporary brush for live preview adjustments
    private val _tempBrush = MutableStateFlow<Brush?>(null)
    val tempBrush: StateFlow<Brush?> = _tempBrush.asStateFlow()
    
    // Brush Studio expanded state
    private val _showBrushStudio = MutableStateFlow(false)
    val showBrushStudio: StateFlow<Boolean> = _showBrushStudio.asStateFlow()
    
    // Filtered brushes based on selected category
    val filteredBrushes: StateFlow<List<Brush>> = combine(
        brushes,
        selectedCategory,
        favorites
    ) { brushes, category, favs ->
        when (category) {
            BrushCategory.FAVORITES -> brushes.filter { it.hashCode() in favs }
            else -> brushes.filter { getBrushCategory(it) == category }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    init {
        loadBrushes()
        loadFavorites()
    }
    
    /**
     * Load all available brushes
     */
    private fun loadBrushes() {
        _brushes.value = listOf(
            // Sketch category
            Brush.pencil(),
            Brush.pen(),
            Brush.calligraphy(),
            
            // Paint category
            Brush.marker(),
            Brush.markerChisel(),
            Brush.airbrush(),
            
            // Texture category
            // Add texture brushes here
            
            // Effects category
            // Add effect brushes here
            
            // Utility
            Brush.eraser()
        )
    }
    
    /**
     * Load saved favorites from preferences
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            // TODO: Load from SharedPreferences or DataStore
            // For now, empty set
            _favorites.value = emptySet()
        }
    }
    
    /**
     * Save favorites to preferences
     */
    private fun saveFavorites() {
        viewModelScope.launch {
            // TODO: Save to SharedPreferences or DataStore
        }
    }
    
    /**
     * Select a category
     */
    fun selectCategory(category: BrushCategory) {
        _selectedCategory.value = category
    }
    
    /**
     * Toggle favorite status for a brush
     */
    fun toggleFavorite(brush: Brush) {
        val brushId = brush.hashCode()
        val updated = _favorites.value.toMutableSet()
        if (brushId in updated) {
            updated.remove(brushId)
        } else {
            updated.add(brushId)
        }
        _favorites.value = updated
        saveFavorites()
    }
    
    /**
     * Set temporary brush for live preview
     */
    fun setTempBrush(brush: Brush) {
        _tempBrush.value = brush
    }
    
    /**
     * Clear temporary brush
     */
    fun clearTempBrush() {
        _tempBrush.value = null
    }
    
    /**
     * Toggle Brush Studio expanded state
     */
    fun toggleBrushStudio() {
        _showBrushStudio.value = !_showBrushStudio.value
    }
    
    /**
     * Duplicate a brush
     */
    fun duplicateBrush(brush: Brush) {
        // Create a copy with modified name
        val copy = brush.copy(size = brush.size + 0.1f) // Slight modification to create unique instance
        val updated = _brushes.value.toMutableList()
        updated.add(copy)
        _brushes.value = updated
    }
    
    /**
     * Delete a custom brush
     */
    fun deleteBrush(brush: Brush) {
        val updated = _brushes.value.toMutableList()
        updated.remove(brush)
        _brushes.value = updated
    }
    
    /**
     * Get category for a brush based on its type
     */
    private fun getBrushCategory(brush: Brush): BrushCategory {
        return when (brush.type) {
            com.artboard.data.model.BrushType.PENCIL,
            com.artboard.data.model.BrushType.PEN,
            com.artboard.data.model.BrushType.CALLIGRAPHY -> BrushCategory.SKETCH
            
            com.artboard.data.model.BrushType.MARKER,
            com.artboard.data.model.BrushType.AIRBRUSH -> BrushCategory.PAINT
            
            com.artboard.data.model.BrushType.ERASER -> BrushCategory.SKETCH // Default to sketch
        }
    }
    
    /**
     * Check if a brush is favorited
     */
    fun isFavorite(brush: Brush): Boolean {
        return brush.hashCode() in _favorites.value
    }
}
