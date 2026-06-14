package com.aibrowser.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.BehaviorConfig
import com.aibrowser.data.models.Message
import com.aibrowser.data.models.ToolCall
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val aiService: AiService,
    private val mcpController: McpController,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _currentAction = MutableStateFlow("")
    val currentAction: StateFlow<String> = _currentAction.asStateFlow()

    private val _actionHistory = MutableStateFlow<List<String>>(emptyList())
    val actionHistory: StateFlow<List<String>> = _actionHistory.asStateFlow()

    private val gson = Gson()

    private var systemPromptContent: String = BehaviorConfig.DEFAULT_SYSTEM_PROMPT
    private var ttsPromptContent: String = BehaviorConfig.DEFAULT_TTS_PROMPT

    private fun createSystemMessage(): Message = Message(
        id = "system",
        role = Message.Role.SYSTEM,
        content = systemPromptContent
    )

    companion object {
        fun describeToolCall(name: String, args: Map<String, Any>): String {
            return when (name) {
                "browser_navigate" -> "Navigating to ${args["url"] ?: "page"}"
                "browser_click" -> "Clicking element ${args.getOrElse("element") { "" }}"
                "browser_type" -> "Typing text"
                "browser_snapshot" -> "Taking snapshot"
                "browser_evaluate" -> "Running JavaScript"
                "browser_wait_for" -> "Waiting for page to load"
                "browser_select" -> "Selecting option"
                "browser_hover" -> "Hovering element"
                "browser_drag" -> "Dragging element"
                "browser_handle_dialog" -> "Handling dialog"
                "browser_console_messages" -> "Reading console"
                "browser_resize" -> "Resizing viewport"
                "browser_refresh" -> "Refreshing page"
                "browser_go_back" -> "Going back"
                "browser_go_forward" -> "Going forward"
                else -> "Executing $name"
            }
        }

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
        viewModelScope.launch {
            val config = settingsRepository.behaviorConfig.first()
            systemPromptContent = config.systemPrompt
            ttsPromptContent = config.ttsPrompt
            _messages.value = listOf(createSystemMessage())
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isLoading.value) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            role = Message.Role.USER,
            content = content
        )
        _messages.value = _messages.value + userMessage
        _actionHistory.value = emptyList()
        _currentAction.value = "Thinking..."
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
                            } else {
                                _isLoading.value = false
                            }
                        } else {
                            _isLoading.value = false
                        }
                    }
                    is AiService.StreamEvent.Error -> {
                        val isNetworkError = event.message.startsWith("Network error")
                        upsertAssistantMessage(assistantId, assistantContent.toString().ifEmpty {
                            if (isNetworkError) "Network error — tap Resume to retry" else "Error: ${event.message}"
                        }, toolCalls)
                        if (isNetworkError) {
                            _isPaused.value = true
                            _isLoading.value = false
                        } else {
                            _isLoading.value = false
                        }
                    }
                }
            }
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
            if (_currentAction.value.isNotBlank()) {
                _actionHistory.value = _actionHistory.value + _currentAction.value
            }
            _currentAction.value = describeToolCall(toolCall.name, toolCall.arguments)
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

        if (_currentAction.value.isNotBlank()) {
            _actionHistory.value = _actionHistory.value + _currentAction.value
        }
        _currentAction.value = ""
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
                    val isNetworkError = event.message.startsWith("Network error")
                    upsertAssistantMessage(followUpId, followUpContent.toString().ifEmpty {
                        if (isNetworkError) "Network error — tap Resume to retry" else "Error: ${event.message}"
                    }, followUpToolCalls)
                    if (isNetworkError) {
                        _isPaused.value = true
                        _isLoading.value = false
                    } else {
                        _isLoading.value = false
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
        viewModelScope.launch {
            val config = settingsRepository.behaviorConfig.first()
            systemPromptContent = config.systemPrompt
            ttsPromptContent = config.ttsPrompt
            _messages.value = listOf(createSystemMessage())
        }
    }

    suspend fun generateTtsText(text: String): String {
        if (text.isBlank()) return text
        val prompt = ttsPromptContent.ifBlank { BehaviorConfig.DEFAULT_TTS_PROMPT }
        val ttsSystem = Message(
            id = "tts_sys",
            role = Message.Role.SYSTEM,
            content = prompt
        )
        val ttsMsg = Message(
            id = UUID.randomUUID().toString(),
            role = Message.Role.USER,
            content = text
        )
        val result = StringBuilder()
        aiService.sendMessage(listOf(ttsSystem, ttsMsg)) { event ->
            when (event) {
                is AiService.StreamEvent.Token -> result.append(event.text)
                else -> {}
            }
        }
        return result.toString().ifBlank { text }
    }
}
