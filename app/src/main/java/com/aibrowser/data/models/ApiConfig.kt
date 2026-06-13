package com.aibrowser.data.models

data class ApiConfig(
    val provider: ApiProvider = ApiProvider.OPENAI,
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = ""
) {
    enum class ApiProvider(val displayName: String, val defaultModel: String, val defaultBaseUrl: String) {
        OPENAI("OpenAI", "gpt-4o", "https://api.openai.com/v1"),
        CLAUDE("Anthropic", "claude-sonnet-4-20250514", "https://api.anthropic.com"),
        CUSTOM("Custom", "", "")
    }
}
