package com.aibrowser.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    var addressBarExpanded by remember { mutableStateOf(true) }
    var urlInput by remember(activeTab?.url) { mutableStateOf(activeTab?.url ?: "") }
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
                onSettingsClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = { voiceModeActive = !voiceModeActive },
                    containerColor = if (voiceModeActive) MaterialTheme.colorScheme.secondaryContainer
                                     else MaterialTheme.colorScheme.surfaceVariant
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
                    containerColor = MaterialTheme.colorScheme.primary
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { addressBarExpanded = !addressBarExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (addressBarExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (addressBarExpanded) "Collapse address bar" else "Expand address bar",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = { browserViewModel.goBack() },
                    enabled = activeTab?.canGoBack == true,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { browserViewModel.goForward() },
                    enabled = activeTab?.canGoForward == true,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Forward", Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { browserViewModel.reloadCurrent() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, "Refresh", Modifier.size(20.dp))
                }
            }

            AnimatedVisibility(
                visible = addressBarExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    singleLine = true,
                    placeholder = { Text("Enter URL") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .heightIn(min = 40.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            browserViewModel.navigateToUrl(urlInput)
                        }
                    ),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }

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
                            if (tab.id == browserViewModel.activeTabId.value) urlInput = url
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
