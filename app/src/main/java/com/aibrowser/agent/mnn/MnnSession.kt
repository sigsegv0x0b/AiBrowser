package com.aibrowser.agent.mnn

import android.util.Log
import com.alibaba.mnnllm.android.llm.LlmSession as OfficialLlmSession
import com.google.gson.Gson
import java.io.File

class MnnSession(
    private val modelDir: String
) {
    private val jni = OfficialLlmSession()
    private var nativePtr: Long = 0

    val isLoaded: Boolean get() = nativePtr != 0L

    fun load(useMmap: Boolean) {
        val libErr = OfficialLlmSession.loadError
        if (libErr != null) throw IllegalStateException("Native libraries not loaded: $libErr")

        val configFile = File(modelDir, "config.json")
        if (!configFile.exists()) throw IllegalStateException("config.json not found in $modelDir")

        val rt = Runtime.getRuntime()
        val usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576
        val maxMb = rt.maxMemory() / 1048576
        Log.d(TAG, "Loading MNN model from: $modelDir (mem: used=${usedMb}MB, max=${maxMb}MB, mmap=$useMmap)")

        val configJson = configFile.readText()
        val isR1 = modelDir.contains("R1", ignoreCase = true) ||
                   modelDir.contains("DeepSeek", ignoreCase = true)

        val gson = Gson()
        val extra = gson.toJson(mapOf(
            "is_r1" to isR1,
            "mmap_dir" to if (useMmap) modelDir else "",
            "keep_history" to true
        ))

        Log.d(TAG, "is_r1=$isR1, mmap_dir=${if (useMmap) modelDir else "(none)"}")
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
