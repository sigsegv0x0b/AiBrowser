package com.aibrowser.ui.screens.settings
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AiService
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.CloudProvider
import com.aibrowser.data.models.ModelInfo
import java.util.UUID

private fun Modifier.noAutofill(): Modifier = this.clearAndSetSemantics { }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudLlmTab(
    config: ApiConfig,
    cloudProviders: List<CloudProvider>,
    activeProviderId: String?,
    aiService: AiService,
    settingsRepository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<CloudProvider?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cloud Providers", style = MaterialTheme.typography.titleMedium)
            FilledTonalButton(onClick = {
                editingProvider = null
                showEditDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        if (cloudProviders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "No cloud providers configured. Tap \"Add\" to create one.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            cloudProviders.forEach { cp ->
                val isActive = cp.id == activeProviderId
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    onClick = { scope.launch { settingsRepository.setActiveProvider(cp.id) } }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    cp.name.ifBlank { cp.provider.displayName },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "${cp.provider.displayName} - ${cp.model}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isActive) {
                                Text(
                                    "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                editingProvider = cp
                                showEditDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                scope.launch { settingsRepository.deleteCloudProvider(cp.id) }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showEditDialog) {
        ProviderEditDialog(
            existing = editingProvider,
            aiService = aiService,
            scope = scope,
            onDismiss = { showEditDialog = false },
            onSave = { cp ->
                scope.launch {
                    settingsRepository.saveCloudProvider(cp)
                    if (editingProvider == null) {
                        settingsRepository.setActiveProvider(cp.id)
                    }
                    showEditDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderEditDialog(
    existing: CloudProvider?,
    aiService: AiService,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit,
    onSave: (CloudProvider) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var provider by remember { mutableStateOf(existing?.provider ?: ApiConfig.ApiProvider.OPENAI) }
    var apiKey by remember { mutableStateOf(existing?.apiKey ?: "") }
    var model by remember { mutableStateOf(existing?.model ?: provider.defaultModel) }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: provider.defaultBaseUrl) }
    var contextSize by remember { mutableStateOf((existing?.contextSize ?: 0).toString()) }
    var maxOutputTokens by remember { mutableStateOf((existing?.maxOutputTokens ?: 0).toString()) }
    var useCustomModel by remember {
        mutableStateOf(existing != null && existing.model.isNotEmpty() &&
                        existing.provider != ApiConfig.ApiProvider.CLAUDE)
    }

    var fetchedModels by remember { mutableStateOf<List<ModelInfo>>(emptyList()) }
    var isFetching by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    val currentConfig = ApiConfig(provider = provider, apiKey = apiKey, model = model, baseUrl = baseUrl)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = { Text(if (existing == null) "Add Provider" else "Edit Provider") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. My GPT-4o") },
                    modifier = Modifier.fillMaxWidth().noAutofill(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = true, imeAction = ImeAction.Next)
                )

                var providerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = provider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        ApiConfig.ApiProvider.entries.filter { it != ApiConfig.ApiProvider.LOCAL_MNN }.forEach { p ->
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
                    modifier = Modifier.fillMaxWidth().noAutofill(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; testResult = null },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth().noAutofill(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isFetching = true
                                fetchedModels = aiService.listModels(currentConfig)
                                if (fetchedModels.isEmpty()) testResult = "No models found or endpoint not supported"
                                isFetching = false
                            }
                        },
                        enabled = !isFetching && apiKey.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isFetching) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Fetch Models")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                isTesting = true
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

                testResult?.let { result ->
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

                if (fetchedModels.isNotEmpty()) {
                    var modelExpanded by remember { mutableStateOf(false) }
                    val displayItems = remember(fetchedModels, useCustomModel, model) {
                        val items = fetchedModels.map { it.id }.toMutableList()
                        if (useCustomModel && model !in items && model.isNotBlank()) items.add(0, model)
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
                            label = { Text("Select Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth().noAutofill(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = true, imeAction = ImeAction.Next)
                    )
                }

                OutlinedTextField(
                    value = contextSize,
                    onValueChange = { contextSize = it },
                    label = { Text("Context Size (tokens)") },
                    modifier = Modifier.fillMaxWidth().noAutofill(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    placeholder = { Text("Auto") }
                )

                OutlinedTextField(
                    value = maxOutputTokens,
                    onValueChange = { maxOutputTokens = it },
                    label = { Text("Max Output Tokens") },
                    modifier = Modifier.fillMaxWidth().noAutofill(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    placeholder = { Text("Auto") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = existing?.id ?: UUID.randomUUID().toString()
                    onSave(
                        CloudProvider(
                            id = id,
                            name = name.ifBlank { provider.displayName },
                            provider = provider,
                            apiKey = apiKey,
                            model = model,
                            baseUrl = baseUrl,
                            contextSize = contextSize.toIntOrNull() ?: 0,
                            maxOutputTokens = maxOutputTokens.toIntOrNull() ?: 0
                        )
                    )
                },
                enabled = apiKey.isNotBlank() && model.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
