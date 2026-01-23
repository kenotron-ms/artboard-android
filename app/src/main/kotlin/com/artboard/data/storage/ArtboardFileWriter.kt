package com.artboard.data.storage

import android.graphics.Bitmap
import android.util.Log
import com.artboard.data.model.Layer
import com.artboard.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes projects to .artboard file format (ZIP-based)
 * 
 * Format structure:
 * - manifest.json (file metadata)
 * - project.json (project data without bitmaps)
 * - layers/layer_0.png (layer bitmaps)
 * - layers/layer_1.png
 * - thumbnails/preview_512.jpg (preview thumbnail)
 */
class ArtboardFileWriter {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
    }
    
    /**
     * Write a project to an .artboard file
     * 
     * @param project The project to save
     * @param outputFile The target .artboard file
     * @param progressTracker Optional progress tracker for UI updates
     * @return Result containing the output file on success, or exception on failure
     */
    suspend fun write(
        project: Project,
        outputFile: File,
        progressTracker: ProgressTracker? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            progressTracker?.updateProgress(0f, "Preparing...")
            
            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            
            FileOutputStream(outputFile).use { fileOut ->
                ZipOutputStream(fileOut).use { zip ->
                    // Write manifest
                    progressTracker?.updateProgress(0.05f, "Writing manifest...")
                    writeManifest(zip, project)
                    
                    // Write project metadata
                    progressTracker?.updateProgress(0.1f, "Writing project data...")
                    writeProjectJson(zip, project)
                    
                    // Write layers (70% of time spent here)
                    val layerCount = project.layers.size
                    project.layers.forEachIndexed { index, layer ->
                        val progress = 0.1f + (index.toFloat() / layerCount) * 0.7f
                        progressTracker?.updateProgress(
                            progress,
                            "Writing layer ${index + 1}/$layerCount..."
                        )
                        writeLayer(zip, layer, index)
                    }
                    
                    // Write thumbnail
                    progressTracker?.updateProgress(0.85f, "Generating thumbnail...")
                    writeThumbnail(zip, project)
                    
                    progressTracker?.updateProgress(1f, "Complete")
                }
            }
            
            Log.i("ArtboardFileWriter", "Successfully wrote project: ${outputFile.name}")
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e("ArtboardFileWriter", "Failed to write project", e)
            Result.failure(e)
        }
    }
    
    /**
     * Write the manifest.json entry
     */
    private fun writeManifest(zip: ZipOutputStream, project: Project) {
        val manifest = Manifest(
            format = "artboard",
            version = FileFormatVersion.CURRENT,
            generator = "Artboard Android",
            generatorVersion = "1.0.0", // TODO: Get from BuildConfig
            created = project.createdAt,
            modified = project.modifiedAt,
            features = listOf("layers", "blend-modes", "opacity")
        )
        
        val entry = ZipEntry("manifest.json")
        zip.putNextEntry(entry)
        zip.write(json.encodeToString(manifest).toByteArray())
        zip.closeEntry()
    }
    
    /**
     * Write the project.json entry (metadata without bitmaps)
     */
    private fun writeProjectJson(zip: ZipOutputStream, project: Project) {
        val projectData = ProjectData(
            id = project.id,
            name = project.name,
            width = project.width,
            height = project.height,
            dpi = 300, // Default DPI
            backgroundColor = project.backgroundColor,
            layers = project.layers.mapIndexed { index, layer ->
                LayerData(
                    id = layer.id,
                    name = layer.name,
                    position = index,
                    opacity = layer.opacity,
                    blendMode = layer.blendMode.name,
                    isVisible = layer.isVisible,
                    isLocked = layer.isLocked,
                    bitmapPath = "layers/layer_$index.png"
                )
            },
            metadata = ProjectMetadata(
                author = "",
                description = "",
                tags = emptyList(),
                totalStrokes = 0, // TODO: Count strokes when stroke system is implemented
                createdAt = project.createdAt,
                modifiedAt = project.modifiedAt,
                dpi = 300
            )
        )
        
        val entry = ZipEntry("project.json")
        zip.putNextEntry(entry)
        zip.write(json.encodeToString(projectData).toByteArray())
        zip.closeEntry()
    }
    
    /**
     * Write a single layer bitmap as PNG
     */
    private fun writeLayer(zip: ZipOutputStream, layer: Layer, index: Int) {
        val entry = ZipEntry("layers/layer_$index.png")
        zip.putNextEntry(entry)
        
        // Compress as PNG (lossless)
        layer.bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100, // Max quality (ignored for PNG but kept for clarity)
            zip
        )
        
        zip.closeEntry()
    }
    
    /**
     * Generate and write thumbnail preview
     */
    private fun writeThumbnail(zip: ZipOutputStream, project: Project) {
        try {
            val thumbnail = generateThumbnail(project, 512)
            
            val entry = ZipEntry("thumbnails/preview_512.jpg")
            zip.putNextEntry(entry)
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, zip)
            zip.closeEntry()
            
            thumbnail.recycle()
        } catch (e: Exception) {
            Log.w("ArtboardFileWriter", "Failed to generate thumbnail", e)
            // Non-critical, continue without thumbnail
        }
    }
    
    /**
     * Generate a thumbnail by compositing visible layers
     */
    private fun generateThumbnail(project: Project, size: Int): Bitmap {
        // Create thumbnail at reduced size
        val scale = size.toFloat() / maxOf(project.width, project.height)
        val thumbWidth = (project.width * scale).toInt()
        val thumbHeight = (project.height * scale).toInt()
        
        val thumbnail = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(thumbnail)
        
        // Draw background
        canvas.drawColor(project.backgroundColor)
        
        // Draw visible layers
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        for (layer in project.layers) {
            if (layer.isVisible) {
                paint.alpha = (layer.opacity * 255).toInt()
                
                // Scale layer bitmap to thumbnail size
                val scaledLayer = Bitmap.createScaledBitmap(
                    layer.bitmap,
                    thumbWidth,
                    thumbHeight,
                    true
                )
                
                canvas.drawBitmap(scaledLayer, 0f, 0f, paint)
                scaledLayer.recycle()
            }
        }
        
        return thumbnail
    }
}
