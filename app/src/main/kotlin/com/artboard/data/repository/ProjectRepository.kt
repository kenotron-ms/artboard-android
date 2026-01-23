package com.artboard.data.repository

import android.content.Context
import android.util.Log
import com.artboard.data.model.Project
import com.artboard.data.model.ProjectSummary
import com.artboard.data.storage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

/**
 * Repository for project data access
 * Handles loading, saving, and managing projects with file I/O
 */
class ProjectRepository(private val context: Context) {
    
    // Storage directories
    private val projectsDir = File(
        context.getExternalFilesDir(null),
        "projects"
    ).apply { mkdirs() }
    
    private val thumbnailsDir = File(
        context.getExternalFilesDir(null),
        "thumbnails"
    ).apply { mkdirs() }
    
    // File I/O components
    private val fileWriter = ArtboardFileWriter()
    private val fileReader = ArtboardFileReader()
    private val fileValidator = FileValidator()
    
    /**
     * Get all projects as lightweight summaries for gallery display
     */
    suspend fun getAllProjects(): List<ProjectSummary> = withContext(Dispatchers.IO) {
        try {
            projectsDir.listFiles { file ->
                file.extension.equals("artboard", ignoreCase = true)
            }?.mapNotNull { file ->
                // Read only metadata (fast, no bitmap loading)
                readProjectSummary(file)
            }?.sortedByDescending { it.modifiedAt } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all projects", e)
            emptyList()
        }
    }
    
