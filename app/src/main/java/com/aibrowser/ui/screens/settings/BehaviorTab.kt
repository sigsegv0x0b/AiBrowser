package com.aibrowser.ui.screens.settings
import kotlinx.coroutines.launch

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.BehaviorConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorSettingsTab(
    behavior: BehaviorConfig,
    notesDirectoryUri: String?,
    settingsRepository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onSave: (BehaviorConfig) -> Unit
) {
    var scrollIntoView by remember(behavior) { mutableStateOf(behavior.scrollIntoView) }
    var blockExternalIntents by remember(behavior) { mutableStateOf(behavior.blockExternalIntents) }
    var ttsPrompt by remember(behavior) { mutableStateOf(behavior.ttsPrompt) }
    var systemPrompt by remember(behavior) { mutableStateOf(behavior.systemPrompt) }

    val context = LocalContext.current
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scope.launch {
                settingsRepository.saveNotesDirectoryUri(uri.toString())
            }
        }
    }

    val directoryName = remember(notesDirectoryUri) {
        if (notesDirectoryUri.isNullOrBlank()) null
        else {
            try {
                DocumentFile.fromTreeUri(context, Uri.parse(notesDirectoryUri))?.name
            } catch (_: Exception) { null }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Interaction", style = MaterialTheme.typography.titleMedium)

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Scroll element into view", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text("When enabled, the browser scrolls to center the target element before interacting.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = scrollIntoView, onCheckedChange = { scrollIntoView = it })
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Block external app intents", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text("Links like twitter:// are loaded as https:// instead of opening an external app.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = blockExternalIntents, onCheckedChange = { blockExternalIntents = it })
        }

        HorizontalDivider()
        Text("Notes Directory", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (directoryName != null) {
                    Text("Directory: $directoryName", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Not set — file_read/file_write/file_list tools will be unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.width(16.dp))
            OutlinedButton(onClick = { directoryPickerLauncher.launch(null) }) {
                Text(if (directoryName != null) "Change" else "Choose")
            }
        }

        HorizontalDivider()
        Text("TTS Prompt", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = ttsPrompt, onValueChange = { ttsPrompt = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = { ttsPrompt = BehaviorConfig.DEFAULT_TTS_PROMPT }) {
            Text("Reset to Default")
        }

        HorizontalDivider()
        Text("System Prompt", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = systemPrompt, onValueChange = { systemPrompt = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = { systemPrompt = BehaviorConfig.DEFAULT_SYSTEM_PROMPT }) {
            Text("Reset to Default")
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                onSave(BehaviorConfig(
                    scrollIntoView = scrollIntoView,
                    blockExternalIntents = blockExternalIntents,
                    ttsPrompt = ttsPrompt,
                    systemPrompt = systemPrompt
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Behavior Settings")
        }

        Spacer(Modifier.height(32.dp))
    }
}

