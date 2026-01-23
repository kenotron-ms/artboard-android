package com.artboard.ui.gallery

import com.artboard.data.model.Project
import com.artboard.data.model.ProjectSummary
import com.artboard.data.model.SortMode
import com.artboard.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GalleryViewModel
 * Tests state management, search, sort, and project operations
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ProjectRepository
    private lateinit var viewModel: GalleryViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = ProjectRepository.getInstance()
        viewModel = GalleryViewModel(repository)
    }
    
    @After
    fun teardown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is loading`() = runTest {
        // ViewModel starts loading projects
        assertTrue(viewModel.isLoading.value)
    }
    
    @Test
    fun `loadProjects populates projects state`() = runTest {
        // Create test projects
        val project1 = Project.create("Project 1", 2048, 2048)
        val project2 = Project.create("Project 2", 4096, 4096)
        
        repository.save(project1)
        repository.save(project2)
        
        // Load projects
        viewModel.loadProjects()
        advanceUntilIdle()
        
        // Verify projects are loaded
        val projects = viewModel.projects.first()
        assertEquals(2, projects.size)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `searchProjects filters by name`() = runTest {
        // Create test projects
        val landscape = Project.create("Landscape", 2048, 2048)
        val portrait = Project.create("Portrait", 1024, 2048)
        val abstractLand = Project.create("Abstract Land", 2048, 2048)
        
        repository.save(landscape)
        repository.save(portrait)
        repository.save(abstractLand)
        
        viewModel.loadProjects()
        advanceUntilIdle()
        
        // Search for "Land"
        viewModel.searchProjects("Land")
        advanceUntilIdle()
        
        // Verify filtered results
        val filtered = viewModel.filteredProjects.first()
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.name.contains("Land", ignoreCase = true) })
    }
    
    @Test
    fun `sortMode NAME_ASC sorts alphabetically`() = runTest {
        // Create projects with different names
        val projectB = Project.create("B Project", 2048, 2048)
        val projectA = Project.create("A Project", 2048, 2048)
        val projectC = Project.create("C Project", 2048, 2048)
        
        repository.save(projectB)
        repository.save(projectA)
        repository.save(projectC)
        
        viewModel.loadProjects()
        advanceUntilIdle()
        
        // Set sort mode to NAME_ASC
        viewModel.setSortMode(SortMode.NAME_ASC)
        advanceUntilIdle()
        
        // Verify alphabetical order
        val sorted = viewModel.filteredProjects.first()
        assertEquals("A Project", sorted[0].name)
        assertEquals("B Project", sorted[1].name)
        assertEquals("C Project", sorted[2].name)
    }
    
    @Test
    fun `sortMode MODIFIED_DESC sorts by most recent first`() = runTest {
        // Create projects with different modified times
        val old = Project.create("Old", 2048, 2048).copy(modifiedAt = 1000L)
        val recent = Project.create("Recent", 2048, 2048).copy(modifiedAt = 3000L)
        val middle = Project.create("Middle", 2048, 2048).copy(modifiedAt = 2000L)
        
        repository.save(old)
        repository.save(recent)
        repository.save(middle)
        
        viewModel.loadProjects()
        advanceUntilIdle()
        
        // Default sort is MODIFIED_DESC
        val sorted = viewModel.filteredProjects.first()
        assertEquals("Recent", sorted[0].name)
        assertEquals("Middle", sorted[1].name)
        assertEquals("Old", sorted[2].name)
    }
    
    @Test
    fun `createNewProject adds project to list`() = runTest {
        // Initial state: no projects
        viewModel.loadProjects()
        advanceUntilIdle()
        
        val initialCount = viewModel.projects.first().size
        
        // Create new project
        viewModel.createNewProject("Test Project", 2048, 2048)
        advanceUntilIdle()
        
        // Verify project was added
        val projects = viewModel.projects.first()
        assertEquals(initialCount + 1, projects.size)
    }
    
    @Test
    fun `deleteProject removes project from list`() = runTest {
        // Create a project
        val project = Project.create("To Delete", 2048, 2048)
        repository.save(project)
        
        viewModel.loadProjects()
        advanceUntilIdle()
        
        val initialCount = viewModel.projects.first().size
        
        // Delete the project
        viewModel.deleteProject(project.id)
        advanceUntilIdle()
        
        // Verify project was removed
        val projects = viewModel.projects.first()
        assertEquals(initialCount - 1, projects.size)
    }
    
    @Test
    fun `duplicateProject creates copy`() = runTest {
        // Create original project
        val original = Project.create("Original", 2048, 2048)
        repository.save(original)
        
        viewModel.loadProjects()
        advanceUntilIdle()
        
        val initialCount = viewModel.projects.first().size
        
        // Duplicate the project
        viewModel.duplicateProject(original.id)
        advanceUntilIdle()
        
        // Verify duplicate was created
        val projects = viewModel.projects.first()
        assertEquals(initialCount + 1, projects.size)
        assertTrue(projects.any { it.name.contains("Copy") })
    }
    
    @Test
    fun `renameProject updates project name`() = runTest {
        // Create project
        val project = Project.create("Old Name", 2048, 2048)
        repository.save(project)
        
        viewModel.loadProjects()
        advanceUntilIdle()
        
        // Rename the project
        viewModel.renameProject(project.id, "New Name")
        advanceUntilIdle()
        
        // Verify name was updated
        val projects = viewModel.projects.first()
        val renamed = projects.find { it.id == project.id }
        assertEquals("New Name", renamed?.name)
    }
    
    @Test
    fun `sortMode SIZE_DESC sorts by canvas size`() = runTest {
        // Create projects with different sizes
        val small = Project.create("Small", 1024, 1024)
        val large = Project.create("Large", 4096, 4096)
        val medium = Project.create("Medium", 2048, 2048)
        
        repository.save(small)
        repository.save(large)
        repository.save(medium)
        
        viewModel.loadProjects()
        advanceUntilIdle()
        
        // Set sort mode to SIZE_DESC
        viewModel.setSortMode(SortMode.SIZE_DESC)
        advanceUntilIdle()
        
        // Verify sorted by size (largest first)
        val sorted = viewModel.filteredProjects.first()
        assertEquals("Large", sorted[0].name)
        assertEquals("Medium", sorted[1].name)
        assertEquals("Small", sorted[2].name)
    }
}
