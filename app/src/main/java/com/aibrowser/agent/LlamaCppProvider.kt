package com.aibrowser.agent

import com.aibrowser.agent.AiService.StreamEvent
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.LlamaCppSettings
import com.aibrowser.data.models.Message
import com.geniex.sdk.LlmWrapper
import com.geniex.sdk.ModelManagerWrapper
import com.geniex.sdk.bean.DeviceIdValue
import com.geniex.sdk.bean.GenerationConfig
import com.geniex.sdk.bean.LlmCreateInput
import com.geniex.sdk.bean.LlmStreamResult
import com.geniex.sdk.bean.ModelConfig
import com.geniex.sdk.bean.SamplerConfig
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlamaCppProvider @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val genieXManager: GenieXManager,
    @ApplicationContext private val context: Context
) {
    private var llmWrapper: LlmWrapper? = null
    private var activeModelPath: String? = null
    private var currentBackend: String = "cpu"
    private var currentNGpuLayers: Int = 0
    private var currentNCtx: Int = 1024

    private val _lastStats = MutableStateFlow<InferenceStats?>(null)
    val lastStats: StateFlow<InferenceStats?> = _lastStats.asStateFlow()
    private val gson = Gson()

    suspend fun sendMessage(
        messages: List<Message>,
        onEvent: (StreamEvent) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val settings = settingsRepository.llamaCppSettings.first()

        if (!settings.isModelReady) {
            onEvent(StreamEvent.Error("No GGUF model selected. Download or pick a model in Settings."))
            return@withContext ""
        }

        try {
            genieXManager.init()
            val wrapper = getOrCreateWrapper(settings) { status ->
                onEvent(StreamEvent.Status(status))
            }

            val genConfig = buildGenerationConfig(settings)

            val prompt = buildPrompt(messages)

            var fullResponse = ""
            var tokenCount = 0L
            val startTime = System.currentTimeMillis()
            wrapper.generateStreamFlow(prompt, genConfig).collect { result ->
                when (result) {
                    is LlmStreamResult.Token -> {
                        fullResponse += result.text
                        tokenCount++
                        onEvent(StreamEvent.Token(result.text))
                        if (tokenCount % 5 == 0L) {
                            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                            val tps = if (elapsed > 0) tokenCount / elapsed else 0.0
                            onEvent(StreamEvent.Status("${tokenCount} tokens, ${"%.1f".format(tps)} tok/s"))
                        }
                    }
                    is LlmStreamResult.Completed -> {
                        result.profile.let { p ->
                            _lastStats.value = InferenceStats(
                                ttftMs = p.ttftMs,
                                promptTokens = p.promptTokens,
                                prefillSpeed = p.prefillSpeed,
                                generatedTokens = p.generatedTokens,
                                decodingSpeed = p.decodingSpeed,
                                device = currentBackend
                            )
                        }
                    }
                    is LlmStreamResult.Error -> {
                        onEvent(StreamEvent.Error(result.throwable.message ?: "llama.cpp error"))
                    }
                }
            }

            val parsedToolCalls = parseToolCallsFromText(fullResponse)
            for (tc in parsedToolCalls) {
                val argsJson = gson.toJson(tc.arguments)
                val id = UUID.randomUUID().toString()
                onEvent(StreamEvent.ToolCallStart(id, tc.name, argsJson))
            }
            val strippedResponse = if (parsedToolCalls.isNotEmpty()) {
                stripToolCallMarkup(fullResponse)
            } else fullResponse

            val thinking = extractThinking(strippedResponse)
            val finalResponse = if (thinking != null) {
                stripThinking(strippedResponse).ifBlank { strippedResponse }
            } else strippedResponse

            onEvent(StreamEvent.Done(finalResponse, thinking))
            finalResponse
        } catch (e: Exception) {
            onEvent(StreamEvent.Error(e.message ?: "llama.cpp error"))
            close()
            ""
        }
    }

    private fun buildGenerationConfig(settings: LlamaCppSettings): GenerationConfig {
        return if (settings.useCustomSampler) {
            GenerationConfig(
                maxTokens = settings.maxTokens,
                samplerConfig = SamplerConfig(
                    temperature = settings.temperature,
                    topP = settings.topP,
                    topK = settings.topK,
                    repetitionPenalty = 1.1f
                )
            )
        } else {
            GenerationConfig(maxTokens = settings.maxTokens)
        }
    }

    private fun buildPrompt(messages: List<Message>): String {
        val sb = StringBuilder()
        for (msg in messages) {
            when (msg.role) {
                Message.Role.SYSTEM -> sb.append("<|system|>\n${msg.content}\n")
                Message.Role.USER -> sb.append("<|user|>\n${msg.content}\n")
                Message.Role.ASSISTANT -> {
                    sb.append("<|assistant|>\n${msg.content}\n")
                    for (tc in msg.toolCalls) {
                        val argsJson = gson.toJson(tc.arguments)
                        sb.append("<|tool_call|>call:${tc.name} {$argsJson}<tool_call|>\n")
                    }
                }
                Message.Role.TOOL -> {
                    val name = msg.toolCallId ?: "unknown"
                    sb.append("<|tool|>\n${name}: ${msg.content}\n")
                }
            }
        }
        sb.append("<|assistant|>\n")
        return sb.toString()
    }

    private suspend fun getOrCreateWrapper(
        settings: LlamaCppSettings,
        onStatus: (String) -> Unit
    ): LlmWrapper {
        val modelName = settings.selectedModel

        val backendLower = settings.backend.lowercase()
        val gpuLayers: Int = when {
            backendLower == "gpu" -> settings.nGpuLayers.coerceIn(1, 999)
            backendLower == "npu" -> 999
            else -> 0
        }
        val nCtx = 1024

        if (llmWrapper != null && activeModelPath == modelName &&
            currentBackend == settings.backend &&
            currentNGpuLayers == gpuLayers &&
            currentNCtx == nCtx) return llmWrapper!!

        close()

        val paths = ModelManagerWrapper.getPaths(modelName)
        if (paths == null) {
            throw RuntimeException("Model not downloaded: $modelName. Download it in Settings first.")
        }
        val modelPath = paths.model_path
        val pluginId = paths.plugin_id.ifEmpty { "llama_cpp" }

        val file = File(modelPath)
        if (!file.exists()) {
            throw RuntimeException("Model file missing: $modelPath")
        }
        if (file.length() == 0L) {
            throw RuntimeException("Model file is empty: $modelPath")
        }

        val deviceId: String
        when {
            backendLower == "gpu" -> {
                deviceId = DeviceIdValue.GPU.value ?: "gpu"
            }
            backendLower == "npu" -> {
                deviceId = DeviceIdValue.NPU.value ?: "npu"
            }
            else -> {
                deviceId = DeviceIdValue.NPU.value ?: "npu"
            }
        }

        val input = LlmCreateInput(
            model_name = paths.model_name,
            model_path = modelPath,
            tokenizer_path = paths.tokenizer_path,
            config = ModelConfig(
                nCtx = 1024,
                nGpuLayers = gpuLayers,
                enable_thinking = false
            ),
            plugin_id = pluginId,
            device_id = deviceId
        )

        onStatus("Loading model...")
        var errorMsg: String? = null
        LlmWrapper.builder()
            .llmCreateInput(input)
            .build()
            .onSuccess { wrapper ->
                llmWrapper = wrapper
                activeModelPath = modelName
                currentBackend = settings.backend
                currentNGpuLayers = gpuLayers
                currentNCtx = nCtx
                onStatus("Model loaded")
            }
            .onFailure { error ->
                errorMsg = error.message ?: "Failed to load model"
                onStatus("Load failed: $errorMsg")
            }

        if (errorMsg != null) throw RuntimeException(errorMsg)
        return llmWrapper!!
    }

    suspend fun testInference(): String = withContext(Dispatchers.IO) {
        val settings = settingsRepository.llamaCppSettings.first()
        if (!settings.isModelReady) return@withContext "No model selected"
        try {
            genieXManager.init()
            val wrapper = getOrCreateWrapper(settings) {}
            val genConfig = buildGenerationConfig(settings.copy(maxTokens = 256))
            val sb = StringBuilder()
            generateAndCollect(wrapper, "hello", genConfig, sb)
            sb.toString().ifBlank { "(empty response)" }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun benchmarkInference(
        onToken: (String) -> Unit,
        onProgress: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val settings = settingsRepository.llamaCppSettings.first()
        if (!settings.isModelReady) throw RuntimeException("No model selected")
        genieXManager.init()
        val wrapper = getOrCreateWrapper(settings) {}
        val genConfig = buildGenerationConfig(settings.copy(maxTokens = 256))
        val sb = StringBuilder()
        var tokenCount = 0L
        val startTime = System.currentTimeMillis()
        wrapper.generateStreamFlow("hello", genConfig).collect { result ->
            when (result) {
                is LlmStreamResult.Token -> {
                    sb.append(result.text)
                    onToken(result.text)
                    tokenCount++
                    if (tokenCount % 5 == 0L) {
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val tps = if (elapsed > 0) tokenCount / elapsed else 0.0
                        onProgress("${tokenCount} tokens, ${"%.1f".format(tps)} tok/s")
                    }
                }
                is LlmStreamResult.Completed -> {
                    result.profile.let { p ->
                        _lastStats.value = InferenceStats(
                            ttftMs = p.ttftMs,
                            promptTokens = p.promptTokens,
                            prefillSpeed = p.prefillSpeed,
                            generatedTokens = p.generatedTokens,
                            decodingSpeed = p.decodingSpeed,
                            device = currentBackend
                        )
                    }
                }
                is LlmStreamResult.Error -> {
                    throw RuntimeException(result.throwable.message ?: "llama.cpp error")
                }
            }
        }
        sb.toString().ifBlank { "(empty response)" }
    }

    private suspend fun generateAndCollect(
        wrapper: LlmWrapper,
        prompt: String,
        genConfig: GenerationConfig,
        sb: StringBuilder
    ) {
        wrapper.generateStreamFlow(prompt, genConfig).collect { result ->
            when (result) {
                is LlmStreamResult.Token -> sb.append(result.text)
                is LlmStreamResult.Completed -> {
                    result.profile.let { p ->
                        _lastStats.value = InferenceStats(
                            ttftMs = p.ttftMs,
                            promptTokens = p.promptTokens,
                            prefillSpeed = p.prefillSpeed,
                            generatedTokens = p.generatedTokens,
                            decodingSpeed = p.decodingSpeed,
                            device = currentBackend
                        )
                    }
                }
                is LlmStreamResult.Error -> {}
            }
        }
    }

    suspend fun close() {
        try { llmWrapper?.stopStream() } catch (_: Exception) {}
        try { llmWrapper?.destroy() } catch (_: Exception) {}
        llmWrapper = null
        activeModelPath = null
        _lastStats.value = null
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
                    .replace("<|\\\"|>", "")
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
            when {
                c == '<' && pos + 1 < argsStr.length && argsStr[pos + 1] == '|' -> {
                    val endIdx = argsStr.indexOf("|>", pos + 2)
                    if (endIdx >= 0) {
                        current.append(argsStr.substring(pos, endIdx + 2))
                        pos = endIdx + 1
                    } else {
                        current.append(c)
                        pos++
                    }
                }
                c == '{' -> { depth++; current.append(c); pos++ }
                c == '}' -> { depth--; current.append(c); pos++ }
                c == ',' && depth == 0 -> {
                    pairs.add(current.toString())
                    current.clear()
                    pos++
                }
                else -> { current.append(c); pos++ }
            }
        }
        if (current.isNotEmpty()) pairs.add(current.toString())
        return pairs
    }

    private fun stripToolCallMarkup(text: String): String {
        return text.replace(Regex("<\\|tool_call>.*?<tool_call\\|>", RegexOption.DOT_MATCHES_ALL), "")
    }

    companion object {
        private val CHANNEL_REGEX = Regex("<channel\\|>(.*?)<\\|channel>", RegexOption.DOT_MATCHES_ALL)
    }

    private fun extractThinking(text: String): String? {
        val parts = mutableListOf<String>()
        for (match in CHANNEL_REGEX.findAll(text)) {
            parts.add(match.groupValues[1].trim())
        }
        return parts.joinToString("\n\n").ifEmpty { null }
    }

    private fun stripThinking(text: String): String {
        return text.replace(CHANNEL_REGEX, "").trim()
    }
}
