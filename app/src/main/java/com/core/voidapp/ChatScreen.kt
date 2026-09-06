package com.core.voidapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.ChatMessage
import com.core.voidapp.data.ChatRole
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.ai.AIRepository
import com.core.voidapp.data.ai.AIResult
import kotlinx.coroutines.launch

/**
 * Dedicated AI Chat screen. Real API integration via AIRepository —
 * no hardcoded responses, no fake conversation. onOpenSettings routes to
 * the SETTINGS tab; the user reaches AI config from there via
 * Integrations -> AI, since that config screen lives inside SettingsScreen's
 * own navigation.
 */
@Composable
fun ChatScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val conversation = remember { VoidRepository.defaultConversation() }
    val messages = VoidRepository.chatMessages
        .filter { it.conversationId == conversation.id }
        .sortedBy { it.timestamp }

    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var configured by remember { mutableStateOf(AIRepository.isConfigured(context)) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size, isSending, error) {
        val lastIndex = messages.size - 1 + (if (isSending || error != null) 1 else 0)
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    fun submit() {
        val text = input.trim()
        if (text.isEmpty() || isSending) return
        input = ""
        error = null
        isSending = true
        scope.launch {
            when (val result = AIRepository.send(context, conversation.id, text)) {
                is AIResult.Success -> {}
                is AIResult.Failure -> error = result.message
            }
            isSending = false
        }
    }

    fun retry() {
        if (isSending) return
        error = null
        isSending = true
        scope.launch {
            when (val result = AIRepository.retry(context, conversation.id)) {
                is AIResult.Success -> {}
                is AIResult.Failure -> error = result.message
            }
            isSending = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VoidColors.Purple, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("VOID AI", color = VoidColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(
                    text = if (configured) "CONNECTED TO ACADEMIC CONTEXT" else "NOT CONFIGURED \u2014 SETTINGS \u2192 INTEGRATIONS",
                    color = if (configured) VoidColors.Success else VoidColors.Warning,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (messages.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Clear conversation",
                    tint = VoidColors.TextSecondary,
                    modifier = Modifier
                        .clickable { showClearConfirm = true }
                        .padding(8.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "AI settings",
                tint = VoidColors.TextSecondary,
                modifier = Modifier
                    .clickable {
                        configured = AIRepository.isConfigured(context)
                        onOpenSettings()
                    }
                    .padding(8.dp)
            )
        }

        HorizontalDivider(color = VoidColors.Border)

        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = VoidColors.Purple.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Ask VOID about your subjects, schedule, or exams.",
                        color = VoidColors.TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { message -> ChatBubble(message) }
                    if (isSending) item { ThinkingBubble() }
                    error?.let { message -> item { ChatErrorCard(message = message, onRetry = { retry() }) } }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().imePadding().padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask VOID anything...", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VoidColors.TextSecondary) },
                maxLines = 5,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = VoidColors.TextPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VoidColors.Purple,
                    unfocusedBorderColor = VoidColors.Border,
                    cursorColor = VoidColors.Purple,
                    focusedTextColor = VoidColors.TextPrimary,
                    unfocusedTextColor = VoidColors.TextPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val canSend = input.isNotBlank() && !isSending
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canSend) VoidColors.Purple.copy(alpha = 0.18f) else VoidColors.Surface2)
                    .border(1.dp, if (canSend) VoidColors.Purple else VoidColors.Border, RoundedCornerShape(14.dp))
                    .clickable(enabled = canSend) { submit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (canSend) VoidColors.Purple else VoidColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear conversation?", fontFamily = FontFamily.Monospace) },
            text = { Text("This deletes the chat history on this device. This cannot be undone.", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = {
                    VoidRepository.clearChatHistory(conversation.id)
                    error = null
                    showClearConfirm = false
                }) { Text("CLEAR", color = VoidColors.Danger, fontFamily = FontFamily.Monospace) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("CANCEL", fontFamily = FontFamily.Monospace) }
            },
            containerColor = VoidColors.Surface
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isUser) VoidColors.Surface2 else VoidColors.Surface)
                .border(1.dp, (if (isUser) VoidColors.Cyan else VoidColors.Purple).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Text(
                text = if (isUser) "YOU" else "VOID AI",
                color = if (isUser) VoidColors.Cyan else VoidColors.Purple,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = message.content, color = VoidColors.TextPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(VoidColors.Surface)
                .border(1.dp, VoidColors.Purple.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Text("VOID AI", color = VoidColors.Purple, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Thinking...", color = VoidColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ChatErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VoidColors.Surface)
            .border(1.dp, VoidColors.Danger.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("AI unavailable.", color = VoidColors.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        Text(message, color = VoidColors.TextPrimary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(VoidColors.Danger.copy(alpha = 0.12f))
                .border(1.dp, VoidColors.Danger, RoundedCornerShape(8.dp))
                .clickable { onRetry() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("RETRY", color = VoidColors.Danger, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}
