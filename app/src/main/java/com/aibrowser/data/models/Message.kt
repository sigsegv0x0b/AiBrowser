package com.aibrowser.data.models

data class Message(
    val id: String,
    val role: Role,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val thinking: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role { USER, ASSISTANT, SYSTEM, TOOL }
}
