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
import com.aibrowser.agent.MnnLlmProvider
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.BehaviorConfig
import com.aibrowser.ui.screens.settings.BehaviorSettingsTab
import com.aibrowser.ui.screens.settings.CloudLlmTab
import com.aibrowser.ui.screens.settings.MnnMarketplaceTab
import com.aibrowser.ui.screens.settings.MnnSettingsTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    aiService: AiService,
    mnnLlmProvider: MnnLlmProvider? = null,
    onBack: () -> Unit
) {
    val config by settingsRepository.apiConfig.collectAsState(initial = ApiConfig())
    val behavior by settingsRepository.behaviorConfig.collectAsState(initial = BehaviorConfig())
    val notesDirectoryUri by settingsRepository.notesDirectoryUri.collectAsState(initial = null)
    val mnnModelPath by settingsRepository.mnnModelPath.collectAsState(initial = "")
    val mnnBackend by settingsRepository.mnnBackend.collectAsState(initial = "cpu")
    val mnnUseMmap by settingsRepository.mnnUseMmap.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cloud LLM", "MNN Local AI", "Marketplace", "Behavior")

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
                1 -> MnnSettingsTab(
                    modelPath = mnnModelPath,
                    backend = mnnBackend,
                    useMmap = mnnUseMmap,
                    settingsRepository = settingsRepository,
                    scope = scope,
                    mnnLlmProvider = mnnLlmProvider
                )
                2 -> MnnMarketplaceTab(
                    settingsRepository = settingsRepository,
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
