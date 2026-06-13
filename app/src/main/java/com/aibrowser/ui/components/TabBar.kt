package com.aibrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aibrowser.browser.TabState

@Composable
fun TabBar(
    tabs: List<TabState>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tabs) { tab ->
                TabChip(
                    title = tab.title,
                    isActive = tab.id == activeTabId,
                    onClick = { onTabClick(tab.id) },
                    onClose = { onCloseTab(tab.id) }
                )
            }
        }

        IconButton(onClick = onNewTab) {
            Icon(Icons.Default.Add, contentDescription = "New Tab")
        }

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun TabChip(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .widthIn(min = 80.dp, max = 160.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
