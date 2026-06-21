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

data class PersistedTab(
    val id: String,
    val url: String,
    val title: String,
    val messages: List<Message>,
    val actionHistory: List<String> = emptyList(),
    val isPaused: Boolean = false
)

fun TabState.toPersisted(): PersistedTab = PersistedTab(
    id = id,
    url = url,
    title = title,
    messages = messages,
    actionHistory = actionHistory,
    isPaused = isPaused
)
