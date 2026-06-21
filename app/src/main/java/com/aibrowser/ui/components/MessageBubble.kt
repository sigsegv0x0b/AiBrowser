package com.aibrowser.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibrowser.data.models.Message
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
    onGenerateTts: ((String, (String) -> Unit) -> Unit)? = null
) {
    val isUser = message.role == Message.Role.USER
    val isTool = message.role == Message.Role.TOOL
    val isAssistant = message.role == Message.Role.ASSISTANT

    val bubbleColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        isTool -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (isAssistant && message.thinking != null && message.thinking.isNotBlank()) {
            var thinkingExpanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                onClick = { thinkingExpanded = !thinkingExpanded }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (thinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (thinkingExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Thinking",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (thinkingExpanded) {
                    Text(
                        text = message.thinking,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (message.content.isNotBlank()) {
            if (isTool) {
                CollapsibleToolContent(message.content, bubbleColor, message.toolName)
            } else {
                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            )
                        )
                        .background(bubbleColor)
                        .padding(12.dp)
                ) {
                    if (isAssistant) {
                        MarkdownText(text = message.content)
                    } else {
                        SelectionContainer {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        if (message.content.isNotBlank() && (!isTool)) {
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TtsSpeaker(text = if (isUser) message.content else stripMarkdown(message.content), onGenerateTts = onGenerateTts)
            }
        }

        message.toolCalls.forEach { toolCall ->
            ToolCallCard(
                toolCall = toolCall,
                modifier = Modifier.padding(top = if (message.content.isNotBlank()) 4.dp else 0.dp)
            )
        }
    }
}

private fun stripMarkdown(text: String): String {
    return text
        .replace(Regex("```[\\s\\S]*?```"), " ")
        .replace(Regex("`[^`]+`"), " ")
        .replace(Regex("\\*\\*"), "")
        .replace(Regex("(?<!\\*)\\*(?!\\*)"), "")
        .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        .replace(Regex("^\\|.*$", RegexOption.MULTILINE), "")
        .replace(Regex("---+"), "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

private enum class TtsState { IDLE, PLAYING, PAUSED }
private enum class TtsMode { RAW, SUMMARIZED }
private const val CHARS_PER_SEC = 15
private const val SKIP_SEC = 10

@Composable
private fun TtsSpeaker(text: String, onGenerateTts: ((String, (String) -> Unit) -> Unit)? = null) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(TtsState.IDLE) }
    var mode by remember { mutableStateOf(TtsMode.RAW) }
    var speakFrom by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf(0L) }
    var startFrom by remember { mutableStateOf(0) }
    var generating by remember { mutableStateOf(false) }
    var summarizedText by remember { mutableStateOf<String?>(null) }

    fun activeText(): String = if (mode == TtsMode.SUMMARIZED) summarizedText ?: text else text

    fun calcPosition(): Int {
        if (startTime == 0L) return speakFrom
        val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        return (startFrom + elapsed * CHARS_PER_SEC).coerceAtMost(activeText().length)
    }

    fun stopTts(ttsInstance: android.speech.tts.TextToSpeech?) {
        ttsInstance?.stop()
        state = TtsState.PAUSED
        speakFrom = calcPosition()
    }

    fun playTts(ttsInstance: android.speech.tts.TextToSpeech?, from: Int) {
        val currentText = activeText()
        if (from >= currentText.length) {
            state = TtsState.IDLE
            speakFrom = 0
            return
        }
        val sub = currentText.substring(from)
        startFrom = from
        startTime = System.currentTimeMillis()
        ttsInstance?.speak(sub, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "utt")
        state = TtsState.PLAYING
    }

    fun startRaw() {
        if (!ready) return
        mode = TtsMode.RAW
        speakFrom = 0
        playTts(tts, 0)
    }

    fun startSummarized() {
        if (!ready || onGenerateTts == null) return
        mode = TtsMode.SUMMARIZED
        if (summarizedText != null) {
            speakFrom = 0
            playTts(tts, 0)
        } else {
            generating = true
            onGenerateTts(text) { result ->
                summarizedText = result
                generating = false
                speakFrom = 0
                playTts(tts, 0)
            }
        }
    }

    DisposableEffect(context) {
        val ref = arrayOfNulls<android.speech.tts.TextToSpeech>(1)
        ref[0] = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                ref[0]?.language = Locale.getDefault()
                ready = true
            }
        }
        tts = ref[0]
        ref[0]?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                state = TtsState.IDLE
                speakFrom = 0
                startTime = 0L
            }
            override fun onError(utteranceId: String?) {}
            override fun onStart(utteranceId: String?) {}
        })
        onDispose {
            ref[0]?.stop()
            ref[0]?.shutdown()
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (generating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).padding(top = 4.dp),
                strokeWidth = 2.dp
            )
        } else if (state == TtsState.IDLE) {
            IconButton(
                onClick = { startRaw() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Read aloud",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onGenerateTts != null) {
                IconButton(
                    onClick = { startSummarized() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Summarize and read aloud",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            IconButton(onClick = { stopTts(tts); playTts(tts, 0) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Replay, "Restart", Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                stopTts(tts)
                playTts(tts, (speakFrom - SKIP_SEC * CHARS_PER_SEC).coerceAtLeast(0))
            }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.SkipPrevious, "-10s", Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                if (state == TtsState.PLAYING) {
                    stopTts(tts)
                } else {
                    playTts(tts, speakFrom)
                }
            }, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (state == TtsState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (state == TtsState.PLAYING) "Pause" else "Play",
                    Modifier.size(20.dp),
                    MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = {
                stopTts(tts)
                playTts(tts, (speakFrom + SKIP_SEC * CHARS_PER_SEC).coerceAtMost(activeText().length))
            }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.SkipNext, "+10s", Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun findTableEnd(text: String, start: Int): Int {
    if (start >= text.length || text[start] != '|') return start
    val lineEnd = text.indexOf('\n', start)
    if (lineEnd < 0) return text.length
    val secondLine = text.indexOf('\n', lineEnd + 1)
    if (secondLine < 0) return start
    val separator = text.substring(lineEnd + 1, secondLine).trim()
    if (!separator.startsWith('|')) return start
    if (!separator.contains("---") && !separator.contains(" -")) return start
    var pos = secondLine + 1
    while (pos < text.length) {
        val nextNewline = text.indexOf('\n', pos)
        val line = if (nextNewline >= 0) text.substring(pos, nextNewline).trim() else text.substring(pos).trim()
        if (!line.startsWith('|') && line.isNotEmpty()) break
        if (nextNewline < 0) { pos = text.length; break }
        pos = nextNewline + 1
    }
    return pos
}

@Composable
private fun CollapsibleToolContent(content: String, backgroundColor: Color, toolName: String? = null) {
    var expanded by remember { mutableStateOf(false) }
    val previewLines = if (content.lines().firstOrNull()?.let { it.startsWith("URL:") } == true) {
        content.lines().take(3).joinToString("\n")
    } else {
        content.take(100)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .widthIn(max = 300.dp)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = toolName?.replace("browser_", "")?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Result",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(16.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = previewLines,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val annotated = remember(text) { parseMarkdown(text) }
    SelectionContainer {
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
    }
}

private fun parseMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Table: consecutive lines starting with |
                text[i] == '|' -> {
                    val end = findTableEnd(text, i)
                    if (end > i) {
                        val table = text.substring(i, end)
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)) {
                            append(table)
                        }
                        i = end
                    } else {
                        append(text[i]); i++
                    }
                }
                // Code block ```...```
                text.startsWith("```", i) -> {
                    val end = text.indexOf("```", i + 3)
                    if (end >= 0) {
                        val code = text.substring(i + 3, end).trim('\n')
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)) {
                            append(code)
                        }
                        i = end + 3
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Inline code `...`
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end >= 0) {
                        val code = text.substring(i + 1, end)
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)) {
                            append(code)
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Link [text](url)
                text[i] == '[' && text.indexOf("](", i) > i -> {
                    val closeBracket = text.indexOf("](", i)
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeBracket > i && closeParen > closeBracket) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        withStyle(SpanStyle(color = Color(0xFF1565C0), fontWeight = FontWeight.Medium)) {
                            append(linkText)
                        }
                        i = closeParen + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Bold **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end >= 0) {
                        val bold = text.substring(i + 2, end)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(bold)
                        }
                        i = end + 2
                    } else {
                        append(text[i]); i++
                    }
                }
                // Italic *text* (but not **)
                text[i] == '*' && (i + 1 >= text.length || text[i + 1] != '*') -> {
                    val end = text.indexOf('*', i + 1)
                    if (end >= 0) {
                        val italic = text.substring(i + 1, end)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(italic)
                        }
                        i = end + 1
                    } else {
                        append(text[i]); i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
