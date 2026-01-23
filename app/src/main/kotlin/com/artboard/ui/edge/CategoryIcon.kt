package com.artboard.ui.edge

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Tool categories for the right edge category bar.
 * 
 * Categories are organized by artist mental model:
 * - BRUSH: "I want to make marks on the canvas"
 * - COLOR: "I want to choose what color to use"
 * - LAYERS: "I want to organize my artwork structure"
 * - TRANSFORM: "I want to change what already exists"
 * - SETTINGS: "I want to configure the app"
 * 
 * Order reflects usage frequency (most used at top for thumb reach):
 * 1. Brush - Most frequently accessed
 * 2. Color - Second most frequent
 * 3. Layers - Moderate frequency
 * 4. Transform - Occasional use
 * 5. Settings - Least frequent (separated by divider)
 */
enum class ToolCategory(
    val displayName: String,
    val contentDescription: String,
    val accessibilityHint: String
) {
    BRUSH(
        displayName = "Brush",
        contentDescription = "Brush tools",
        accessibilityHint = "Double tap to open brush panel"
    ),
    COLOR(
        displayName = "Color",
        contentDescription = "Color picker",
        accessibilityHint = "Double tap to choose colors"
    ),
    LAYERS(
        displayName = "Layers",
        contentDescription = "Layers panel",
        accessibilityHint = "Double tap to manage layers"
    ),
    TRANSFORM(
        displayName = "Transform",
        contentDescription = "Transform tools",
        accessibilityHint = "Double tap for transform options"
    ),
    SETTINGS(
        displayName = "Settings",
        contentDescription = "Settings",
        accessibilityHint = "Double tap to open settings"
    );

    companion object {
        /**
         * Primary categories (above the separator line)
         */
        val primary: List<ToolCategory> = listOf(BRUSH, COLOR, LAYERS, TRANSFORM)

        /**
         * Secondary categories (below the separator line)
         */
        val secondary: List<ToolCategory> = listOf(SETTINGS)
    }
}

/**
 * Icon resources for each category.
 * 
 * Icons follow Material Design guidelines:
 * - Outlined style for inactive state
 * - Filled style for active/selected state
 * - 24x24dp optical size within 48x48dp touch target
 * 
 * Icon design principles:
 * - Simple, geometric shapes
 * - 1-2 elements maximum
 * - Recognizable at 24dp
 * - 2dp consistent stroke weight
 */
object CategoryIcons {
    
    /**
     * Get the outlined (inactive) icon for a category
     */
    fun getOutlinedIcon(category: ToolCategory): ImageVector = when (category) {
        ToolCategory.BRUSH -> Icons.Outlined.Brush
        ToolCategory.COLOR -> Icons.Outlined.ColorLens
        ToolCategory.LAYERS -> Icons.Outlined.Layers
        ToolCategory.TRANSFORM -> Icons.Outlined.OpenWith
        ToolCategory.SETTINGS -> Icons.Outlined.Settings
    }

    /**
     * Get the filled (active) icon for a category
     */
    fun getFilledIcon(category: ToolCategory): ImageVector = when (category) {
        ToolCategory.BRUSH -> Icons.Filled.Brush
        ToolCategory.COLOR -> Icons.Filled.ColorLens
        ToolCategory.LAYERS -> Icons.Filled.Layers
        ToolCategory.TRANSFORM -> Icons.Filled.OpenWith
        ToolCategory.SETTINGS -> Icons.Filled.Settings
    }

    /**
     * Get the appropriate icon based on active state.
     * Active state shows filled icon, inactive shows outlined.
     */
    fun getIcon(category: ToolCategory, isActive: Boolean): ImageVector =
        if (isActive) getFilledIcon(category) else getOutlinedIcon(category)
}

/**
 * Color definitions for the edge control bar.
 * 
 * Based on design spec:
 * - Inactive: #8E8E93 (Gray 500 - 40% contrast)
 * - Active: #007AFF (System Blue)
 * - Selected indicator: #FF9500 (Orange dot)
 * - Pressed background: #007AFF1F (12% blue)
 */
object EdgeBarColors {
    /** Icon color when inactive */
    const val ICON_INACTIVE = 0xFF8E8E93
    
    /** Icon color on hover/touch start */
    const val ICON_HOVER = 0xFF636366
    
    /** Icon color when active (panel open) */
    const val ICON_ACTIVE = 0xFF007AFF
    
    /** Icon color when pressed */
    const val ICON_PRESSED = 0xFF0056B3
    
    /** Background when hovered (3% black overlay) */
    const val BG_HOVER = 0x08000000
    
    /** Background when active (8% blue tint) */
    const val BG_ACTIVE = 0x14007AFF
    
    /** Background when pressed (12% blue tint) */
    const val BG_PRESSED = 0x1F007AFF
    
    /** Orange dot indicator for selected tool */
    const val INDICATOR_DOT = 0xFFFF9500
    
    /** Separator line color */
    const val SEPARATOR = 0xFFC6C6C8
    
    /** Disabled icon color (30% gray) */
    const val ICON_DISABLED = 0x4D8E8E93
    
    // Dark mode variants
    /** Icon color when active (dark mode) */
    const val ICON_ACTIVE_DARK = 0xFF0A84FF
    
    /** Background when active (dark mode, 12% blue) */
    const val BG_ACTIVE_DARK = 0x1F0A84FF
    
    /** Background when pressed (dark mode, 20% blue) */
    const val BG_PRESSED_DARK = 0x330A84FF
    
    /** Separator line color (dark mode) */
    const val SEPARATOR_DARK = 0xFF38383A
    
    /** Orange dot indicator (dark mode) */
    const val INDICATOR_DOT_DARK = 0xFFFF9F0A
}

/**
 * Dimension constants for the edge control bar.
 * All values in dp as specified in design doc.
 */
object EdgeBarDimensions {
    /** Total width of the edge bar */
    const val BAR_WIDTH = 56
    
    /** Button size (touch target) */
    const val BUTTON_SIZE = 48
    
    /** Icon size within button */
    const val ICON_SIZE = 24
    
    /** Gap between buttons */
    const val BUTTON_GAP = 4
    
    /** Inset from screen edge */
    const val EDGE_INSET = 8
    
    /** Button corner radius */
    const val BUTTON_CORNER_RADIUS = 12
    
    /** Selected indicator dot size */
    const val INDICATOR_DOT_SIZE = 8
    
    /** Separator line width */
    const val SEPARATOR_WIDTH = 32
    
    /** Separator line height */
    const val SEPARATOR_HEIGHT = 1
    
    /** Separator margin (above and below) */
    const val SEPARATOR_MARGIN = 8
}

/**
 * Animation constants for edge bar interactions.
 */
object EdgeBarAnimations {
    /** Press animation scale factor */
    const val PRESS_SCALE = 0.92f
    
    /** Press animation duration in ms */
    const val PRESS_DURATION_MS = 80
    
    /** Release animation uses spring with these parameters */
    const val RELEASE_DAMPING = 0.7f
    const val RELEASE_STIFFNESS = 300f
    
    /** Selection dot animation duration in ms */
    const val DOT_ANIMATION_DURATION_MS = 250
    
    /** Selection dot overshoot scale */
    const val DOT_OVERSHOOT_SCALE = 1.2f
}
