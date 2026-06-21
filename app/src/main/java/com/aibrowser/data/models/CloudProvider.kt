package com.aibrowser.data.models

data class CloudProvider(
    val id: String,
    val name: String,
    val provider: ApiConfig.ApiProvider,
    val apiKey: String,
    val model: String,
    val baseUrl: String,
    val contextSize: Int = 0,
    val maxOutputTokens: Int = 0
)
