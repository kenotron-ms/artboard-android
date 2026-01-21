package com.artboard.data.model

import java.util.UUID

/**
 * Represents a complete art project with layers
 */
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val width: Int,               // Canvas width in pixels
    val height: Int,              // Canvas height in pixels
    val layers: List<Layer>,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val backgroundColor: Int = android.graphics.Color.WHITE
) {
    /**
     * Get the currently active layer index (last layer by default)
     */
    fun getActiveLayerIndex(): Int {
        return (layers.size - 1).coerceAtLeast(0)
    }
    
    /**
     * Create a new project with an additional layer
     */
    fun addLayer(layer: Layer): Project {
        return copy(
            layers = layers + layer,
            modifiedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Create a new project with a layer removed
     */
    fun removeLayer(index: Int): Project {
        if (layers.size <= 1) {
            // Keep at least one layer
            return this
        }
        return copy(
            layers = layers.filterIndexed { i, _ -> i != index },
            modifiedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Create a new project with a layer updated
     */
    fun updateLayer(index: Int, layer: Layer): Project {
        return copy(
            layers = layers.mapIndexed { i, l -> if (i == index) layer else l },
            modifiedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Create a new project with layers reordered
     */
    fun moveLayer(fromIndex: Int, toIndex: Int): Project {
        val mutableLayers = layers.toMutableList()
        val layer = mutableLayers.removeAt(fromIndex)
        mutableLayers.add(toIndex, layer)
        return copy(
            layers = mutableLayers,
            modifiedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Update modification timestamp
     */
    fun touch(): Project {
        return copy(modifiedAt = System.currentTimeMillis())
    }
    
    companion object {
        /**
         * Create a new blank project
         */
        fun create(
            name: String = "Untitled",
            width: Int = 2048,
            height: Int = 2048
        ): Project {
            val initialLayer = Layer.create(width, height, "Background")
            return Project(
                name = name,
                width = width,
                height = height,
                layers = listOf(initialLayer)
            )
        }
    }
}
