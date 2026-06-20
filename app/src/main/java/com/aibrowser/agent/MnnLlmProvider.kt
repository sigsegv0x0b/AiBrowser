package com.aibrowser.agent

import android.util.Log
import com.aibrowser.agent.mnn.MnnSession
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.Message
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MnnLlmProvider @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val gson = Gson()
    @Volatile private var session: MnnSession? = null
    @Volatile private var currentModelPath: String? = null

    suspend fun sendMessage(
        messages: List<Message>,
        onEvent: (AiService.StreamEvent) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val modelPath = settingsRepository.mnnModelPath.first()
        if (modelPath.isBlank()) {
            onEvent(AiService.StreamEvent.Error("MNN model not configured. Go to Settings."))
            return@withContext ""
        }

        val useMmap = settingsRepository.mnnUseMmap.first()
        ensureSession(modelPath, useMmap)

        val prompt = buildPrompt(messages)
        var fullResponse = ""

        try {
            session!!.generate(prompt, object : com.aibrowser.agent.mnn.GenerateProgressListener {
                override fun onProgress(token: String?): Boolean {
                    if (token != null) {
                        fullResponse += token
                        onEvent(AiService.StreamEvent.Token(token))
                    }
                    return false
                }
            })
            onEvent(AiService.StreamEvent.Done(fullResponse))
        } catch (e: Exception) {
            Log.e(TAG, "Inference error", e)
            onEvent(AiService.StreamEvent.Error("MNN error: ${e.message}"))
        }

        fullResponse
    }

    private suspend fun ensureSession(modelPath: String, useMmap: Boolean) {
        if (session != null && currentModelPath == modelPath && session!!.isLoaded) return
        releaseSession()
        session = MnnSession(modelDir = modelPath).also { it.load(useMmap) }
        currentModelPath = modelPath
    }

    fun releaseSession() {
        session?.release()
        session = null
        currentModelPath = null
    }

    suspend fun streamTest(prompt: String, onToken: (String) -> Unit): String = withContext(Dispatchers.IO) {
        val modelPath = settingsRepository.mnnModelPath.first()
        if (modelPath.isBlank()) throw IllegalStateException("No MNN model configured")

        val useMmap = settingsRepository.mnnUseMmap.first()

        val modelDir = java.io.File(modelPath)
        if (!modelDir.exists() || !modelDir.isDirectory) throw IllegalStateException("Model directory not found: $modelPath")
        val configFile = java.io.File(modelDir, "config.json")
        if (!configFile.exists()) throw IllegalStateException("config.json not found in $modelPath")

        var testSession: MnnSession? = null
        try {
            testSession = MnnSession(modelDir = modelPath)
            testSession.load(useMmap)
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

        val testSession = MnnSession(modelDir = modelPath)
        return@withContext try {
            testSession.load(settingsRepository.mnnUseMmap.first())
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

    private fun buildPrompt(messages: List<Message>): String {
        val sb = StringBuilder()
        for (msg in messages) {
            when (msg.role) {
                Message.Role.SYSTEM -> sb.append("<|system|>\n${msg.content}\n")
                Message.Role.USER -> sb.append("<|user|>\n${msg.content}\n")
                Message.Role.ASSISTANT -> sb.append("<|assistant|>\n${msg.content}\n")
                Message.Role.TOOL -> sb.append("<|tool|>\n${msg.content}\n")
            }
        }
        sb.append("<|assistant|>\n")
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
