package com.stoolkit.app.data.model

/**
 * Represents a tool category in the app
 */
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
