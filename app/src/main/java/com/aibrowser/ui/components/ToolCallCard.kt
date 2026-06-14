package com.aibrowser.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aibrowser.data.models.ToolCall

private fun describeToolCall(name: String, args: Map<String, Any>): String {
    return when (name) {
        "browser_navigate" -> "Navigate to ${args["url"]}"
        "browser_navigate_back" -> "Go back"
        "browser_click" -> "Click ${args["target"]}${if (args["doubleClick"] == true) " (double)" else ""}"
        "browser_type" -> "Type \"${args["text"]}\" into ${args["target"]}"
        "browser_fill_form" -> "Fill form (${(args["fields"] as? List<*>)?.size ?: 0} fields)"
        "browser_select_option" -> "Select ${args["values"]} on ${args["target"]}"
        "browser_hover" -> "Hover ${args["target"]}"
        "browser_press_key" -> "Press key: ${args["key"]}"
        "browser_snapshot" -> "Snapshot page (depth=${args["depth"]}, boxes=${args["boxes"]})"
        "browser_take_screenshot" -> "Take screenshot"
        "browser_evaluate" -> "Run JS: ${(args["function"] as? String)?.take(80)}"
        "browser_wait_for" -> "Wait for \"${args["text"]}\""
        "browser_tabs" -> "Tab action: ${args["action"]}${args["url"]?.let { " → $it" } ?: ""}"
        else -> "$name(${args.entries.joinToString { "${it.key}=${it.value}" }})"
    }
}

@Composable
fun ToolCallCard(
    toolCall: ToolCall,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (toolCall.status) {
        ToolCall.ToolStatus.PENDING -> MaterialTheme.colorScheme.outline
        ToolCall.ToolStatus.RUNNING -> MaterialTheme.colorScheme.primary
        ToolCall.ToolStatus.DONE -> MaterialTheme.colorScheme.tertiary
        ToolCall.ToolStatus.ERROR -> MaterialTheme.colorScheme.error
    }

    val statusIcon = when (toolCall.status) {
        ToolCall.ToolStatus.PENDING -> "\u23F3"
        ToolCall.ToolStatus.RUNNING -> "\u2699\uFE0F"
        ToolCall.ToolStatus.DONE -> "\u2713"
        ToolCall.ToolStatus.ERROR -> "\u2717"
    }

    val description = remember(toolCall.name, toolCall.arguments) {
        describeToolCall(toolCall.name, toolCall.arguments)
    }

    Column(
        modifier = modifier
            .widthIn(max = 300.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .animateContentSize()
            .clickable { expanded = !expanded }
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = statusIcon, style = MaterialTheme.typography.labelSmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
                Text(
                    text = toolCall.name.replace("browser_", ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand",
                modifier = Modifier.size(16.dp)
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Arguments: ${toolCall.arguments}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            toolCall.result?.let { result ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Result: $result",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (toolCall.status == ToolCall.ToolStatus.ERROR)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
