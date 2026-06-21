package com.aibrowser.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibrowser.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabManager: TabManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _tabs = MutableStateFlow<List<TabState>>(emptyList())
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    init {
        viewModelScope.launch {
            val persisted = settingsRepository.persistedTabs.first()
            if (persisted.isNotEmpty()) {
                tabManager.restoreTabs(persisted)
            } else if (tabManager.tabs.isEmpty()) {
                tabManager.createTab("https://www.google.com")
            }
            refresh()
        }
    }

    fun createTab(url: String = "about:blank") {
        tabManager.createTab(url)
        refresh()
    }

    fun closeTab(id: String) {
        tabManager.closeTab(id)
        refresh()
    }

    fun setActiveTab(id: String) {
        tabManager.setActiveTab(id)
        refresh()
    }

    fun getActiveTab(): TabState? = tabManager.getActiveTab()

    fun getTab(id: String): TabState? = tabManager.getTab(id)

    fun updateTab(id: String, update: (TabState) -> TabState) {
        tabManager.updateTab(id, update)
        refresh()
    }

    fun refresh() {
        _tabs.value = tabManager.tabs
        _activeTabId.value = tabManager.activeTabId.value
    }

    fun goBack() {
        val id = _activeTabId.value ?: return
        tabManager.goBack(id)
        refresh()
    }

    fun goForward() {
        val id = _activeTabId.value ?: return
        tabManager.goForward(id)
        refresh()
    }

    fun reloadCurrent() {
        val id = _activeTabId.value ?: return
        tabManager.reload(id)
    }

    fun navigateToUrl(url: String) {
        val id = _activeTabId.value ?: return
        var finalUrl = url.trim()
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            finalUrl = "https://$finalUrl"
        }
        tabManager.loadUrl(id, finalUrl)
        refresh()
    }
}
