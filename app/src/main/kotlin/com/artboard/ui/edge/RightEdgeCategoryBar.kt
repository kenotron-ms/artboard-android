package com.artboard.ui.edge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Right edge category bar for the Artboard canvas screen.
 * 
 * A vertically-centered bar positioned at the right edge of the screen
 * containing 5 category buttons (Brush, Color, Layers, Transform, Settings)
 * plus undo/redo buttons at the bottom.
 * 
 * Layout Structure:
 * ```
 *       56dp
 *     ┌────┐
 *     │ 🖌 │ ← Brush (paintbrush)
 *     ├────┤
 *     │ ◐  │ ← Color (half circle)
 *     ├────┤
 *     │ ☷  │ ← Layers (stacked)
 *     ├────┤
 *     │ ⬚  │ ← Transform (box)
 *     ├────┤
 *     │ ⚙  │ ← Settings (gear)
 *     ├────┤
 *     │────│ ← Separator line
 *     ├────┤
 *     │ ↶  │ ← Undo
 *     ├────┤
 *     │ ↷  │ ← Redo
 *     └────┘
 * ```
 * 
 * Design Principles (from EdgeControlBar.md):
 * - Canvas visibility: 96%+ of screen dedicated to artwork
 * - Zero cognitive overhead for tool access
 * - Touch-first, precision-second
 * - Progressive disclosure: reveal complexity only when needed
 * 
 * Screen Space Budget:
 * - Bar only: 56dp width (5.2% of 1080p width)
 * - Bar + open panel: 280dp (25.9%, temporary)
 * 
 * @param state The state holder for the edge bar
 * @param onCategorySelected Callback when a category button is tapped
 * @param onUndo Callback when undo button is tapped
 * @param onRedo Callback when redo button is tapped
 * @param modifier Modifier for the bar container
 */
@Composable
fun RightEdgeCategoryBar(
    state: RightEdgeCategoryBarState,
    onCategorySelected: (ToolCategory) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use AnimatedVisibility for auto-hide support
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300)),
        modifier = modifier
    ) {
        RightEdgeCategoryBarContent(
            activeCategory = state.activeCategory,
            selectedCategory = state.selectedCategory,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            onCategorySelected = onCategorySelected,
            onUndo = onUndo,
            onRedo = onRedo
        )
    }
}

/**
 * Internal content composable for the edge bar.
 * Separated from visibility animation for cleaner composition.
 */
@Composable
private fun RightEdgeCategoryBarContent(
    activeCategory: ToolCategory?,
    selectedCategory: ToolCategory?,
    canUndo: Boolean,
    canRedo: Boolean,
    onCategorySelected: (ToolCategory) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(EdgeBarDimensions.BAR_WIDTH.dp)
            .fillMaxHeight()
            .padding(end = EdgeBarDimensions.EDGE_INSET.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Primary category buttons (above separator)
            CategoryButtonGroup(
                categories = ToolCategory.primary,
                activeCategory = activeCategory,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected
            )

            // Settings button (below primary, above separator)
            Spacer(modifier = Modifier.height(EdgeBarDimensions.BUTTON_GAP.dp))
            
            CategoryButton(
                category = ToolCategory.SETTINGS,
                isActive = activeCategory == ToolCategory.SETTINGS,
                hasSelectedTool = selectedCategory == ToolCategory.SETTINGS,
                onClick = { onCategorySelected(ToolCategory.SETTINGS) }
            )

            // Undo/Redo section with separator
            Spacer(modifier = Modifier.height(EdgeBarDimensions.SEPARATOR_MARGIN.dp))
            
            UndoRedoButtons(
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo
            )
        }
    }
}

/**
 * Group of category buttons with consistent spacing.
 */