    /**
     * Load a complete project by ID
     */
    suspend fun load(projectId: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val file = File(projectsDir, "$projectId.artboard")
            load(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load project: $projectId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Load a project from a specific file
     */
    suspend fun load(file: File): Result<Project> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(
                    FileNotFoundException("Project file not found: ${file.name}")
                )
            }
            
            // Validate file before loading
            when (val validationResult = fileValidator.validate(file)) {
                is ValidationResult.Error -> {
                    Log.e(TAG, "File validation failed: ${validationResult.message}")
                    return@withContext Result.failure(
                        IOException("Invalid project file: ${validationResult.message}")
                    )
                }
                is ValidationResult.Warning -> {
                    Log.w(TAG, "File validation warning: ${validationResult.message}")
                }
                else -> {
                    // Valid
                }
            }
            
            // Load project
            fileReader.read(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load project from file: ${file.name}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save a project (create or update)
     * 
     * @param project The project to save
     * @param outputFile Optional specific file to save to (for auto-save, etc.)
     * @param progressTracker Optional progress tracker for UI updates
     */
    suspend fun save(
        project: Project,
        outputFile: File? = null,
        progressTracker: ProgressTracker? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = outputFile ?: File(projectsDir, "${project.id}.artboard")
            
            // Update modified timestamp
            val updatedProject = project.copy(modifiedAt = System.currentTimeMillis())
            
            // Write to file
            val result = fileWriter.write(updatedProject, file, progressTracker)
            
            if (result.isSuccess) {
                Log.i(TAG, "Successfully saved project: ${project.name}")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save project: ${project.name}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a project by ID
     */
    suspend fun delete(projectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(projectsDir, "$projectId.artboard")
            
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    return@withContext Result.failure(
                        IOException("Failed to delete project file")
                    )
                }
            }
            
            // Delete thumbnail if exists
            val thumbnailFile = File(thumbnailsDir, "$projectId.jpg")
            if (thumbnailFile.exists()) {
                thumbnailFile.delete()
            }
            
            Log.i(TAG, "Successfully deleted project: $projectId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete project: $projectId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Duplicate a project
     */
    suspend fun duplicate(projectId: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            // Load original
            val loadResult = load(projectId)
            if (loadResult.isFailure) {
                return@withContext Result.failure(
                    loadResult.exceptionOrNull() ?: IOException("Failed to load original project")
                )
            }
            
            val original = loadResult.getOrThrow()
            
            // Create copy with new ID
            val copy = original.copy(
                id = UUID.randomUUID().toString(),
                name = "${original.name} Copy",
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
            
            // Save copy
            val saveResult = save(copy)
            if (saveResult.isFailure) {
                return@withContext Result.failure(
                    saveResult.exceptionOrNull() ?: IOException("Failed to save duplicate")
                )
            }
            
            Log.i(TAG, "Successfully duplicated project: ${original.name}")
            Result.success(copy)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to duplicate project: $projectId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get thumbnail path for a project
     */
    private fun getThumbnailPath(projectId: String): String {
        return File(thumbnailsDir, "$projectId.jpg").absolutePath
    }
    
    /**
     * Read project summary without loading full bitmaps
     */
    private suspend fun readProjectSummary(file: File): ProjectSummary? {
        return try {
            val metadataResult = fileReader.readMetadata(file)
            
            if (metadataResult.isFailure) {
                Log.w(TAG, "Failed to read project summary: ${file.name}")
                return null
            }
            
            val projectData = metadataResult.getOrThrow()
            
            ProjectSummary(
                id = projectData.id,
                name = projectData.name,
                thumbnailPath = file.absolutePath, // Will extract thumbnail on-demand
                width = projectData.width,
                height = projectData.height,
                layerCount = projectData.layers.size,
                strokeCount = projectData.metadata.totalStrokes,
                createdAt = projectData.metadata.createdAt,
                modifiedAt = projectData.metadata.modifiedAt,
                fileSizeMB = file.length() / (1024f * 1024f),
                tags = projectData.metadata.tags
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read project summary: ${file.name}", e)
            null
        }
    }
    
    /**
     * Extract and cache thumbnail for a project
     * Returns cached thumbnail if available, otherwise extracts from .artboard file
     */
    suspend fun getThumbnail(projectId: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val thumbnailFile = File(thumbnailsDir, "$projectId.jpg")
            
            // Return cached thumbnail if exists and is recent
            if (thumbnailFile.exists()) {
                val projectFile = File(projectsDir, "$projectId.artboard")
                if (thumbnailFile.lastModified() >= projectFile.lastModified()) {
                    return@withContext Result.success(thumbnailFile)
                }
            }
            
            // Extract thumbnail from .artboard file
            val projectFile = File(projectsDir, "$projectId.artboard")
            if (!projectFile.exists()) {
                return@withContext Result.failure(FileNotFoundException("Project not found"))
            }
            
            val bitmapResult = fileReader.readThumbnail(projectFile)
            if (bitmapResult.isFailure) {
                return@withContext Result.failure(
                    bitmapResult.exceptionOrNull() ?: IOException("Failed to extract thumbnail")
                )
            }
            
            val bitmap = bitmapResult.getOrThrow()
            
            // Save to cache
            thumbnailFile.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            bitmap.recycle()
            
            Result.success(thumbnailFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get thumbnail: $projectId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if repository has any projects
     */
    suspend fun hasProjects(): Boolean = withContext(Dispatchers.IO) {
        try {
            val files = projectsDir.listFiles { file ->
                file.extension.equals("artboard", ignoreCase = true)
            }
            !files.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for projects", e)
            false
        }
    }
    
    /**
     * Get total storage used by projects in bytes
     */
    suspend fun getStorageUsed(): Long = withContext(Dispatchers.IO) {
        try {
            val files = projectsDir.listFiles { file ->
                file.extension.equals("artboard", ignoreCase = true)
            } ?: return@withContext 0L
            
            files.sumOf { it.length() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate storage", e)
            0L
        }
    }
    
    /**
     * Validate a project file
     */
    suspend fun validateFile(file: File): ValidationResult {
        return fileValidator.validate(file)
    }
    
    companion object {
        private const val TAG = "ProjectRepository"
        
        // Singleton instance
        @Volatile
        private var instance: ProjectRepository? = null
        
        fun getInstance(context: Context): ProjectRepository {
            return instance ?: synchronized(this) {
                instance ?: ProjectRepository(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
        
        // For compatibility with existing code that doesn't pass context
        fun getInstance(): ProjectRepository {
            return instance ?: throw IllegalStateException(
                "ProjectRepository not initialized. Call getInstance(context) first."
            )
        }
    }
}
