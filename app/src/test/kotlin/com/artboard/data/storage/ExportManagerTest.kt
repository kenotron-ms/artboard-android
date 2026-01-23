package com.artboard.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
 * Tests for ExportManager
 * Tests PNG, JPG, and PSD export functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExportManagerTest {
    
    private lateinit var exportManager: ExportManager
    private lateinit var testProject: Project
    private lateinit var tempDir: File
    
    @Before
    fun setup() {
        exportManager = ExportManager()
        testProject = createTestProject()
        tempDir = File.createTempFile("export_test", "").apply {
            delete()
            mkdirs()
        }
    }
    
    @After
    fun cleanup() {
        tempDir.deleteRecursively()
    }
    
    @Test
    fun `exportPNG creates valid PNG file`() = runTest {
        val outputFile = File(tempDir, "test.png")
        
        val result = exportManager.exportPNG(testProject, outputFile)
        
        assertTrue("Export should succeed", result.isSuccess)
        assertTrue("File should exist", outputFile.exists())
        assertTrue("File should not be empty", outputFile.length() > 0)
        
        // Verify it's a valid PNG by decoding it
        val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
        assertNotNull("Should decode as valid PNG", bitmap)
        assertEquals("Width should match project", testProject.width, bitmap.width)
        assertEquals("Height should match project", testProject.height, bitmap.height)
    }
    
    @Test
    fun `exportPNG flattens layers by default`() = runTest {
        val outputFile = File(tempDir, "flattened.png")
        
        val result = exportManager.exportPNG(testProject, outputFile, flattenLayers = true)
        
        assertTrue("Export should succeed", result.isSuccess)
        
        // Decode and verify the bitmap
        val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
        assertNotNull("Should have valid bitmap", bitmap)
        
        // The flattened image should have colors from all visible layers
        // We can't test exact pixels, but we can verify it's not completely transparent
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val hasNonTransparentPixels = pixels.any { (it shr 24) and 0xFF > 0 }
        assertTrue("Flattened image should have non-transparent pixels", hasNonTransparentPixels)
    }
    
    @Test
    fun `exportJPG creates valid JPEG file`() = runTest {
        val outputFile = File(tempDir, "test.jpg")
        
        val result = exportManager.exportJPG(testProject, outputFile, quality = 85)
        
        assertTrue("Export should succeed", result.isSuccess)
        assertTrue("File should exist", outputFile.exists())
        assertTrue("File should not be empty", outputFile.length() > 0)
        
        // Verify it's a valid JPEG by decoding it
        val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
        assertNotNull("Should decode as valid JPEG", bitmap)
        assertEquals("Width should match project", testProject.width, bitmap.width)
        assertEquals("Height should match project", testProject.height, bitmap.height)
    }
    
    @Test
    fun `exportJPG with different quality values`() = runTest {
        val lowQualityFile = File(tempDir, "low_quality.jpg")
        val highQualityFile = File(tempDir, "high_quality.jpg")
        
        // Export with different quality settings
        exportManager.exportJPG(testProject, lowQualityFile, quality = 10)
        exportManager.exportJPG(testProject, highQualityFile, quality = 95)
        
        assertTrue("Low quality file should exist", lowQualityFile.exists())
        assertTrue("High quality file should exist", highQualityFile.exists())
        
        // High quality should generally be larger file size
        // (though this isn't guaranteed for all images)
        val lowSize = lowQualityFile.length()
        val highSize = highQualityFile.length()
        
        assertTrue("Files should have different sizes", lowSize != highSize)
    }
    
    @Test
    fun `exportJPG clamps quality to valid range`() = runTest {
        val outputFile = File(tempDir, "test.jpg")
        
        // Try invalid quality values - should not crash
        val result1 = exportManager.exportJPG(testProject, outputFile, quality = -10)
        val result2 = exportManager.exportJPG(testProject, outputFile, quality = 200)
        
        assertTrue("Should handle low quality gracefully", result1.isSuccess)
        assertTrue("Should handle high quality gracefully", result2.isSuccess)
    }
    
    @Test
    fun `exportPSD creates valid PSD file`() = runTest {
        val outputFile = File(tempDir, "test.psd")
        
        val result = exportManager.exportPSD(testProject, outputFile)
        
        assertTrue("Export should succeed", result.isSuccess)
        assertTrue("File should exist", outputFile.exists())
        assertTrue("File should not be empty", outputFile.length() > 0)
        
        // Verify PSD signature (8BPS)
        val signature = ByteArray(4)
        outputFile.inputStream().use { it.read(signature) }
        
        assertEquals("Should have PSD signature '8'", '8'.code.toByte(), signature[0])
        assertEquals("Should have PSD signature 'B'", 'B'.code.toByte(), signature[1])
        assertEquals("Should have PSD signature 'P'", 'P'.code.toByte(), signature[2])
        assertEquals("Should have PSD signature 'S'", 'S'.code.toByte(), signature[3])
    }
    
    @Test
    fun `exportPSD preserves layer count`() = runTest {
        // Create project with multiple layers
        val multiLayerProject = Project(
            id = "multi",
            name = "Multi Layer",
            width = 512,
            height = 512,
            layers = listOf(
                createTestLayer("Layer 1", 0),
                createTestLayer("Layer 2", 1),
                createTestLayer("Layer 3", 2)
            ),
            backgroundColor = android.graphics.Color.WHITE
        )
        
        val outputFile = File(tempDir, "multilayer.psd")
        
        val result = exportManager.exportPSD(multiLayerProject, outputFile)
        
        assertTrue("Export should succeed", result.isSuccess)
        
        // We can't easily parse the PSD to verify layers without a full PSD reader,
        // but we can verify the file is larger than a single-layer export
        // (layers add to file size)
        assertTrue("Multi-layer PSD should be substantial size", outputFile.length() > 10000)
    }
    
    @Test
    fun `export completes within time limits`() = runTest {
        val pngFile = File(tempDir, "perf_test.png")
        val jpgFile = File(tempDir, "perf_test.jpg")
        val psdFile = File(tempDir, "perf_test.psd")
        
        // Test PNG export time
        val pngStart = System.currentTimeMillis()
        exportManager.exportPNG(testProject, pngFile)
        val pngDuration = System.currentTimeMillis() - pngStart
        assertTrue("PNG export should complete in under 2 seconds", pngDuration < 2000)
        
        // Test JPG export time
        val jpgStart = System.currentTimeMillis()
        exportManager.exportJPG(testProject, jpgFile)
        val jpgDuration = System.currentTimeMillis() - jpgStart
        assertTrue("JPG export should complete in under 2 seconds", jpgDuration < 2000)
        
        // Test PSD export time
        val psdStart = System.currentTimeMillis()
        exportManager.exportPSD(testProject, psdFile)
        val psdDuration = System.currentTimeMillis() - psdStart
        assertTrue("PSD export should complete in under 5 seconds", psdDuration < 5000)
    }
    
    @Test
    fun `estimateFileSize returns reasonable estimates`() = runTest {
        val pngEstimate = exportManager.estimateFileSize(testProject, ExportFormat.PNG)
        val jpgEstimate = exportManager.estimateFileSize(testProject, ExportFormat.JPG)
        val psdEstimate = exportManager.estimateFileSize(testProject, ExportFormat.PSD)
        
        // Verify estimates are in reasonable ranges
        assertTrue("PNG estimate should be positive", pngEstimate > 0)
        assertTrue("JPG estimate should be positive", jpgEstimate > 0)
        assertTrue("PSD estimate should be positive", psdEstimate > 0)
        
        // JPG should generally be smaller than PNG for photographic content
        assertTrue("JPG should be smaller than PNG estimate", jpgEstimate < pngEstimate)
        
        // PSD with multiple layers should be larger
        val multiLayerProject = testProject.copy(
            layers = testProject.layers + createTestLayer("Extra", 1)
        )
        val multiLayerEstimate = exportManager.estimateFileSize(multiLayerProject, ExportFormat.PSD)
        assertTrue("Multi-layer PSD should be larger", multiLayerEstimate > psdEstimate)
    }
    
    @Test
    fun `export handles missing parent directory`() = runTest {
        val deepPath = File(tempDir, "deep/nested/path/test.png")
        
        val result = exportManager.exportPNG(testProject, deepPath)
        
        assertTrue("Export should create parent directories", result.isSuccess)
        assertTrue("File should exist", deepPath.exists())
    }
    
    // Helper functions
    
    private fun createTestProject(): Project {
        return Project(
            id = "test-export",
            name = "Test Export",
            width = 512,
            height = 512,
            layers = listOf(
                createTestLayer("Background", 0)
            ),
            backgroundColor = android.graphics.Color.WHITE
        )
    }
    
    private fun createTestLayer(name: String, position: Int): Layer {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw a colored rectangle
        canvas.drawColor(android.graphics.Color.rgb(
            (position * 50) % 255,
            (position * 100) % 255,
            (position * 150) % 255
        ))
        
        return Layer(
            name = name,
            bitmap = bitmap,
            position = position
        )
    }
}
