package com.artboard.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artboard.data.model.Project
import com.artboard.data.model.ProjectSummary
import com.artboard.data.model.SortMode
import com.artboard.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Gallery Screen
 * Manages project list, search, sort, and gallery operations
 */
class GalleryViewModel(
    private val repository: ProjectRepository
) : ViewModel() {
    
    // Raw project list from repository
    private val _projects = MutableStateFlow<List<ProjectSummary>>(emptyList())
    val projects: StateFlow<List<ProjectSummary>> = _projects.asStateFlow()
    
    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Sort mode state
    private val _sortMode = MutableStateFlow(SortMode.MODIFIED_DESC)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Filtered and sorted projects (combined flow)
    val filteredProjects: StateFlow<List<ProjectSummary>> = combine(
        _projects,
        _searchQuery,
        _sortMode
    ) { projs, query, sort ->
        var filtered = projs
        
        // Apply search filter
        if (query.isNotEmpty()) {
            filtered = filtered.filter { proj ->
                proj.name.contains(query, ignoreCase = true) ||
                proj.tags.any { it.contains(query, ignoreCase = true) }
            }
        }
        
        // Apply sort
        filtered = when (sort) {
            SortMode.MODIFIED_DESC -> filtered.sortedByDescending { it.modifiedAt }
            SortMode.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
            SortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortMode.SIZE_DESC -> filtered.sortedByDescending { it.width * it.height }
        }
        
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    
    init {
        loadProjects()
    }
    
    /**
     * Load all projects from repository
     */
    fun loadProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val loaded = repository.getAllProjects()
                _projects.value = loaded
            } catch (e: Exception) {
                _error.value = "Failed to load projects: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update search query (triggers filter)
     */
    fun searchProjects(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Change sort mode (triggers re-sort)
     */
    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }
    
    /**
     * Create a new blank project
     */
    fun createNewProject(
        name: String = "Untitled ${_projects.value.size + 1}",
        width: Int = 2048,
        height: Int = 2048
    ) {
        viewModelScope.launch {
            try {
                val project = Project.create(
                    name = name,
                    width = width,
                    height = height
                )
                repository.save(project)
                loadProjects()
            } catch (e: Exception) {
                _error.value = "Failed to create project: ${e.message}"
            }
        }
    }
    
    /**
     * Delete a project by ID
     */
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                repository.delete(projectId)
                loadProjects()
            } catch (e: Exception) {
                _error.value = "Failed to delete project: ${e.message}"
            }
        }
    }
    
    /**
     * Duplicate a project
     */
    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            try {
                repository.duplicate(projectId)
                loadProjects()
            } catch (e: Exception) {
                _error.value = "Failed to duplicate project: ${e.message}"
            }
        }
    }
    
    /**
     * Rename a project
     */
    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            try {
                val result = repository.load(projectId)
                if (result.isSuccess) {
                    val project = result.getOrThrow()
                    repository.save(project.copy(name = newName))
                    loadProjects()
                } else {
                    _error.value = "Failed to load project: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to rename project: ${e.message}"
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
