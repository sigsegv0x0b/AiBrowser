package com.aibrowser.agent

import android.util.Log
import com.aibrowser.agent.mnn.MnnSession
import com.aibrowser.agent.mnn.MnnSession.MnnSettings
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.Message
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MnnLlmProvider @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val gson = Gson()
    @Volatile private var session: MnnSession? = null
    @Volatile private var currentModelPath: String? = null
    private var keepAliveJob: Job? = null
    private var idleSince: Long = 0L

    suspend fun sendMessage(
        messages: List<Message>,
        onEvent: (AiService.StreamEvent) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val modelPath = settingsRepository.mnnModelPath.first()
        if (modelPath.isBlank()) {
            onEvent(AiService.StreamEvent.Error("MNN model not configured. Go to Settings."))
            return@withContext ""
        }

        val settings = loadSettings()
        ensureSession(modelPath, settings)
        pauseKeepAlive()

        val prompt = buildPrompt(messages, session?.modelType ?: "", session?.isClaudeDistilled ?: false)
        var fullResponse = ""
        var toolCalls = false

        try {
            onEvent(AiService.StreamEvent.Status("Generating..."))
            val modelType = session?.modelType ?: ""
            val isLfm = modelType.equals("lfm2", ignoreCase = true)
            val isDistilled = session?.isClaudeDistilled ?: false
            val parser = ToolCallParser(startInThink = !isLfm && !isDistilled)

            session!!.generate(prompt, object : com.aibrowser.agent.mnn.GenerateProgressListener {
                override fun onProgress(token: String?): Boolean {
                    if (token != null) {
                        val result = parser.feed(token)
                        when (result) {
                            is ParserResult.Thinking -> {} // buffered internally
                            is ParserResult.ToolCall -> {
                                toolCalls = true
                                onEvent(AiService.StreamEvent.ToolCallStart(
                                    result.id, result.name, result.args
                                ))
                            }
                            is ParserResult.Text -> {
                                fullResponse += result.text
                                onEvent(AiService.StreamEvent.Token(result.text))
                            }
                            null -> {} // buffering
                        }
                    }
                    return false
                }
            })

            val thinking = parser.getThinking()
            onEvent(AiService.StreamEvent.Done(fullResponse, thinking))
        } catch (e: Exception) {
            Log.e(TAG, "Inference error", e)
            onEvent(AiService.StreamEvent.Error("MNN error: ${e.message}"))
        } catch (e: Error) {
            Log.e(TAG, "Fatal native error, releasing session: ${e.message}", e)
            try { releaseSession() } catch (_: Exception) {}
            onEvent(AiService.StreamEvent.Error("MNN crashed: ${e.message}. Model session released — try again."))
        }

        resumeKeepAlive()
        fullResponse
    }

    private suspend fun ensureSession(modelPath: String, settings: MnnSettings) {
        if (session != null && currentModelPath == modelPath && session!!.isLoaded) return
        releaseSession()
        try {
            session = MnnSession(modelDir = modelPath).also { it.load(settings) }
            currentModelPath = modelPath
        } catch (e: Error) {
            Log.e(TAG, "Native crash during model load, session released: ${e.message}")
            session = null
            currentModelPath = null
            throw IllegalStateException("MNN model load crashed: ${e.message}. Try disabling mmap or check model files.", e)
        }
    }

    private suspend fun loadSettings(): MnnSettings {
        return MnnSettings(
            useMmap = settingsRepository.mnnUseMmap.first(),
            promptCache = settingsRepository.mnnPromptCache.first(),
            temperature = settingsRepository.mnnTemperature.first(),
            topP = settingsRepository.mnnTopP.first(),
            topK = settingsRepository.mnnTopK.first(),
            precision = settingsRepository.mnnPrecision.first(),
            threads = settingsRepository.mnnThreads.first(),
            maxTokens = settingsRepository.mnnMaxTokens.first()
        )
    }

    fun releaseSession() {
        keepAliveJob?.cancel()
        session?.release()
        session = null
        currentModelPath = null
    }

    private fun pauseKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }

    fun resumeKeepAlive() {
        keepAliveJob?.cancel()
        idleSince = System.currentTimeMillis()
        keepAliveJob = CoroutineScope(Dispatchers.IO).launch {
            delay(300_000) // 5 minutes
            val elapsed = System.currentTimeMillis() - idleSince
            if (elapsed >= 290_000) {
                Log.d(TAG, "Session idle for ${elapsed/1000}s, releasing")
                releaseSession()
            }
        }
    }

    fun keepAlive() {
        val s = session
        if (s != null && s.isLoaded) {
            resumeKeepAlive()
        }
    }

    suspend fun streamTest(prompt: String, onToken: (String) -> Unit): String = withContext(Dispatchers.IO) {
        val modelPath = settingsRepository.mnnModelPath.first()
        if (modelPath.isBlank()) throw IllegalStateException("No MNN model configured")

        val settings = loadSettings()
        val modelDir = java.io.File(modelPath)
        if (!modelDir.exists() || !modelDir.isDirectory) throw IllegalStateException("Model directory not found: $modelPath")
        val configFile = java.io.File(modelDir, "config.json")
        if (!configFile.exists()) throw IllegalStateException("config.json not found in $modelPath")

        var testSession: MnnSession? = null
        try {
            testSession = MnnSession(modelDir = modelPath)
            testSession.load(settings)
            var fullResponse = ""
            testSession.generate(prompt, object : com.aibrowser.agent.mnn.GenerateProgressListener {
                override fun onProgress(token: String?): Boolean {
                    if (token != null) {
                        fullResponse += token
                        onToken(token)
                    }
                    return false
                }
            })
            fullResponse
        } catch (e: Throwable) {
            val diag = modelDiagnostics(modelDir)
            throw Exception("${e.message}\n\nModel files:\n$diag", e)
        } finally {
            try { testSession?.release() } catch (_: Exception) {}
        }
    }

    suspend fun testInference(): String = withContext(Dispatchers.IO) {
        val modelPath = settingsRepository.mnnModelPath.first()
        if (modelPath.isBlank()) return@withContext "No MNN model configured"

        val settings = loadSettings()
        val testSession = MnnSession(modelDir = modelPath)
        return@withContext try {
            testSession.load(settings)
            var output = ""
            testSession.generate("Hello", object : com.aibrowser.agent.mnn.GenerateProgressListener {
                override fun onProgress(token: String?): Boolean {
                    if (token != null) output += token
                    return output.length > 50
                }
            })
            testSession.release()
            "OK (${output.length} chars)"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun buildPrompt(messages: List<Message>, modelType: String, isClaudeDistilled: Boolean): String {
        val isLfm = modelType.equals("lfm2", ignoreCase = true)
        val isDistilled = isClaudeDistilled
        val hasTools = true

        // Find index of last USER message (for thinking rendering in assistant history)
        var lastUserIdx = -1
        for (i in messages.indices) {
            if (messages[i].role == Message.Role.USER) lastUserIdx = i
        }

        val sb = StringBuilder()
        var hasSystem = false

        var i = 0
        while (i < messages.size) {
            val msg = messages[i]
            when (msg.role) {
                Message.Role.SYSTEM -> {
                    sb.append("<|im_start|>system\n")
                    if (hasTools) {
                        if (isLfm) {
                            sb.append("List of tools: [")
                            val tools = ToolDefinitions.getToolsForApi()
                            tools.forEachIndexed { idx, tool ->
                                sb.append(gson.toJson(tool))
                                if (idx < tools.size - 1) sb.append(", ")
                            }
                            sb.append("]\n\n")
                            sb.append("When you need to call a function, respond with a JSON object: {\"name\": \"function_name\", \"arguments\": {...}}. Do not include any other text when calling a function.\n\n")
                        } else if (isDistilled) {
                            sb.append("# Tools\n\nYou may call one or more functions to assist with the user query.\n\n")
                            sb.append("You are provided with function signatures within <tools></tools> XML tags:\n<tools>\n")
                            for (tool in ToolDefinitions.getToolsForApi()) {
                                sb.append(gson.toJson(tool)).append("\n")
                            }
                            sb.append("</tools>\n\n")
                            sb.append("For each function call, return a json object with function name and arguments within <tool_call> XML tags:\n")
                            sb.append("<tool_call>\n{\"name\": <function-name>, \"arguments\": <args-json-object>}\n</tool_call>\n\n")
                        } else {
                            sb.append("# Tools\n\nYou have access to the following functions:\n\n<tools>\n")
                            for (tool in ToolDefinitions.getToolsForApi()) {
                                sb.append(gson.toJson(tool)).append("\n")
                            }
                            sb.append("</tools>\n\n")
                            sb.append("If you choose to call a function ONLY reply in the following format with NO suffix:\n\n")
                            sb.append("<tool_call>\n<function=example_function_name>\n<parameter=example_parameter_1>\nvalue_1\n</parameter>\n")
                            sb.append("<parameter=example_parameter_2>\nThis is the value for the second parameter\nthat can span\nmultiple lines\n</parameter>\n")
                            sb.append("</function>\n</tool_call>\n\n")
                            sb.append("<IMPORTANT>\nReminder:\n- Function calls MUST follow the specified format: an inner <function=...></function> block must be nested within <tool_call> XML tags\n- Required parameters MUST be specified\n- You may provide optional reasoning for your function call in natural language BEFORE the function call, but NOT after\n- If there is no function call available, answer the question like normal with your current knowledge and do not tell the user about function calls\n</IMPORTANT>\n\n")
                        }
                    }
                    sb.append(msg.content).append("<|im_end|>\n")
                    hasSystem = true
                }
                Message.Role.USER -> {
                    val content = msg.content
                    if (content.startsWith("<tool_response>") || msg.toolCallId != null) {
                        if (!content.startsWith("<tool_response>")) {
                            sb.append("<|im_start|>user\n<tool_response>\n").append(content).append("\n</tool_response><|im_end|>\n")
                        } else {
                            sb.append("<|im_start|>user\n").append(content).append("<|im_end|>\n")
                        }
                    } else {
                        sb.append("<|im_start|>user\n").append(content).append("<|im_end|>\n")
                    }
                }
                Message.Role.ASSISTANT -> {
                    sb.append("<|im_start|>assistant\n")
                    // Render thinking for assistant messages after the last user query
                    if (i > lastUserIdx && !msg.thinking.isNullOrBlank()) {
                        sb.append("<think>").append(msg.thinking).append("</think>\n\n")
                    }
                    if (msg.toolCalls.isNotEmpty()) {
                        if (isLfm) {
                            for (tc in msg.toolCalls) {
                                sb.append("{\"name\": \"").append(tc.name).append("\", \"arguments\": ")
                                sb.append(gson.toJson(tc.arguments)).append("}")
                            }
                        } else if (isDistilled) {
                            for (tc in msg.toolCalls) {
                                if (msg.content.isNotBlank()) sb.append("\n")
                                sb.append("<tool_call>\n{\"name\": \"").append(tc.name).append("\", \"arguments\": ")
                                sb.append(gson.toJson(tc.arguments)).append("}\n</tool_call>\n")
                            }
                        } else {
                            for (tc in msg.toolCalls) {
                                if (msg.content.isNotBlank()) sb.append("\n\n")
                                sb.append("<tool_call>\n<function=").append(tc.name).append(">\n")
                                for ((key, value) in tc.arguments) {
                                    sb.append("<parameter=").append(key).append(">\n")
                                    sb.append(value.toString()).append("\n")
                                    sb.append("</parameter>\n")
                                }
                                sb.append("</function>\n</tool_call>\n")
                            }
                        }
                    }
                    if (msg.content.isNotBlank()) {
                        sb.append(msg.content)
                    }
                    sb.append("<|im_end|>\n")
                }
                Message.Role.TOOL -> {
                    // Group consecutive TOOL messages into a single user block
                    sb.append("<|im_start|>user\n")
                    while (i < messages.size && messages[i].role == Message.Role.TOOL) {
                        sb.append("<tool_response>\n").append(messages[i].content).append("\n</tool_response>\n")
                        i++
                    }
                    sb.append("<|im_end|>\n")
                    continue // i already advanced
                }
            }
            i++
        }

        if (!hasSystem) {
            sb.insert(0, "<|im_start|>system\nYou are a browser automation assistant.<|im_end|>\n")
        }

        // Generation prompt: only force thinking for base Qwen (not distilled, not LFM2)
        sb.append("<|im_start|>assistant\n")
        if (!isLfm && !isDistilled) {
            sb.append("<think>\n")
        }
        return sb.toString()
    }

    private fun modelDiagnostics(dir: java.io.File): String {
        val sb = StringBuilder()
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        dir.walkTopDown().sortedBy { it.absolutePath }.forEach { file ->
            if (file.isFile) {
                val size = file.length()
                var sha = "?"
                try {
                    digest.reset()
                    file.inputStream().use { input ->
                        val buf = ByteArray(65536)
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            digest.update(buf, 0, n)
                        }
                    }
                    sha = digest.digest().joinToString("") { "%02x".format(it) }
                } catch (_: Exception) {}
                val sizeStr = when {
                    size >= 1_000_000_000 -> "%.2f GB".format(size / 1_000_000_000.0)
                    size >= 1_000_000 -> "%.1f MB".format(size / 1_000_000.0)
                    size >= 1_000 -> "%.1f KB".format(size / 1_000.0)
                    else -> "$size B"
                }
                sb.appendLine("  ${file.name}  $sizeStr  $sha")
            }
        }
        return sb.toString().trimEnd()
    }

    companion object {
        private const val TAG = "MnnLlmProvider"
    }
}

