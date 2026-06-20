package com.alibaba.mnnllm.android.llm

import android.util.Log

class LlmSession {
    companion object {
        var loadError: String? = null
            private set

        init {
            try {
                System.loadLibrary("MNN")
                System.loadLibrary("mnnllmapp")
                Log.d("LlmSession", "Native libraries loaded successfully")
            } catch (e: Throwable) {
                loadError = "${e.javaClass.simpleName}: ${e.message}"
                Log.e("LlmSession", "Failed to load native libraries: $loadError", e)
            }
        }
    }

    external fun initNative(
        configPath: String?,
        history: List<String>?,
        mergedConfigStr: String?,
        configJsonStr: String?
    ): Long

    external fun submitNative(
        instanceId: Long,
        input: String,
        keepHistory: Boolean,
        listener: com.aibrowser.agent.mnn.GenerateProgressListener
    ): HashMap<String, Any>

    external fun resetNative(instanceId: Long)
    external fun releaseNative(instanceId: Long)
    external fun getDebugInfoNative(instanceId: Long): String
}
