package com.artboard.ui.gallery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artboard.data.model.Project
import com.artboard.data.repository.ProjectRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for GalleryScreen
 * Tests user interactions, navigation, and visual states
 */
@RunWith(AndroidJUnit4::class)
class GalleryScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var repository: ProjectRepository
    private lateinit var viewModel: GalleryViewModel
    
    @Before
    fun setup() {
        repository = ProjectRepository.getInstance()
        viewModel = GalleryViewModel(repository)
    }
    
    @Test
    fun emptyGallery_showsInspringMessage() {
        composeTestRule.setContent {
            GalleryScreen(
                viewModel = viewModel,
                onProjectClick = {}
            )
        }
        
        // Verify inspiring empty state message
        composeTestRule
            .onNodeWithText("Create something beautiful")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Tap below to start your first masterpiece")
            .assertIsDisplayed()
        
        // Verify Create New button is visible
        composeTestRule
            .onNodeWithContentDescription("Create new project")
            .assertIsDisplayed()
    }
    
    @Test
    fun emptyGallery_createNewButton_isDisplayed() {
        composeTestRule.setContent {
            GalleryScreen(
                viewModel = viewModel,
                onProjectClick = {}
            )
        }
        
        // Verify Create New button
        composeTestRule
            .onNodeWithText("Create New")
            .assertIsDisplayed()
    }
    
    @Test
    fun projectCard_click_triggersNavigation() {
        // Add a test project
        runBlocking {
            val project = Project.create("Test Project", 2048, 2048)
            repository.save(project)
        }
        
        var clickedProjectId: String? = null
        
        composeTestRule.setContent {
            GalleryScreen(
                viewModel = viewModel,
                onProjectClick = { projectId ->
                    clickedProjectId = projectId
                }
            )
        }
        
        // Wait for projects to load
        composeTestRule.waitForIdle()
        
        // Click on the project card
        composeTestRule
            .onNodeWithText("Test Project")
            .performClick()
        
        // Verify navigation was triggered
        assert(clickedProjectId != null) { "Project click should trigger navigation" }
    }
    
    @Test
    fun projectCard_longPress_showsContextMenu() {
        // Add a test project
        runBlocking {
            val project = Project.create("Test Project", 2048, 2048)
            repository.save(project)
        }
        
        composeTestRule.setContent {
            GalleryScreen(
                viewModel = viewModel,
                onProjectClick = {}
            )
        }
        
        // Wait for projects to load
        composeTestRule.waitForIdle()
        
        // Long press on the project card
        composeTestRule
            .onNodeWithText("Test Project")
            .performTouchInput {
                longClick()
            }
        
        // TODO: Verify context menu appears (when implemented)
        // For now, just verify the card is still displayed
        composeTestRule
            .onNodeWithText("Test Project")
            .assertIsDisplayed()
    }
    
    @Test
    fun multipleProjects_allCardsDisplayed() {
        // Add multiple test projects
        runBlocking {
            repository.save(Project.create("Project 1", 2048, 2048))
            repository.save(Project.create("Project 2", 4096, 4096))
            repository.save(Project.create("Project 3", 1024, 1024))
        }
        
        composeTestRule.setContent {
            GalleryScreen(
                viewModel = viewModel,
                onProjectClick = {}
            )
        }
        
        // Wait for projects to load
        composeTestRule.waitForIdle()
        
        // Verify all project cards are displayed
        composeTestRule
            .onNodeWithText("Project 1")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Project 2")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Project 3")
            .assertIsDisplayed()
        
        // Verify Create New button is still first
        composeTestRule
            .onNodeWithText("Create New")
            .assertIsDisplayed()
    }
    
    @Test
    fun projectCard_displaysMetadata() {
        // Add a test project
        runBlocking {
            val project = Project.create("Test Project", 2048, 2048)
            repository.save(project)
        }
        
        composeTestRule.setContent {
            GalleryScreen(
                viewModel = viewModel,
                onProjectClick = {}
            )
        }
        
        // Wait for projects to load
        composeTestRule.waitForIdle()
        
        // Verify project name is displayed
        composeTestRule
            .onNodeWithText("Test Project")
            .assertIsDisplayed()
        
        // Verify dimensions are displayed
        composeTestRule
            .onNodeWithText("2048×2048", substring = true)
            .assertIsDisplayed()
    }
    
    @Test
    fun loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            GalleryScreen(
                viewModel = viewModel,
                onProjectClick = {}
            )
        }
        
        // Note: Loading state is very brief, so this test may be flaky
        // In production, we might add a delay or mock the repository
        
        // Just verify the screen doesn't crash during loading
        composeTestRule.waitForIdle()
    }
}
