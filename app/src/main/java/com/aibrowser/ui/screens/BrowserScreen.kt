package com.aibrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AgentViewModel
import com.aibrowser.browser.BrowserViewModel
import com.aibrowser.data.SettingsRepository
import com.aibrowser.ui.components.AgentStatusBubble
import com.aibrowser.ui.components.TabBar
import com.aibrowser.ui.components.VoiceInputBar
import com.aibrowser.ui.components.WebViewContainer

@Composable
fun BrowserScreen(
    browserViewModel: BrowserViewModel,
    agentViewModel: AgentViewModel,
    settingsRepository: SettingsRepository,
    onNavigateToAgent: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val tabs by browserViewModel.tabs.collectAsState()
    val activeTabId by browserViewModel.activeTabId.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }
    val isLoading by agentViewModel.isLoading.collectAsState()
    val currentAction by agentViewModel.currentAction.collectAsState()
    val actionHistory by agentViewModel.actionHistory.collectAsState()
    val messages by agentViewModel.messages.collectAsState()
    val behavior by settingsRepository.behaviorConfig.collectAsState(initial = com.aibrowser.data.models.BehaviorConfig())
    var voiceModeActive by remember { mutableStateOf(false) }

    val responseText = remember(isLoading, messages) {
        if (!isLoading) {
            messages.lastOrNull { it.role == com.aibrowser.data.models.Message.Role.ASSISTANT }?.content ?: ""
        } else ""
    }

    var ttsText by remember { mutableStateOf("") }
    LaunchedEffect(isLoading) {
        if (!isLoading && responseText.isNotBlank()) {
            ttsText = agentViewModel.generateTtsText(responseText)
        }
    }

    Scaffold(
        topBar = {
            TabBar(
                tabs = tabs,
                activeTabId = activeTabId,
                onTabClick = { browserViewModel.setActiveTab(it) },
                onCloseTab = { browserViewModel.closeTab(it) },
                onNewTab = { browserViewModel.createTab() },
                onSettingsClick = onNavigateToSettings,
                onNavigate = { browserViewModel.navigateToUrl(it) },
                onBack = { browserViewModel.goBack() },
                onForward = { browserViewModel.goForward() },
                onReload = { browserViewModel.reloadCurrent() }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = {
                        agentViewModel.sendMessage("Summarize the current page concisely.")
                        onNavigateToAgent()
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.alpha(if (isLoading) 0.25f else 0.5f)
                ) {
                    Icon(
                        Icons.Default.Summarize,
                        contentDescription = "Summarize page",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                FloatingActionButton(
                    onClick = { voiceModeActive = !voiceModeActive },
                    containerColor = if (voiceModeActive) MaterialTheme.colorScheme.secondaryContainer
                                     else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.alpha(if (voiceModeActive) 1f else 0.5f)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = if (voiceModeActive) "Close voice mode" else "Voice mode",
                        tint = if (voiceModeActive) MaterialTheme.colorScheme.onSecondaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FloatingActionButton(
                    onClick = onNavigateToAgent,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alpha(0.5f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Agent")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                activeTab?.let { tab ->
                    WebViewContainer(
                        tab = tab,
                        blockExternalIntents = behavior.blockExternalIntents,
                        onIntentBlocked = { url ->
                            agentViewModel.addSystemMessage(url)
                        },
                        onTitleChanged = { title ->
                            browserViewModel.updateTab(tab.id) { it.copy(title = title) }
                        },
                        onUrlChanged = { url ->
                            browserViewModel.updateTab(tab.id) { it.copy(url = url) }
                        },
                        onLoadingChanged = { loading ->
                            browserViewModel.updateTab(tab.id) { it.copy(isLoading = loading) }
                        },
                        onNavigationStateChanged = { canGoBack, canGoForward ->
                            browserViewModel.updateTab(tab.id) {
                                it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: run {
                    Text(
                        text = "No tabs open",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (voiceModeActive) {
                    VoiceInputBar(
                        isLoading = isLoading,
                        actionHistory = actionHistory,
                        currentAction = currentAction,
                        responseText = responseText,
                        ttsText = ttsText,
                        onResult = { text ->
                            agentViewModel.sendMessage(text)
                        },
                        onDismiss = { voiceModeActive = false },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
            if (!voiceModeActive) {
                AgentStatusBubble(
                    isLoading = isLoading,
                    actionHistory = actionHistory,
                    currentAction = currentAction
                )
            }
        }
    }
}