sealed class ParserResult {
    data class Thinking(val text: String) : ParserResult()
    data class Text(val text: String) : ParserResult()
    data class ToolCall(val id: String, val name: String, val args: String) : ParserResult()
}

class ToolCallParser(startInThink: Boolean = false) {
    private val buffer = StringBuilder()
    private val thinkBuffer = StringBuilder()
    var insideThink = startInThink
        private set
    var insideToolCall = false
        private set

    fun getThinking(): String? = thinkBuffer.toString().trim().ifEmpty { null }

    fun feed(token: String): ParserResult? {
        buffer.append(token)
        val text = buffer.toString()

        // Detect </think> end
        if (insideThink) {
            val thinkEnd = text.indexOf("</think>")
            if (thinkEnd >= 0) {
                thinkBuffer.append(text.substring(0, thinkEnd))
                insideThink = false
                buffer.clear()
                buffer.append(text.substring(thinkEnd + "</think>".length))
                return feed("")  // reprocess remainder
            }
            thinkBuffer.append(token)
            return null
        }

        // Detect <think> start
        val thinkStart = text.indexOf("<think>")
        if (thinkStart >= 0 && !insideToolCall) {
            val before = text.substring(0, thinkStart)
            val after = text.substring(thinkStart + "<think>".length)
            thinkBuffer.clear()
            insideThink = true
            buffer.clear()
            buffer.append(after)
            if (before.isNotBlank()) return ParserResult.Text(before)
            return feed("")  // reprocess remainder
        }

        if (!insideToolCall) {
            // Qwen format: <tool_call>...</tool_call>
            val tcIdx = text.indexOf("<tool_call>")
            val lfmStart = text.indexOf("<|tool_call_start|>")

            val startIdx: Int
            val startLen: Int
            val endTag: String
            if (lfmStart >= 0 && (tcIdx < 0 || lfmStart < tcIdx)) {
                startIdx = lfmStart
                startLen = "<|tool_call_start|>".length
                endTag = "<|tool_call_end|>"
            } else if (tcIdx >= 0) {
                startIdx = tcIdx
                startLen = "<tool_call>".length
                endTag = "</tool_call>"
            } else {
                // JSON function call (LFM2 without delimiters)
                val jsonIdx = text.indexOf("{\"name\":")
                if (jsonIdx >= 0) {
                    val result = tryParseJsonCall(text, jsonIdx)
                    if (result != null) return result
                }
                return ParserResult.Text(token)
            }

            insideToolCall = true
            buffer.clear()
            buffer.append(text.substring(startIdx + startLen))
            return null
        }

        // Inside tool call - look for completion
        val lfmEnd = text.indexOf("<|tool_call_end|>")
        val xmlEnd = text.indexOf("</tool_call>")

        if (lfmEnd >= 0 && (xmlEnd < 0 || lfmEnd < xmlEnd)) {
            val jsonBlock = text.substring(0, lfmEnd).trim()
            val result = parseLfmToolCall(jsonBlock)
            reset()
            return result ?: ParserResult.Text(text.substring(lfmEnd + "<|tool_call_end|>".length))
        }

        if (xmlEnd >= 0) {
            val block = text.substring(0, xmlEnd + "</tool_call>".length)
            val name = extractTag(block, "function")
            if (name != null) {
                val params = extractParameters(block)
                val id = "mnn_${System.currentTimeMillis()}"
                val json = Gson().toJson(params)
                reset()
                return ParserResult.ToolCall(id, name, json)
            }
            // Claude-distilled format: {"name": "...", "arguments": {...}} inside <tool_call>
            val inner = block.substringAfter("<tool_call>").substringBefore("</tool_call>").trim()
            if (inner.startsWith("{")) {
                val result = tryParseJsonCall(inner, 0)
                if (result != null) { reset(); return result }
            }
            reset()
            return null
        }

        return null
    }

