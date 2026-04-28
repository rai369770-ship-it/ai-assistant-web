package com.blindtechnexus.app.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a tool category in the app
 */
@Serializable
enum class ToolCategory {
    AI_TOOLS,
    AUDIO_TOOLS,
    PRODUCTIVITY_TOOLS,
    VIDEO_TOOLS,
    IMAGE_TOOLS,
    DEVICE_TOOLS
}

/**
 * Represents a tool available in the app
 */
@Serializable
data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val category: ToolCategory
)

/**
 * Represents a group of tools by category
 */
data class CategoryGroup(
    val name: ToolCategory,
    val tools: List<Tool>
)
