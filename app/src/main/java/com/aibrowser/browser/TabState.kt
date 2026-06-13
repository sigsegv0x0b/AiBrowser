package com.aibrowser.browser

import android.webkit.WebView

data class TabState(
    val id: String,
    val url: String = "about:blank",
    val title: String = "New Tab",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val webView: WebView? = null
)
