package com.aibrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AgentViewModel
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.ui.components.MessageBubble
import kotlinx.coroutines.delay

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
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

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
                                    ApiConfig.ApiProvider.entries.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p.displayName) },
                                            onClick = {
                                                agentViewModel.setProvider(p)
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
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
