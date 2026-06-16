package com.aibrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AiService
import com.aibrowser.agent.LocalLlmProvider
import com.aibrowser.agent.LocalModelManager
import com.aibrowser.agent.TestResult
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.BehaviorConfig
import com.aibrowser.data.models.LocalLlmConfig
import com.aibrowser.data.models.ModelInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    aiService: AiService,
    localModelManager: LocalModelManager,
    localLlmProvider: LocalLlmProvider,
    onBack: () -> Unit
) {
    val config by settingsRepository.apiConfig.collectAsState(initial = ApiConfig())
    val behavior by settingsRepository.behaviorConfig.collectAsState(initial = BehaviorConfig())
    val localLlmConfig by settingsRepository.localLlmConfig.collectAsState(initial = LocalLlmConfig())
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cloud LLM", "Local LLM", "Behavior")

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
                1 -> LocalLlmTab(
                    localLlmConfig = localLlmConfig,
                    localModelManager = localModelManager,
                    settingsRepository = settingsRepository,
                    localLlmProvider = localLlmProvider,
                    scope = scope
                )
                2 -> BehaviorSettingsTab(
                    behavior = behavior,
                    onSave = { scope.launch { settingsRepository.saveBehaviorConfig(it) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudLlmTab(
    config: ApiConfig,
    aiService: AiService,
    settingsRepository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    var provider by remember(config) { mutableStateOf(config.provider) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var model by remember(config) { mutableStateOf(config.model) }
    var baseUrl by remember(config) { mutableStateOf(config.baseUrl) }
    var contextSize by remember(config) { mutableStateOf(config.contextSize.toString()) }
    var maxOutputTokens by remember(config) { mutableStateOf(config.maxOutputTokens.toString()) }
    var useCustomModel by remember(config) { mutableStateOf(config.model.isNotEmpty() && config.provider != ApiConfig.ApiProvider.CLAUDE && config.provider != ApiConfig.ApiProvider.LOCAL) }

    var fetchedModels by remember { mutableStateOf<List<ModelInfo>>(emptyList()) }
    var isFetching by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    val currentConfig = ApiConfig(provider = provider, apiKey = apiKey, model = model, baseUrl = baseUrl)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Provider", style = MaterialTheme.typography.titleMedium)
        var providerExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = providerExpanded,
            onExpandedChange = { providerExpanded = it }
        ) {
            OutlinedTextField(
                value = provider.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = providerExpanded,
                onDismissRequest = { providerExpanded = false }
            ) {
                ApiConfig.ApiProvider.entries.filter { it != ApiConfig.ApiProvider.LOCAL }.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.displayName) },
                        onClick = {
                            provider = p
                            model = p.defaultModel
                            baseUrl = p.defaultBaseUrl
                            contextSize = ""
                            maxOutputTokens = ""
                            useCustomModel = false
                            fetchedModels = emptyList()
                            testResult = null
                            providerExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; testResult = null },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it; testResult = null },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isFetching = true
                        testResult = null
                        fetchedModels = aiService.listModels(currentConfig)
                        if (fetchedModels.isEmpty()) testResult = "No models found or endpoint not supported"
                        isFetching = false
                    }
                },
                enabled = !isFetching && provider != ApiConfig.ApiProvider.CLAUDE && apiKey.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (isFetching) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Fetch Models")
            }
            Button(
                onClick = {
                    scope.launch {
                        isTesting = true
                        testResult = null
                        testResult = aiService.testConnection(currentConfig)
                        isTesting = false
                    }
                },
                enabled = !isTesting && model.isNotBlank() && apiKey.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (isTesting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Test")
            }
        }

        if (testResult != null) {
            val result = testResult!!
            val isError = result.startsWith("Error")
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(result, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }

        HorizontalDivider()
        Text("Model", style = MaterialTheme.typography.titleMedium)

        if (fetchedModels.isNotEmpty()) {
            var modelExpanded by remember { mutableStateOf(false) }
            val modelIds = remember(fetchedModels) { fetchedModels.map { it.id } }
            val displayItems = remember(fetchedModels, useCustomModel) {
                val items = modelIds.toMutableList()
                if (useCustomModel && model !in modelIds && model.isNotBlank()) items.add(0, model)
                items
            }
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = it }
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                    label = { Text("Select Model") }
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    displayItems.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = {
                                model = m
                                useCustomModel = false
                                modelExpanded = false
                                val info = fetchedModels.find { it.id == m }
                                if (info != null) {
                                    if (info.contextSize != null) contextSize = info.contextSize.toString()
                                    if (info.maxOutput != null) maxOutputTokens = info.maxOutput.toString()
                                }
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Custom...") },
                        onClick = { useCustomModel = true; modelExpanded = false }
                    )
                }
            }
        }

        if (useCustomModel || fetchedModels.isEmpty()) {
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text(if (fetchedModels.isEmpty()) "Model" else "Custom Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }

        OutlinedTextField(
            value = contextSize,
            onValueChange = { contextSize = it },
            label = { Text("Context Size (tokens)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            placeholder = { Text("Auto") }
        )

        OutlinedTextField(
            value = maxOutputTokens,
            onValueChange = { maxOutputTokens = it },
            label = { Text("Max Output Tokens") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            placeholder = { Text("Auto") }
        )

        Button(
            onClick = {
                scope.launch {
                    settingsRepository.saveApiConfig(
                        ApiConfig(
                            provider = provider,
                            apiKey = apiKey,
                            model = model,
                            baseUrl = baseUrl,
                            contextSize = contextSize.toIntOrNull() ?: 0,
                            maxOutputTokens = maxOutputTokens.toIntOrNull() ?: 0
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalLlmTab(
    localLlmConfig: LocalLlmConfig,
    localModelManager: LocalModelManager,
    settingsRepository: SettingsRepository,
    localLlmProvider: LocalLlmProvider,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var localBackend by remember(localLlmConfig) { mutableStateOf(localLlmConfig.backend) }
    var selectedModelId by remember(localLlmConfig) { mutableStateOf(localLlmConfig.downloadedModelId) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatus by remember { mutableStateOf<String?>(null) }
    var downloadSpeed by remember { mutableFloatStateOf(0f) }
    var maxTokens by remember(localLlmConfig) {
        mutableStateOf(if (localLlmConfig.maxTokens > 0) localLlmConfig.maxTokens.toString() else "")
    }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestResult?>(null) }

    val availableModels = localModelManager.availableModels

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Select & download a model to run locally on device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("How it works", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text("Models run on your Android device using LiteRT-LM with NPU acceleration (Qualcomm Hexagon). No internet required after download. Requires 4-8 GB RAM depending on model size.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        HorizontalDivider()
        Text("Model", style = MaterialTheme.typography.titleMedium)

        availableModels.forEach { remoteModel ->
            val isDownloaded = localModelManager.isModelDownloaded(remoteModel.id)
            val isSelected = selectedModelId == remoteModel.id
            val fileSize = if (isDownloaded) localModelManager.getModelSize(remoteModel.id) else 0L
            val isCurrentDownload = isDownloading && isSelected

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSelected && isDownloaded -> MaterialTheme.colorScheme.primaryContainer
                        isDownloaded -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(remoteModel.displayName, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(2.dp))
                            val sizeText = if (remoteModel.sizeMb >= 1000) "%.1f GB".format(remoteModel.sizeMb / 1000f) else "${remoteModel.sizeMb} MB"
                            Text("$sizeText  ·  ${remoteModel.minRamGb}+ GB RAM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isDownloaded) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }

                    if (isDownloaded) {
                        Spacer(Modifier.height(4.dp))
                        Text("${fileSize / (1024 * 1024)} MB on disk",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isDownloaded) {
                            Button(
                                onClick = {
                                    selectedModelId = remoteModel.id
                                    scope.launch {
                                        settingsRepository.saveLocalLlmConfig(
                                            localLlmConfig.copy(
                                                downloadedModelPath = localModelManager.getDownloadedModelPath(remoteModel.id) ?: "",
                                                downloadedModelId = remoteModel.id,
                                                backend = localBackend
                                            )
                                        )
                                            settingsRepository.setProvider(ApiConfig.ApiProvider.LOCAL)
                                    }
                                },
                                enabled = !isSelected
                            ) {
                                Text(if (isSelected) "Active" else "Use Model")
                            }
                            OutlinedButton(
                                onClick = {
                                    localModelManager.deleteModel(remoteModel.id)
                                    if (selectedModelId == remoteModel.id) {
                                        selectedModelId = ""
                                        scope.launch {
                                            settingsRepository.saveLocalLlmConfig(
                                                localLlmConfig.copy(
                                                    downloadedModelPath = "",
                                                    downloadedModelId = "",
                                                    backend = localBackend
                                                )
                                            )
                                        }
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete")
                            }
                        } else {
                            Button(
                                onClick = {
                                    selectedModelId = remoteModel.id
                                    scope.launch {
                                        isDownloading = true
                                        downloadStatus = null
                                        downloadProgress = 0f
                                        downloadSpeed = 0f
                                        val result = localModelManager.downloadModel(remoteModel.id) { progress, speed ->
                                            downloadProgress = progress
                                            downloadSpeed = speed
                                        }
                                        result.fold(
                                            onSuccess = { path ->
                                                downloadStatus = "Download complete"
                                                scope.launch {
                                                    settingsRepository.saveLocalLlmConfig(
                                                        localLlmConfig.copy(
                                                            downloadedModelPath = path,
                                                            downloadedModelId = remoteModel.id,
                                                            backend = localBackend
                                                        )
                                                    )
                                                    settingsRepository.saveApiConfig(
                                                        ApiConfig(provider = ApiConfig.ApiProvider.LOCAL)
                                                    )
                                                }
                                            },
                                            onFailure = { error ->
                                                downloadStatus = "Error: ${error.message}"
                                            }
                                        )
                                        isDownloading = false
                                    }
                                },
                                enabled = !isDownloading
                            ) {
                                if (isCurrentDownload) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    val pct = (downloadProgress * 100).toInt()
                                    val speedStr = if (downloadSpeed > 0.01f) " · %.1f MB/s".format(downloadSpeed) else " · … MB/s"
                                    Text("$pct%$speedStr")
                                } else {
                                    val partialSize = localModelManager.getPartialDownloadSize(remoteModel.id)
                                    if (partialSize > 0) {
                                        val pct = (partialSize.toFloat() / (remoteModel.sizeMb.toLong() * 1024 * 1024)).coerceAtMost(0.99f)
                                        Text("Resume (${(pct * 100).toInt()}%)")
                                    } else {
                                        val btnSize = if (remoteModel.sizeMb >= 1000) "%.1f GB".format(remoteModel.sizeMb / 1000f) else "${remoteModel.sizeMb} MB"
                                        Text("Download ($btnSize)")
                                    }
                                }
                            }
                        }
                    }

                    if (isCurrentDownload) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                    }
                }
            }
        }

        if (downloadStatus != null) {
            val status = downloadStatus!!
            val isError = status.startsWith("Error")
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(status, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }

        HorizontalDivider()
        Text("Backend", style = MaterialTheme.typography.titleMedium)

        var backendExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = backendExpanded,
            onExpandedChange = { backendExpanded = it }
        ) {
            OutlinedTextField(
                value = localBackend.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(backendExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = backendExpanded,
                onDismissRequest = { backendExpanded = false }
            ) {
                LocalLlmConfig.Backend.entries.forEach { b ->
                    DropdownMenuItem(
                        text = { Text(b.displayName) },
                        onClick = {
                            localBackend = b
                            backendExpanded = false
                            scope.launch {
                                settingsRepository.saveLocalLlmConfig(
                                    localLlmConfig.copy(backend = b)
                                )
                            }
                        }
                    )
                }
            }
        }

        Text("Auto tries NPU → GPU → CPU. NPU requires Qualcomm Hexagon DSP.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider()
        Text("Generation", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = maxTokens,
            onValueChange = { newVal ->
                maxTokens = newVal
                val intVal = newVal.toIntOrNull()
                if (intVal != null || newVal.isEmpty()) {
                    scope.launch {
                        settingsRepository.saveLocalLlmConfig(
                            localLlmConfig.copy(maxTokens = intVal ?: 0)
                        )
                    }
                }
            },
            label = { Text("Max Tokens (Context Window)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            placeholder = { Text("16384 (model default)") },
            supportingText = { Text("Sets KV cache size. Higher = more memory. Blank = auto.") }
        )

        if (localLlmConfig.isModelReady) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        isTesting = true
                        testResult = null
                        testResult = localLlmProvider.testInference()
                        isTesting = false
                    }
                },
                enabled = !isTesting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Text("Test Inference")
                }
            }
        }

        if (testResult != null) {
            val result = testResult!!
            val isError = result.response.startsWith("Error") || result.response.startsWith("No model")
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Response:", style = MaterialTheme.typography.labelSmall)
                    Text(result.response, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Backend: ${result.backend}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (localLlmConfig.isModelReady) {
            HorizontalDivider()
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Ready", style = MaterialTheme.typography.titleSmall)
                    Text("Model: ${localLlmConfig.downloadedModelId}", style = MaterialTheme.typography.bodySmall)
                    Text("Backend: ${localLlmConfig.backend.displayName}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BehaviorSettingsTab(
    behavior: BehaviorConfig,
    onSave: (BehaviorConfig) -> Unit
) {
    var scrollIntoView by remember(behavior) { mutableStateOf(behavior.scrollIntoView) }
    var blockExternalIntents by remember(behavior) { mutableStateOf(behavior.blockExternalIntents) }
    var ttsPrompt by remember(behavior) { mutableStateOf(behavior.ttsPrompt) }
    var systemPrompt by remember(behavior) { mutableStateOf(behavior.systemPrompt) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Interaction", style = MaterialTheme.typography.titleMedium)

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Scroll element into view", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text("When enabled, the browser scrolls to center the target element before interacting.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = scrollIntoView, onCheckedChange = { scrollIntoView = it })
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Block external app intents", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text("Links like twitter:// are loaded as https:// instead of opening an external app.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = blockExternalIntents, onCheckedChange = { blockExternalIntents = it })
        }

        HorizontalDivider()
        Text("TTS Prompt", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = ttsPrompt, onValueChange = { ttsPrompt = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = { ttsPrompt = BehaviorConfig.DEFAULT_TTS_PROMPT }) {
            Text("Reset to Default")
        }

        HorizontalDivider()
        Text("System Prompt", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = systemPrompt, onValueChange = { systemPrompt = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = { systemPrompt = BehaviorConfig.DEFAULT_SYSTEM_PROMPT }) {
            Text("Reset to Default")
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                onSave(BehaviorConfig(
                    scrollIntoView = scrollIntoView,
                    blockExternalIntents = blockExternalIntents,
                    ttsPrompt = ttsPrompt,
                    systemPrompt = systemPrompt
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Behavior Settings")
        }

        Spacer(Modifier.height(32.dp))
    }
}
