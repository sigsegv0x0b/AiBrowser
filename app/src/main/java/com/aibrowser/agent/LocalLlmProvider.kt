package com.aibrowser.agent

import android.content.Context
import com.aibrowser.agent.AiService.StreamEvent
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.LocalLlmConfig
import com.aibrowser.data.models.Message
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLlmProvider @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val perfManager: AppPerformanceManager,
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null
    private var activeBackend: LocalLlmConfig.Backend? = null
    private val gson = Gson()

    suspend fun sendMessage(
        messages: List<Message>,
        onEvent: (StreamEvent) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val config = settingsRepository.localLlmConfig.first()

        if (!config.isModelReady) {
            onEvent(StreamEvent.Error("No local model downloaded. Go to Settings to download one."))
            return@withContext ""
        }

        try {
            perfManager.startPerformanceSession(context, 120_000)
            val engineInstance = getOrCreateEngine(config) { status ->
                onEvent(StreamEvent.Status(status))
            }
            perfManager.stopPerformanceSession()
            onEvent(StreamEvent.Status(""))
            val systemMessage = messages.firstOrNull { it.role == Message.Role.SYSTEM }
            val systemContents = systemMessage?.let { Contents.of(it.content) }

            val historyMessages = messages
                .dropWhile { it.role == Message.Role.SYSTEM }

            val lastUserIndex = historyMessages.indexOfLast { it.role == Message.Role.USER }
            if (lastUserIndex < 0) {
                onEvent(StreamEvent.Error("No user message to send"))
                return@withContext ""
            }

            val toolCallNamesById = buildToolCallNamesById(historyMessages)

            val (initialMessages, userInput) = prepareContext(
                historyMessages, lastUserIndex, toolCallNamesById
            )

            val tokenLimit = config.maxTokens.takeIf { it > 0 } ?: 16384
            val safeBudget = tokenLimit - 512
            val finalInitialMessages = truncateToFit(initialMessages, systemContents, userInput, safeBudget)

            val convConfig = ConversationConfig(
                systemInstruction = systemContents,
                initialMessages = finalInitialMessages,
                tools = listOf(com.google.ai.edge.litertlm.tool(BrowserToolSet())),
                automaticToolCalling = false
            )

            val conv = engineInstance.createConversation(convConfig)

            var fullResponse = ""
            val liteToolCalls = mutableListOf<com.google.ai.edge.litertlm.ToolCall>()
            var parseError = false
            try {
                conv.sendMessageAsync(userInput).collect { msg ->
                    val text = msg.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("") { it.text }
                    if (text.isNotEmpty()) {
                        fullResponse += text
                        onEvent(StreamEvent.Token(text))
                    }
                    if (msg.toolCalls.isNotEmpty()) {
                        liteToolCalls.clear()
                        liteToolCalls.addAll(msg.toolCalls)
                    }
                }
            } catch (e: Exception) {
                if (e.message?.contains("tool call", ignoreCase = true) == true ||
                    e.message?.contains("parse", ignoreCase = true) == true
                ) {
                    parseError = true
                } else {
                    throw e
                }
            } finally {
                conv.close()
            }

            if (parseError || (liteToolCalls.isEmpty() && fullResponse.contains("<|tool_call>"))) {
                val parsedToolCalls = parseToolCallsFromText(fullResponse)
                for (tc in parsedToolCalls) {
                    val argsJson = gson.toJson(tc.arguments)
                    val id = UUID.randomUUID().toString()
                    onEvent(StreamEvent.ToolCallStart(id, tc.name, argsJson))
                }
                if (parsedToolCalls.isNotEmpty()) {
                    fullResponse = stripToolCallMarkup(fullResponse)
                }
            } else if (liteToolCalls.isNotEmpty()) {
                for (tc in liteToolCalls) {
                    val argsJson = gson.toJson(tc.arguments)
                    val id = UUID.randomUUID().toString()
                    onEvent(StreamEvent.ToolCallStart(id, tc.name, argsJson))
                }
            }

            onEvent(StreamEvent.Done(fullResponse))
            fullResponse
        } catch (e: Exception) {
            val msg = e.message?.removePrefix("Status Code: 3. Message: ") ?: "Unknown error"
            onEvent(StreamEvent.Error(msg))
            close()
            ""
        }
    }

    private fun truncateToFit(
        messages: List<com.google.ai.edge.litertlm.Message>,
        system: Contents?,
        userInput: String,
        budget: Int
    ): List<com.google.ai.edge.litertlm.Message> {
        val systemTokens = system?.let { messageTokenEstimate(it.contents) } ?: 0
        var total = systemTokens + userInput.length / 2 + messages.size * 4
        val estimates = messages.map { msg -> messageTokenEstimate(msg) }
        for (est in estimates) total += est

        var truncated = messages
        for (est in estimates) {
            if (total <= budget) break
            total -= est + 4
            truncated = truncated.drop(1)
        }
        return truncated
    }

    private fun messageTokenEstimate(msg: com.google.ai.edge.litertlm.Message): Int {
        return messageTokenEstimate(msg.contents.contents) +
            msg.toolCalls.sumOf { tc ->
                (tc.name.length + gson.toJson(tc.arguments).length + 4) / 2
            }
    }

    private fun messageTokenEstimate(contents: List<com.google.ai.edge.litertlm.Content>): Int {
        return contents.joinToString("") { c ->
            when (c) {
                is com.google.ai.edge.litertlm.Content.Text -> c.text
                is com.google.ai.edge.litertlm.Content.ToolResponse -> "${c.name}: ${c.response}"
                else -> ""
            }
        }.length / 2
    }

    private data class ToolCallData(val name: String, val arguments: Map<String, Any>)

    private fun parseToolCallsFromText(text: String): List<ToolCallData> {
        val toolCalls = mutableListOf<ToolCallData>()
        val regex = Regex("<\\|tool_call>(.*?)<tool_call\\|>", RegexOption.DOT_MATCHES_ALL)
        for (match in regex.findAll(text)) {
            val inner = match.groupValues[1].trim()
            val funcName = inner.substringBefore("{").removePrefix("call:").trim()
            val argsStr = inner.substringAfter("{").substringBeforeLast("}")
            val arguments = mutableMapOf<String, Any>()
            val pairs = splitArgPairs(argsStr)
            for (pair in pairs) {
                val colonIdx = pair.indexOf(':')
                if (colonIdx < 0) continue
                val key = pair.substring(0, colonIdx)
                val rawVal = pair.substring(colonIdx + 1)
                val value = rawVal
                    .replace("<|\"|>", "")
                    .replace("<|\\\\\"|>", "")
                    .trim()
                arguments[key] = value
            }
            toolCalls.add(ToolCallData(funcName, arguments))
        }
        return toolCalls
    }

    private fun splitArgPairs(argsStr: String): List<String> {
        val pairs = mutableListOf<String>()
        var pos = 0
        var depth = 0
        val current = StringBuilder()
        while (pos < argsStr.length) {
            val c = argsStr[pos]
            if (c == '<' && argsStr.startsWith("<|", pos)) {
                // Enter quote marker, skip to end
                val endIdx = argsStr.indexOf("|>", pos + 2)
                if (endIdx >= 0) {
                    current.append(argsStr.substring(pos, endIdx + 2))
                    pos = endIdx + 1
                } else {
                    current.append(c)
                }
            } else if (c == '{') {
                depth++
                current.append(c)
            } else if (c == '}') {
                depth--
                current.append(c)
            } else if (c == ',' && depth == 0) {
                pairs.add(current.toString())
                current.clear()
            } else {
                current.append(c)
            }
            pos++
        }
        if (current.isNotEmpty()) pairs.add(current.toString())
        return pairs
    }

    private fun stripToolCallMarkup(text: String): String {
        return text.replace(Regex("<\\|tool_call>.*?<tool_call\\|>", RegexOption.DOT_MATCHES_ALL), "")
    }

    private fun buildToolCallNamesById(
        historyMessages: List<Message>
    ): Map<String, String> {
        return historyMessages
            .filter { it.role == Message.Role.ASSISTANT }
            .flatMap { msg -> msg.toolCalls.map { it.id to it.name } }
            .toMap()
    }

    private fun prepareContext(
        historyMessages: List<Message>,
        lastUserIndex: Int,
        toolCallNamesById: Map<String, String>
    ): Pair<List<com.google.ai.edge.litertlm.Message>, String> {
        val hasToolResultsAfterLastUser = historyMessages
            .drop(lastUserIndex + 1)
            .any { it.role == Message.Role.TOOL }

        if (hasToolResultsAfterLastUser) {
            val allLiteMsgs = historyMessages.mapNotNull { msg ->
                msg.toLiteMessage(toolCallNamesById)
            }
            return allLiteMsgs to ""
        }

        val initialMessages = historyMessages
            .take(lastUserIndex)
            .mapNotNull { msg -> msg.toLiteMessage(toolCallNamesById) }
        val userInput = historyMessages[lastUserIndex].content

        return initialMessages to userInput
    }

    private fun Message.toLiteMessage(
        toolCallNamesById: Map<String, String>
    ): com.google.ai.edge.litertlm.Message? {
        return when (role) {
            Message.Role.USER ->
                com.google.ai.edge.litertlm.Message.user(content)

            Message.Role.ASSISTANT -> {
                if (toolCalls.isNotEmpty()) {
                    val liteToolCalls = toolCalls.map {
                        com.google.ai.edge.litertlm.ToolCall(it.name, it.arguments)
                    }
                    com.google.ai.edge.litertlm.Message.model(
                        Contents.of(content), liteToolCalls
                    )
                } else {
                    com.google.ai.edge.litertlm.Message.model(content)
                }
            }

            Message.Role.TOOL -> {
                val toolName = toolCallNamesById[toolCallId] ?: "unknown"
                val contents = Contents.of(
                    listOf(com.google.ai.edge.litertlm.Content.ToolResponse(toolName, content))
                )
                com.google.ai.edge.litertlm.Message.tool(contents)
            }

            else -> null
        }
    }

    private fun getOrCreateEngine(
        config: LocalLlmConfig,
        onStatus: ((String) -> Unit)? = null
    ): Engine {
        val current = engine
        if (current != null && current.isInitialized()) return current

        current?.close()
        engine = null

        val backendsToTry = when (config.backend) {
            LocalLlmConfig.Backend.NPU -> listOf(Backend.NPU())
            LocalLlmConfig.Backend.GPU -> listOf(Backend.GPU())
            LocalLlmConfig.Backend.CPU -> listOf(Backend.CPU())
            LocalLlmConfig.Backend.AUTO -> listOf(Backend.NPU(), Backend.GPU(), Backend.CPU())
        }

        var lastError: Exception? = null
        for (backend in backendsToTry) {
            try {
                val displayName = when (backend) {
                    is Backend.NPU -> "NPU"
                    is Backend.GPU -> "GPU"
                    is Backend.CPU -> "CPU"
                }
                onStatus?.invoke("Trying $displayName backend...")
                val newEngine = Engine(
                    EngineConfig(
                        modelPath = config.downloadedModelPath,
                        backend = backend,
                        maxNumTokens = config.maxTokens.takeIf { it > 0 } ?: 16384,
                        cacheDir = config.downloadedModelPath.substringBeforeLast("/")
                    )
                )
                newEngine.initialize()
                engine = newEngine
                activeBackend = when (backend) {
                    is Backend.NPU -> LocalLlmConfig.Backend.NPU
                    is Backend.GPU -> LocalLlmConfig.Backend.GPU
                    is Backend.CPU -> LocalLlmConfig.Backend.CPU
                }
                onStatus?.invoke("$displayName ready")
                return newEngine
            } catch (e: Exception) {
                lastError = e
            }
        }

        throw lastError ?: RuntimeException("Failed to initialize engine")
    }

    suspend fun testInference(): TestResult = withContext(Dispatchers.IO) {
        val config = settingsRepository.localLlmConfig.first()

        if (!config.isModelReady) {
            return@withContext TestResult("No model downloaded", "none")
        }

        try {
            val engineInstance = getOrCreateEngine(config)
            val conv = engineInstance.createConversation(ConversationConfig())
            try {
                var response = ""
                conv.sendMessageAsync("hello").collect { msg ->
                    val text = msg.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("") { it.text }
                    response += text
                }
                TestResult(response, activeBackend?.displayName ?: "Unknown")
            } finally {
                conv.close()
            }
        } catch (e: Exception) {
            TestResult("Error: ${e.message}", activeBackend?.displayName ?: "Unknown")
        }
    }

    fun close() {
        engine?.close()
        engine = null
    }
}

data class TestResult(val response: String, val backend: String)
