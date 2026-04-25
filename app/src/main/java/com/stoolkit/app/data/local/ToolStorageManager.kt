package com.blindtechnexus.app.data.local

import android.util.Log
import com.blindtechnexus.app.data.model.Tool
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ToolStorageManager {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val rootFolder = File(ROOT_PATH)
    private val favoritesFile = File(rootFolder, FAVORITES_FILE_NAME)
    private val pinnedFile = File(rootFolder, PINNED_FILE_NAME)

    fun ensureStorageFiles() {
        runCatching {
            if (!rootFolder.exists()) {
                rootFolder.mkdirs()
            }
            if (!favoritesFile.exists()) {
                favoritesFile.writeText("[]")
            }
            if (!pinnedFile.exists()) {
                pinnedFile.writeText("[]")
            }
        }.onFailure {
            Log.e(TAG, "Failed to initialize storage files", it)
        }
    }

    fun readFavorites(): List<Tool> = readToolsFromFile(favoritesFile)

    fun readPinnedTools(): List<Tool> = readToolsFromFile(pinnedFile)

    fun saveFavorites(tools: List<Tool>) {
        saveToolsToFile(favoritesFile, tools)
    }

    fun savePinnedTools(tools: List<Tool>) {
        saveToolsToFile(pinnedFile, tools)
    }

    private fun readToolsFromFile(file: File): List<Tool> {
        ensureStorageFiles()
        return runCatching {
            val fileContent = file.readText().ifBlank { "[]" }
            json.decodeFromString<List<Tool>>(fileContent)
        }.getOrElse {
            Log.e(TAG, "Failed to read file ${file.absolutePath}", it)
            emptyList()
        }
    }

    private fun saveToolsToFile(file: File, tools: List<Tool>) {
        ensureStorageFiles()
        runCatching {
            file.writeText(json.encodeToString(tools.distinctBy { it.id }))
        }.onFailure {
            Log.e(TAG, "Failed to save file ${file.absolutePath}", it)
        }
    }

    companion object {
        private const val TAG = "ToolStorageManager"
        private const val ROOT_PATH = "/storage/emulated/0/.btn_files"
        private const val FAVORITES_FILE_NAME = "favorites.json"
        private const val PINNED_FILE_NAME = "pinnedTools.json"
    }
}
