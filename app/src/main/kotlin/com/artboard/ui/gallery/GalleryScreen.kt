package com.artboard.ui.gallery

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artboard.data.model.ProjectSummary
import com.artboard.ui.gallery.components.CreateNewButton
import com.artboard.ui.gallery.components.EmptyGalleryState
import com.artboard.ui.gallery.components.ProjectCard

/**
 * Gallery Screen - Main entry point for project browsing
 * 
 * Design Requirements:
 * - Background: #1A1A1A (deep charcoal)
 * - Grid: Adaptive 256dp cards with 16dp gaps
 * - Create New always first (prominent)
 * - 60 FPS scrolling (LazyGrid optimization)
 * - Custom components throughout (NO Material Design defaults)
 */
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = viewModel(),
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect states from ViewModel
    val projects by viewModel.filteredProjects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // Deep charcoal background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header would go here (search, sort, menu)
            // TODO: Add GalleryHeader in future iteration
            
            // Content based on state
            when {
                isLoading -> LoadingState()
                
                error != null -> ErrorState(
                    error = error!!,
                    onRetry = viewModel::loadProjects
                )
                
                projects.isEmpty() && searchQuery.isEmpty() -> {
                    // True empty state - no projects at all
                    EmptyGalleryState(
                        onCreateNew = { viewModel.createNewProject() }
                    )
                }
                
                projects.isEmpty() && searchQuery.isNotEmpty() -> {
                    // Empty search results
                    EmptySearchState(query = searchQuery)
                }
                
                else -> {
                    // Show project grid
                    ProjectGrid(
                        projects = projects,
                        onProjectClick = onProjectClick,
                        onProjectLongPress = { project ->
                            // TODO: Show context menu
                        },
                        onCreateNew = { viewModel.createNewProject() }
                    )
                }
            }
        }
    }
}

/**
 * Project grid with Create New button as first item
 * Uses LazyVerticalGrid for 60 FPS performance
 */
@Composable
private fun ProjectGrid(
    projects: List<ProjectSummary>,
    onProjectClick: (String) -> Unit,
    onProjectLongPress: (ProjectSummary) -> Unit,
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(256.dp), // Adaptive grid for responsive design
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Create New button always first (position 1)
        item(key = "create_new") {
            CreateNewButton(
                onClick = onCreateNew,
                modifier = Modifier.animateItemPlacement(
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessLow
                    )
                )
            )
        }
        
        // Project cards (position 2+)
        items(
            items = projects,
            key = { it.id }
        ) { project ->
            ProjectCard(
                project = project,
                onClick = { onProjectClick(project.id) },
                onLongPress = { onProjectLongPress(project) },
                modifier = Modifier.animateItemPlacement(
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessLow
                    )
                )
            )
        }
    }
}

/**
 * Loading state - shown while projects are loading
 */
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        CircularProgressIndicator(
            color = Color(0xFF4A90E2) // Vibrant blue accent
        )
    }
}

/**
 * Error state - shown when loading fails
 */
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Oops!",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Text(
                text = error,
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color(0xFFAAAAAA)
                ),
                modifier = Modifier.padding(top = 12.dp)
            )
            
            // Simple retry button (would be styled in production)
            Text(
                text = "Tap to Retry",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4A90E2)
                ),
                modifier = Modifier
                    .padding(top = 24.dp)
                    // .clickable { onRetry() } // TODO: Add clickable
            )
        }
    }
}

/**
 * Empty search state - shown when search has no results
 */
@Composable
private fun EmptySearchState(
    query: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No projects found",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Text(
                text = "Try a different search term",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color(0xFFAAAAAA)
                ),
                modifier = Modifier.padding(top = 12.dp)
            )
            
            Text(
                text = "Searched for: \"$query\"",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF888888)
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
