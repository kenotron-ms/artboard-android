package com.artboard.data.storage

import android.util.Log
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

/**
 * Validates .artboard files for integrity and compatibility
 */
class FileValidator {
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    /**
     * Validate an .artboard file
     * Checks:
     * - File exists and is readable
     * - File is a valid ZIP
     * - Contains required entries (manifest.json, project.json)
     * - Version is supported
     * - All referenced layer files exist
     */
    suspend fun validate(file: File): ValidationResult {
        try {
            // Check file exists
            if (!file.exists()) {
                return ValidationResult.Error("File does not exist")
            }
            
            if (!file.canRead()) {
                return ValidationResult.Error("File is not readable")
            }
            
            // Check minimum file size (empty ZIP would be ~22 bytes)
            if (file.length() < 100) {
                return ValidationResult.Error("File is too small to be valid")
            }
            
            // Check it's a ZIP file
            val entries = mutableSetOf<String>()
            var hasManifest = false
            var hasProjectJson = false
            var manifest: Manifest? = null
            var projectData: ProjectData? = null
            
            try {
                ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zip ->
                    var entry = zip.nextEntry
                    
                    while (entry != null) {
                        entries.add(entry.name)
                        
                        when (entry.name) {
                            "manifest.json" -> {
                                hasManifest = true
                                try {
                                    val content = zip.readBytes().decodeToString()
                                    manifest = json.decodeFromString<Manifest>(content)
                                } catch (e: Exception) {
                                    Log.e("FileValidator", "Failed to parse manifest", e)
                                }
                            }
                            "project.json" -> {
                                hasProjectJson = true
                                try {
                                    val content = zip.readBytes().decodeToString()
                                    projectData = json.decodeFromString<ProjectData>(content)
                                } catch (e: Exception) {
                                    Log.e("FileValidator", "Failed to parse project data", e)
                                }
                            }
                        }
                        
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } catch (e: Exception) {
                return ValidationResult.Error("File is not a valid ZIP: ${e.message}")
            }
            
            // Check required entries
            if (!hasManifest) {
                return ValidationResult.Error("Missing manifest.json")
            }
            
            if (!hasProjectJson) {
                return ValidationResult.Error("Missing project.json")
            }
            
            // Check version compatibility
            manifest?.let {
                if (!FileFormatVersion.isSupported(it.version)) {
                    return ValidationResult.Error(
                        "Unsupported version: ${it.version}. " +
                        "Supported versions: ${FileFormatVersion.MIN_SUPPORTED}-${FileFormatVersion.CURRENT}"
                    )
                }
            }
            
            // Check all referenced layer files exist
            projectData?.let { project ->
                val missingLayers = mutableListOf<String>()
                
                for (layer in project.layers) {
                    if (!entries.contains(layer.bitmapPath)) {
                        missingLayers.add(layer.bitmapPath)
                    }
                }
                
                if (missingLayers.isNotEmpty()) {
                    return ValidationResult.Error(
                        "Missing layer files: ${missingLayers.joinToString(", ")}"
                    )
                }
            }
            
            // Check for warnings
            val data = projectData
            if (data != null && data.layers.isEmpty()) {
                return ValidationResult.Warning("Project has no layers")
            }
            
            return ValidationResult.Valid
            
        } catch (e: Exception) {
            Log.e("FileValidator", "Validation error", e)
            return ValidationResult.Error("Validation failed: ${e.message}")
        }
    }
    
    /**
     * Quick check if file is likely a valid .artboard file
     */
    fun quickCheck(file: File): Boolean {
        if (!file.exists() || !file.canRead()) {
            return false
        }
        
        // Check extension
        if (!file.name.endsWith(".artboard", ignoreCase = true)) {
            return false
        }
        
        // Check it's a ZIP by reading first few bytes
        try {
            FileInputStream(file).use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                
                if (read < 4) {
                    return false
                }
                
                // ZIP files start with PK (0x50 0x4B)
                return header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
            }
        } catch (e: Exception) {
            return false
        }
    }
}
