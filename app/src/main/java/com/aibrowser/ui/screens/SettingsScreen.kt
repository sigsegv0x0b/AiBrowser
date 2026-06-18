package com.aibrowser.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.aibrowser.agent.AiService
import com.aibrowser.agent.GgufModelCatalog
import com.aibrowser.agent.LlamaCppProvider
import com.aibrowser.agent.LocalLlmProvider
import com.aibrowser.agent.LocalModelManager
import com.aibrowser.agent.ModelDownloader
import com.aibrowser.agent.TestResult
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.AvailableGgufModel
import com.aibrowser.data.models.BehaviorConfig
import com.aibrowser.data.models.LlamaCppSettings
import com.aibrowser.data.models.LocalLlmConfig
import com.aibrowser.data.models.ModelInfo
import com.geniex.sdk.ModelManagerWrapper
import com.geniex.sdk.bean.HubSource
import com.geniex.sdk.bean.ModelPullInput
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    var useCustomModel by remember(config) { mutableStateOf(config.model.isNotEmpty() && config.provider != ApiConfig.ApiProvider.CLAUDE && config.provider != ApiConfig.ApiProvider.LOCAL_LLAMACPP && config.provider != ApiConfig.ApiProvider.LOCAL_LITERT) }

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
                ApiConfig.ApiProvider.entries.filter { it != ApiConfig.ApiProvider.LOCAL_LLAMACPP && it != ApiConfig.ApiProvider.LOCAL_LITERT }.forEach { p ->
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
private fun LiteRtTab(
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
                                            settingsRepository.setProvider(ApiConfig.ApiProvider.LOCAL_LITERT)
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
                                                        ApiConfig(provider = ApiConfig.ApiProvider.LOCAL_LITERT)
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
            HorizontalDivider()
            Text("Benchmark", style = MaterialTheme.typography.titleMedium)
            var benchmarkResult by remember { mutableStateOf<String?>(null) }
            var isBenchmarking by remember { mutableStateOf(false) }
            var benchmarkStep by remember { mutableStateOf<String?>(null) }

            Button(
                onClick = {
                    isBenchmarking = true
                    benchmarkResult = null
                    benchmarkStep = null
                    scope.launch {
                        try {
                            benchmarkStep = "Loading model..."
                            val testResult = localLlmProvider.testInference()
                            benchmarkStep = "Running inference..."
                            val stats = localLlmProvider.lastStats.value
                            benchmarkStep = null

                            benchmarkResult = buildString {
                                appendLine("Model: ${localLlmConfig.downloadedModelId}")
                                appendLine("Backend: ${stats?.device ?: testResult.backend}")
                                appendLine("Output: \"${testResult.response.take(120).trimEnd()}...\"")
                                if (stats != null && stats.decodingSpeed > 0) {
                                    appendLine("——————————")
                                    appendLine("TTFT: ${"%.0f".format(stats.ttftMs)} ms")
                                    appendLine("Decoding: ${"%.1f".format(stats.decodingSpeed)} tok/s")
                                    appendLine("Tokens: ${stats.generatedTokens} gen / ${stats.promptTokens} prompt")
                                } else {
                                    appendLine("(no profile data)")
                                }
                            }
                        } catch (e: Exception) {
                            benchmarkResult = "Error: ${e.message}"
                            benchmarkStep = "Failed"
                        } finally {
                            isBenchmarking = false
                        }
                    }
                },
                enabled = !isBenchmarking,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isBenchmarking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isBenchmarking) "Running..." else "Run Benchmark")
            }

            benchmarkStep?.let { step ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (step == "Failed") MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            benchmarkResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                val isError = result.startsWith("Error")
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(result, modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LlamaCppTab(
    llamaCppSettings: LlamaCppSettings,
    settingsRepository: SettingsRepository,
    llamaCppProvider: LlamaCppProvider,
    modelDownloader: ModelDownloader,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var filterQuery by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadSpeed by remember { mutableFloatStateOf(0f) }
    var downloadStatus by remember { mutableStateOf<String?>(null) }
    var selectedModel by remember(llamaCppSettings) { mutableStateOf(llamaCppSettings.selectedModel) }
    var backend by remember(llamaCppSettings) { mutableStateOf(llamaCppSettings.backend) }
    var nGpuLayers by remember(llamaCppSettings) { mutableStateOf(llamaCppSettings.nGpuLayers.toString()) }
    var contextWindow by remember(llamaCppSettings) { mutableStateOf(llamaCppSettings.contextWindow.toString()) }
    var maxTokens by remember(llamaCppSettings) { mutableStateOf(llamaCppSettings.maxTokens.toString()) }
    var temperature by remember(llamaCppSettings) { mutableStateOf(llamaCppSettings.temperature.toString()) }
    var topK by remember(llamaCppSettings) { mutableStateOf(llamaCppSettings.topK.toString()) }
    var topP by remember(llamaCppSettings) { mutableStateOf(llamaCppSettings.topP.toString()) }
    var useCustomSampler by remember(llamaCppSettings) { mutableStateOf(llamaCppSettings.useCustomSampler) }
    var downloadingModelId by remember { mutableStateOf<String?>(null) }
    var hfToken by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        hfToken = settingsRepository.hfToken.first() ?: ""
    }

    val catalogModels = remember { GgufModelCatalog.models }
    val hasToken = hfToken.isNotBlank()
    val filteredModels = remember(filterQuery, catalogModels, hasToken) {
        val base = if (hasToken) catalogModels else catalogModels.filter { !it.isGated }
        if (filterQuery.isBlank()) base
        else base.filter { m ->
            m.displayName.contains(filterQuery, ignoreCase = true) ||
            m.description.contains(filterQuery, ignoreCase = true) ||
            m.quant.contains(filterQuery, ignoreCase = true) ||
            m.repo.contains(filterQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Download a GGUF model from HuggingFace.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("llama.cpp via GenieX", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text("Uses Qualcomm GenieX SDK with llama.cpp backend. GGUF models run on-device with NPU/GPU acceleration. Requires 4-8 GB RAM depending on model size.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        HorizontalDivider()
        Text("HF Token", style = MaterialTheme.typography.titleMedium)
        Text("Set a HuggingFace access token to download gated models. Create one at huggingface.co/settings/tokens.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = hfToken,
            onValueChange = { hfToken = it },
            label = { Text("HuggingFace Token") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            placeholder = { Text("hf_...") }
        )
        Button(
            onClick = {
                scope.launch { settingsRepository.saveHfToken(hfToken) }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Token")
        }

        HorizontalDivider()
        Text("Available Models", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = filterQuery,
            onValueChange = { filterQuery = it },
            label = { Text("Filter models") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp)
                )
            },
            placeholder = { Text("Search by name, quant, or description...") }
        )

        filteredModels.forEach { model ->
            var isDownloaded by remember { mutableStateOf(false) }
            LaunchedEffect(model.repo) {
                isDownloaded = ModelManagerWrapper.getPaths(model.repo) != null
            }
            val isSelected = selectedModel == model.repo
            val isCurrentDownload = isDownloading && downloadingModelId == model.id

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isDownloaded -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(model.displayName, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(2.dp))
                            Text(model.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            Text("%.1f GB  ·  %s  ·  %d+ GB RAM  ·  %s context".format(
                                model.sizeGb, model.quant, model.minRamGb,
                                if (model.contextSize >= 1000) "${model.contextSize / 1000}k" else "${model.contextSize}"
                            ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isDownloaded) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isDownloaded) {
                            Button(
                                onClick = {
                                    selectedModel = model.repo
                                    scope.launch {
                                        settingsRepository.saveLlamaCppSettings(
                                            llamaCppSettings.copy(selectedModel = model.repo)
                                        )
                                        settingsRepository.setProvider(ApiConfig.ApiProvider.LOCAL_LLAMACPP)
                                    }
                                },
                                enabled = !isSelected
                            ) {
                                Text(if (isSelected) "Active" else "Use Model")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        ModelManagerWrapper.remove(model.repo)
                                        if (isSelected) {
                                            selectedModel = ""
                                            settingsRepository.saveLlamaCppSettings(
                                                llamaCppSettings.copy(selectedModel = "")
                                            )
                                        }
                                        isDownloaded = false
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
                                    scope.launch {
                                        downloadingModelId = model.id
                                        isDownloading = true
                                        downloadStatus = null
                                        downloadProgress = 0f
                                        try {
                                            val input = ModelPullInput(
                                                model_name = model.repo,
                                                quant = model.quant,
                                                hub = HubSource.HUGGINGFACE
                                            )
                                            ModelManagerWrapper.pullFlow(input).collect { event ->
                                                when (event) {
                                                    is ModelManagerWrapper.PullEvent.Progress -> {
                                                        val total = event.files.sumOf { if (it.total_bytes > 0) it.total_bytes else 0L }
                                                        val done = event.files.sumOf { it.downloaded_bytes }
                                                        downloadProgress = if (total > 0) done.toFloat() / total else 0f
                                                        downloadSpeed = 0f
                                                    }
                                                    is ModelManagerWrapper.PullEvent.Completed -> {
                                                        selectedModel = model.repo
                                                        downloadStatus = "Downloaded: ${model.displayName}"
                                                        isDownloaded = true
                                                        settingsRepository.saveLlamaCppSettings(
                                                            llamaCppSettings.copy(selectedModel = model.repo)
                                                        )
                                                        settingsRepository.setProvider(ApiConfig.ApiProvider.LOCAL_LLAMACPP)
                                                    }
                                                    is ModelManagerWrapper.PullEvent.Error -> {
                                                        downloadStatus = "Error: ${event.message}"
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            downloadStatus = "Error: ${e.message}"
                                        } finally {
                                            isDownloading = false
                                            downloadingModelId = null
                                        }
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
                                    Text("${(downloadProgress * 100).toInt()}%")
                                } else {
                                    Text("Download (%.1f GB)".format(model.sizeGb))
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

        if (filteredModels.isEmpty() && filterQuery.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("No models matching \"$filterQuery\"",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall)
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
        Text("Model Path", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = selectedModel,
            onValueChange = { selectedModel = it },
            label = { Text("GGUF file path") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("/data/.../model.gguf") }
        )

        HorizontalDivider()
        Text("Backend", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "cpu" to "CPU",
                "gpu" to "GPU",
                "npu" to "NPU"
            ).forEach { (value, label) ->
                FilterChip(
                    selected = backend == value,
                    onClick = { backend = value },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (backend == "gpu") {
            OutlinedTextField(
                value = nGpuLayers,
                onValueChange = { nGpuLayers = it },
                label = { Text("GPU Layers") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                supportingText = { Text("1–999 layers offloaded to GPU") }
            )
        }

        HorizontalDivider()
        Text("Inference Parameters", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = contextWindow,
            onValueChange = { contextWindow = it },
            label = { Text("Context Window (tokens)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            supportingText = { Text("Larger = more memory, smaller = faster") }
        )

        OutlinedTextField(
            value = maxTokens,
            onValueChange = { maxTokens = it },
            label = { Text("Max Output Tokens") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            supportingText = { Text("Max tokens per response") }
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Use Custom Sampler", style = MaterialTheme.typography.bodyLarge)
                Text("When off, bridge defaults are used (temperature, topK, topP are ignored).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = useCustomSampler, onCheckedChange = { useCustomSampler = it })
        }

        if (useCustomSampler) {
            OutlinedTextField(
                value = temperature,
                onValueChange = { temperature = it },
                label = { Text("Temperature") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                supportingText = { Text("0-2, lower = more deterministic") }
            )

            OutlinedTextField(
                value = topK,
                onValueChange = { topK = it },
                label = { Text("Top-K") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                supportingText = { Text("Limits token selection to top K") }
            )

            OutlinedTextField(
                value = topP,
                onValueChange = { topP = it },
                label = { Text("Top-P") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                supportingText = { Text("Nucleus sampling threshold (0-1)") }
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    settingsRepository.saveLlamaCppSettings(
                        LlamaCppSettings(
                            selectedModel = selectedModel,
                            backend = backend,
                            nGpuLayers = nGpuLayers.toIntOrNull() ?: 33,
                            contextWindow = contextWindow.toIntOrNull() ?: 8192,
                            maxTokens = maxTokens.toIntOrNull() ?: 4096,
                            temperature = temperature.toFloatOrNull() ?: 0.7f,
                            topK = topK.toIntOrNull() ?: 40,
                            topP = topP.toFloatOrNull() ?: 0.95f,
                            useCustomSampler = useCustomSampler
                        )
                    )
                    if (selectedModel.isNotBlank()) {
                        settingsRepository.setProvider(ApiConfig.ApiProvider.LOCAL_LLAMACPP)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        if (selectedModel.isNotBlank()) {
            HorizontalDivider()
            Text("Benchmark", style = MaterialTheme.typography.titleMedium)
            var benchmarkResult by remember { mutableStateOf<String?>(null) }
            var isBenchmarking by remember { mutableStateOf(false) }
            var benchmarkStep by remember { mutableStateOf<String?>(null) }

            Button(
                onClick = {
                    isBenchmarking = true
                    benchmarkResult = null
                    benchmarkStep = null
                    scope.launch {
                        settingsRepository.saveLlamaCppSettings(
                            LlamaCppSettings(
                                selectedModel = selectedModel,
                                backend = backend,
                                nGpuLayers = nGpuLayers.toIntOrNull() ?: 33,
                                contextWindow = contextWindow.toIntOrNull() ?: 8192,
                                maxTokens = maxTokens.toIntOrNull() ?: 4096,
                                temperature = temperature.toFloatOrNull() ?: 0.7f,
                                topK = topK.toIntOrNull() ?: 40,
                                topP = topP.toFloatOrNull() ?: 0.95f,
                                useCustomSampler = useCustomSampler
                            )
                        )
                        try {
                            benchmarkStep = "Loading model..."
                            var outputText = ""
                            val output = llamaCppProvider.benchmarkInference(
                                onToken = { outputText += it },
                                onProgress = { benchmarkStep = it }
                            )
                            outputText = output
                            benchmarkStep = "Running inference..."
                            val stats = llamaCppProvider.lastStats.value
                            benchmarkStep = null

                            benchmarkResult = buildString {
                                appendLine("Model: $selectedModel")
                                appendLine("Backend: ${stats?.device ?: backend}")
                                appendLine("Output: \"${outputText.take(120).trimEnd()}...\"")
                                if (stats != null && stats.decodingSpeed > 0) {
                                    appendLine("————————————————————")
                                    appendLine("TTFT: ${"%.2f".format(stats.ttftMs)} ms")
                                    appendLine("Prompt Tokens: ${stats.promptTokens}")
                                    appendLine("Prefill Speed: ${"%.2f".format(stats.prefillSpeed)} tok/s")
                                    appendLine("Generated Tokens: ${stats.generatedTokens}")
                                    appendLine("Decoding Speed: ${"%.2f".format(stats.decodingSpeed)} tok/s")
                                } else {
                                    appendLine("(no profile data)")
                                }
                            }
                        } catch (e: Exception) {
                            benchmarkResult = "Error: ${e.message}"
                            benchmarkStep = "Failed"
                        } finally {
                            isBenchmarking = false
                        }
                    }
                },
                enabled = !isBenchmarking,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isBenchmarking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isBenchmarking) "Running..." else "Run Benchmark")
            }

            benchmarkStep?.let { step ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (step == "Failed") MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            benchmarkResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                val isError = result.startsWith("Error")
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(result, modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
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
    notesDirectoryUri: String?,
    settingsRepository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onSave: (BehaviorConfig) -> Unit
) {
    var scrollIntoView by remember(behavior) { mutableStateOf(behavior.scrollIntoView) }
    var blockExternalIntents by remember(behavior) { mutableStateOf(behavior.blockExternalIntents) }
    var ttsPrompt by remember(behavior) { mutableStateOf(behavior.ttsPrompt) }
    var systemPrompt by remember(behavior) { mutableStateOf(behavior.systemPrompt) }

    val context = LocalContext.current
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scope.launch {
                settingsRepository.saveNotesDirectoryUri(uri.toString())
            }
        }
    }

    val directoryName = remember(notesDirectoryUri) {
        if (notesDirectoryUri.isNullOrBlank()) null
        else {
            try {
                DocumentFile.fromTreeUri(context, Uri.parse(notesDirectoryUri))?.name
            } catch (_: Exception) { null }
        }
    }

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
        Text("Notes Directory", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (directoryName != null) {
                    Text("Directory: $directoryName", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Not set — file_read/file_write/file_list tools will be unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.width(16.dp))
            OutlinedButton(onClick = { directoryPickerLauncher.launch(null) }) {
                Text(if (directoryName != null) "Change" else "Choose")
            }
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
