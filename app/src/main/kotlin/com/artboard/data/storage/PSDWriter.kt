package com.artboard.data.storage

import android.graphics.Bitmap
import android.util.Log
import com.artboard.data.model.BlendMode
import com.artboard.data.model.Layer
import com.artboard.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes projects to Photoshop PSD format with layer preservation
 * 
 * Implements a subset of the PSD specification sufficient for layer export:
 * - Header section
 * - Color Mode Data section
 * - Image Resources section
 * - Layer and Mask Information section
 * - Image Data section
 * 
 * Preserves:
 * - Layer names
 * - Layer opacity
 * - Blend modes (mapped to Photoshop equivalents)
 * - Layer order
 * - Layer visibility
 * 
 * Limitations:
 * - Maximum 30,000 x 30,000 pixels (PSD spec limit)
 * - RGB color mode only
 * - 8-bit depth only
 * - No layer effects/styles
 * - No adjustment layers
 */
class PSDWriter {
    
    companion object {
        // PSD File format constants
        private const val PSD_SIGNATURE = "8BPS"
        private const val PSD_VERSION = 1
        private const val COLOR_MODE_RGB = 3
        private const val CHANNELS_RGB = 3
        private const val CHANNELS_RGBA = 4
        private const val BIT_DEPTH = 8
        
        // PSD limits
        private const val MAX_DIMENSION = 30000
    }
    
