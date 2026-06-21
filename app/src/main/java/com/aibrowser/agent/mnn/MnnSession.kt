package com.aibrowser.agent.mnn

import android.util.Log
import com.alibaba.mnnllm.android.llm.LlmSession as OfficialLlmSession
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File

class MnnSession(
    private val modelDir: String
) {
    private val jni = OfficialLlmSession()
    private var nativePtr: Long = 0

    val isLoaded: Boolean get() = nativePtr != 0L
    var modelType: String = ""
        private set
    var isClaudeDistilled: Boolean = false
        private set

    data class MnnSettings(
        val useMmap: Boolean = false,
        val promptCache: Boolean = true,
        val temperature: Float = 0.7f,
        val topP: Float = 0.95f,
        val topK: Int = 20,
        val precision: String = "low",
        val threads: Int = 4,
        val maxTokens: Int = 2048
    )

    fun load(settings: MnnSettings) {
        val libErr = OfficialLlmSession.loadError
        if (libErr != null) throw IllegalStateException("Native libraries not loaded: $libErr")

        val configFile = File(modelDir, "config.json")
        if (!configFile.exists()) throw IllegalStateException("config.json not found in $modelDir")

        val rt = Runtime.getRuntime()
        val usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576
        val maxMb = rt.maxMemory() / 1048576
        Log.d(TAG, "Loading MNN model from: $modelDir (mem: used=${usedMb}MB, max=${maxMb}MB, mmap=${settings.useMmap})")

        // Read and merge settings into config
        val gson = Gson()
        val configObj = JsonParser.parseString(configFile.readText()).asJsonObject
        configObj.addProperty("prompt_cache", settings.promptCache)
        configObj.addProperty("temperature", settings.temperature)
        configObj.addProperty("topP", settings.topP)
        configObj.addProperty("topK", settings.topK)
        configObj.addProperty("precision", settings.precision)
        configObj.addProperty("thread_num", settings.threads)
        configObj.addProperty("max_tokens", settings.maxTokens)

        val configJson = gson.toJson(configObj.asMap())

        // Parse model type and detect Claude-distilled variant from llm_config.json
        val llmConfigFile = File(modelDir, "llm_config.json")
        if (llmConfigFile.exists()) {
            try {
                val obj = JsonParser.parseString(llmConfigFile.readText()).asJsonObject
                modelType = obj.get("model_type")?.asString ?: ""
                val chatTemplate = obj.get("jinja")?.asJsonObject?.get("chat_template")?.asString ?: ""
                isClaudeDistilled = (chatTemplate.contains("\"name\":") &&
                                     !chatTemplate.contains("<function=")) ||
                                    modelDir.contains("Claude", ignoreCase = true) ||
                                    modelDir.contains("Distilled", ignoreCase = true)
                Log.d(TAG, "Model type: $modelType, claudeDistilled: $isClaudeDistilled")
            } catch (_: Exception) {}
        }

        val isR1 = modelDir.contains("R1", ignoreCase = true) ||
                   modelDir.contains("DeepSeek", ignoreCase = true)

        val extra = gson.toJson(mapOf(
            "is_r1" to isR1,
            "mmap_dir" to if (settings.useMmap) modelDir else "",
            "keep_history" to true
        ))

        Log.d(TAG, "is_r1=$isR1, settings: prompt_cache=${settings.promptCache}, temp=${settings.temperature}, topP=${settings.topP}, topK=${settings.topK}, precision=${settings.precision}, threads=${settings.threads}")
        nativePtr = jni.initNative(configFile.absolutePath, null, configJson, extra)

        if (nativePtr == 0L) {
            throw IllegalStateException("MNN model load failed for: $modelDir")
        }
        Log.d(TAG, "MNN model loaded, nativePtr=$nativePtr")
    }

    fun generate(prompt: String, listener: GenerateProgressListener): Map<String, Any> {
        check(nativePtr != 0L) { "Model not loaded" }
        return jni.submitNative(nativePtr, prompt, true, listener)
    }

    fun reset() {
        if (nativePtr != 0L) {
            jni.resetNative(nativePtr)
        }
    }

    fun release() {
        if (nativePtr != 0L) {
            jni.releaseNative(nativePtr)
            nativePtr = 0
        }
    }

    val debugInfo: String
        get() = if (nativePtr != 0L) jni.getDebugInfoNative(nativePtr) else "not loaded"

    companion object {
        private const val TAG = "MnnSession"
        val loadError: String?
            get() = OfficialLlmSession.loadError
    }
}