@Composable
private fun CategoryButtonGroup(
    categories: List<ToolCategory>,
    activeCategory: ToolCategory?,
    selectedCategory: ToolCategory?,
    onCategorySelected: (ToolCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EdgeBarDimensions.BUTTON_GAP.dp)
    ) {
        categories.forEach { category ->
            CategoryButton(
                category = category,
                isActive = activeCategory == category,
                hasSelectedTool = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

/**
 * State holder for the RightEdgeCategoryBar.
 * 
 * Manages the current active category (panel open), selected category
 * (tool from that category is selected), undo/redo availability,
 * and visibility state for auto-hide behavior.
 * 
 * Usage:
 * ```kotlin
 * val edgeBarState = rememberRightEdgeCategoryBarState()
 * 
 * RightEdgeCategoryBar(
 *     state = edgeBarState,
 *     onCategorySelected = { category ->
 *         // Toggle panel or handle selection
 *         edgeBarState.activeCategory = 
 *             if (edgeBarState.activeCategory == category) null else category
 *     },
 *     onUndo = { viewModel.undo() },
 *     onRedo = { viewModel.redo() }
 * )
 * ```
 */
@Stable
class RightEdgeCategoryBarState(
    initialActiveCategory: ToolCategory? = null,
    initialSelectedCategory: ToolCategory? = null,
    initialCanUndo: Boolean = false,
    initialCanRedo: Boolean = false,
    initialIsVisible: Boolean = true
) {
    /**
     * The currently active category (its panel is open).
     * Null when no panel is open.
     */
    var activeCategory: ToolCategory? by mutableStateOf(initialActiveCategory)

    /**
     * The category containing the currently selected tool.
     * Shows the orange indicator dot on this category button.
     * Null when no tool is explicitly selected (default brush).
     */
    var selectedCategory: ToolCategory? by mutableStateOf(initialSelectedCategory)

    /**
     * Whether undo action is available.
     */
    var canUndo: Boolean by mutableStateOf(initialCanUndo)

    /**
     * Whether redo action is available.
     */
    var canRedo: Boolean by mutableStateOf(initialCanRedo)

    /**
     * Whether the edge bar is visible.
     * Used for auto-hide behavior during drawing.
     */
    var isVisible: Boolean by mutableStateOf(initialIsVisible)

    /**
     * Toggle the active category.
     * If the category is already active, close its panel.
     * Otherwise, open the new category's panel.
     */
    fun toggleCategory(category: ToolCategory) {
        activeCategory = if (activeCategory == category) null else category
    }

    /**
     * Close any open panel.
     */
    fun closePanel() {
        activeCategory = null
    }

    /**
     * Select a tool from a category.
     * Sets the selected category and closes the panel.
     */
    fun selectToolFromCategory(category: ToolCategory) {
        selectedCategory = category
        activeCategory = null
    }

    /**
     * Show the edge bar (after auto-hide).
     */
    fun show() {
        isVisible = true
    }

    /**
     * Hide the edge bar (for auto-hide during drawing).
     */
    fun hide() {
        isVisible = false
    }
}

/**
 * Remember and create a RightEdgeCategoryBarState.
 * 
 * @param initialActiveCategory Initial active category (panel open)
 * @param initialSelectedCategory Initial selected category (tool selected)
 * @param initialCanUndo Initial undo availability
 * @param initialCanRedo Initial redo availability
 * @param initialIsVisible Initial visibility
 * @return Remembered state instance
 */
@Composable
fun rememberRightEdgeCategoryBarState(
    initialActiveCategory: ToolCategory? = null,
    initialSelectedCategory: ToolCategory? = ToolCategory.BRUSH,
    initialCanUndo: Boolean = false,
    initialCanRedo: Boolean = false,
    initialIsVisible: Boolean = true
): RightEdgeCategoryBarState {
    return remember {
        RightEdgeCategoryBarState(
            initialActiveCategory = initialActiveCategory,
            initialSelectedCategory = initialSelectedCategory,
            initialCanUndo = initialCanUndo,
            initialCanRedo = initialCanRedo,
            initialIsVisible = initialIsVisible
        )
    }
}

/**
 * Simplified RightEdgeCategoryBar for basic usage.
 * 
 * Uses callbacks directly instead of a state holder.
 * Suitable for simple use cases or when state is managed externally.
 * 
 * @param activeCategory Currently active category (panel open)
 * @param selectedCategory Category with selected tool (shows dot indicator)
 * @param canUndo Whether undo is available
 * @param canRedo Whether redo is available
 * @param onCategorySelected Callback when category is tapped
 * @param onUndo Callback when undo is tapped
 * @param onRedo Callback when redo is tapped
 * @param modifier Modifier for the bar
 */
@Composable
fun RightEdgeCategoryBar(
    activeCategory: ToolCategory?,
    selectedCategory: ToolCategory?,
    canUndo: Boolean,
    canRedo: Boolean,
    onCategorySelected: (ToolCategory) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    RightEdgeCategoryBarContent(
        activeCategory = activeCategory,
        selectedCategory = selectedCategory,
        canUndo = canUndo,
        canRedo = canRedo,
        onCategorySelected = onCategorySelected,
        onUndo = onUndo,
        onRedo = onRedo,
        modifier = modifier
    )
}
