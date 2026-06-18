package com.aibrowser.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.GgufModelCatalog
import com.aibrowser.agent.HfFileInfo
import com.aibrowser.agent.HfModelSummary
import com.aibrowser.agent.HfSearchService
import com.aibrowser.agent.LlamaCppProvider
import com.aibrowser.agent.ModelDownloader
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.AvailableGgufModel
import com.aibrowser.data.models.LlamaCppSettings
import com.geniex.sdk.ModelManagerWrapper
import com.geniex.sdk.bean.HubSource
import com.geniex.sdk.bean.ModelPullInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlamaCppTab(
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
    var hfDownloadedModels by remember { mutableStateOf<List<AvailableGgufModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        hfToken = settingsRepository.hfToken.first() ?: ""
    }

    val catalogModels = remember { GgufModelCatalog.models }
    val hasToken = hfToken.isNotBlank()
    val filteredModels = remember(filterQuery, catalogModels, hfDownloadedModels, hasToken) {
        val base = (if (hasToken) catalogModels else catalogModels.filter { !it.isGated }) + hfDownloadedModels
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
                                        downloadSpeed = 0f
                                        var lastBytes = 0L
                                        var lastTime = System.currentTimeMillis()
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
                                                    val now = System.currentTimeMillis()
                                                    val dt = (now - lastTime).coerceAtLeast(1)
                                                    if (dt >= 1000) {
                                                        downloadSpeed = (done - lastBytes).toFloat() / (dt / 1000f) / (1024f * 1024f)
                                                        lastBytes = done
                                                        lastTime = now
                                                    }
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
                                    val pct = (downloadProgress * 100).toInt()
                                    val speedStr = if (downloadSpeed > 0.01f) " · %.1f MB/s".format(downloadSpeed) else ""
                                    Text("$pct%$speedStr")
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
        HfSearchSection(
            hfToken = hfToken,
            settingsRepository = settingsRepository,
            llamaCppSettings = llamaCppSettings,
            scope = scope,
            onModelSelected = { selectedModel = it },
            onHfModelDownloaded = { hfDownloadedModels = hfDownloadedModels + it }
        )

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

