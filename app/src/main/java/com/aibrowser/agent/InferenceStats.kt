package com.aibrowser.agent

data class InferenceStats(
    val ttftMs: Double = 0.0,
    val promptTokens: Long = 0,
    val prefillSpeed: Double = 0.0,
    val generatedTokens: Long = 0,
    val decodingSpeed: Double = 0.0,
    val device: String = "unknown"
)
