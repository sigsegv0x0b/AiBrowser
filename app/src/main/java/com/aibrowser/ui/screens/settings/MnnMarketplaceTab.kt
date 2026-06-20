package com.aibrowser.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.mnn.market.DownloadedMnnModel
import com.aibrowser.agent.mnn.market.MarketModel
import com.aibrowser.agent.mnn.market.ModelMarket
import com.aibrowser.data.SettingsRepository
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

@Composable
fun MnnMarketplaceTab(
    settingsRepository: SettingsRepository,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    val gson = remember { Gson() }
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    data class DownloadState(val progress: Float = 0f, val speedMbS: Float = 0f,
                             val downloadedBytes: Long = 0, val totalBytes: Long = 0,
                             val modelId: String = "", val modelName: String = "")

    var models by remember { mutableStateOf<List<MarketModel>>(emptyList()) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    var downloadStates by remember { mutableStateOf<Map<String, DownloadState>>(emptyMap()) }
    var downloadErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var persistedDownloads by remember { mutableStateOf<Map<String, DownloadedMnnModel>>(emptyMap()) }

    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open("model_market.json").bufferedReader().readText()
                val market = gson.fromJson(json, ModelMarket::class.java)
                models = market.models
                    .filter { it.sources.containsKey("HuggingFace") }
                    .filter { !it.tags.contains("ImageGen") && !it.tags.contains("AudioGen") }
            } catch (e: Exception) {
                loadError = "Failed to load: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) {
        settingsRepository.downloadedModels.collect { list ->
            persistedDownloads = list.associateBy { it.modelId }
        }
    }

    val allTags = remember(models) {
        models.flatMap { it.tags }.distinct().sorted()
    }

    val filteredModels = run {
        var result = if (selectedTags.isEmpty()) models
        else models.filter { it.tags.containsAll(selectedTags) }
        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.modelName.contains(searchQuery, ignoreCase = true) ||
                it.vendor.contains(searchQuery, ignoreCase = true)
            }
        }
        result.sortedBy { it.vendor }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (loadError != null) {
            Text("Error: $loadError", color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp))
        }

        if (filteredModels.isEmpty() && loadError == null && models.isNotEmpty()) {
            Text("No models match filter", modifier = Modifier.padding(16.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search models...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        if (allTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                allTags.forEach { tag ->
                    FilterChip(
                        selected = tag in selectedTags,
                        onClick = {
                            selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                        },
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredModels) { model ->
                val modelId = model.sources["HuggingFace"] ?: return@items
                val dl = downloadStates[modelId]
                val progress = dl?.progress ?: 0f
                val speed = dl?.speedMbS ?: 0f
                val error = downloadErrors[modelId]
                val persisted = persistedDownloads[modelId]

                val isComplete = persisted?.complete == true || progress >= 1f
                val isPartial = persisted != null && !persisted.complete && persisted.downloadedBytes > 0
                val isDownloading = progress in 0.01f..0.99f

                val displayProgress = when {
                    isComplete -> 1f
                    isPartial -> (persisted!!.downloadedBytes.toFloat() / persisted.totalBytes.toFloat()).coerceIn(0f, 1f)
                    else -> progress
                }
                val displayDownloaded = persisted?.downloadedBytes ?: 0
                val displayTotal = persisted?.totalBytes ?: (model.fileSize)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(model.modelName, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(model.vendor, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                                model.tags.forEach { tag ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            Text("${model.sizeGb} GB", style = MaterialTheme.typography.bodySmall)

                            if (isPartial && !isDownloading) {
                                LinearProgressIndicator(
                                    progress = { displayProgress },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                                Text("${formatBytes(displayDownloaded)} / ${formatBytes(displayTotal)}",
                                    style = MaterialTheme.typography.labelSmall)
                            }

                            if (isDownloading) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                                val dlState = dl ?: DownloadState()
                                if (speed > 0f) {
                                    Text("%.1f MB/s  ${formatBytes(dlState.downloadedBytes)} / ${formatBytes(dlState.totalBytes)}"
                                        .format(speed), style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            if (error != null) {
                                Text(error, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        if (isComplete) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Done", color = MaterialTheme.colorScheme.primary)
                                TextButton(onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val path = context.filesDir.absolutePath + "/mnn_models/hf/" + modelId
                                        File(path).deleteRecursively()
                                        settingsRepository.removeDownloadedModel(modelId)
                                    }
                                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            }
                        } else if (isPartial && !isDownloading) {
                            Column(horizontalAlignment = Alignment.End) {
                                Button(onClick = {
                                    scope.launch {
                                        downloadModel(context, client, settingsRepository,
                                            modelId, model.modelName, model.fileSize,
                                            offsetBytes = persisted!!.downloadedBytes
                                        ) { p, spd, dlBytes, totBytes ->
                                            downloadStates = downloadStates + (modelId to
                                                DownloadState(p, spd, dlBytes, totBytes, modelId, model.modelName))
                                        }
                                    }
                                }) { Text("Resume") }
                                TextButton(onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val path = context.filesDir.absolutePath + "/mnn_models/hf/" + modelId
                                        File(path).deleteRecursively()
                                        settingsRepository.removeDownloadedModel(modelId)
                                    }
                                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            }
                        } else if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Button(onClick = {
                                scope.launch {
                                    val modelDir = context.filesDir.absolutePath + "/mnn_models/hf/" + modelId
                                    settingsRepository.saveDownloadedModel(DownloadedMnnModel(
                                        modelId = modelId, modelName = model.modelName,
                                        downloadPath = modelDir, totalBytes = model.fileSize,
                                        timestamp = System.currentTimeMillis()
                                    ))
                                    downloadModel(context, client, settingsRepository,
                                        modelId, model.modelName, model.fileSize
                                    ) { p, spd, dlBytes, totBytes ->
                                        downloadStates = downloadStates + (modelId to
                                            DownloadState(p, spd, dlBytes, totBytes, modelId, model.modelName))
                                    }
                                }
                            }) { Text("Download") }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes.toFloat() / 1048576f
    return if (mb >= 1000) "%.2f GB".format(mb / 1024f) else "%.1f MB".format(mb)
}

private suspend fun downloadModel(
    context: Context,
    client: OkHttpClient,
    repo: SettingsRepository,
    modelId: String,
    modelName: String,
    totalBytes: Long,
    offsetBytes: Long = 0,
    onProgress: (Float, Float, Long, Long) -> Unit
) = withContext(Dispatchers.IO) {
    var totalDownloaded = offsetBytes
    var totalSize = totalBytes

    try {
        onProgress(0f, 0f, totalDownloaded, totalSize)

        val modelDir = File(context.filesDir, "mnn_models/hf/$modelId")
        modelDir.mkdirs()

        val apiUrl = "https://huggingface.co/api/models/$modelId/tree/main?recursive=true"
        val treeJson = client.newCall(Request.Builder().url(apiUrl).build()).execute().use { resp ->
            resp.body?.string() ?: throw Exception("Failed to get file list")
        }
        val gson = Gson()
        val tree = gson.fromJson(treeJson, Array<HfTreeItem>::class.java) ?: emptyArray()
        val files = tree.filter { it.type == "file" }
        totalSize = files.sumOf { it.size }

        for (file in files) {
            val fileUrl = "https://huggingface.co/$modelId/resolve/main/${file.path}"
            val targetFile = File(modelDir, file.path)
            val incompleteFile = File(modelDir, file.path + ".incomplete")
            targetFile.parentFile?.mkdirs()

            // Skip fully downloaded files
            if (targetFile.exists() && targetFile.length() >= file.size) {
                totalDownloaded += targetFile.length()
                continue
            }

            // Resume from incomplete
            var fileDownloaded = if (incompleteFile.exists()) incompleteFile.length() else 0L
            if (fileDownloaded >= file.size) {
                incompleteFile.renameTo(targetFile)
                totalDownloaded += targetFile.length()
                continue
            }

            downloadWithResume(client, fileUrl, incompleteFile, targetFile,
                file.size, fileDownloaded,
                modelId, repo, totalDownloaded, totalSize,
                onProgress
            ) { d ->
                totalDownloaded += d
            }
        }

        repo.markDownloadComplete(modelId, totalSize)
        onProgress(1f, 0f, totalSize, totalSize)
    } catch (e: Exception) {
        repo.updateDownloadProgress(modelId, totalDownloaded)
        onProgress(0f, 0f, totalDownloaded, totalSize)
        throw e
    }
}

private fun downloadWithResume(
    client: OkHttpClient,
    url: String,
    incompleteFile: File,
    targetFile: File,
    expectedSize: Long,
    existingBytes: Long,
    modelId: String,
    repo: SettingsRepository,
    totalDownloaded: Long,
    totalSize: Long,
    onProgress: (Float, Float, Long, Long) -> Unit,
    onDelta: (Long) -> Unit
) {
    val maxRetries = 3
    var lastException: Exception? = null

    for (attempt in 0 until maxRetries) {
        try {
            val req = Request.Builder().url(url).get()
                .header("Accept-Encoding", "identity")
            if (existingBytes > 0) {
                req.header("Range", "bytes=$existingBytes-")
            }
            req.build().let { request ->
                client.newCall(request).execute().use { resp ->
                    if (resp.code == 416) {
                        incompleteFile.renameTo(targetFile)
                        return
                    }
                    if (!resp.isSuccessful && resp.code != 206) {
                        throw Exception("HTTP ${resp.code}")
                    }

                    var written = existingBytes
                    val raf = RandomAccessFile(incompleteFile, "rw").also {
                        if (existingBytes > 0) it.seek(existingBytes)
                    }

                    try {
                        resp.body?.byteStream()?.use { input ->
                            val buf = ByteArray(65536)
                            var n: Int
                            var lastReport = System.currentTimeMillis()
                            var lastBytes = written
                            var bytesSinceSave = 0L

                            while (input.read(buf).also { n = it } != -1) {
                                raf.write(buf, 0, n)
                                written += n
                                bytesSinceSave += n

                                val now = System.currentTimeMillis()
                                val elapsed = now - lastReport
                                if (elapsed > 200) {
                                    val dp = (written - lastBytes).toFloat() / 1048576f
                                    val spd = dp / (elapsed / 1000f)
                                    val prog = if (totalSize > 0)
                                        (totalDownloaded + written).toFloat() / totalSize.toFloat()
                                    else 0f
                                    onProgress(prog, spd, totalDownloaded + written, totalSize)
                                    lastReport = now
                                    lastBytes = written
                                }
                                if (bytesSinceSave > 5_000_000) {
                                    kotlinx.coroutines.runBlocking {
                                        repo.updateDownloadProgress(modelId, totalDownloaded + written)
                                    }
                                    bytesSinceSave = 0
                                }
                            }
                        }
                    } finally {
                        raf.close()
                    }

                    incompleteFile.renameTo(targetFile)
                    onDelta(written - existingBytes)
                    return
                }
            }
        } catch (e: Exception) {
            lastException = e
            if (attempt < maxRetries - 1) {
                try { Thread.sleep(2000) } catch (_: InterruptedException) {}
            }
        }
    }
    throw lastException ?: Exception("Download failed after $maxRetries retries")
}

data class HfTreeItem(
    val path: String = "",
    val type: String = "",
    val size: Long = 0
)
