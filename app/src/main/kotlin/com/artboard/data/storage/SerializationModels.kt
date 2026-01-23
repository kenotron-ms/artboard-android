package com.artboard.data.storage

import kotlinx.serialization.Serializable

/**
 * Manifest stored in .artboard files
 * Contains file format metadata and version information
 */
@Serializable
data class Manifest(
    val format: String = "artboard",
    val version: String = "1.0",
    val generator: String = "Artboard Android",
    val generatorVersion: String = "1.0.0",
    val created: Long = System.currentTimeMillis(),
    val modified: Long = System.currentTimeMillis(),
    val features: List<String> = listOf("layers", "blend-modes", "strokes")
)

/**
 * Project metadata for serialization
 */
@Serializable
data class ProjectMetadata(
    val author: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val totalStrokes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val dpi: Int = 300
)

/**
 * Serializable project data (excludes bitmaps)
 */
@Serializable
data class ProjectData(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val dpi: Int = 300,
    val backgroundColor: Int,
    val layers: List<LayerData>,
    val metadata: ProjectMetadata
)

/**
 * Serializable layer data (references bitmap by path)
 */
@Serializable
data class LayerData(
    val id: String,
    val name: String,
    val position: Int,
    val opacity: Float,
    val blendMode: String,
    val isVisible: Boolean,
    val isLocked: Boolean,
    val bitmapPath: String
)

/**
 * File format version for compatibility checks
 */
object FileFormatVersion {
    const val CURRENT = "1.0"
    const val MIN_SUPPORTED = "1.0"
    
    fun isSupported(version: String): Boolean {
        return version >= MIN_SUPPORTED && version <= CURRENT
    }
}

/**
 * Validation result for file integrity checks
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Warning(val message: String) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
