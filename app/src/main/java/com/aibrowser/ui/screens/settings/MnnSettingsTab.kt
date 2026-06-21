package com.aibrowser.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.MnnLlmProvider
import com.aibrowser.agent.mnn.MnnSession
import com.aibrowser.agent.mnn.market.DownloadedMnnModel
import com.aibrowser.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MnnSettingsTab(
    modelPath: String,
    backend: String,
    useMmap: Boolean,
    promptCache: Boolean,
    maxTokens: Int,
    temperature: Float,
    topP: Float,
    topK: Int,
    precision: String,
    threads: Int,
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    mnnLlmProvider: MnnLlmProvider? = null
) {
    val context = LocalContext.current
    var downloadedModels by remember { mutableStateOf<List<DownloadedMnnModel>>(emptyList()) }
    var testPrompt by remember { mutableStateOf("hello") }
    var testOutput by remember { mutableStateOf("") }
    var testRunning by remember { mutableStateOf(false) }
    var testError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        settingsRepository.downloadedModels.collect { downloadedModels = it }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("MNN Local AI", style = MaterialTheme.typography.headlineSmall)

        val libError = MnnSession.loadError
        if (libError != null) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text("Native library error: $libError",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (downloadedModels.isNotEmpty()) {
            Text("Downloaded Models", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 250.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(downloadedModels) { dm ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dm.modelName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (dm.complete) "Complete" else "Partial",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (dm.complete) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                            if (dm.complete) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        settingsRepository.saveMnnModelPath(dm.downloadPath)
                                        settingsRepository.setActiveProvider(null)
                                    }
                                }, modifier = Modifier.padding(end = 4.dp)) { Text("Use") }
                            }
                            TextButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    File(dm.downloadPath).deleteRecursively()
                                    settingsRepository.removeDownloadedModel(dm.modelId)
                                }
                            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = modelPath,
            onValueChange = { scope.launch { settingsRepository.saveMnnModelPath(it) } },
            label = { Text("Model directory path") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        Text("Inference Settings", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        // Backend
        Text("Backend", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("cpu", "opencl").forEach { b ->
                FilterChip(
                    selected = backend == b,
                    onClick = { scope.launch { settingsRepository.saveMnnBackend(b) } },
                    label = { Text(b.uppercase()) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Precision
        Text("Precision", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("low", "high").forEach { p ->
                FilterChip(
                    selected = precision == p,
                    onClick = { scope.launch { settingsRepository.saveMnnPrecision(p) } },
                    label = { Text(p.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Threads
        OutlinedTextField(
            value = threads.toString(),
            onValueChange = { it.toIntOrNull()?.let { n ->
                scope.launch { settingsRepository.saveMnnThreads(n) }
            }},
            label = { Text("Thread number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(8.dp))

        // Max tokens
        OutlinedTextField(
            value = maxTokens.toString(),
            onValueChange = { it.toIntOrNull()?.let { n ->
                scope.launch { settingsRepository.saveMnnMaxTokens(n) }
            }},
            label = { Text("Max new tokens") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(12.dp))

        Text("Sampler Settings", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        // Temperature slider
        SliderSetting("Temperature", temperature, 0f, 2f, 2) { scope.launch { settingsRepository.saveMnnTemperature(it) } }
        Spacer(Modifier.height(4.dp))

        // Top P slider
        SliderSetting("Top P", topP, 0f, 1f, 2) { scope.launch { settingsRepository.saveMnnTopP(it) } }
        Spacer(Modifier.height(4.dp))

        // Top K slider
        TopKSlider("Top K", topK) { scope.launch { settingsRepository.saveMnnTopK(it) } }

        Spacer(Modifier.height(12.dp))

        // Prompt cache toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = promptCache,
                onCheckedChange = { scope.launch { settingsRepository.saveMnnPromptCache(it) } }
            )
            Spacer(Modifier.width(8.dp))
            Text("Prompt Cache (KV cache for faster responses)")
        }

        Spacer(Modifier.height(4.dp))

        // Mmap toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = useMmap,
                onCheckedChange = { scope.launch { settingsRepository.saveMnnUseMmap(it) } }
            )
            Spacer(Modifier.width(8.dp))
            Text("Use mmap")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = {
            scope.launch {
                settingsRepository.setActiveProvider(null)
            }
        }) { Text("Set as Active Provider") }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        // Test inference
        Text("Test Inference", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = testPrompt,
            onValueChange = { testPrompt = it },
            label = { Text("Prompt") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                if (testRunning || mnnLlmProvider == null) return@Button
                testRunning = true
                testOutput = ""
                testError = null
                scope.launch(Dispatchers.IO) {
                    try {
                        mnnLlmProvider.streamTest(testPrompt) { token ->
                            testOutput += token
                        }
                    } catch (e: Throwable) {
                        testError = e.message ?: e.javaClass.simpleName
                    } finally {
                        testRunning = false
                    }
                }
            }) {
                if (testRunning) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text("Run Test")
            }

            if (testRunning) {
                Spacer(Modifier.width(8.dp))
                Text("Generating...", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (testError != null) {
            Spacer(Modifier.height(4.dp))
            SelectionContainer {
                Text("Error: $testError", color = MaterialTheme.colorScheme.error)
            }
        }

        if (testOutput.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Output:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        Text(testOutput, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    decimals: Int,
    onChange: (Float) -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("%.${decimals}f".format(value), style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TopKSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value.toString(), style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 1f..100f,
            steps = 98,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
