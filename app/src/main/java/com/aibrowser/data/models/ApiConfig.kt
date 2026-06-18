package com.aibrowser.data.models

data class ApiConfig(
    val provider: ApiProvider = ApiProvider.OPENAI,
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = "",
    val contextSize: Int = 0,
    val maxOutputTokens: Int = 0
) {
    enum class ApiProvider(val displayName: String, val defaultModel: String, val defaultBaseUrl: String) {
        OPENAI("OpenAI", "gpt-4o", "https://api.openai.com/v1"),
        CLAUDE("Anthropic", "claude-sonnet-4-20250514", "https://api.anthropic.com"),
        CUSTOM("Custom", "", ""),
        LOCAL_LLAMACPP("llama.cpp Local", "", ""),
        LOCAL_LITERT("LiteRT Local", "", "")
    }
}

data class ModelInfo(
    val id: String,
    val contextSize: Int? = null,
    val maxOutput: Int? = null
)
