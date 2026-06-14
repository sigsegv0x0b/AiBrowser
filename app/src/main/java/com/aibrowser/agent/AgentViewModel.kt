package com.aibrowser.agent

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibrowser.browser.TabManager
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.BehaviorConfig
import com.aibrowser.data.models.Message
import com.aibrowser.data.models.ToolCall
import com.aibrowser.service.AgentForegroundService
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val aiService: AiService,
    private val mcpController: McpController,
    private val settingsRepository: SettingsRepository,
    private val tabManager: TabManager,
    @ApplicationContext private val context: Context
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

    private var currentTabId: String? = null
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
            tabManager.activeTabId.collect { newTabId ->
                if (newTabId != null && newTabId != currentTabId) {
                    saveToTab(currentTabId)
                    currentTabId = newTabId
                    loadFromTab(newTabId)
                }
            }
        }

        viewModelScope.launch {
            combine(_isLoading, _currentAction) { loading, action ->
                loading to action
            }.collect { (loading, action) ->
                updateForegroundService(loading, action)
            }
        }
    }

    fun sendMessage(content: String) {
        val tabId = currentTabId ?: return
        if (content.isBlank() || _isLoading.value) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            role = Message.Role.USER,
            content = content
        )
        tabAppendMessage(tabId, userMessage)
        _actionHistory.value = emptyList()
        _currentAction.value = "Thinking..."
        _isLoading.value = true
        saveToTab(tabId)

        viewModelScope.launch {
            val assistantContent = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()
            val assistantId = UUID.randomUUID().toString()

            val messages = tabManager.getTab(tabId)?.messages ?: return@launch
            aiService.sendMessage(messages) { event ->
                when (event) {
                    is AiService.StreamEvent.Token -> {
                        assistantContent.append(event.text)
                        tabUpsertAssistantMessage(tabId, assistantId, assistantContent.toString(), toolCalls)
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
                        tabUpsertAssistantMessage(tabId, assistantId, assistantContent.toString(), toolCalls)
                    }
                    is AiService.StreamEvent.Done -> {
                        if (assistantContent.isNotEmpty() || toolCalls.isNotEmpty()) {
                            tabUpsertAssistantMessage(tabId, assistantId, assistantContent.toString(), toolCalls)

                            if (toolCalls.isNotEmpty()) {
                                viewModelScope.launch { executeToolCalls(tabId, toolCalls) }
                            } else {
                                tabSetLoading(tabId, false)
                            }
                        } else {
                            tabSetLoading(tabId, false)
                        }
                    }
                    is AiService.StreamEvent.Error -> {
                        val isNetworkError = event.message.startsWith("Network error")
                        val fallback = if (isNetworkError) "Network error — tap Resume to retry" else "Error: ${event.message}"
                        val msg = if (assistantContent.isNotEmpty()) assistantContent.toString() else fallback
                        tabUpsertAssistantMessage(tabId, assistantId, msg, toolCalls)
                        if (isNetworkError) {
                            tabSetPaused(tabId, true)
                            tabSetLoading(tabId, false)
                        } else {
                            tabSetLoading(tabId, false)
                        }
                    }
                }
            }
        }
    }

    private fun tabAppendMessage(tabId: String, message: Message) {
        tabManager.updateTab(tabId) { it.copy(messages = it.messages + message) }
        syncFlowsFromTab(tabId)
    }

    private fun tabUpsertAssistantMessage(tabId: String, id: String, content: String, toolCalls: List<ToolCall>) {
        tabManager.updateTab(tabId) { tab ->
            val existing = tab.messages.indexOfFirst { it.id == id }
            val msg = Message(id = id, role = Message.Role.ASSISTANT, content = content, toolCalls = toolCalls)
            val newMessages = if (existing >= 0) {
                tab.messages.toMutableList().apply { set(existing, msg) }
            } else {
                tab.messages + msg
            }
            tab.copy(messages = newMessages)
        }
        syncFlowsFromTab(tabId)
    }

    private fun tabAppendToolResult(tabId: String, content: String, toolCallId: String) {
        tabManager.updateTab(tabId) { tab ->
            tab.copy(messages = tab.messages + Message(
                id = UUID.randomUUID().toString(),
                role = Message.Role.TOOL,
                content = content,
                toolCallId = toolCallId
            ))
        }
        syncFlowsFromTab(tabId)
    }

    private fun tabUpdateToolCallStatus(tabId: String, id: String, status: ToolCall.ToolStatus, result: String? = null) {
        tabManager.updateTab(tabId) { tab ->
            tab.copy(messages = tab.messages.map { msg ->
                msg.copy(
                    toolCalls = msg.toolCalls.map { tc ->
                        if (tc.id == id) tc.copy(status = status, result = result ?: tc.result) else tc
                    }
                )
            })
        }
        syncFlowsFromTab(tabId)
    }

    private fun tabSetLoading(tabId: String, loading: Boolean) {
        tabManager.updateTab(tabId) { it.copy(agentIsLoading = loading) }
        if (tabId == currentTabId) _isLoading.value = loading
    }

    private fun tabSetPaused(tabId: String, paused: Boolean) {
        tabManager.updateTab(tabId) { it.copy(isPaused = paused) }
        if (tabId == currentTabId) _isPaused.value = paused
    }

    private fun syncFlowsFromTab(tabId: String) {
        if (tabId == currentTabId) {
            val tab = tabManager.getTab(tabId)
            if (tab != null) {
                _messages.value = tab.messages
                _isLoading.value = tab.agentIsLoading
                _currentAction.value = tab.currentAction
                _actionHistory.value = tab.actionHistory
                _isPaused.value = tab.isPaused
            }
        }
    }

    private fun tabSetCurrentAction(tabId: String, action: String) {
        tabManager.updateTab(tabId) { it.copy(currentAction = action) }
        if (tabId == currentTabId) _currentAction.value = action
    }

    private fun tabAppendActionHistory(tabId: String, action: String) {
        tabManager.updateTab(tabId) { it.copy(actionHistory = it.actionHistory + action) }
        if (tabId == currentTabId) {
            val tab = tabManager.getTab(tabId)
            if (tab != null) _actionHistory.value = tab.actionHistory
        }
    }

    private suspend fun executeToolCalls(tabId: String, toolCalls: List<ToolCall>) {
        for (toolCall in toolCalls) {
            val prevAction = tabManager.getTab(tabId)?.currentAction ?: ""
            if (prevAction.isNotBlank()) {
                tabAppendActionHistory(tabId, prevAction)
            }
            tabSetCurrentAction(tabId, describeToolCall(toolCall.name, toolCall.arguments))
            tabUpdateToolCallStatus(tabId, toolCall.id, ToolCall.ToolStatus.RUNNING)
            val result = mcpController.executeToolCall(toolCall)
            tabUpdateToolCallStatus(tabId, toolCall.id, result.status, result.result)
            tabAppendToolResult(tabId, result.result ?: "No result", toolCall.id)
        }

        val prevAction = tabManager.getTab(tabId)?.currentAction ?: ""
        if (prevAction.isNotBlank()) {
            tabAppendActionHistory(tabId, prevAction)
        }
        tabSetCurrentAction(tabId, "")
        val paused = tabManager.getTab(tabId)?.isPaused ?: false
        if (toolCalls.isNotEmpty() && !paused) {
            sendFollowUp(tabId)
        } else if (toolCalls.isNotEmpty()) {
            tabSetLoading(tabId, false)
        }
    }

    private suspend fun sendFollowUp(tabId: String) {
        val followUpContent = StringBuilder()
        val followUpToolCalls = mutableListOf<ToolCall>()
        val followUpId = UUID.randomUUID().toString()

        val messages = tabManager.getTab(tabId)?.messages ?: return
        aiService.sendMessage(messages) { event ->
            when (event) {
                is AiService.StreamEvent.Token -> {
                    followUpContent.append(event.text)
                    tabUpsertAssistantMessage(tabId, followUpId, followUpContent.toString(), followUpToolCalls)
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
                    tabUpsertAssistantMessage(tabId, followUpId, followUpContent.toString(), followUpToolCalls)
                }
                is AiService.StreamEvent.Done -> {
                    val finalContent = when {
                        followUpContent.isNotEmpty() -> followUpContent.toString()
                        followUpToolCalls.isNotEmpty() -> ""
                        else -> "(no response)"
                    }
                    tabUpsertAssistantMessage(tabId, followUpId, finalContent, followUpToolCalls)
                    if (followUpToolCalls.isNotEmpty()) {
                        val paused = tabManager.getTab(tabId)?.isPaused ?: false
                        if (!paused) {
                            viewModelScope.launch { executeToolCalls(tabId, followUpToolCalls) }
                        } else {
                            tabSetLoading(tabId, false)
                        }
                    } else {
                        tabSetLoading(tabId, false)
                    }
                }
                is AiService.StreamEvent.Error -> {
                    val isNetworkError = event.message.startsWith("Network error")
                    val fallback = if (isNetworkError) "Network error — tap Resume to retry" else "Error: ${event.message}"
                    val msg = if (followUpContent.isNotEmpty()) followUpContent.toString() else fallback
                    tabUpsertAssistantMessage(tabId, followUpId, msg, followUpToolCalls)
                    if (isNetworkError) {
                        tabSetPaused(tabId, true)
                        tabSetLoading(tabId, false)
                    } else {
                        tabSetLoading(tabId, false)
                    }
                }
            }
        }
    }

    fun pause() {
        val tabId = currentTabId ?: return
        _isPaused.value = true
        tabSetPaused(tabId, true)
    }

    fun resume(content: String) {
        val tabId = currentTabId ?: return
        if (content.isBlank()) return
        _isPaused.value = false
        _isLoading.value = true
        tabSetPaused(tabId, false)
        tabSetLoading(tabId, true)

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            role = Message.Role.USER,
            content = content
        )
        tabAppendMessage(tabId, userMessage)
        viewModelScope.launch { sendFollowUp(tabId) }
    }

    fun clearChat() {
        val tabId = currentTabId ?: return
        viewModelScope.launch {
            val config = settingsRepository.behaviorConfig.first()
            systemPromptContent = config.systemPrompt
            ttsPromptContent = config.ttsPrompt
            val freshMessages = listOf(createSystemMessage())
            _messages.value = freshMessages
            _isLoading.value = false
            _currentAction.value = ""
            _actionHistory.value = emptyList()
            _isPaused.value = false
            tabManager.updateTab(tabId) { it.copy(
                messages = freshMessages,
                agentIsLoading = false,
                currentAction = "",
                actionHistory = emptyList(),
                isPaused = false
            )}
        }
    }

    private fun saveToTab(tabId: String?) {
        val id = tabId ?: return
        tabManager.updateTab(id) { it.copy(
            messages = _messages.value,
            agentIsLoading = _isLoading.value,
            currentAction = _currentAction.value,
            actionHistory = _actionHistory.value,
            isPaused = _isPaused.value
        )}
    }

    private fun loadFromTab(tabId: String) {
        val tab = tabManager.getTab(tabId) ?: return
        if (tab.messages.isEmpty()) {
            viewModelScope.launch {
                val config = settingsRepository.behaviorConfig.first()
                systemPromptContent = config.systemPrompt
                ttsPromptContent = config.ttsPrompt
                val freshMessages = listOf(createSystemMessage())
                _messages.value = freshMessages
                _isLoading.value = tab.agentIsLoading
                _currentAction.value = tab.currentAction
                _actionHistory.value = tab.actionHistory
                _isPaused.value = tab.isPaused
                tabManager.updateTab(tabId) { it.copy(messages = freshMessages) }
            }
        } else {
            _messages.value = tab.messages
            _isLoading.value = tab.agentIsLoading
            _currentAction.value = tab.currentAction
            _actionHistory.value = tab.actionHistory
            _isPaused.value = tab.isPaused
            systemPromptContent = tab.messages.firstOrNull { it.role == Message.Role.SYSTEM }?.content
                ?: BehaviorConfig.DEFAULT_SYSTEM_PROMPT
            ttsPromptContent = BehaviorConfig.DEFAULT_TTS_PROMPT
        }
    }

    private fun updateForegroundService(isLoading: Boolean, action: String = "") {
        val intent = Intent(context, AgentForegroundService::class.java).apply {
            putExtra(AgentForegroundService.EXTRA_ACTION, action.ifBlank { "Working..." })
        }
        if (isLoading) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
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
