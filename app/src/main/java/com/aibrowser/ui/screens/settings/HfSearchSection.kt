package com.aibrowser.ui.screens.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.HfFileInfo
import com.aibrowser.agent.HfModelSummary
import com.aibrowser.agent.HfSearchService
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.AvailableGgufModel
import com.aibrowser.data.models.LlamaCppSettings
import com.geniex.sdk.ModelManagerWrapper
import com.geniex.sdk.bean.HubSource
import com.geniex.sdk.bean.ModelPullInput
import kotlinx.coroutines.launch

@Composable
fun HfSearchSection(
    hfToken: String,
    settingsRepository: SettingsRepository,
    llamaCppSettings: LlamaCppSettings,
    scope: kotlinx.coroutines.CoroutineScope,
    onModelSelected: (String) -> Unit,
    onHfModelDownloaded: (AvailableGgufModel) -> Unit
) {
    var searchExpanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { searchExpanded = !searchExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Search HuggingFace", style = MaterialTheme.typography.titleMedium)
            Icon(
                if (searchExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (searchExpanded) "Collapse" else "Expand"
            )
        }

        if (searchExpanded) {
            var searchQuery by remember { mutableStateOf("") }
            var searchResults by remember { mutableStateOf<List<HfModelSummary>?>(null) }
            var isSearching by remember { mutableStateOf(false) }
            var searchError by remember { mutableStateOf<String?>(null) }
            var expandedModelId by remember { mutableStateOf<String?>(null) }
            var modelFiles by remember { mutableStateOf<Map<String, List<HfFileInfo>>>(emptyMap()) }
            var modelDownloaded by remember { mutableStateOf<Set<String>>(emptySet()) }
            var hfDownloadingFile by remember { mutableStateOf<String?>(null) }
            var hfDownloadProgress by remember { mutableFloatStateOf(0f) }
            var hfDownloadBytes by remember { mutableLongStateOf(0L) }
            var hfDownloadTotal by remember { mutableLongStateOf(0L) }
            var hfDownloadSpeed by remember { mutableFloatStateOf(0f) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("llama 3b gguf") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isSearching
                )
                Button(
                    onClick = {
                        if (searchQuery.isBlank()) return@Button
                        isSearching = true
                        searchError = null
                        searchResults = null
                        expandedModelId = null
                        modelFiles = emptyMap()
                        scope.launch {
                            try {
                                val service = HfSearchService(hfToken)
                                searchResults = service.searchModels(searchQuery)
                            } catch (e: Exception) {
                                searchError = e.message
                            } finally {
                                isSearching = false
                            }
                        }
                    },
                    enabled = !isSearching
                ) {
                    if (isSearching) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Search")
                }
            }

            searchError?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            searchResults?.let { results ->
                if (results.isEmpty()) {
                    Text("No models found.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("${results.size} results", style = MaterialTheme.typography.labelSmall)
                    results.forEach { summary ->
                        val isExpanded = expandedModelId == summary.modelId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    if (isExpanded) {
                                        expandedModelId = null
                                    } else {
                                        expandedModelId = summary.modelId
                                        scope.launch {
                                            try {
                                                val service = HfSearchService(hfToken)
                                                val files = service.getModelFiles(summary.modelId)
                                                modelFiles = modelFiles + (summary.modelId to files)
                                                val paths = ModelManagerWrapper.getPaths(summary.modelId)
                                                if (paths != null) {
                                                    modelDownloaded = modelDownloaded + summary.modelId
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(summary.modelId, style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium)
                                            if (summary.isGated) {
                                                Spacer(Modifier.width(4.dp))
                                                Text("G!", style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Text("\uD83D\uDCE5 ${summary.downloads}  \u2764\uFE0F ${summary.likes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                }

                                if (isExpanded) {
                                    val files = modelFiles[summary.modelId]
                                    if (files == null) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else if (files.isEmpty()) {
                                        Text("No GGUF files found.", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        files.forEach { file ->
                                            val isHfDownloading = hfDownloadingFile == file.filename
                                            val alreadyDownloaded = summary.modelId in modelDownloaded
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(HfSearchService.extractQuantLabel(file.filename),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium)
                                                    Text(HfSearchService.formatSizeMb(file.size),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                if (alreadyDownloaded) {
                                                    Text("Downloaded", style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary)
                                                } else if (isHfDownloading) {
                                                    Column(modifier = Modifier.fillMaxWidth()) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                            Spacer(Modifier.width(8.dp))
                                                            val pct = (hfDownloadProgress * 100).toInt()
                                                            val speedStr = if (hfDownloadSpeed > 0.01f) " · %.1f MB/s".format(hfDownloadSpeed) else ""
                                                            Text("$pct%$speedStr", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                        if (hfDownloadTotal > 0) {
                                                            Text("${"%.1f".format(hfDownloadBytes / 1048576f)} / ${"%.1f".format(hfDownloadTotal / 1048576f)} MB",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        LinearProgressIndicator(
                                                            progress = { hfDownloadProgress },
                                                            modifier = Modifier.fillMaxWidth().height(4.dp)
                                                        )
                                                    }
                                                } else {
                                                    TextButton(onClick = {
                                                        scope.launch {
                                                            hfDownloadingFile = file.filename
                                                            hfDownloadProgress = 0f
                                                            hfDownloadBytes = 0L
                                                            hfDownloadTotal = 0L
                                                            hfDownloadSpeed = 0f
                                                            var lastBytes = 0L
                                                            var lastTime = System.currentTimeMillis()
                                                            try {
                                                                val quant = HfSearchService.extractQuantLabel(file.filename)
                                                                val input = ModelPullInput(
                                                                    model_name = summary.modelId,
                                                                    quant = quant,
                                                                    hub = HubSource.HUGGINGFACE
                                                                )
                                                                ModelManagerWrapper.pullFlow(input).collect { event ->
                                                                    when (event) {
                                                                        is ModelManagerWrapper.PullEvent.Progress -> {
                                                                            val total = event.files.sumOf { if (it.total_bytes > 0) it.total_bytes else 0L }
                                                                            val done = event.files.sumOf { it.downloaded_bytes }
                                                                            hfDownloadProgress = if (total > 0) done.toFloat() / total else 0f
                                                                            hfDownloadBytes = done
                                                                            hfDownloadTotal = total
                                                                            val now = System.currentTimeMillis()
                                                                            val dt = (now - lastTime).coerceAtLeast(1)
                                                                            if (dt >= 1000) {
                                                                                val speed = (done - lastBytes).toFloat() / (dt / 1000f) / (1024f * 1024f)
                                                                                hfDownloadSpeed = speed
                                                                                lastBytes = done
                                                                                lastTime = now
                                                                            }
                                                                        }
                                                                        is ModelManagerWrapper.PullEvent.Completed -> {
                                                                            onModelSelected(summary.modelId)
                                                                            modelDownloaded = modelDownloaded + summary.modelId
                                                                            val quant = HfSearchService.extractQuantLabel(file.filename)
                                                                            val m = AvailableGgufModel(
                                                                                id = summary.modelId,
                                                                                displayName = "${summary.modelId.split("/").last()} $quant",
                                                                                repo = summary.modelId,
                                                                                filename = file.filename,
                                                                                description = "HF: ${summary.modelId}",
                                                                                sizeBytes = file.size,
                                                                                quant = quant,
                                                                                contextSize = 8192,
                                                                                minRamGb = 4,
                                                                                isGated = summary.isGated
                                                                            )
                                                                            onHfModelDownloaded(m)
                                                                            settingsRepository.saveLlamaCppSettings(
                                                                                llamaCppSettings.copy(selectedModel = summary.modelId)
                                                                            )
                                                                            settingsRepository.setProvider(ApiConfig.ApiProvider.LOCAL_LLAMACPP)
                                                                        }
                                                                        is ModelManagerWrapper.PullEvent.Error -> {}
                                                                    }
                                                                }
                                                            } catch (_: Exception) {}
                                                            hfDownloadingFile = null
                                                        }
                                                    }) {
                                                        Text("Download", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
