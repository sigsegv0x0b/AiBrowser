package com.aibrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AgentViewModel
import com.aibrowser.browser.BrowserViewModel
import com.aibrowser.ui.components.TabBar
import com.aibrowser.ui.components.WebViewContainer

@Composable
fun BrowserScreen(
    browserViewModel: BrowserViewModel,
    agentViewModel: AgentViewModel,
    onNavigateToAgent: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val tabs by browserViewModel.tabs.collectAsState()
    val activeTabId by browserViewModel.activeTabId.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }

    Scaffold(
        topBar = {
            TabBar(
                tabs = tabs,
                activeTabId = activeTabId,
                onTabClick = { browserViewModel.setActiveTab(it) },
                onCloseTab = { browserViewModel.closeTab(it) },
                onNewTab = { browserViewModel.createTab() },
                onSettingsClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAgent,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Agent")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            activeTab?.let { tab ->
                WebViewContainer(
                    tab = tab,
                    onTitleChanged = { title ->
                        browserViewModel.updateTab(tab.id) { it.copy(title = title) }
                    },
                    onUrlChanged = { url ->
                        browserViewModel.updateTab(tab.id) { it.copy(url = url) }
                    },
                    onLoadingChanged = { loading ->
                        browserViewModel.updateTab(tab.id) { it.copy(isLoading = loading) }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } ?: run {
                Text(
                    text = "No tabs open",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
