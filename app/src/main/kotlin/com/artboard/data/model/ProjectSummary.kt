package com.artboard.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight project summary for gallery display
 * Contains only essential information for thumbnail display
 */
data class ProjectSummary(
    val id: String,
    val name: String,
    val thumbnailPath: String,
    val width: Int,
    val height: Int,
    val layerCount: Int,
    val strokeCount: Int,
    val createdAt: Long,
    val modifiedAt: Long,
    val fileSizeMB: Float,
    val tags: List<String> = emptyList()
) {
    /**
     * Format the modified date in a user-friendly way
     * Examples: "Just now", "2 min ago", "5 hours ago", "3 days ago", "Jan 21, 2026"
     */
    fun formattedDate(): String {
        val now = System.currentTimeMillis()
        val diff = now - modifiedAt
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} min ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(Date(modifiedAt))
        }
    }
    
    companion object {
        /**
         * Create a ProjectSummary from a full Project
         */
        fun fromProject(project: Project, thumbnailPath: String = ""): ProjectSummary {
            return ProjectSummary(
                id = project.id,
                name = project.name,
                thumbnailPath = thumbnailPath,
                width = project.width,
                height = project.height,
                layerCount = project.layers.size,
                strokeCount = 0, // TODO: Track stroke count if needed
                createdAt = project.createdAt,
                modifiedAt = project.modifiedAt,
                fileSizeMB = 0f, // Calculate from actual file size
                tags = emptyList()
            )
        }
    }
}

/**
 * Sort modes for project gallery
 */
enum class SortMode {
    MODIFIED_DESC,  // Most recently modified first (default)
    CREATED_DESC,   // Most recently created first
    NAME_ASC,       // Alphabetical by name
    SIZE_DESC       // Largest canvas size first
}
