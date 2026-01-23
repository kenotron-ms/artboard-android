package com.artboard.domain.io

import android.content.Context
import android.util.Log
import com.artboard.data.model.Project
import com.artboard.data.repository.ProjectRepository
import com.artboard.data.storage.NoOpProgressTracker
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages automatic background saving of projects
 * 
 * Features:
 * - Auto-saves every 2 minutes by default
 * - Only saves if project has been modified (dirty flag)
 * - Saves to a temporary auto-save directory
 * - Provides crash recovery from auto-save files
 * - Non-blocking background operation
 * 
 * Usage:
 * ```
 * val autoSaveManager = AutoSaveManager(context, repository)
 * autoSaveManager.start(project, viewModelScope)
 * 
 * // Mark project as modified when user makes changes
 * autoSaveManager.markDirty()
 * 
 * // Stop auto-save when leaving screen
 * autoSaveManager.stop()
 * ```
 */
class AutoSaveManager(
    private val context: Context,
    private val repository: ProjectRepository,
    private val saveInterval: Long = 120_000L // 2 minutes
) {
    private var autoSaveJob: Job? = null
    private val isDirty = AtomicBoolean(false)
    private var lastSaveTime = 0L
    private var currentProject: Project? = null
    
    private val autoSaveDir = File(
        context.getExternalFilesDir(null),
        "autosave"
    ).apply { mkdirs() }
    
    /**
     * Start auto-save for a project
     * 
     * @param project The project to auto-save
     * @param scope CoroutineScope to run auto-save in (typically viewModelScope)
     */
    fun start(project: Project, scope: CoroutineScope) {
        stop() // Stop any existing auto-save
        
        currentProject = project
        isDirty.set(false)
        lastSaveTime = System.currentTimeMillis()
        
        autoSaveJob = scope.launch {
            Log.i(TAG, "Auto-save started for project: ${project.name}")
            
            while (isActive) {
                delay(saveInterval)
                
                if (isDirty.get()) {
                    performAutoSave()
                }
            }
        }
    }
    
    /**
     * Stop auto-save
     */
    fun stop() {
        autoSaveJob?.cancel()
        autoSaveJob = null
        currentProject = null
        isDirty.set(false)
        
        Log.i(TAG, "Auto-save stopped")
    }
    
    /**
     * Mark project as modified (dirty)
     * This will trigger an auto-save on the next interval
     */
    fun markDirty() {
        isDirty.set(true)
    }
    
    /**
     * Force an immediate auto-save (bypasses interval)
     * Useful for manual "Save" actions
     */
    suspend fun saveNow(): Result<File> {
        return performAutoSave()
    }
    
    /**
     * Perform the auto-save operation
     */
    private suspend fun performAutoSave(): Result<File> = withContext(Dispatchers.IO) {
        val project = currentProject
        
        if (project == null) {
            Log.w(TAG, "No project to auto-save")
            return@withContext Result.failure(IllegalStateException("No project loaded"))
        }
        
        try {
            val autoSaveFile = getAutoSaveFile(project.id)
            
            Log.d(TAG, "Auto-saving project: ${project.name} to ${autoSaveFile.name}")
            
            // Use NoOpProgressTracker since this is background operation
            val result = repository.save(
                project = project,
                outputFile = autoSaveFile,
                progressTracker = NoOpProgressTracker
            )
            
            if (result.isSuccess) {
                lastSaveTime = System.currentTimeMillis()
                isDirty.set(false)
                
                Log.i(TAG, "Auto-save successful: ${project.name}")
                
                // Clean up old auto-save files (keep last 3)
                cleanupOldAutoSaves(project.id)
            } else {
                Log.e(TAG, "Auto-save failed", result.exceptionOrNull())
            }
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Auto-save error", e)
            // Don't crash - just log and continue
            Result.failure(e)
        }
    }
    
    /**
     * Get the auto-save file path for a project
     * Format: {projectId}_autosave_{timestamp}.artboard
     */
    private fun getAutoSaveFile(projectId: String): File {
        val timestamp = System.currentTimeMillis()
        return File(autoSaveDir, "${projectId}_autosave_$timestamp.artboard")
    }
    
    /**
     * Clean up old auto-save files, keeping only the most recent N
     */
    private fun cleanupOldAutoSaves(projectId: String, keepCount: Int = 3) {
        try {
            val autoSaveFiles = autoSaveDir.listFiles { file ->
                file.name.startsWith(projectId) && file.name.contains("autosave")
            }?.sortedByDescending { it.lastModified() } ?: return
            
            // Delete all but the most recent N files
            autoSaveFiles.drop(keepCount).forEach { file ->
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "Deleted old auto-save: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup old auto-saves", e)
        }
    }
    
    /**
     * Recover a project from auto-save after a crash
     * Returns the most recent auto-save file for the given project ID
     * 
     * @param projectId The ID of the project to recover
     * @return The recovered project, or null if no auto-save found
     */
    suspend fun recoverFromCrash(projectId: String): Project? = withContext(Dispatchers.IO) {
        try {
            val autoSaveFiles = autoSaveDir.listFiles { file ->
                file.name.startsWith(projectId) && file.name.contains("autosave")
            }?.sortedByDescending { it.lastModified() }
            
            if (autoSaveFiles.isNullOrEmpty()) {
                Log.i(TAG, "No auto-save files found for project: $projectId")
                return@withContext null
            }
            
            val mostRecent = autoSaveFiles.first()
            Log.i(TAG, "Found auto-save file: ${mostRecent.name}")
            
            // Try to load the auto-save
            val result = repository.load(mostRecent)
            
            if (result.isSuccess) {
                Log.i(TAG, "Successfully recovered project from auto-save")
                result.getOrNull()
            } else {
                Log.e(TAG, "Failed to load auto-save file", result.exceptionOrNull())
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recovering from auto-save", e)
            null
        }
    }
    
    /**
     * Check if there are any auto-save files for a project
     * Useful for showing "Recover unsaved work?" dialog
     */
    fun hasAutoSave(projectId: String): Boolean {
        val autoSaveFiles = autoSaveDir.listFiles { file ->
            file.name.startsWith(projectId) && file.name.contains("autosave")
        }
        return !autoSaveFiles.isNullOrEmpty()
    }
    
    /**
     * Delete all auto-save files for a project
     * Should be called after successful manual save
     */
    fun clearAutoSaves(projectId: String) {
        try {
            val autoSaveFiles = autoSaveDir.listFiles { file ->
                file.name.startsWith(projectId) && file.name.contains("autosave")
            }
            
            autoSaveFiles?.forEach { file ->
                file.delete()
                Log.d(TAG, "Deleted auto-save: ${file.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear auto-saves", e)
        }
    }
    
    /**
     * Get time since last auto-save in milliseconds
     */
    fun getTimeSinceLastSave(): Long {
        return System.currentTimeMillis() - lastSaveTime
    }
    
    /**
     * Check if auto-save is currently running
     */
    fun isRunning(): Boolean {
        return autoSaveJob?.isActive == true
    }
    
    companion object {
        private const val TAG = "AutoSaveManager"
    }
}
