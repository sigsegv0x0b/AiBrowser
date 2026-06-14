package com.aibrowser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AgentStatusBubble(
    isLoading: Boolean,
    actionHistory: List<String>,
    currentAction: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val scrollState = rememberScrollState()

        LaunchedEffect(actionHistory.size, currentAction) {
            delay(50)
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(end = 72.dp)
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp),
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
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp).padding(top = 6.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f).heightIn(max = 200.dp)) {
                    Text(
                        text = "Executing...",
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
                    }
                }
            }
        }
    }
}
