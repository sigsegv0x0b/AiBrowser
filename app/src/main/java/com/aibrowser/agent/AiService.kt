package com.aibrowser.agent

import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.Message
import com.aibrowser.data.models.ModelInfo
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    private val client: OkHttpClient,
    private val settingsRepository: SettingsRepository
) {
    private val gson = Gson()

    sealed class StreamEvent {
        data class Token(val text: String) : StreamEvent()
        data class ToolCallStart(val id: String, val name: String, val args: String) : StreamEvent()
        data class Done(val fullResponse: String) : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    suspend fun sendMessage(
        messages: List<Message>,
        onEvent: (StreamEvent) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val config = settingsRepository.apiConfig.first()

        if (config.apiKey.isBlank()) {
            onEvent(StreamEvent.Error("API key not configured. Go to Settings."))
            return@withContext ""
        }

        try {
            val requestBody = buildRequestBody(config, messages)
            val request = buildRequest(config, requestBody)

            executeWithRetry(request).use { response ->
                if (!response.isSuccessful) {
                    val error = response.body?.string() ?: "Unknown error"
                    onEvent(StreamEvent.Error("API error ${response.code}: $error"))
                    return@withContext ""
                }

                val body = response.body?.string() ?: ""
                val fullResponse = parseStreamingResponse(body, onEvent)
                onEvent(StreamEvent.Done(fullResponse))
                fullResponse
            }
        } catch (e: IOException) {
            onEvent(StreamEvent.Error("Network error: ${e.message}"))
            ""
        }
    }

    private suspend fun executeWithRetry(request: Request): Response {
        var lastException: IOException? = null
        repeat(3) { attempt ->
            try {
                return client.newCall(request).execute()
            } catch (e: IOException) {
                lastException = e
                if (attempt < 2) delay((1000L shl attempt))
            }
        }
        throw lastException ?: IOException("Request failed after 3 retries")
    }

    private fun buildRequestBody(config: ApiConfig, messages: List<Message>): String {
        val apiMessages = messages.map { msg ->
            val entry = mutableMapOf<String, Any?>(
                "role" to when (msg.role) {
                    Message.Role.USER -> "user"
                    Message.Role.ASSISTANT -> "assistant"
                    Message.Role.SYSTEM -> "system"
                    Message.Role.TOOL -> "tool"
                },
                "content" to msg.content
            )

            if (msg.role == Message.Role.ASSISTANT && msg.toolCalls.isNotEmpty()) {
                entry["tool_calls"] = msg.toolCalls.map { tc ->
                    mapOf(
                        "id" to tc.id,
                        "type" to "function",
                        "function" to mapOf(
                            "name" to tc.name,
                            "arguments" to gson.toJson(tc.arguments)
                        )
                    )
                }
            }

            if (msg.role == Message.Role.TOOL && msg.toolCallId != null) {
                entry["tool_call_id"] = msg.toolCallId
            }

            entry
        }

        val body = mutableMapOf<String, Any>(
            "model" to config.model,
            "messages" to apiMessages,
            "tools" to ToolDefinitions.getToolsForApi(),
            "tool_choice" to "auto"
        )

        if (config.provider != ApiConfig.ApiProvider.CLAUDE) {
            body["stream"] = true
        }

        return gson.toJson(body)
    }

    private fun buildRequest(config: ApiConfig, body: String): Request {
        val url = when (config.provider) {
            ApiConfig.ApiProvider.CLAUDE -> "${config.baseUrl}/v1/messages"
            else -> "${config.baseUrl}/chat/completions"
        }

        val builder = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))

        when (config.provider) {
            ApiConfig.ApiProvider.OPENAI, ApiConfig.ApiProvider.CUSTOM -> {
                builder.addHeader("Authorization", "Bearer ${config.apiKey}")
            }
            ApiConfig.ApiProvider.CLAUDE -> {
                builder.addHeader("x-api-key", config.apiKey)
                builder.addHeader("anthropic-version", "2023-06-01")
            }
        }

        return builder.build()
    }

    suspend fun testConnection(config: ApiConfig): String = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) return@withContext "Error: API key is empty"

        try {
            val url = when (config.provider) {
                ApiConfig.ApiProvider.CLAUDE -> "${config.baseUrl}/v1/messages"
                else -> "${config.baseUrl}/chat/completions"
            }

            val body = when (config.provider) {
                ApiConfig.ApiProvider.CLAUDE -> gson.toJson(
                    mapOf("model" to config.model, "messages" to listOf(mapOf("role" to "user", "content" to "hi")), "max_tokens" to 1)
                )
                else -> gson.toJson(
                    mapOf("model" to config.model, "messages" to listOf(mapOf("role" to "user", "content" to "hi")), "max_tokens" to 1, "stream" to false)
                )
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json".toMediaType()))

            when (config.provider) {
                ApiConfig.ApiProvider.OPENAI, ApiConfig.ApiProvider.CUSTOM -> {
                    requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
                }
                ApiConfig.ApiProvider.CLAUDE -> {
                    requestBuilder.addHeader("x-api-key", config.apiKey)
                    requestBuilder.addHeader("anthropic-version", "2023-06-01")
                }
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    "OK (${response.code})"
                } else {
                    val error = response.body?.string() ?: "Unknown error"
                    "Error ${response.code}: ${error.take(200)}"
                }
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun listModels(config: ApiConfig): List<ModelInfo> = withContext(Dispatchers.IO) {
        if (config.provider == ApiConfig.ApiProvider.CLAUDE) return@withContext emptyList()

        try {
            val url = "${config.baseUrl}/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JsonParser.parseString(body).asJsonObject
                val data = json.getAsJsonArray("data") ?: return@withContext emptyList()
                data.map { modelObj ->
                    val obj = modelObj.asJsonObject
                    ModelInfo(
                        id = obj.get("id").asString,
                        contextSize = parseNullableInt(obj, "context_length")
                            ?: parseNullableInt(obj, "context_size")
                            ?: parseNullableInt(obj, "max_context_length")
                            ?: parseNullableInt(obj, "max_context"),
                        maxOutput = parseNullableInt(obj, "max_output")
                            ?: parseNullableInt(obj, "max_tokens")
                            ?: parseNullableInt(obj, "max_output_tokens")
                    )
                }.sortedBy { it.id }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseNullableInt(obj: com.google.gson.JsonObject, key: String): Int? {
        return try {
            val el = obj.get(key)
            if (el != null && !el.isJsonNull) el.asInt else null
        } catch (_: Exception) { null }
    }

    private fun parseStreamingResponse(body: String, onEvent: (StreamEvent) -> Unit): String {
        if (!body.contains("data: ")) {
            return parseNonStreamingResponse(body, onEvent)
        }
        return parseSSEResponse(body, onEvent)
    }

    private fun parseNonStreamingResponse(body: String, onEvent: (StreamEvent) -> Unit): String {
        try {
            val json = JsonParser.parseString(body).asJsonObject
            val choices = json.getAsJsonArray("choices") ?: return ""
            if (choices.size() == 0) return ""
            val message = choices[0].asJsonObject.getAsJsonObject("message") ?: return ""
            val content = message.get("content")?.asString ?: ""

            if (content.isNotEmpty()) {
                onEvent(StreamEvent.Token(content))
            }

            val toolCalls = message.getAsJsonArray("tool_calls")
            if (toolCalls != null) {
                for (tc in toolCalls) {
                    val tcObj = tc.asJsonObject
                    val id = tcObj.get("id")?.asString ?: continue
                    val func = tcObj.getAsJsonObject("function") ?: continue
                    val name = func.get("name")?.asString ?: continue
                    val args = func.get("arguments")?.asString ?: "{}"
                    onEvent(StreamEvent.ToolCallStart(id, name, args))
                }
            }

            return content
        } catch (e: Exception) {
            onEvent(StreamEvent.Error("Parse error: ${e.message}"))
            return ""
        }
    }

    private fun parseSSEResponse(body: String, onEvent: (StreamEvent) -> Unit): String {
        var fullContent = ""
        var toolCallId = ""
        var toolCallName = ""
        val toolCallArgs = StringBuilder()
        var inToolCall = false

        for (line in body.lines()) {
            if (!line.startsWith("data: ")) continue
            val data = line.removePrefix("data: ").trim()
            if (data == "[DONE]") break

            try {
                val json = JsonParser.parseString(data).asJsonObject
                val choices = json.getAsJsonArray("choices") ?: continue
                if (choices.size() == 0) continue
                val delta = choices[0].asJsonObject.getAsJsonObject("delta") ?: continue

                if (delta.has("content") && !delta.get("content").isJsonNull) {
                    val token = delta.get("content").asString
                    fullContent += token
                    onEvent(StreamEvent.Token(token))
                }

                if (delta.has("tool_calls") && !delta.get("tool_calls").isJsonNull) {
                    val toolCalls = delta.getAsJsonArray("tool_calls")
                    for (tc in toolCalls) {
                        val tcObj = tc.asJsonObject
                        val index = tcObj.get("index")?.asInt ?: 0

                        if (tcObj.has("id") && !tcObj.get("id").isJsonNull) {
                            if (inToolCall && toolCallId.isNotEmpty()) {
                                onEvent(StreamEvent.ToolCallStart(toolCallId, toolCallName, toolCallArgs.toString()))
                            }
                            toolCallId = tcObj.get("id").asString
                            toolCallName = ""
                            toolCallArgs.clear()
                            inToolCall = true
                        }

                        if (tcObj.has("function")) {
                            val func = tcObj.getAsJsonObject("function")
                            if (func.has("name") && !func.get("name").isJsonNull) {
                                toolCallName = func.get("name").asString
                            }
                            if (func.has("arguments") && !func.get("arguments").isJsonNull) {
                                toolCallArgs.append(func.get("arguments").asString)
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        if (inToolCall && toolCallId.isNotEmpty()) {
            onEvent(StreamEvent.ToolCallStart(toolCallId, toolCallName, toolCallArgs.toString()))
        }

        return fullContent
    }
}
