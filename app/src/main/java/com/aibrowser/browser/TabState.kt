package com.aibrowser.browser

import android.webkit.WebView
import com.aibrowser.data.models.Message

data class TabState(
    val id: String,
    val url: String = "about:blank",
    val title: String = "New Tab",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val webView: WebView? = null,
    val messages: List<Message> = emptyList(),
    val agentIsLoading: Boolean = false,
    val currentAction: String = "",
    val actionHistory: List<String> = emptyList(),
    val isPaused: Boolean = false
)
