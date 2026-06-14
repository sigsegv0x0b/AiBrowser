package com.aibrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AiService
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.BehaviorConfig
import com.aibrowser.data.models.ModelInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    aiService: AiService,
    onBack: () -> Unit
) {
    val config by settingsRepository.apiConfig.collectAsState(initial = ApiConfig())
    val behavior by settingsRepository.behaviorConfig.collectAsState(initial = BehaviorConfig())
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("LLM", "Behavior")

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
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> LlmSettingsTab(
                    config = config,
                    aiService = aiService,
                    settingsRepository = settingsRepository,
                    scope = scope,
                    focusManager = focusManager
                )
                1 -> BehaviorSettingsTab(
                    behavior = behavior,
                    onSave = { scope.launch { settingsRepository.saveBehaviorConfig(it) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LlmSettingsTab(
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
    var useCustomModel by remember(config) { mutableStateOf(config.model.isNotEmpty() && config.provider != ApiConfig.ApiProvider.CLAUDE) }

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
        Text("API Provider", style = MaterialTheme.typography.titleMedium)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = provider.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ApiConfig.ApiProvider.entries.forEach { p ->
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
                            expanded = false
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
                        if (fetchedModels.isEmpty()) {
                            testResult = "No models found or endpoint not supported"
                        }
                        isFetching = false
                    }
                },
                enabled = !isFetching && provider != ApiConfig.ApiProvider.CLAUDE && apiKey.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Fetch Models")
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isTesting = true
                        testResult = null
                        val result = aiService.testConnection(currentConfig)
                        testResult = result
                        isTesting = false
                    }
                },
                enabled = !isTesting && model.isNotBlank() && apiKey.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Test")
                }
            }
        }

        if (testResult != null) {
            val isError = testResult!!.startsWith("Error")
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = testResult ?: "",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text("Model", style = MaterialTheme.typography.titleMedium)

        if (fetchedModels.isNotEmpty()) {
            var modelExpanded by remember { mutableStateOf(false) }
            val modelIds = remember(fetchedModels) { fetchedModels.map { it.id } }
            val displayItems = remember(fetchedModels, useCustomModel) {
                val items = modelIds.toMutableList()
                if (useCustomModel && model !in modelIds && model.isNotBlank()) {
                    items.add(0, model)
                }
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
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
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
                        onClick = {
                            useCustomModel = true
                            modelExpanded = false
                        }
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

        Text("Context Size (tokens)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = contextSize,
            onValueChange = { contextSize = it.filter { c -> c.isDigit() } },
            label = { Text("e.g. 128000") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            placeholder = { Text("Auto") }
        )

        Text("Max Output Tokens", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = maxOutputTokens,
            onValueChange = { maxOutputTokens = it.filter { c -> c.isDigit() } },
            label = { Text("e.g. 8192") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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

@Composable
private fun BehaviorSettingsTab(
    behavior: BehaviorConfig,
    onSave: (BehaviorConfig) -> Unit
) {
    var scrollIntoView by remember(behavior) { mutableStateOf(behavior.scrollIntoView) }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Scroll element into view",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "When enabled, the browser will scroll the viewport to center the target element before clicking, typing, or selecting it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = scrollIntoView,
                onCheckedChange = { scrollIntoView = it }
            )
        }

        HorizontalDivider()
        Text("TTS Prompt", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = ttsPrompt,
            onValueChange = { ttsPrompt = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(
            onClick = {
                ttsPrompt = BehaviorConfig.DEFAULT_TTS_PROMPT
            }
        ) {
            Text("Reset to Default")
        }

        HorizontalDivider()
        Text("System Prompt", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = systemPrompt,
            onValueChange = { systemPrompt = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(
            onClick = {
                systemPrompt = BehaviorConfig.DEFAULT_SYSTEM_PROMPT
            }
        ) {
            Text("Reset to Default")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                onSave(
                    BehaviorConfig(
                        scrollIntoView = scrollIntoView,
                        ttsPrompt = ttsPrompt,
                        systemPrompt = systemPrompt
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Behavior Settings")
        }

        Spacer(Modifier.height(32.dp))
    }
}
