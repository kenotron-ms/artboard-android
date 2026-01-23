package com.artboard.domain.io

import android.content.Context
import com.artboard.data.model.Project
import com.artboard.data.repository.ProjectRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests for AutoSaveManager
 * Tests auto-save behavior and crash recovery
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AutoSaveManagerTest {
    
    private lateinit var context: Context
    private lateinit var repository: ProjectRepository
    private lateinit var autoSaveManager: AutoSaveManager
    private lateinit var testProject: Project
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        repository = ProjectRepository(context)
        autoSaveManager = AutoSaveManager(
            context = context,
            repository = repository,
            saveInterval = 1000L // 1 second for testing
        )
        testProject = createTestProject()
    }
    
    @After
    fun cleanup() {
        autoSaveManager.stop()
        autoSaveManager.clearAutoSaves(testProject.id)
    }
    
    @Test
    fun `auto-save starts and stops correctly`() = runTest {
        assertFalse("Should not be running initially", autoSaveManager.isRunning())
        
        autoSaveManager.start(testProject, this)
        
        assertTrue("Should be running after start", autoSaveManager.isRunning())
        
        autoSaveManager.stop()
        
        assertFalse("Should not be running after stop", autoSaveManager.isRunning())
    }
    
    @Test
    fun `markDirty triggers auto-save after interval`() = runTest {
        autoSaveManager.start(testProject, this)
        
        // Mark as dirty
        autoSaveManager.markDirty()
        
        // Wait for auto-save interval
        delay(1500) // 1.5 seconds (interval is 1 second)
        
        // Check that auto-save file was created
        val hasAutoSave = autoSaveManager.hasAutoSave(testProject.id)
        assertTrue("Auto-save should have been created", hasAutoSave)
    }
    
    @Test
    fun `auto-save does not save if not dirty`() = runTest {
        autoSaveManager.start(testProject, this)
        
        // Don't mark as dirty
        
        // Wait for auto-save interval
        delay(1500)
        
        // Check that no auto-save file was created
        val hasAutoSave = autoSaveManager.hasAutoSave(testProject.id)
        assertFalse("Auto-save should not be created if not dirty", hasAutoSave)
    }
    
    @Test
    fun `saveNow forces immediate save`() = runTest {
        autoSaveManager.start(testProject, this)
        autoSaveManager.markDirty()
        
        // Save immediately without waiting for interval
        val result = autoSaveManager.saveNow()
        
        assertTrue("Immediate save should succeed", result.isSuccess)
        assertTrue("Auto-save file should exist", autoSaveManager.hasAutoSave(testProject.id))
    }
    
    @Test
    fun `recoverFromCrash returns most recent auto-save`() = runTest {
        // Save project multiple times
        autoSaveManager.start(testProject, this)
        autoSaveManager.markDirty()
        autoSaveManager.saveNow()
        
        delay(100)
        
        autoSaveManager.markDirty()
        autoSaveManager.saveNow()
        
        // Recover
        val recovered = autoSaveManager.recoverFromCrash(testProject.id)
        
        assertNotNull("Should recover project", recovered)
        assertEquals("Should have correct project ID", testProject.id, recovered?.id)
    }
    
    @Test
    fun `recoverFromCrash returns null if no auto-save`() = runTest {
        val recovered = autoSaveManager.recoverFromCrash("non-existent-project")
        
        assertNull("Should return null for non-existent project", recovered)
    }
    
    @Test
    fun `clearAutoSaves removes all auto-save files`() = runTest {
        // Create some auto-saves
        autoSaveManager.start(testProject, this)
        autoSaveManager.markDirty()
        autoSaveManager.saveNow()
        
        assertTrue("Should have auto-save before clearing", 
            autoSaveManager.hasAutoSave(testProject.id))
        
        // Clear
        autoSaveManager.clearAutoSaves(testProject.id)
        
        assertFalse("Should not have auto-save after clearing", 
            autoSaveManager.hasAutoSave(testProject.id))
    }
    
    @Test
    fun `auto-save keeps only recent files`() = runTest {
        autoSaveManager.start(testProject, this)
        
        // Create multiple auto-saves
        for (i in 1..5) {
            autoSaveManager.markDirty()
            autoSaveManager.saveNow()
            delay(100) // Small delay to ensure different timestamps
        }
        
        // AutoSaveManager should keep only the 3 most recent
        val autoSaveDir = context.getExternalFilesDir(null)?.let { 
            java.io.File(it, "autosave")
        }
        
        val autoSaveFiles = autoSaveDir?.listFiles { file ->
            file.name.startsWith(testProject.id) && file.name.contains("autosave")
        }
        
        assertNotNull("Auto-save directory should exist", autoSaveFiles)
        assertTrue("Should keep at most 3 auto-save files", 
            autoSaveFiles!!.size <= 3)
    }
    
    @Test
    fun `getTimeSinceLastSave returns elapsed time`() = runTest {
        autoSaveManager.start(testProject, this)
        autoSaveManager.markDirty()
        autoSaveManager.saveNow()
        
        delay(500)
        
        val elapsed = autoSaveManager.getTimeSinceLastSave()
        
        assertTrue("Elapsed time should be at least 500ms", elapsed >= 500)
        assertTrue("Elapsed time should be less than 1 second", elapsed < 1000)
    }
    
    @Test
    fun `auto-save handles save errors gracefully`() = runTest {
        // Create a repository that fails
        val failingRepository = mock<ProjectRepository>()
        whenever(failingRepository.save(any(), any(), any()))
            .thenReturn(Result.failure(Exception("Simulated failure")))
        
        val manager = AutoSaveManager(context, failingRepository, 1000L)
        
        manager.start(testProject, this)
        manager.markDirty()
        
        // Should not crash
        delay(1500)
        
        assertTrue("Should still be running after error", manager.isRunning())
    }
    
    @Test
    fun `multiple auto-saves don't interfere`() = runTest {
        val project1 = createTestProject("project-1")
        val project2 = createTestProject("project-2")
        
        val manager1 = AutoSaveManager(context, repository, 1000L)
        val manager2 = AutoSaveManager(context, repository, 1000L)
        
        manager1.start(project1, this)
        manager2.start(project2, this)
        
        manager1.markDirty()
        manager2.markDirty()
        
        delay(1500)
        
        assertTrue("Project 1 should have auto-save", manager1.hasAutoSave(project1.id))
        assertTrue("Project 2 should have auto-save", manager2.hasAutoSave(project2.id))
        
        manager1.stop()
        manager2.stop()
    }
    
    // Helper functions
    
    private fun createTestProject(id: String = "test-project"): Project {
        return Project.create(
            name = "Test Project",
            width = 512,
            height = 512
        ).copy(id = id)
    }
}
