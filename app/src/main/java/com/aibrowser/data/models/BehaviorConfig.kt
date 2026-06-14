package com.aibrowser.data.models

data class BehaviorConfig(
    val scrollIntoView: Boolean = true,
    val blockExternalIntents: Boolean = true,
    val ttsPrompt: String = DEFAULT_TTS_PROMPT,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {
    companion object {
        const val DEFAULT_TTS_PROMPT = "You are a text-to-speech preprocessor. Rewrite the following text to be spoken aloud naturally. Remove all markdown formatting, code blocks, bullet points, numbered lists, links, URLs, and special characters. Keep only the plain conversational text. Make it flow naturally for speech. Keep it concise. Output ONLY the plain text, nothing else."
        const val DEFAULT_SYSTEM_PROMPT = "You are a browser automation assistant. You have access to browser tools to help the user complete tasks. For every new page, first use browser_snapshot to understand the page content. After receiving tool results, always continue with the next action or provide a response to the user. Do not stop after a single tool call - keep analyzing and acting until the task is complete."
    }
}
