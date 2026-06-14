package com.aibrowser.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibrowser.data.models.Message

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
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
        if (message.content.isNotBlank()) {
            if (isTool) {
                CollapsibleToolContent(message.content, bubbleColor)
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

        message.toolCalls.forEach { toolCall ->
            ToolCallCard(
                toolCall = toolCall,
                modifier = Modifier.padding(top = if (message.content.isNotBlank()) 4.dp else 0.dp)
            )
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
private fun CollapsibleToolContent(content: String, backgroundColor: Color) {
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
                    text = "Snapshot Result",
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
