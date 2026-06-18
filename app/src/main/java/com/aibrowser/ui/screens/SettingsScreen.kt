package com.aibrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AiService
import com.aibrowser.agent.LlamaCppProvider
import com.aibrowser.agent.LocalLlmProvider
import com.aibrowser.agent.LocalModelManager
import com.aibrowser.agent.ModelDownloader
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.BehaviorConfig
import com.aibrowser.data.models.LlamaCppSettings
import com.aibrowser.data.models.LocalLlmConfig
import com.aibrowser.ui.screens.settings.BehaviorSettingsTab
import com.aibrowser.ui.screens.settings.CloudLlmTab
import com.aibrowser.ui.screens.settings.LiteRtTab
import com.aibrowser.ui.screens.settings.LlamaCppTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    aiService: AiService,
    localModelManager: LocalModelManager,
    localLlmProvider: LocalLlmProvider,
    llamaCppProvider: LlamaCppProvider,
    modelDownloader: ModelDownloader,
    onBack: () -> Unit
) {
    val config by settingsRepository.apiConfig.collectAsState(initial = ApiConfig())
    val behavior by settingsRepository.behaviorConfig.collectAsState(initial = BehaviorConfig())
    val localLlmConfig by settingsRepository.localLlmConfig.collectAsState(initial = LocalLlmConfig())
    val notesDirectoryUri by settingsRepository.notesDirectoryUri.collectAsState(initial = null)
    val llamaCppSettings by settingsRepository.llamaCppSettings.collectAsState(initial = LlamaCppSettings())
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cloud LLM", "llama.cpp Local AI", "LiteRT local ai", "Behavior")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> CloudLlmTab(
                    config = config,
                    aiService = aiService,
                    settingsRepository = settingsRepository,
                    scope = scope,
                    focusManager = focusManager
                )
                1 -> LlamaCppTab(
                    llamaCppSettings = llamaCppSettings,
                    settingsRepository = settingsRepository,
                    llamaCppProvider = llamaCppProvider,
                    modelDownloader = modelDownloader,
                    scope = scope
                )
                2 -> LiteRtTab(
                    localLlmConfig = localLlmConfig,
                    localModelManager = localModelManager,
                    settingsRepository = settingsRepository,
                    localLlmProvider = localLlmProvider,
                    scope = scope
                )
                3 -> BehaviorSettingsTab(
                    behavior = behavior,
                    notesDirectoryUri = notesDirectoryUri,
                    settingsRepository = settingsRepository,
                    scope = scope,
                    onSave = { scope.launch { settingsRepository.saveBehaviorConfig(it) } }
                )
            }
        }
    }
}
