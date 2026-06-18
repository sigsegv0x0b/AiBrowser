package com.aibrowser.ui.screens.settings
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AiService
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.ModelInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudLlmTab(
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
