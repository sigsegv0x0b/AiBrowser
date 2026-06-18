package com.aibrowser.data.models

data class AvailableGgufModel(
    val id: String,
    val displayName: String,
    val repo: String,
    val filename: String,
    val description: String,
    val sizeBytes: Long,
    val quant: String,
    val contextSize: Int,
    val minRamGb: Int,
    val isGated: Boolean = false
) {
    val sizeGb: Float get() = sizeBytes / (1024f * 1024f * 1024f)
}
