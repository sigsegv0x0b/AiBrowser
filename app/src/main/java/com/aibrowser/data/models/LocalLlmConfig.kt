package com.aibrowser.data.models

data class LocalLlmConfig(
    val downloadedModelPath: String = "",
    val downloadedModelId: String = "",
    val backend: Backend = Backend.AUTO,
    val maxTokens: Int = 0
) {
    enum class Backend(val displayName: String) {
        AUTO("Auto (NPU\u2192GPU\u2192CPU)"),
        NPU("NPU (Qualcomm Hexagon)"),
        GPU("GPU (OpenCL)"),
        CPU("CPU (XNNPack)")
    }

    val isModelReady: Boolean get() = downloadedModelPath.isNotBlank()
}
