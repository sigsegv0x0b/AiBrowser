package com.aibrowser.agent.mnn.market

import com.google.gson.annotations.SerializedName

data class ModelMarket(
    val version: String = "",
    val models: List<MarketModel> = emptyList()
)

data class MarketModel(
    val modelName: String = "",
    val tags: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val vendor: String = "",
    @SerializedName("size_gb")
    val sizeGb: Float = 0f,
    @SerializedName("file_size")
    val fileSize: Long = 0,
    val sources: Map<String, String> = emptyMap(),
    @SerializedName("min_app_version")
    val minAppVersion: String? = null
)

data class DownloadedMnnModel(
    val modelId: String = "",
    val modelName: String = "",
    val downloadPath: String = "",
    val complete: Boolean = false,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val timestamp: Long = 0
)
