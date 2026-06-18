package com.aibrowser.agent

data class HfModelSummary(
    val modelId: String,
    val downloads: Long = 0,
    val likes: Long = 0,
    val isGated: Boolean = false,
    val trendingScore: Int = 0
)

data class HfFileInfo(
    val filename: String,
    val size: Long
)

data class HfModelInfo(
    val modelId: String,
    val author: String?,
    val description: String?,
    val pipelineTag: String?,
    val libraryName: String?,
    val tags: List<String> = emptyList(),
    val downloads: Long = 0,
    val likes: Long = 0,
    val isGated: Boolean = false
)
