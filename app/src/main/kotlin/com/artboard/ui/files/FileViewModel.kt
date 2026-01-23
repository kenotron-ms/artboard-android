package com.artboard.ui.files

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artboard.data.model.Project
import com.artboard.data.repository.ProjectRepository
import com.artboard.data.storage.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for file operations (save, load, export, import)
 * Manages UI state and coordinates file I/O operations
 */
class FileViewModel(
    private val context: Context,
    private val repository: ProjectRepository
) : ViewModel() {
    
    private val exportManager = ExportManager()
    private val importManager = ImportManager(context)
    
    // Progress state
    private val _progressState = MutableStateFlow(ProgressState())
    val progressState: StateFlow<ProgressState> = _progressState.asStateFlow()
    
    // Operation result state
    private val _operationResult = MutableStateFlow<FileOperationResult?>(null)
    val operationResult: StateFlow<FileOperationResult?> = _operationResult.asStateFlow()
    
    // Export dialog state
    private val _exportDialogState = MutableStateFlow(ExportDialogState())
    val exportDialogState: StateFlow<ExportDialogState> = _exportDialogState.asStateFlow()
    
    /**
     * Save a project
     */
    fun saveProject(project: Project, outputFile: File? = null) {
        viewModelScope.launch {
            _progressState.value = ProgressState(
                isVisible = true,
                progress = 0f,
                message = "Saving project..."
            )
            
            val progressTracker = UIProgressTracker { progress, message ->
                _progressState.value = ProgressState(
                    isVisible = true,
                    progress = progress,
                    message = message
                )
            }
            
            val result = repository.save(project, outputFile, progressTracker)
            
            _progressState.value = ProgressState(isVisible = false)
            
            _operationResult.value = if (result.isSuccess) {
                FileOperationResult.Success(
                    message = "Project saved successfully",
                    file = result.getOrNull()
                )
            } else {
                FileOperationResult.Error(
                    message = "Failed to save project: ${result.exceptionOrNull()?.message}",
                    exception = result.exceptionOrNull()
                )
            }
        }
    }
    
    /**
     * Load a project
     */
    fun loadProject(projectId: String, onSuccess: (Project) -> Unit) {
        viewModelScope.launch {
            _progressState.value = ProgressState(
                isVisible = true,
                progress = 0f,
                message = "Loading project..."
            )
            
            val progressTracker = UIProgressTracker { progress, message ->
                _progressState.value = ProgressState(
                    isVisible = true,
                    progress = progress,
                    message = message
                )
            }
            
            val result = repository.load(projectId)
            
            _progressState.value = ProgressState(isVisible = false)
            
            if (result.isSuccess) {
                _operationResult.value = FileOperationResult.Success(
                    message = "Project loaded successfully"
                )
                onSuccess(result.getOrThrow())
            } else {
                _operationResult.value = FileOperationResult.Error(
                    message = "Failed to load project: ${result.exceptionOrNull()?.message}",
                    exception = result.exceptionOrNull()
                )
            }
        }
    }
    
    /**
     * Export a project to PNG
     */
    fun exportPNG(
        project: Project,
        outputFile: File,
        flattenLayers: Boolean = true
    ) {
        viewModelScope.launch {
            _progressState.value = ProgressState(
                isVisible = true,
                progress = 0f,
                message = "Exporting PNG..."
            )
            
            val progressTracker = UIProgressTracker { progress, message ->
                _progressState.value = ProgressState(
                    isVisible = true,
                    progress = progress,
                    message = message
                )
            }
            
            val result = exportManager.exportPNG(
                project,
                outputFile,
                flattenLayers,
                progressTracker
            )
            
            _progressState.value = ProgressState(isVisible = false)
            
            _operationResult.value = if (result.isSuccess) {
                FileOperationResult.Success(
                    message = "Exported to PNG successfully",
                    file = result.getOrNull()
                )
            } else {
                FileOperationResult.Error(
                    message = "Failed to export PNG: ${result.exceptionOrNull()?.message}",
                    exception = result.exceptionOrNull()
                )
            }
        }
    }
    
    /**
     * Export a project to JPG
     */
    fun exportJPG(
        project: Project,
        outputFile: File,
        quality: Int = 85
    ) {
        viewModelScope.launch {
            _progressState.value = ProgressState(
                isVisible = true,
                progress = 0f,
                message = "Exporting JPG..."
            )
            
            val progressTracker = UIProgressTracker { progress, message ->
                _progressState.value = ProgressState(
                    isVisible = true,
                    progress = progress,
                    message = message
                )
            }
            
            val result = exportManager.exportJPG(
                project,
                outputFile,
                quality,
                progressTracker
            )
            
            _progressState.value = ProgressState(isVisible = false)
            
            _operationResult.value = if (result.isSuccess) {
                FileOperationResult.Success(
                    message = "Exported to JPG successfully",
                    file = result.getOrNull()
                )
            } else {
                FileOperationResult.Error(
                    message = "Failed to export JPG: ${result.exceptionOrNull()?.message}",
                    exception = result.exceptionOrNull()
                )
            }
        }
    }
    
    /**
     * Export a project to PSD
     */
    fun exportPSD(
        project: Project,
        outputFile: File
    ) {
        viewModelScope.launch {
            _progressState.value = ProgressState(
                isVisible = true,
                progress = 0f,
                message = "Exporting PSD..."
            )
            
            val progressTracker = UIProgressTracker { progress, message ->
                _progressState.value = ProgressState(
                    isVisible = true,
                    progress = progress,
                    message = message
                )
            }
            
            val result = exportManager.exportPSD(
                project,
                outputFile,
                progressTracker
            )
            
            _progressState.value = ProgressState(isVisible = false)
            
            _operationResult.value = if (result.isSuccess) {
                FileOperationResult.Success(
                    message = "Exported to PSD successfully",
                    file = result.getOrNull()
                )
            } else {
                FileOperationResult.Error(
                    message = "Failed to export PSD: ${result.exceptionOrNull()?.message}",
                    exception = result.exceptionOrNull()
                )
            }
        }
    }
    
    /**
     * Import an image as a new layer
     */
    fun importAsLayer(
        project: Project,
        imageUri: Uri,
        layerName: String,
        fitToCanvas: Boolean = true,
        onSuccess: (Project) -> Unit
    ) {
        viewModelScope.launch {
            _progressState.value = ProgressState(
                isVisible = true,
                progress = 0f,
                message = "Importing image..."
            )
            
            val progressTracker = UIProgressTracker { progress, message ->
                _progressState.value = ProgressState(
                    isVisible = true,
                    progress = progress,
                    message = message
                )
            }
            
            val result = importManager.importAsLayer(
                project,
                imageUri,
                layerName,
                fitToCanvas,
                progressTracker
            )
            
            _progressState.value = ProgressState(isVisible = false)
            
            if (result.isSuccess) {
                _operationResult.value = FileOperationResult.Success(
                    message = "Image imported successfully"
                )
                onSuccess(result.getOrThrow())
            } else {
                _operationResult.value = FileOperationResult.Error(
                    message = "Failed to import image: ${result.exceptionOrNull()?.message}",
                    exception = result.exceptionOrNull()
                )
            }
        }
    }
    
    /**
     * Show export dialog
     */
    fun showExportDialog(project: Project) {
        _exportDialogState.value = ExportDialogState(
            isVisible = true,
            projectName = project.name,
            selectedFormat = ExportFormat.PNG,
            jpgQuality = 85
        )
    }
    
    /**
     * Hide export dialog
     */
    fun hideExportDialog() {
        _exportDialogState.value = ExportDialogState(isVisible = false)
    }
    
    /**
     * Update export format selection
     */
    fun selectExportFormat(format: ExportFormat) {
        _exportDialogState.value = _exportDialogState.value.copy(
            selectedFormat = format
        )
    }
    
    /**
     * Update JPG quality
     */
    fun updateJpgQuality(quality: Int) {
        _exportDialogState.value = _exportDialogState.value.copy(
            jpgQuality = quality.coerceIn(1, 100)
        )
    }
    
    /**
     * Clear operation result
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }
    
    /**
     * Get file size estimate for export
     */
    fun getExportSizeEstimate(project: Project, format: ExportFormat): String {
        val bytes = exportManager.estimateFileSize(project, format)
        return formatFileSize(bytes)
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}

/**
 * Result of a file operation
 */
sealed class FileOperationResult {
    data class Success(
        val message: String,
        val file: File? = null
    ) : FileOperationResult()
    
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : FileOperationResult()
}

/**
 * State for export dialog
 */
data class ExportDialogState(
    val isVisible: Boolean = false,
    val projectName: String = "",
    val selectedFormat: ExportFormat = ExportFormat.PNG,
    val jpgQuality: Int = 85
)
