package com.aibrowser.browser

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.aibrowser.agent.StealthInjector
import com.aibrowser.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val _tabs = mutableListOf<TabState>()
    val tabs: List<TabState> get() = _tabs.toList()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val saveScope = CoroutineScope(Dispatchers.IO)
    private var saveJob: Job? = null

    private fun createWebView(): WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.allowFileAccess = true
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.userAgentString = settings.userAgentString
            .replace("; wv", "")
            .replace("Android WebView", "Chrome")
        isFocusable = true
        isFocusableInTouchMode = true
        setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        evaluateJavascript(StealthInjector.getInjectionScript(), null)
    }

    fun createTab(url: String = "about:blank"): TabState {
        val id = UUID.randomUUID().toString().take(8)
        val webView = createWebView()
        val tab = TabState(id = id, url = url, webView = webView)
        _tabs.add(tab)
        _activeTabId.value = id
        if (url != "about:blank") {
            webView.loadUrl(url)
        }
        scheduleSave()
        return tab
    }

    fun closeTab(id: String) {
        val tab = _tabs.find { it.id == id } ?: return
        tab.webView?.destroy()
        _tabs.removeAll { it.id == id }
        if (_tabs.isEmpty()) {
            createTab("about:blank")
            return
        }
        if (_activeTabId.value == id) {
            _activeTabId.value = _tabs.lastOrNull()?.id
        }
        scheduleSave()
    }

    fun setActiveTab(id: String) {
        if (_tabs.any { it.id == id }) {
            _activeTabId.value = id
        }
    }

    fun getActiveTab(): TabState? = _tabs.find { it.id == _activeTabId.value }

    fun getTab(id: String): TabState? = _tabs.find { it.id == id }

    fun updateTab(id: String, update: (TabState) -> TabState) {
        val index = _tabs.indexOfFirst { it.id == id }
        if (index >= 0) {
            _tabs[index] = update(_tabs[index])
            scheduleSave()
        }
    }

    fun goBack(tabId: String) {
        getTab(tabId)?.webView?.goBack()
    }

    fun goForward(tabId: String) {
        getTab(tabId)?.webView?.goForward()
    }

    fun reload(tabId: String) {
        getTab(tabId)?.webView?.reload()
    }

    fun loadUrl(tabId: String, url: String) {
        getTab(tabId)?.webView?.loadUrl(url)
    }

    suspend fun restoreTabs(persisted: List<PersistedTab>) {
        _tabs.clear()
        for (p in persisted) {
            val webView = createWebView()
            val tab = TabState(
                id = p.id,
                url = p.url,
                title = p.title,
                webView = webView,
                messages = p.messages,
                actionHistory = p.actionHistory,
                isPaused = p.isPaused
            )
            _tabs.add(tab)
            if (p.url != "about:blank") {
                webView.loadUrl(p.url)
            }
        }
        _activeTabId.value = _tabs.firstOrNull()?.id
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = saveScope.launch {
            delay(500)
            val snapshot = _tabs.map { it.toPersisted() }
            settingsRepository.saveTabs(snapshot)
        }
    }

    fun getBrowserDataSize(): Long {
        val webviewDir = File(context.filesDir.parentFile, "app_webview")
        if (!webviewDir.exists()) return 0L
        return dirSize(webviewDir)
    }

    private fun dirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            if (f.isDirectory) size += dirSize(f)
            else size += f.length()
        }
        return size
    }

    fun clearBrowserData(onComplete: () -> Unit) {
        CookieManager.getInstance().removeAllCookies { value ->
            mainHandler.post {
                WebStorage.getInstance().deleteAllData()
                for (tab in _tabs) {
                    tab.webView?.apply {
                        clearCache(true)
                        clearHistory()
                        clearFormData()
                    }
                }
                CookieManager.getInstance().flush()
                for (tab in _tabs) {
                    tab.webView?.reload()
                }
                onComplete()
            }
        }
    }
}
