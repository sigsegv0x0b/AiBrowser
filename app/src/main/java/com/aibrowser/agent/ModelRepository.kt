package com.aibrowser.agent

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloader: ModelDownloader
) {
    private val gson = Gson()

    fun getDownloadedModels(): List<DownloadedModel> {
        return modelDownloader.listDownloadedModels().mapNotNull { file ->
            val parts = file.name.split("__", limit = 2)
            if (parts.size < 2) return@mapNotNull null
            val repo = parts[0]
            val filename = parts[1]
            DownloadedModel(
                repo = repo,
                filename = filename,
                localPath = file.absolutePath,
                sizeBytes = file.length()
            )
        }
    }
}

data class DownloadedModel(
    val repo: String,
    val filename: String,
    val localPath: String,
    val sizeBytes: Long
)
