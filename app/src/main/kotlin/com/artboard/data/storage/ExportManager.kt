package com.artboard.data.storage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.artboard.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages export operations for various image formats
 * Supports: PNG, JPG, PSD
 */
class ExportManager {
    
    private val psdWriter = PSDWriter()
    
    /**
     * Export project as PNG with all visible layers flattened
     * 
     * @param project The project to export
     * @param outputFile The target PNG file
     * @param flattenLayers If true, composite all visible layers. If false, export only current layer
     * @param progressTracker Optional progress tracker
     * @return Result containing the output file on success
     */
    suspend fun exportPNG(
        project: Project,
        outputFile: File,
        flattenLayers: Boolean = true,
        progressTracker: ProgressTracker? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            progressTracker?.updateProgress(0f, "Preparing PNG export...")
            
            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            
            val bitmap = if (flattenLayers) {
                progressTracker?.updateProgress(0.3f, "Flattening layers...")
                flattenProject(project)
            } else {
                // Export only the top visible layer
                project.layers.lastOrNull { it.isVisible }?.bitmap
                    ?: throw IOException("No visible layers to export")
            }
            
            progressTracker?.updateProgress(0.7f, "Writing PNG file...")
            
            FileOutputStream(outputFile).use { out ->
                val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                if (!success) {
                    throw IOException("Failed to compress PNG")
                }
            }
            
            // Clean up if we created a flattened bitmap
            if (flattenLayers) {
                bitmap.recycle()
            }
            
            progressTracker?.updateProgress(1f, "PNG export complete")
            
            Log.i("ExportManager", "Successfully exported PNG: ${outputFile.name}")
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e("ExportManager", "Failed to export PNG", e)
            Result.failure(e)
        }
    }
    
    /**
     * Export project as JPG with all visible layers flattened
     * 
     * @param project The project to export
     * @param outputFile The target JPG file
     * @param quality JPEG quality (1-100, default 85)
     * @param progressTracker Optional progress tracker
     * @return Result containing the output file on success
     */
    suspend fun exportJPG(
        project: Project,
        outputFile: File,
        quality: Int = 85,
        progressTracker: ProgressTracker? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            progressTracker?.updateProgress(0f, "Preparing JPG export...")
            
            // Validate quality
            val validQuality = quality.coerceIn(1, 100)
            
            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            
            progressTracker?.updateProgress(0.2f, "Flattening layers...")
            
            // JPG doesn't support transparency, so we always flatten
            val flattened = flattenProject(project)
            
            progressTracker?.updateProgress(0.7f, "Compressing JPEG (quality: $validQuality)...")
            
            FileOutputStream(outputFile).use { out ->
                val success = flattened.compress(Bitmap.CompressFormat.JPEG, validQuality, out)
                if (!success) {
                    throw IOException("Failed to compress JPEG")
                }
            }
            
            flattened.recycle()
            
            progressTracker?.updateProgress(1f, "JPG export complete")
            
            Log.i("ExportManager", "Successfully exported JPG: ${outputFile.name}")
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e("ExportManager", "Failed to export JPG", e)
            Result.failure(e)
        }
    }
    
    /**
     * Export project as PSD with layers preserved
     * 
     * @param project The project to export
     * @param outputFile The target PSD file
     * @param progressTracker Optional progress tracker
     * @return Result containing the output file on success
     */
    suspend fun exportPSD(
        project: Project,
        outputFile: File,
        progressTracker: ProgressTracker? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            progressTracker?.updateProgress(0f, "Preparing PSD export...")
            
            // Delegate to PSD writer
            val result = psdWriter.write(project, outputFile, progressTracker)
            
            if (result.isSuccess) {
                Log.i("ExportManager", "Successfully exported PSD: ${outputFile.name}")
            }
            
            result
            
        } catch (e: Exception) {
            Log.e("ExportManager", "Failed to export PSD", e)
            Result.failure(e)
        }
    }
    
    /**
     * Flatten all visible layers into a single bitmap
     * Background color is drawn first, then layers in order
     */
    private fun flattenProject(project: Project): Bitmap {
        val flattened = Bitmap.createBitmap(
            project.width,
            project.height,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(flattened)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        // Draw background color
        canvas.drawColor(project.backgroundColor)
        
        // Draw visible layers in order (bottom to top)
        for (layer in project.layers) {
            if (layer.isVisible) {
                // Set layer opacity
                paint.alpha = (layer.opacity * 255).toInt()
                
                // Draw layer bitmap
                // Note: Blend modes are not applied here for simplicity
                // For full blend mode support, use LayerManager.composite()
                canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
            }
        }
        
        return flattened
    }
    
    /**
     * Get file size estimate for export
     * Useful for showing disk space warnings
     */
    fun estimateFileSize(project: Project, format: ExportFormat): Long {
        val pixelCount = project.width.toLong() * project.height.toLong()
        
        return when (format) {
            ExportFormat.PNG -> {
                // PNG: roughly 4 bytes per pixel (varies with compression)
                pixelCount * 4
            }
            ExportFormat.JPG -> {
                // JPG: roughly 0.5-1 bytes per pixel (depends on quality)
                pixelCount / 2
            }
            ExportFormat.PSD -> {
                // PSD: ~4 bytes per pixel per layer + overhead
                val layerCount = project.layers.size
                pixelCount * 4 * layerCount + 1024 * 100 // +100KB overhead
            }
        }
    }
}

/**
 * Supported export formats
 */
enum class ExportFormat(val extension: String, val displayName: String) {
    PNG("png", "PNG"),
    JPG("jpg", "JPEG"),
    PSD("psd", "Photoshop (PSD)")
}
