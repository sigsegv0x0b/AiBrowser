package com.aibrowser.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibrowser.data.models.Message
import com.aibrowser.data.models.ToolCall
import com.google.gson.Gson
import com.google.gson.JsonElement
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

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val gson = Gson()

    companion object {
        private val SYSTEM_PROMPT = Message(
            id = "system",
            role = Message.Role.SYSTEM,
            content = "You are a browser automation assistant. You have access to browser tools to help the user complete tasks. " +
                "For every new page, first use browser_snapshot to understand the page content. " +
                "After receiving tool results, always continue with the next action or provide a response to the user. " +
                "Do not stop after a single tool call - keep analyzing and acting until the task is complete."
        )

        fun jsonToAny(element: JsonElement): Any? {
            return when {
                element.isJsonNull -> null
                element.isJsonPrimitive -> {
                    val prim = element.asJsonPrimitive
                    when {
                        prim.isBoolean -> prim.asBoolean
                        prim.isNumber -> {
                            val num = prim.asNumber
                            val double = num.toDouble()
                            if (double == double.toLong().toDouble()) num.toLong() else double
                        }
                        else -> prim.asString
                    }
                }
                element.isJsonArray -> element.asJsonArray.map { jsonToAny(it) }
                element.isJsonObject -> element.asJsonObject.entrySet().associate { it.key to jsonToAny(it.value) }
                else -> null
            }
        }
    }

    init {
        _messages.value = listOf(SYSTEM_PROMPT)
    }

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
            val assistantId = UUID.randomUUID().toString()

            aiService.sendMessage(_messages.value) { event ->
                when (event) {
                    is AiService.StreamEvent.Token -> {
                        assistantContent.append(event.text)
                        upsertAssistantMessage(assistantId, assistantContent.toString(), toolCalls)
                    }
                    is AiService.StreamEvent.ToolCallStart -> {
                        val args = try {
                            gson.fromJson(event.args, JsonObject::class.java)
                                .entrySet().mapNotNull { e ->
                                    val v = jsonToAny(e.value)
                                    if (v != null) e.key to v else null
                                }.toMap()
                        } catch (_: Exception) { emptyMap() }
                        toolCalls.add(
                            ToolCall(event.id, event.name, args, ToolCall.ToolStatus.PENDING)
                        )
                        upsertAssistantMessage(assistantId, assistantContent.toString(), toolCalls)
                    }
                    is AiService.StreamEvent.Done -> {
                        if (assistantContent.isNotEmpty() || toolCalls.isNotEmpty()) {
                            upsertAssistantMessage(assistantId, assistantContent.toString(), toolCalls)

                            if (toolCalls.isNotEmpty()) {
                                viewModelScope.launch { executeToolCalls(toolCalls) }
                            }
                        }
                    }
                    is AiService.StreamEvent.Error -> {
                        upsertAssistantMessage(assistantId, assistantContent.toString().ifEmpty {
                            "Error: ${event.message}"
                        }, toolCalls)
                    }
                }
            }

            _isLoading.value = false
        }
    }

    private fun upsertAssistantMessage(id: String, content: String, toolCalls: List<ToolCall>) {
        val existing = _messages.value.indexOfFirst { it.id == id }
        val msg = Message(id = id, role = Message.Role.ASSISTANT, content = content, toolCalls = toolCalls)
        if (existing >= 0) {
            _messages.value = _messages.value.toMutableList().apply { set(existing, msg) }
        } else {
            _messages.value = _messages.value + msg
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
                content = result.result ?: "No result",
                toolCallId = toolCall.id
            )
        }

        if (toolCalls.isNotEmpty() && !_isPaused.value) {
            sendFollowUp()
        } else if (toolCalls.isNotEmpty()) {
            _isLoading.value = false
        }
    }

    fun pause() {
        _isPaused.value = true
    }

    fun resume(content: String) {
        if (content.isBlank()) return
        _isPaused.value = false
        _isLoading.value = true
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            role = Message.Role.USER,
            content = content
        )
        _messages.value = _messages.value + userMessage
        viewModelScope.launch { sendFollowUp() }
    }

    private suspend fun sendFollowUp() {
        val followUpContent = StringBuilder()
        val followUpToolCalls = mutableListOf<ToolCall>()
        val followUpId = UUID.randomUUID().toString()

        aiService.sendMessage(_messages.value) { event ->
            when (event) {
                is AiService.StreamEvent.Token -> {
                    followUpContent.append(event.text)
                    upsertAssistantMessage(followUpId, followUpContent.toString(), followUpToolCalls)
                }
                is AiService.StreamEvent.ToolCallStart -> {
                    val args = try {
                        gson.fromJson(event.args, JsonObject::class.java)
                            .entrySet().mapNotNull { e ->
                                val v = jsonToAny(e.value)
                                if (v != null) e.key to v else null
                            }.toMap()
                    } catch (_: Exception) { emptyMap() }
                    followUpToolCalls.add(ToolCall(event.id, event.name, args, ToolCall.ToolStatus.PENDING))
                    upsertAssistantMessage(followUpId, followUpContent.toString(), followUpToolCalls)
                }
                is AiService.StreamEvent.Done -> {
                    if (followUpContent.isNotEmpty()) {
                        upsertAssistantMessage(followUpId, followUpContent.toString(), followUpToolCalls)
                    } else if (followUpToolCalls.isNotEmpty()) {
                        upsertAssistantMessage(followUpId, "", followUpToolCalls)
                    } else {
                        upsertAssistantMessage(followUpId, "(no response)", followUpToolCalls)
                    }
                    if (followUpToolCalls.isNotEmpty() && !_isPaused.value) {
                        viewModelScope.launch { executeToolCalls(followUpToolCalls) }
                    } else {
                        if (!_isPaused.value) _isLoading.value = false
                    }
                }
                is AiService.StreamEvent.Error -> {
                    upsertAssistantMessage(followUpId, followUpContent.toString().ifEmpty {
                        "Error: ${event.message}"
                    }, followUpToolCalls)
                    _isLoading.value = false
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
        _messages.value = listOf(SYSTEM_PROMPT)
    }
}
