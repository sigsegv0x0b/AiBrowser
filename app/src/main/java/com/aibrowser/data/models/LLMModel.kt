package com.aibrowser.data.models

data class LLMModel(
    val id: String,
    val repo: String,
    val filename: String,
    val displayName: String,
    val sizeGb: Float,
    val quant: String = "",
    val contextSize: Int = 0
)
