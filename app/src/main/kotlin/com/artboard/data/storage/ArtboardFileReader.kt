package com.artboard.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.artboard.data.model.BlendMode
import com.artboard.data.model.Layer
import com.artboard.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Reads projects from .artboard file format
 * 
 * Performs validation and reconstructs the full Project object
 * including all layer bitmaps.
 */
class ArtboardFileReader {
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    /**
     * Read a project from an .artboard file
     * 
     * @param inputFile The .artboard file to read
     * @param progressTracker Optional progress tracker for UI updates
     * @return Result containing the loaded Project on success, or exception on failure
     */
    suspend fun read(
        inputFile: File,
        progressTracker: ProgressTracker? = null
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            progressTracker?.updateProgress(0f, "Opening file...")
            
            if (!inputFile.exists()) {
                return@withContext Result.failure(IOException("File does not exist"))
            }
            
            // First pass: Read and validate manifest
            progressTracker?.updateProgress(0.05f, "Validating file...")
            val manifest = readManifest(inputFile)
            
            if (!FileFormatVersion.isSupported(manifest.version)) {
                return@withContext Result.failure(
                    UnsupportedOperationException(
                        "Unsupported file version: ${manifest.version}. " +
                        "Supported: ${FileFormatVersion.MIN_SUPPORTED}-${FileFormatVersion.CURRENT}"
                    )
                )
            }
            
            // Second pass: Read project data and layer bitmaps
            progressTracker?.updateProgress(0.1f, "Reading project data...")
            
            var projectData: ProjectData? = null
            val layerBitmaps = mutableMapOf<Int, Bitmap>()
            var processedLayers = 0
            
            ZipInputStream(BufferedInputStream(FileInputStream(inputFile))).use { zip ->
                var entry = zip.nextEntry
                
                while (entry != null) {
                    when {
                        entry.name == "project.json" -> {
                            val content = zip.readBytes().decodeToString()
                            projectData = json.decodeFromString<ProjectData>(content)
                        }
                        
                        entry.name.startsWith("layers/layer_") && entry.name.endsWith(".png") -> {
                            val position = extractLayerPosition(entry.name)
                            
                            // Update progress based on expected layer count
                            val expectedLayers = projectData?.layers?.size ?: 1
                            val layerProgress = 0.1f + (processedLayers.toFloat() / expectedLayers) * 0.8f
                            progressTracker?.updateProgress(
                                layerProgress,
                                "Loading layer ${position + 1}..."
                            )
                            
                            // Decode bitmap from stream
                            val bitmap = BitmapFactory.decodeStream(zip)
                                ?: throw IOException("Failed to decode layer bitmap: $position")
                            
                            layerBitmaps[position] = bitmap
                            processedLayers++
                        }
                    }
                    
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            
            progressTracker?.updateProgress(0.95f, "Reconstructing project...")
            
            // Reconstruct Project object
            val data = projectData ?: throw IOException("No project data found in file")
            
            // Build layers with bitmaps
            val layers = data.layers.mapIndexed { index, layerData ->
                val bitmap = layerBitmaps[index]
                    ?: throw IOException("Missing bitmap for layer $index")
                
                Layer(
                    id = layerData.id,
                    name = layerData.name,
                    bitmap = bitmap,
                    opacity = layerData.opacity,
                    blendMode = try {
                        BlendMode.valueOf(layerData.blendMode)
                    } catch (e: IllegalArgumentException) {
                        Log.w("ArtboardFileReader", "Unknown blend mode: ${layerData.blendMode}, using NORMAL")
                        BlendMode.NORMAL
                    },
                    isVisible = layerData.isVisible,
                    isLocked = layerData.isLocked,
                    position = index,
                    thumbnail = null // Will be regenerated on demand
                )
            }
            
            val project = Project(
                id = data.id,
                name = data.name,
                width = data.width,
                height = data.height,
                layers = layers,
                backgroundColor = data.backgroundColor,
                createdAt = data.metadata.createdAt,
                modifiedAt = data.metadata.modifiedAt
            )
            
            progressTracker?.updateProgress(1f, "Complete")
            
            Log.i("ArtboardFileReader", "Successfully loaded project: ${project.name}")
            Result.success(project)
            
        } catch (e: Exception) {
            Log.e("ArtboardFileReader", "Failed to read project", e)
            
            // Clean up any bitmaps that were loaded before the error
            // (Garbage collection will handle this, but being explicit is good practice)
            
            Result.failure(e)
        }
    }
    
    /**
     * Extract layer position from filename
     * "layers/layer_0.png" -> 0
     * "layers/layer_12.png" -> 12
     */
    private fun extractLayerPosition(filename: String): Int {
        return try {
            filename
                .substringAfter("layer_")
                .substringBefore(".png")
                .toInt()
        } catch (e: NumberFormatException) {
            throw IOException("Invalid layer filename: $filename", e)
        }
    }
    
    /**
     * Read and parse the manifest from the ZIP file
     */
    private suspend fun readManifest(file: File): Manifest {
        ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zip ->
            var entry = zip.nextEntry
            
            while (entry != null) {
                if (entry.name == "manifest.json") {
                    val content = zip.readBytes().decodeToString()
                    return json.decodeFromString<Manifest>(content)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        
        throw IOException("No manifest.json found in file")
    }
    
    /**
     * Quick read of just project metadata (no bitmaps)
     * Useful for displaying project lists without loading full projects
     */
    suspend fun readMetadata(file: File): Result<ProjectData> = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zip ->
                var entry = zip.nextEntry
                
                while (entry != null) {
                    if (entry.name == "project.json") {
                        val content = zip.readBytes().decodeToString()
                        val projectData = json.decodeFromString<ProjectData>(content)
                        return@withContext Result.success(projectData)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            
            Result.failure(IOException("No project.json found"))
            
        } catch (e: Exception) {
            Log.e("ArtboardFileReader", "Failed to read metadata", e)
            Result.failure(e)
        }
    }
    
    /**
     * Extract thumbnail from .artboard file without loading full project
     */
    suspend fun readThumbnail(file: File): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zip ->
                var entry = zip.nextEntry
                
                while (entry != null) {
                    if (entry.name == "thumbnails/preview_512.jpg") {
                        val bitmap = BitmapFactory.decodeStream(zip)
                            ?: throw IOException("Failed to decode thumbnail")
                        return@withContext Result.success(bitmap)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            
            Result.failure(IOException("No thumbnail found"))
            
        } catch (e: Exception) {
            Log.e("ArtboardFileReader", "Failed to read thumbnail", e)
            Result.failure(e)
        }
    }
}
