package com.aibrowser.agent

import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.Message
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
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

            client.newCall(request).execute().use { response ->
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
        } catch (e: Exception) {
            onEvent(StreamEvent.Error("Network error: ${e.message}"))
            ""
        }
    }

    private fun buildRequestBody(config: ApiConfig, messages: List<Message>): String {
        val apiMessages = messages.map { msg ->
            mapOf(
                "role" to when (msg.role) {
                    Message.Role.USER -> "user"
                    Message.Role.ASSISTANT -> "assistant"
                    Message.Role.SYSTEM -> "system"
                    Message.Role.TOOL -> "tool"
                },
                "content" to msg.content
            )
        }

        val body = mutableMapOf<String, Any>(
            "model" to config.model,
            "messages" to apiMessages,
            "tools" to ToolDefinitions.getToolsForApi(),
            "tool_choice" to "auto"
        )

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

    private fun parseStreamingResponse(body: String, onEvent: (StreamEvent) -> Unit): String {
        val lines = body.lines()
        var fullContent = ""
        var inToolCall = false
        var toolCallId = ""
        var toolCallName = ""
        var toolCallArgs = StringBuilder()

        for (line in lines) {
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

                        if (tcObj.has("id") && !tcObj.get("id").isJsonNull) {
                            if (inToolCall && toolCallId.isNotEmpty()) {
                                onEvent(StreamEvent.ToolCallStart(toolCallId, toolCallName, toolCallArgs.toString()))
                            }
                            toolCallId = tcObj.get("id").asString
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
