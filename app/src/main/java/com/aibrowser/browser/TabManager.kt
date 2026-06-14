package com.aibrowser.browser

import android.content.Context
import android.webkit.WebView
import com.aibrowser.agent.StealthInjector
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _tabs = mutableListOf<TabState>()
    val tabs: List<TabState> get() = _tabs.toList()

    private var _activeTabId: String? = null
    val activeTabId: String? get() = _activeTabId

    fun createTab(url: String = "about:blank"): TabState {
        val id = UUID.randomUUID().toString().take(8)
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.userAgentString = settings.userAgentString
                .replace("; wv", "")
                .replace("Android WebView", "Chrome")
            evaluateJavascript(StealthInjector.getInjectionScript(), null)
        }
        val tab = TabState(id = id, url = url, webView = webView)
        _tabs.add(tab)
        _activeTabId = id
        if (url != "about:blank") {
            webView.loadUrl(url)
        }
        return tab
    }

    fun closeTab(id: String) {
        val tab = _tabs.find { it.id == id } ?: return
        tab.webView?.destroy()
        _tabs.removeAll { it.id == id }
        if (_activeTabId == id) {
            _activeTabId = _tabs.lastOrNull()?.id
        }
    }

    fun setActiveTab(id: String) {
        if (_tabs.any { it.id == id }) {
            _activeTabId = id
        }
    }

    fun getActiveTab(): TabState? = _tabs.find { it.id == _activeTabId }

    fun getTab(id: String): TabState? = _tabs.find { it.id == id }

    fun updateTab(id: String, update: (TabState) -> TabState) {
        val index = _tabs.indexOfFirst { it.id == id }
        if (index >= 0) {
            _tabs[index] = update(_tabs[index])
        }
    }
}