    /**
     * Write a project to PSD format
     */
    suspend fun write(
        project: Project,
        outputFile: File,
        progressTracker: ProgressTracker? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            progressTracker?.updateProgress(0f, "Preparing PSD export...")
            
            // Validate dimensions
            if (project.width > MAX_DIMENSION || project.height > MAX_DIMENSION) {
                return@withContext Result.failure(
                    IOException("Canvas size exceeds PSD maximum ($MAX_DIMENSION x $MAX_DIMENSION)")
                )
            }
            
            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            
            DataOutputStream(FileOutputStream(outputFile).buffered()).use { out ->
                // Write header
                progressTracker?.updateProgress(0.1f, "Writing PSD header...")
                writeHeader(out, project)
                
                // Write color mode data (empty for RGB)
                progressTracker?.updateProgress(0.15f, "Writing color data...")
                writeColorModeData(out)
                
                // Write image resources (minimal)
                progressTracker?.updateProgress(0.2f, "Writing image resources...")
                writeImageResources(out, project)
                
                // Write layer and mask information
                progressTracker?.updateProgress(0.3f, "Writing layer data...")
                writeLayerAndMaskInfo(out, project, progressTracker)
                
                // Write merged image data
                progressTracker?.updateProgress(0.9f, "Writing composite image...")
                writeMergedImageData(out, project)
                
                progressTracker?.updateProgress(1f, "PSD export complete")
            }
            
            Log.i("PSDWriter", "Successfully wrote PSD: ${outputFile.name}")
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e("PSDWriter", "Failed to write PSD", e)
            Result.failure(e)
        }
    }
    
    /**
     * Write PSD file header
     */
    private fun writeHeader(out: DataOutputStream, project: Project) {
        out.writeBytes(PSD_SIGNATURE)  // Signature
        out.writeShort(PSD_VERSION)     // Version
        out.writeInt(0)                 // Reserved (6 bytes)
        out.writeShort(0)
        out.writeShort(CHANNELS_RGB)    // Number of channels
        out.writeInt(project.height)    // Height
        out.writeInt(project.width)     // Width
        out.writeShort(BIT_DEPTH)       // Bits per channel
        out.writeShort(COLOR_MODE_RGB)  // Color mode (RGB)
    }
    
    /**
     * Write color mode data section (empty for RGB)
     */
    private fun writeColorModeData(out: DataOutputStream) {
        out.writeInt(0) // Length = 0 (no color mode data for RGB)
    }
    
    /**
     * Write image resources section
     */
    private fun writeImageResources(out: DataOutputStream, project: Project) {
        val resourcesData = ByteBuffer.allocate(1024)
        resourcesData.order(ByteOrder.BIG_ENDIAN)
        
        // Resolution info (1005)
        writeImageResource(resourcesData, 0x03ED, createResolutionInfo())
        
        // Write length and data
        out.writeInt(resourcesData.position())
        out.write(resourcesData.array(), 0, resourcesData.position())
    }
    
    /**
     * Create resolution info resource (72 DPI)
     */
    private fun createResolutionInfo(): ByteArray {
        val buffer = ByteBuffer.allocate(16)
        buffer.order(ByteOrder.BIG_ENDIAN)
        
        // Horizontal resolution: 72 DPI (fixed point 16.16)
        buffer.putInt(72 shl 16)
        buffer.putShort(1) // Display unit (pixels per inch)
        
        // Vertical resolution: 72 DPI
        buffer.putInt(72 shl 16)
        buffer.putShort(1) // Display unit (pixels per inch)
        
        return buffer.array()
    }
    
    /**
     * Write an image resource
     */
    private fun writeImageResource(buffer: ByteBuffer, resourceId: Int, data: ByteArray) {
        buffer.put("8BIM".toByteArray()) // Signature
        buffer.putShort(resourceId.toShort()) // Resource ID
        buffer.putShort(0) // Name (empty)
        buffer.putInt(data.size) // Data size
        buffer.put(data)
        
        // Pad to even length
        if (data.size % 2 != 0) {
            buffer.put(0)
        }
    }
    
    /**
     * Write layer and mask information section
     */
    private fun writeLayerAndMaskInfo(
        out: DataOutputStream,
        project: Project,
        progressTracker: ProgressTracker?
    ) {
        // Build layer data in memory first to calculate size
        val layerData = buildLayerData(project, progressTracker)
        
        // Write section length
        out.writeInt(layerData.size)
        
        // Write layer data
        out.write(layerData)
    }
    
    /**
     * Build layer data section
     */
    private fun buildLayerData(project: Project, progressTracker: ProgressTracker?): ByteArray {
        val buffer = ByteBuffer.allocate(1024 * 1024 * 10) // 10MB initial buffer
        buffer.order(ByteOrder.BIG_ENDIAN)
        
        // Layer info section length (placeholder, will update later)
        val lengthPos = buffer.position()
        buffer.putInt(0)
        
        // Layer count (negative means first alpha channel is transparency)
        buffer.putShort((-project.layers.size).toShort())
        
        // Write layer records
        val layerCount = project.layers.size
        project.layers.forEachIndexed { index, layer ->
            val progress = 0.3f + (index.toFloat() / layerCount) * 0.5f
            progressTracker?.updateProgress(progress, "Processing layer ${index + 1}/$layerCount...")
            
            writeLayerRecord(buffer, layer, project)
        }
        
        // Write layer channel image data
        project.layers.forEach { layer ->
            writeLayerChannelData(buffer, layer)
        }
        
        // Update layer info length
        val currentPos = buffer.position()
        val layerInfoLength = currentPos - lengthPos - 4
        buffer.putInt(lengthPos, layerInfoLength)
        
        // Global layer mask info (empty)
        buffer.putInt(0)
        
        return buffer.array().copyOfRange(0, buffer.position())
    }
    
    /**
     * Write a single layer record
     */
    private fun writeLayerRecord(buffer: ByteBuffer, layer: Layer, project: Project) {
        // Layer rectangle
        buffer.putInt(0) // Top
        buffer.putInt(0) // Left
        buffer.putInt(project.height) // Bottom
        buffer.putInt(project.width) // Right
        
        // Number of channels (RGBA = 4)
        buffer.putShort(CHANNELS_RGBA.toShort())
        
        // Channel information
        // Transparency channel (-1)
        buffer.putShort((-1).toShort())
        val channelSize = project.width * project.height
        buffer.putInt(channelSize + 2) // +2 for compression flag
        
        // R, G, B channels
        for (channelId in 0..2) {
            buffer.putShort(channelId.toShort())
            buffer.putInt(channelSize + 2)
        }
        
        // Blend mode signature
        buffer.put("8BIM".toByteArray())
        
        // Blend mode key
        val blendModeKey = mapBlendModeToPS(layer.blendMode)
        buffer.put(blendModeKey.toByteArray())
        
        // Opacity (0-255)
        buffer.put((layer.opacity * 255).toInt().toByte())
        
        // Clipping (0 = base)
        buffer.put(0)
        
        // Flags (bit 0 = transparency protected, bit 1 = visible)
        val flags = if (layer.isVisible) 0 else 2
        buffer.put(flags.toByte())
        
        // Filler
        buffer.put(0)
        
        // Extra data section
        val extraDataStart = buffer.position()
        buffer.putInt(0) // Length placeholder
        
        // Layer mask data (empty)
        buffer.putInt(0)
        
        // Layer blending ranges (empty)
        buffer.putInt(0)
        
        // Layer name (Pascal string)
        val nameBytes = layer.name.toByteArray(Charsets.UTF_8)
        val nameBytesLength = minOf(nameBytes.size, 255)
        buffer.put(nameBytesLength.toByte())
        buffer.put(nameBytes, 0, nameBytesLength)
        
        // Pad to multiple of 4
        val padding = (4 - ((nameBytesLength + 1) % 4)) % 4
        repeat(padding) { buffer.put(0) }
        
        // Update extra data length
        val extraDataEnd = buffer.position()
        buffer.putInt(extraDataStart, extraDataEnd - extraDataStart - 4)
    }
    
    /**
     * Write layer channel data (raw pixel data)
     */
    private fun writeLayerChannelData(buffer: ByteBuffer, layer: Layer) {
        val pixels = IntArray(layer.bitmap.width * layer.bitmap.height)
        layer.bitmap.getPixels(pixels, 0, layer.bitmap.width, 0, 0, layer.bitmap.width, layer.bitmap.height)
        
        // Write each channel separately (A, R, G, B)
        for (channel in -1..2) {
            // Compression flag (0 = raw data)
            buffer.putShort(0)
            
            when (channel) {
                -1 -> { // Alpha channel
                    pixels.forEach { pixel ->
                        buffer.put(((pixel shr 24) and 0xFF).toByte())
                    }
                }
                0 -> { // Red channel
                    pixels.forEach { pixel ->
                        buffer.put(((pixel shr 16) and 0xFF).toByte())
                    }
                }
                1 -> { // Green channel
                    pixels.forEach { pixel ->
                        buffer.put(((pixel shr 8) and 0xFF).toByte())
                    }
                }
                2 -> { // Blue channel
                    pixels.forEach { pixel ->
                        buffer.put((pixel and 0xFF).toByte())
                    }
                }
            }
        }
    }
    
    /**
     * Write merged/composite image data
     */
    private fun writeMergedImageData(out: DataOutputStream, project: Project) {
        // Compression flag (0 = raw)
        out.writeShort(0)
        
        // Composite all visible layers
        val composite = compositeProject(project)
        val pixels = IntArray(composite.width * composite.height)
        composite.getPixels(pixels, 0, composite.width, 0, 0, composite.width, composite.height)
        
        // Write RGB channels
        for (channel in 0..2) {
            pixels.forEach { pixel ->
                val value = when (channel) {
                    0 -> (pixel shr 16) and 0xFF // Red
                    1 -> (pixel shr 8) and 0xFF  // Green
                    else -> pixel and 0xFF        // Blue
                }
                out.writeByte(value)
            }
        }
        
        composite.recycle()
    }
    
    /**
     * Composite all visible layers into a single bitmap
     */
    private fun compositeProject(project: Project): Bitmap {
        val composite = Bitmap.createBitmap(project.width, project.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(composite)
        
        // Draw background
        canvas.drawColor(project.backgroundColor)
        
        // Draw visible layers
        val paint = android.graphics.Paint()
        project.layers.forEach { layer ->
            if (layer.isVisible) {
                paint.alpha = (layer.opacity * 255).toInt()
                canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
            }
        }
        
        return composite
    }
    
    /**
     * Map Artboard blend modes to Photoshop blend mode keys
     */
    private fun mapBlendModeToPS(blendMode: BlendMode): String {
        return when (blendMode) {
            BlendMode.NORMAL -> "norm"
            BlendMode.MULTIPLY -> "mul "
            BlendMode.SCREEN -> "scrn"
            BlendMode.OVERLAY -> "over"
            BlendMode.ADD -> "lite" // Linear Dodge (Add)
            BlendMode.DARKEN -> "dark"
            BlendMode.LIGHTEN -> "lite"
            BlendMode.COLOR_DODGE -> "div "
            BlendMode.COLOR_BURN -> "idiv"
            BlendMode.SOFT_LIGHT -> "sLit"
            BlendMode.HARD_LIGHT -> "hLit"
            BlendMode.DIFFERENCE -> "diff"
            BlendMode.EXCLUSION -> "smud"
            BlendMode.HUE -> "hue "
            BlendMode.SATURATION -> "sat "
            BlendMode.COLOR -> "colr"
            BlendMode.LUMINOSITY -> "lum "
            BlendMode.XOR -> "norm" // XOR not supported in PS, fallback to normal
        }
    }
}
