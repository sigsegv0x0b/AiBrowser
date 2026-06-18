package com.aibrowser.ui.screens.settings
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.LocalLlmProvider
import com.aibrowser.agent.LocalModelManager
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.LocalLlmConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiteRtTab(
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

