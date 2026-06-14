package com.aibrowser.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.Locale

private enum class VoiceBarState { LISTENING, PROCESSING, PAUSED, SPEAKING }

@Composable
fun VoiceInputBar(
    isLoading: Boolean,
    actionHistory: List<String>,
    currentAction: String,
    responseText: String,
    ttsText: String,
    onResult: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(VoiceBarState.LISTENING) }
    var partialText by remember { mutableStateOf("") }
    var submittedText by remember { mutableStateOf("") }
    var permissionGranted by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }

    val recognizer: SpeechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    fun startListening() {
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) startListening()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionGranted = true
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(recognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (state == VoiceBarState.LISTENING && (
                        error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    )) {
                    startListening()
                }
            }
            override fun onResults(results: Bundle?) {
                val r = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!r.isNullOrEmpty() && r[0].isNotBlank()) {
                    submittedText = r[0]
                    state = VoiceBarState.PROCESSING
                    onResult(r[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val r = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!r.isNullOrEmpty()) {
                    partialText = r[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        recognizer.setRecognitionListener(listener)
        onDispose {
            recognizer.destroy()
        }
    }

    DisposableEffect(context) {
        val ref = arrayOfNulls<TextToSpeech>(1)
        ref[0] = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ref[0]?.language = Locale.getDefault()
                ttsReady = true
            }
        }
        tts = ref[0]
        onDispose {
            ref[0]?.stop()
            ref[0]?.shutdown()
        }
    }

    LaunchedEffect(isLoading, ttsText) {
        if (!isLoading && state == VoiceBarState.PROCESSING) {
            if (responseText.isNotBlank()) {
                if (ttsText.isNotBlank() && ttsReady) {
                    tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onDone(utteranceId: String?) {
                            state = VoiceBarState.LISTENING
                            partialText = ""
                            submittedText = ""
                            if (permissionGranted) startListening()
                        }
                        override fun onError(utteranceId: String?) {
                            state = VoiceBarState.LISTENING
                            partialText = ""
                            submittedText = ""
                            if (permissionGranted) startListening()
                        }
                        override fun onStart(utteranceId: String?) {}
                    })
                    tts?.speak(ttsText, TextToSpeech.QUEUE_FLUSH, null, "voice_response")
                    state = VoiceBarState.SPEAKING
                }
            } else {
                state = VoiceBarState.LISTENING
                partialText = ""
                submittedText = ""
                if (permissionGranted) startListening()
            }
        }
    }

    var dots by remember { mutableStateOf("") }
    LaunchedEffect(state) {
        if (state == VoiceBarState.LISTENING || state == VoiceBarState.SPEAKING) {
            while (true) {
                dots = ""; delay(400)
                dots = "."; delay(400)
                dots = ".."; delay(400)
                dots = "..."; delay(400)
            }
        }
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(actionHistory.size, currentAction) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 72.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            when (state) {
                VoiceBarState.LISTENING -> {
                    IconButton(
                        onClick = {
                            if (partialText.isBlank()) {
                                recognizer.cancel()
                                state = VoiceBarState.PAUSED
                            } else {
                                val text = partialText
                                recognizer.cancel()
                                submittedText = text
                                state = VoiceBarState.PROCESSING
                                onResult(text)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.StopCircle,
                            "Stop",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = partialText.ifEmpty { "Listening$dots" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (partialText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        if (partialText.isNotEmpty()) {
                            Text(
                                text = "Listening$dots",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close", Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                VoiceBarState.PROCESSING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).padding(top = 6.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f).heightIn(max = 220.dp)) {
                        Text(
                            text = submittedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            actionHistory.forEach { action ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = action,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (currentAction.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = currentAction,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (!isLoading && responseText.isNotBlank() && ttsText.isBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Preparing reply...",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close", Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                VoiceBarState.PAUSED -> {
                    IconButton(
                        onClick = {
                            state = VoiceBarState.LISTENING
                            partialText = ""
                            if (permissionGranted) startListening()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            "Resume",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Paused",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close", Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                VoiceBarState.SPEAKING -> {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        "Speaking",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).padding(top = 6.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f).heightIn(max = 220.dp)) {
                        Text(
                            text = "Reading reply$dots",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            actionHistory.forEach { action ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = action,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (currentAction.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = currentAction,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close", Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
