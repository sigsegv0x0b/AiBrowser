package com.aibrowser.data.models

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>,
    val status: ToolStatus = ToolStatus.PENDING,
    val result: String? = null
) {
    enum class ToolStatus { PENDING, RUNNING, DONE, ERROR }
}
