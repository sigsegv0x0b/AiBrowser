package com.aibrowser.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibrowser.data.models.Message
import com.aibrowser.data.models.ToolCall
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val aiService: AiService,
    private val mcpController: McpController
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val gson = Gson()

    fun sendMessage(content: String) {
        if (content.isBlank() || _isLoading.value) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            role = Message.Role.USER,
            content = content
        )
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            val assistantContent = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()

            aiService.sendMessage(_messages.value) { event ->
                when (event) {
                    is AiService.StreamEvent.Token -> {
                        assistantContent.append(event.text)
                    }
                    is AiService.StreamEvent.ToolCallStart -> {
                        val args = try {
                            gson.fromJson(event.args, JsonObject::class.java)
                                .entrySet().associate { it.key to it.value.asString }
                        } catch (_: Exception) { emptyMap() }
                        toolCalls.add(
                            ToolCall(event.id, event.name, args, ToolCall.ToolStatus.PENDING)
                        )
                    }
                    is AiService.StreamEvent.Done -> {
                        if (assistantContent.isNotEmpty() || toolCalls.isNotEmpty()) {
                            val assistantMsg = Message(
                                id = UUID.randomUUID().toString(),
                                role = Message.Role.ASSISTANT,
                                content = assistantContent.toString(),
                                toolCalls = toolCalls.toList()
                            )
                            _messages.value = _messages.value + assistantMsg

                            if (toolCalls.isNotEmpty()) {
                                executeToolCalls(toolCalls)
                            }
                        }
                    }
                    is AiService.StreamEvent.Error -> {
                        _messages.value = _messages.value + Message(
                            id = UUID.randomUUID().toString(),
                            role = Message.Role.ASSISTANT,
                            content = "Error: ${event.message}"
                        )
                    }
                }
            }

            _isLoading.value = false
        }
    }

    private suspend fun executeToolCalls(toolCalls: List<ToolCall>) {
        for (toolCall in toolCalls) {
            updateToolCallStatus(toolCall.id, ToolCall.ToolStatus.RUNNING)
            val result = mcpController.executeToolCall(toolCall)
            updateToolCallStatus(toolCall.id, result.status, result.result)

            _messages.value = _messages.value + Message(
                id = UUID.randomUUID().toString(),
                role = Message.Role.TOOL,
                content = result.result ?: "No result"
            )
        }

        if (toolCalls.isNotEmpty()) {
            val followUpContent = StringBuilder()
            val followUpToolCalls = mutableListOf<ToolCall>()

            aiService.sendMessage(_messages.value) { event ->
                when (event) {
                    is AiService.StreamEvent.Token -> followUpContent.append(event.text)
                    is AiService.StreamEvent.ToolCallStart -> {
                        val args = try {
                            gson.fromJson(event.args, JsonObject::class.java)
                                .entrySet().associate { it.key to it.value.asString }
                        } catch (_: Exception) { emptyMap() }
                        followUpToolCalls.add(ToolCall(event.id, event.name, args, ToolCall.ToolStatus.PENDING))
                    }
                    is AiService.StreamEvent.Done -> {
                        if (followUpContent.isNotEmpty() || followUpToolCalls.isNotEmpty()) {
                            _messages.value = _messages.value + Message(
                                id = UUID.randomUUID().toString(),
                                role = Message.Role.ASSISTANT,
                                content = followUpContent.toString(),
                                toolCalls = followUpToolCalls.toList()
                            )
                            if (followUpToolCalls.isNotEmpty()) {
                                viewModelScope.launch { executeToolCalls(followUpToolCalls) }
                            }
                        }
                    }
                    is AiService.StreamEvent.Error -> {
                        _messages.value = _messages.value + Message(
                            id = UUID.randomUUID().toString(),
                            role = Message.Role.ASSISTANT,
                            content = "Error: ${event.message}"
                        )
                    }
                }
            }
        }
    }

    private fun updateToolCallStatus(id: String, status: ToolCall.ToolStatus, result: String? = null) {
        _messages.value = _messages.value.map { msg ->
            msg.copy(
                toolCalls = msg.toolCalls.map { tc ->
                    if (tc.id == id) tc.copy(status = status, result = result ?: tc.result) else tc
                }
            )
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}
