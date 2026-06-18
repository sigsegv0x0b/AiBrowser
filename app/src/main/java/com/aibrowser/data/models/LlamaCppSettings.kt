package com.aibrowser.data.models

data class LlamaCppSettings(
    val selectedModel: String = "",
    val backend: String = "cpu",
    val nGpuLayers: Int = 33,
    val contextWindow: Int = 8192,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val useCustomSampler: Boolean = false
) {
    val isModelReady: Boolean get() = selectedModel.isNotBlank()
}