    private fun parseLfmToolCall(jsonBlock: String): ParserResult.ToolCall? {
        return try {
            val gson = Gson()
            val arrType = object : com.google.gson.reflect.TypeToken<Array<Map<String, Any>>>() {}.type
            val arr: Array<Map<String, Any>> = gson.fromJson(jsonBlock, arrType)
            if (arr.isNotEmpty()) {
                val obj = arr[0]
                val name = obj["name"] as? String ?: return null
                val args = obj["arguments"] as? Map<*, *> ?: emptyMap<String, Any>()
                val id = "mnn_${System.currentTimeMillis()}"
                ParserResult.ToolCall(id, name, gson.toJson(args))
            } else null
        } catch (_: Exception) {
            tryParseJsonCall(jsonBlock, 0)
        }
    }

    private fun tryParseJsonCall(text: String, startIdx: Int): ParserResult.ToolCall? {
        try {
            val substr = text.substring(startIdx)
            val braceEnd = findMatchingBrace(substr)
            if (braceEnd < 0) return null
            val json = substr.substring(0, braceEnd + 1)
            val gson = Gson()
            val obj = gson.fromJson(json, Map::class.java) as? Map<*, *> ?: return null
            val name = obj["name"] as? String ?: return null
            val args = obj["arguments"] as? Map<*, *> ?: return null
            val id = "mnn_${System.currentTimeMillis()}"
            // Keep only from after the JSON in buffer
            val remaining = text.substring(startIdx + braceEnd + 1)
            buffer.clear()
            buffer.append(remaining)
            reset()
            return ParserResult.ToolCall(id, name, gson.toJson(args))
        } catch (_: Exception) {
            return null
        }
    }

    private fun findMatchingBrace(s: String): Int {
        var depth = 0
        for (i in s.indices) {
            when (s[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun reset() {
        buffer.clear()
        insideToolCall = false
    }

    private fun extractTag(text: String, tag: String): String? {
        val prefix = "<$tag="
        val start = text.indexOf(prefix)
        if (start < 0) return null
        val nameStart = start + prefix.length
        val nameEnd = text.indexOf(">", nameStart)
        if (nameEnd < 0) return null
        return text.substring(nameStart, nameEnd).trim()
    }

    private fun extractParameters(text: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val regex = Regex("<parameter=([^>]+)>([\\s\\S]*?)</parameter>")
        regex.findAll(text).forEach { match ->
            params[match.groupValues[1]] = match.groupValues[2].trim()
        }
        return params
    }
}
