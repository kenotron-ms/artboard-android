package com.artboard.data.storage

import android.graphics.Bitmap
import com.artboard.data.model.BlendMode
import com.artboard.data.model.Layer
import com.artboard.data.model.Project
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tests for ArtboardFileWriter and ArtboardFileReader
 * Tests the complete save/load cycle
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ArtboardFileTest {
    
    private lateinit var writer: ArtboardFileWriter
    private lateinit var reader: ArtboardFileReader
    private lateinit var validator: FileValidator
    private lateinit var tempFile: File
    
    @Before
    fun setup() {
        writer = ArtboardFileWriter()
        reader = ArtboardFileReader()
        validator = FileValidator()
        tempFile = File.createTempFile("test_project", ".artboard")
    }
    
    @After
    fun cleanup() {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
    
    @Test
    fun `write creates valid zip file`() = runTest {
        // Create a simple project
        val project = createTestProject()
        
        // Write to file
        val result = writer.write(project, tempFile)
        
        // Verify success
        assertTrue("Write should succeed", result.isSuccess)
        assertTrue("File should exist", tempFile.exists())
        assertTrue("File should not be empty", tempFile.length() > 0)
        
        // Verify it's a valid ZIP by checking file header
        val header = tempFile.inputStream().use { 
            ByteArray(4).also { it.read() }
        }
        assertEquals("Should have ZIP signature", 0x50, header[0].toInt())
        assertEquals("Should have ZIP signature", 0x4B, header[1].toInt())
    }
    
    @Test
    fun `write and read roundtrip preserves data`() = runTest {
        // Create project with multiple layers and settings
        val original = Project(
            id = "test-project-123",
            name = "Test Project",
            width = 1024,
            height = 768,
            layers = listOf(
                createTestLayer("Background", 0, opacity = 1f, blendMode = BlendMode.NORMAL),
                createTestLayer("Layer 1", 1, opacity = 0.8f, blendMode = BlendMode.MULTIPLY),
                createTestLayer("Layer 2", 2, opacity = 0.5f, blendMode = BlendMode.SCREEN)
            ),
            backgroundColor = android.graphics.Color.WHITE,
            createdAt = 1234567890L,
            modifiedAt = 1234567890L
        )
        
        // Write
        val writeResult = writer.write(original, tempFile)
        assertTrue("Write should succeed", writeResult.isSuccess)
        
        // Read
        val readResult = reader.read(tempFile)
        assertTrue("Read should succeed", readResult.isSuccess)
        
        val loaded = readResult.getOrThrow()
        
        // Verify project metadata preserved
        assertEquals("ID should match", original.id, loaded.id)
        assertEquals("Name should match", original.name, loaded.name)
        assertEquals("Width should match", original.width, loaded.width)
        assertEquals("Height should match", original.height, loaded.height)
        assertEquals("Background color should match", original.backgroundColor, loaded.backgroundColor)
        assertEquals("Layer count should match", original.layers.size, loaded.layers.size)
        
        // Verify layer data preserved
        original.layers.forEachIndexed { index, originalLayer ->
            val loadedLayer = loaded.layers[index]
            
            assertEquals("Layer $index: ID should match", originalLayer.id, loadedLayer.id)
            assertEquals("Layer $index: Name should match", originalLayer.name, loadedLayer.name)
            assertEquals("Layer $index: Opacity should match", originalLayer.opacity, loadedLayer.opacity, 0.01f)
            assertEquals("Layer $index: Blend mode should match", originalLayer.blendMode, loadedLayer.blendMode)
            assertEquals("Layer $index: Visibility should match", originalLayer.isVisible, loadedLayer.isVisible)
            assertEquals("Layer $index: Lock state should match", originalLayer.isLocked, loadedLayer.isLocked)
            
            // Verify bitmap dimensions
            assertEquals("Layer $index: Bitmap width should match", 
                originalLayer.bitmap.width, loadedLayer.bitmap.width)
            assertEquals("Layer $index: Bitmap height should match", 
                originalLayer.bitmap.height, loadedLayer.bitmap.height)
        }
    }
    
    @Test
    fun `read validates file before loading`() = runTest {
        // Create an invalid file (not a ZIP)
        tempFile.writeText("This is not a ZIP file")
        
        // Try to read
        val result = reader.read(tempFile)
        
        // Should fail
        assertTrue("Read should fail for invalid file", result.isFailure)
    }
    
    @Test
    fun `read handles missing file`() = runTest {
        val nonExistentFile = File("/tmp/does_not_exist_12345.artboard")
        
        val result = reader.read(nonExistentFile)
        
        assertTrue("Read should fail for missing file", result.isFailure)
    }
    
    @Test
    fun `validator detects valid file`() = runTest {
        // Create and write a valid project
        val project = createTestProject()
        writer.write(project, tempFile)
        
        // Validate
        val result = validator.validate(tempFile)
        
        assertTrue("Validator should accept valid file", result is ValidationResult.Valid)
    }
    
    @Test
    fun `validator detects invalid file`() = runTest {
        // Write garbage data
        tempFile.writeBytes(ByteArray(100) { 0 })
        
        val result = validator.validate(tempFile)
        
        assertTrue("Validator should reject invalid file", result is ValidationResult.Error)
    }
    
    @Test
    fun `validator detects missing manifest`() = runTest {
        // Create a ZIP without manifest
        java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zip ->
            val entry = java.util.zip.ZipEntry("project.json")
            zip.putNextEntry(entry)
            zip.write("{}".toByteArray())
            zip.closeEntry()
        }
        
        val result = validator.validate(tempFile)
        
        assertTrue("Validator should reject file without manifest", 
            result is ValidationResult.Error)
        assertTrue("Error message should mention manifest",
            (result as ValidationResult.Error).message.contains("manifest", ignoreCase = true))
    }
    
    @Test
    fun `readMetadata returns project info without loading bitmaps`() = runTest {
        // Create and save project
        val project = createTestProject()
        writer.write(project, tempFile)
        
        // Read only metadata
        val result = reader.readMetadata(tempFile)
        
        assertTrue("Metadata read should succeed", result.isSuccess)
        
        val metadata = result.getOrThrow()
        assertEquals("Should have correct project ID", project.id, metadata.id)
        assertEquals("Should have correct project name", project.name, metadata.name)
        assertEquals("Should have correct dimensions", project.width, metadata.width)
        assertEquals("Should have correct layer count", project.layers.size, metadata.layers.size)
    }
    
    @Test
    fun `readThumbnail extracts preview image`() = runTest {
        // Create and save project
        val project = createTestProject()
        writer.write(project, tempFile)
        
        // Read thumbnail
        val result = reader.readThumbnail(tempFile)
        
        assertTrue("Thumbnail read should succeed", result.isSuccess)
        
        val thumbnail = result.getOrThrow()
        assertNotNull("Thumbnail should not be null", thumbnail)
        assertTrue("Thumbnail should have reasonable dimensions", 
            thumbnail.width > 0 && thumbnail.height > 0)
    }
    
    @Test
    fun `large project saves and loads within time limit`() = runTest {
        // Create project with many layers
        val layers = (0 until 10).map { index ->
            createTestLayer("Layer $index", index)
        }
        
        val project = Project(
            id = "large-project",
            name = "Large Project",
            width = 2048,
            height = 2048,
            layers = layers,
            backgroundColor = android.graphics.Color.WHITE
        )
        
        // Save
        val saveStart = System.currentTimeMillis()
        val saveResult = writer.write(project, tempFile)
        val saveDuration = System.currentTimeMillis() - saveStart
        
        assertTrue("Save should succeed", saveResult.isSuccess)
        assertTrue("Save should complete in under 5 seconds", saveDuration < 5000)
        
        // Load
        val loadStart = System.currentTimeMillis()
        val loadResult = reader.read(tempFile)
        val loadDuration = System.currentTimeMillis() - loadStart
        
        assertTrue("Load should succeed", loadResult.isSuccess)
        assertTrue("Load should complete in under 10 seconds", loadDuration < 10000)
        
        val loaded = loadResult.getOrThrow()
        assertEquals("All layers should be loaded", 10, loaded.layers.size)
    }
    
    // Helper functions
    
    private fun createTestProject(): Project {
        return Project(
            id = "test-project",
            name = "Test Project",
            width = 512,
            height = 512,
            layers = listOf(
                createTestLayer("Layer 1", 0)
            ),
            backgroundColor = android.graphics.Color.WHITE
        )
    }
    
    private fun createTestLayer(
        name: String,
        position: Int,
        opacity: Float = 1f,
        blendMode: BlendMode = BlendMode.NORMAL
    ): Layer {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        
        // Draw something on the bitmap so it's not empty
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(
            (position * 30) % 255,
            (position * 60) % 255,
            (position * 90) % 255
        ))
        
        return Layer(
            name = name,
            bitmap = bitmap,
            opacity = opacity,
            blendMode = blendMode,
            position = position,
            isVisible = true,
            isLocked = false
        )
    }
}
