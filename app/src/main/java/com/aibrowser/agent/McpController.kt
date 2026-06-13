package com.aibrowser.agent

import com.aibrowser.data.models.ToolCall
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpController @Inject constructor(
    private val toolExecutor: ToolExecutor
) {
    private val gson = Gson()

    suspend fun executeToolCall(toolCall: ToolCall): ToolCall {
        return try {
            val result = toolExecutor.execute(toolCall.name, toolCall.arguments)
            toolCall.copy(status = ToolCall.ToolStatus.DONE, result = result)
        } catch (e: Exception) {
            toolCall.copy(status = ToolCall.ToolStatus.ERROR, result = "Error: ${e.message}")
        }
    }

    fun parseToolCalls(responseContent: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()
        try {
            val json = JsonParser.parseString(responseContent).asJsonObject
            if (json.has("tool_calls")) {
                val calls = json.getAsJsonArray("tool_calls")
                for (call in calls) {
                    val obj = call.asJsonObject
                    val function = obj.getAsJsonObject("function")
                    val args = try {
                        gson.fromJson(function.get("arguments"), JsonObject::class.java)
                            .entrySet().associate { it.key to it.value.asString }
                    } catch (_: Exception) {
                        emptyMap()
                    }
                    toolCalls.add(
                        ToolCall(
                            id = obj.get("id")?.asString ?: "",
                            name = function.get("name").asString,
                            arguments = args,
                            status = ToolCall.ToolStatus.PENDING
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        return toolCalls
    }
}
