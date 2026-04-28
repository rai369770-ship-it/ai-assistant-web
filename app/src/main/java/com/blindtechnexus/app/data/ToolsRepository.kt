package com.blindtechnexus.app.data

import com.blindtechnexus.app.data.model.CategoryGroup
import com.blindtechnexus.app.data.model.Tool
import com.blindtechnexus.app.data.model.ToolCategory

/**
 * Repository providing tool data for the app
 */
object ToolsRepository {
    
    val toolsData: List<Tool> = listOf(
        // AI Tools
        Tool(
            id = "blindtechnexus-ai",
            name = "Blind Tech Nexus AI",
            description = "Multi-model native AI for writing, coding, and planning anything.",
            category = ToolCategory.AI_TOOLS
        ),
        Tool(
            id = "auto-tts",
            name = "Auto TTS",
            description = "Multilingual text-to-speech with automatic language detection and synthesis.",
            category = ToolCategory.AI_TOOLS
        ),
        Tool(
            id = "audio-video-transcriber",
            name = "Audio & Video Transcriber",
            description = "Transcribe your audio and video files accurately using AI.",
            category = ToolCategory.AI_TOOLS
        ),
        Tool(
            id = "youtube-toolkit",
            name = "YouTube Toolkit",
            description = "Download, summarize, and transcribe YouTube videos effortlessly.",
            category = ToolCategory.AI_TOOLS
        ),
        Tool(
            id = "ai-image-generator",
            name = "AI Image Generator & Editor",
            description = "Generate and edit stunning images using advanced AI technology.",
            category = ToolCategory.AI_TOOLS
        ),
        Tool(
            id = "ai-image-analysis",
            name = "AI Image Analysis",
            description = "Analyze images with AI to extract insights and information.",
            category = ToolCategory.AI_TOOLS
        ),
        Tool(
            id = "ai-personal-assistant",
            name = "AI Personal Assistant",
            description = "Create custom AI assistants to chat, talk, and complete tasks.",
            category = ToolCategory.AI_TOOLS
        ),
        Tool(
            id = "ai-future-predictor",
            name = "AI Future Predictor",
            description = "Explore AI-powered predictions and insights about future trends.",
            category = ToolCategory.AI_TOOLS
        ),
        // Audio Tools
        Tool(
            id = "text-to-speech",
            name = "Text to Speech Converter",
            description = "Multilingual TTS synthesizer with local and online engine support.",
            category = ToolCategory.AUDIO_TOOLS
        ),
        Tool(
            id = "voice-recorder",
            name = "Voice Recorder",
            description = "Record your voice with customizable quality and format options.",
            category = ToolCategory.AUDIO_TOOLS
        ),
        Tool(
            id = "media-player",
            name = "Media Player",
            description = "Play and listen to your favorite audio and video files.",
            category = ToolCategory.AUDIO_TOOLS
        ),
        // Productivity Tools
        Tool(
            id = "pdf-toolkit",
            name = "PDF Toolkit",
            description = "Create, read, and organize PDF documents with ease.",
            category = ToolCategory.PRODUCTIVITY_TOOLS
        ),
        Tool(
            id = "notepad",
            name = "Notepad",
            description = "Write, edit, and manage your notes efficiently.",
            category = ToolCategory.PRODUCTIVITY_TOOLS
        ),
        Tool(
            id = "reminder",
            name = "Reminder",
            description = "Set and manage reminders to stay organized and on track.",
            category = ToolCategory.PRODUCTIVITY_TOOLS
        ),
        // Video Tools
        Tool(
            id = "screen-recorder",
            name = "Screen Recorder",
            description = "Record your screen with customizable settings and options.",
            category = ToolCategory.VIDEO_TOOLS
        ),
        // Image Tools
        Tool(
            id = "camera",
            name = "Camera",
            description = "Capture photos and videos with advanced camera features.",
            category = ToolCategory.IMAGE_TOOLS
        ),
        Tool(
            id = "image-toolkit",
            name = "Image Toolkit",
            description = "Edit, enhance, and transform your images with powerful tools.",
            category = ToolCategory.IMAGE_TOOLS
        ),
        // Device Tools
        Tool(
            id = "device-info",
            name = "Device Info",
            description = "View detailed information about your device specifications.",
            category = ToolCategory.DEVICE_TOOLS
        )
    )
    
    /**
     * Get all tools grouped by category
     */
    fun getToolsByCategory(): List<CategoryGroup> {
        val categories = mutableMapOf<ToolCategory, MutableList<Tool>>()
        
        toolsData.forEach { tool ->
            if (!categories.containsKey(tool.category)) {
                categories[tool.category] = mutableListOf()
            }
            categories[tool.category]?.add(tool)
        }
        
        return categories.entries.map { (name, tools) ->
            CategoryGroup(name = name, tools = tools)
        }
    }
}
