package com.aibrowser.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AgentViewModel
import com.aibrowser.ui.components.MessageBubble
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    agentViewModel: AgentViewModel,
    onBack: () -> Unit
) {
    val messages by agentViewModel.messages.collectAsState()
    val isLoading by agentViewModel.isLoading.collectAsState()
    val isPaused by agentViewModel.isPaused.collectAsState()
    val currentAction by agentViewModel.currentAction.collectAsState()
    val providerName by agentViewModel.providerName.collectAsState()
    val cloudProviders by agentViewModel.cloudProviders.collectAsState()
    val activeProviderId by agentViewModel.activeProviderId.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var exportResult by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    var ttsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val ref = arrayOfNulls<TextToSpeech>(1)
        ref[0] = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ref[0]?.language = Locale.getDefault()
                ttsReady = true
            }
        }
        ttsRef.value = ref[0]
        onDispose {
            ref[0]?.stop()
            ref[0]?.shutdown()
        }
    }

    val lastAssistantContent = remember(messages) {
        messages.lastOrNull { it.role == com.aibrowser.data.models.Message.Role.ASSISTANT }?.content ?: ""
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI Agent")
                        if (providerName.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                SuggestionChip(
                                    onClick = { expanded = true },
                                    label = {
                                        Text(providerName, style = MaterialTheme.typography.labelSmall)
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("MNN Local") },
                                        onClick = {
                                            agentViewModel.setActiveProvider(null)
                                            expanded = false
                                        }
                                    )
                                    cloudProviders.forEach { cp ->
                                        DropdownMenuItem(
                                            text = { Text(cp.name.ifBlank { cp.provider.displayName }) },
                                            onClick = {
                                                agentViewModel.setActiveProvider(cp.id)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    val name = agentViewModel.exportChat()
                                    exportResult = "Saved to $name"
                                } catch (e: Exception) {
                                    exportResult = "Export failed: ${e.message}"
                                }
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Export chat")
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            importFiles = agentViewModel.listMsglogFiles()
                            showImportDialog = true
                        }
                    }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import chat")
                    }
                    if (isLoading) {
                        IconButton(onClick = { agentViewModel.pause() }) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                        }
                    }
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { agentViewModel.clearChat() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear chat")
                        }
                    }
                    if (!isLoading && lastAssistantContent.isNotBlank() && ttsReady) {
                        IconButton(onClick = {
                            val tts = ttsRef.value
                            if (isSpeaking) {
                                tts?.stop()
                                isSpeaking = false
                            } else {
                                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                                    override fun onDone(utteranceId: String?) { isSpeaking = false }
                                    override fun onError(utteranceId: String?) { isSpeaking = false }
                                    override fun onStart(utteranceId: String?) {}
                                })
                                scope.launch {
                                    val text = agentViewModel.generateTtsText(lastAssistantContent)
                                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "agent_tts")
                                    isSpeaking = true
                                }
                            }
                        }) {
                            Icon(
                                if (isSpeaking) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop reading" else "Read aloud"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(exportResult) {
            exportResult?.let {
                snackbarHostState.showSnackbar(it)
                exportResult = null
            }
        }
        LaunchedEffect(importError) {
            importError?.let {
                snackbarHostState.showSnackbar(it)
                importError = null
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            onGenerateTts = if (message.role == com.aibrowser.data.models.Message.Role.ASSISTANT) {
                                { text, callback ->
                                    scope.launch {
                                        callback(agentViewModel.generateTtsText(text))
                                    }
                                }
                            } else null
                        )
                    }
                }

                if (isLoading) {
                    ThinkingIndicator(
                        text = currentAction.ifBlank { "Thinking" },
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (isPaused) "Add instructions..." else "Type a command...") },
                        enabled = !isLoading || isPaused
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isPaused) {
                        IconButton(
                            onClick = {
                                agentViewModel.resume(inputText)
                                inputText = ""
                            },
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                agentViewModel.sendMessage(inputText)
                                inputText = ""
                            },
                            enabled = inputText.isNotBlank() && !isLoading
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Chat") },
            text = {
                if (importFiles.isEmpty()) {
                    Text("No msglog_*.json files found in notes directory")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(importFiles) { file ->
                            TextButton(
                                onClick = {
                                    showImportDialog = false
                                    scope.launch {
                                        try {
                                            agentViewModel.importChat(file)
                                        } catch (e: Exception) {
                                            importError = "Import failed: ${e.message}"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(file, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThinkingIndicator(
    text: String = "Thinking",
    modifier: Modifier = Modifier
) {
    var dots by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            dots = ""
            delay(400)
            dots = "."
            delay(400)
            dots = ".."
            delay(400)
            dots = "..."
            delay(400)
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text("$text$dots", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
