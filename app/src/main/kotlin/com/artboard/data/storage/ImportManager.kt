package com.artboard.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.artboard.data.model.Layer
import com.artboard.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Manages importing images as new layers
 * Supports: JPG, PNG, BMP, WebP
 */
class ImportManager(private val context: Context) {
    
    /**
     * Import an image from a URI as a new layer
     * 
     * @param project The target project
     * @param imageUri The URI of the image to import
     * @param layerName Name for the new layer
     * @param fitToCanvas If true, resize image to fit canvas while maintaining aspect ratio
     * @param progressTracker Optional progress tracker
     * @return Result containing updated Project with new layer
     */
    suspend fun importAsLayer(
        project: Project,
        imageUri: Uri,
        layerName: String = "Imported Layer",
        fitToCanvas: Boolean = true,
        progressTracker: ProgressTracker? = null
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            progressTracker?.updateProgress(0f, "Opening image...")
            
            // Open input stream from URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(IOException("Failed to open image"))
            
            progressTracker?.updateProgress(0.2f, "Decoding image...")
            
            // Decode bitmap
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                return@withContext Result.failure(IOException("Failed to decode image"))
            }
            
            // Fix orientation based on EXIF data
            progressTracker?.updateProgress(0.4f, "Correcting orientation...")
            bitmap = correctOrientation(context, imageUri, bitmap)
            
            // Resize to fit canvas if requested
            if (fitToCanvas) {
                progressTracker?.updateProgress(0.6f, "Resizing to fit canvas...")
                bitmap = resizeToFitCanvas(bitmap, project.width, project.height)
            } else {
                // Center crop or pad to canvas size
                progressTracker?.updateProgress(0.6f, "Fitting to canvas...")
                bitmap = fitToCanvas(bitmap, project.width, project.height)
            }
            
            progressTracker?.updateProgress(0.8f, "Creating layer...")
            
            // Create new layer
            val newLayer = Layer(
                name = layerName,
                bitmap = bitmap,
                position = project.layers.size,
                opacity = 1f,
                isVisible = true,
                isLocked = false
            )
            
            // Add layer to project
            val updatedProject = project.addLayer(newLayer)
            
            progressTracker?.updateProgress(1f, "Import complete")
            
            Log.i("ImportManager", "Successfully imported layer: $layerName")
            Result.success(updatedProject)
            
        } catch (e: Exception) {
            Log.e("ImportManager", "Failed to import image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Import an image from a file path
     */
    suspend fun importAsLayer(
        project: Project,
        imageFile: File,
        layerName: String = imageFile.nameWithoutExtension,
        fitToCanvas: Boolean = true,
        progressTracker: ProgressTracker? = null
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            if (!imageFile.exists()) {
                return@withContext Result.failure(IOException("File does not exist"))
            }
            
            val uri = Uri.fromFile(imageFile)
            importAsLayer(project, uri, layerName, fitToCanvas, progressTracker)
            
        } catch (e: Exception) {
            Log.e("ImportManager", "Failed to import file", e)
            Result.failure(e)
        }
    }
    
    /**
     * Correct image orientation based on EXIF data
     * Some cameras/phones rotate images but store the rotation in EXIF rather than pixels
     */
    private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return bitmap
            
            val exif = ExifInterface(inputStream)
            inputStream.close()
            
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap // No rotation needed
            }
            
            val rotated = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            
            return rotated
            
        } catch (e: Exception) {
            Log.w("ImportManager", "Failed to read EXIF orientation", e)
            return bitmap
        }
    }
    
    /**
     * Resize image to fit within canvas bounds while maintaining aspect ratio
     * Image will be centered on canvas with transparent padding if needed
     */
    private fun resizeToFitCanvas(bitmap: Bitmap, canvasWidth: Int, canvasHeight: Int): Bitmap {
        val imageWidth = bitmap.width
        val imageHeight = bitmap.height
        
        // Calculate scale to fit within canvas
        val scaleWidth = canvasWidth.toFloat() / imageWidth
        val scaleHeight = canvasHeight.toFloat() / imageHeight
        val scale = minOf(scaleWidth, scaleHeight)
        
        val scaledWidth = (imageWidth * scale).toInt()
        val scaledHeight = (imageHeight * scale).toInt()
        
        // Scale the bitmap
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        
        // If exact fit, return scaled bitmap
        if (scaledWidth == canvasWidth && scaledHeight == canvasHeight) {
            return scaled
        }
        
        // Otherwise, center on transparent canvas
        val result = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        
        val left = (canvasWidth - scaledWidth) / 2f
        val top = (canvasHeight - scaledHeight) / 2f
        
        canvas.drawBitmap(scaled, left, top, null)
        scaled.recycle()
        
        return result
    }
    
    /**
     * Fit image to exact canvas size
     * If image is smaller, center it with transparent padding
     * If image is larger, center crop it
     */
    private fun fitToCanvas(bitmap: Bitmap, canvasWidth: Int, canvasHeight: Int): Bitmap {
        val imageWidth = bitmap.width
        val imageHeight = bitmap.height
        
        // If exact match, return as-is
        if (imageWidth == canvasWidth && imageHeight == canvasHeight) {
            return bitmap
        }
        
        val result = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        
        // Calculate centering
        val left = (canvasWidth - imageWidth) / 2f
        val top = (canvasHeight - imageHeight) / 2f
        
        canvas.drawBitmap(bitmap, left, top, null)
        bitmap.recycle()
        
        return result
    }
    
    /**
     * Check if a file is a supported image format
     */
    fun isSupportedFormat(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("jpg", "jpeg", "png", "bmp", "webp")
    }
    
    /**
     * Check if a URI points to a supported image format
     */
    fun isSupportedFormat(uri: Uri): Boolean {
        val path = uri.path ?: return false
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in listOf("jpg", "jpeg", "png", "bmp", "webp")
    }
    
    /**
     * Get image dimensions without loading full bitmap
     * Useful for showing warnings before import
     */
    fun getImageDimensions(uri: Uri): Pair<Int, Int>? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            return Pair(options.outWidth, options.outHeight)
            
        } catch (e: Exception) {
            Log.e("ImportManager", "Failed to get image dimensions", e)
            return null
        }
    }
}
