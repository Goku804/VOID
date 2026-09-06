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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.ChatConversation
import com.core.voidapp.data.ChatMessage
import com.core.voidapp.data.ChatRole
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.ai.AIRepository
import com.core.voidapp.data.ai.AIResult
import kotlinx.coroutines.launch

/**
 * The isolated, full-screen AI Chat surface (opened via the floating AI
 * button in MainActivity — this screen no longer lives in the bottom
 * nav). Supports multiple parallel conversations via a slide-in sidebar,
 * Claude-style: a "New chat" action, a scrollable thread list, per-thread
 * delete, and auto-titling from each thread's first message.
 *
 * Real API integration via AIRepository — no hardcoded responses, no fake
 * conversation, and (when the user has granted it in Settings ->
 * Integrations -> AI) real tool-calling so the AI can create, edit,
 * delete, and analyze VOID data on the user's behalf, not just talk about
 * it. onOpenSettings routes to the SETTINGS tab and closes this overlay;
 * onClose backs all the way out to whatever tab was open before.
 */
@Composable
fun ChatScreen(onOpenSettings: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val clipboard = LocalClipboardManager.current

    var currentConversationId by remember {
        mutableStateOf(
            VoidRepository.allConversations().firstOrNull()?.id
                ?: VoidRepository.createConversation().id
        )
    }

    val conversations = VoidRepository.allConversations()
    val currentConversation = conversations.find { it.id == currentConversationId }
        ?: conversations.firstOrNull()
        ?: VoidRepository.createConversation().also { currentConversationId = it.id }

    val messages = VoidRepository.messagesFor(currentConversation.id)

    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var configured by remember { mutableStateOf(AIRepository.isConfigured(context)) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isSending, error, currentConversation.id) {
        val lastIndex = messages.size - 1 + (if (isSending || error != null) 1 else 0)
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    fun submit() {
        val text = input.trim()
        if (text.isEmpty() || isSending) return
        input = ""
        error = null
        isSending = true
        val conversationId = currentConversation.id
        scope.launch {
            when (val result = AIRepository.send(context, conversationId, text)) {
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
        val conversationId = currentConversation.id
        scope.launch {
            when (val result = AIRepository.retry(context, conversationId)) {
                is AIResult.Success -> {}
                is AIResult.Failure -> error = result.message
            }
            isSending = false
        }
    }

    fun switchTo(id: String) {
        currentConversationId = id
        error = null
        scope.launch { drawerState.close() }
    }

    fun startNewChat() {
        val created = VoidRepository.createConversation()
        switchTo(created.id)
    }

    fun removeConversation(id: String) {
        VoidRepository.deleteConversation(id)
        if (id == currentConversationId) {
            val next = VoidRepository.allConversations().firstOrNull() ?: VoidRepository.createConversation()
            currentConversationId = next.id
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = VoidColors.Surface, drawerContentColor = VoidColors.TextPrimary) {
                ConversationSidebar(
                    conversations = conversations,
                    currentId = currentConversation.id,
                    onSelect = ::switchTo,
                    onNewChat = ::startNewChat,
                    onDelete = ::removeConversation
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Conversations",
                    tint = VoidColors.TextSecondary,
                    modifier = Modifier.clickable { scope.launch { drawerState.open() } }.padding(end = 10.dp)
                )
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VoidColors.Purple, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        currentConversation.title.ifBlank { "VOID AI" },
                        color = VoidColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (configured) "CONNECTED \u00b7 FULL APP ACCESS" else "NOT CONFIGURED \u2014 SETTINGS \u2192 INTEGRATIONS",
                        color = if (configured) VoidColors.Success else VoidColors.Warning,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (messages.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear this chat",
                        tint = VoidColors.TextSecondary,
                        modifier = Modifier.clickable { showClearConfirm = true }.padding(8.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New chat",
                    tint = VoidColors.TextSecondary,
                    modifier = Modifier.clickable { startNewChat() }.padding(8.dp)
                )
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
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close AI Chat",
                    tint = VoidColors.TextSecondary,
                    modifier = Modifier.clickable { onClose() }.padding(8.dp)
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
                            "Ask VOID anything \u2014 it can also create, edit, and delete your subjects, tasks, exams, and Circle Plan slots directly.",
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
                        items(messages, key = { it.id }) { message ->
                            ChatBubble(message, onCopy = { clipboard.setText(AnnotatedString(it)) })
                        }
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
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear this chat?", fontFamily = FontFamily.Monospace) },
            text = { Text("This deletes the messages in this conversation on this device. This cannot be undone.", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = {
                    VoidRepository.clearChatHistory(currentConversation.id)
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

/**
 * Claude-style sidebar: a prominent "New chat" action up top, then every
 * conversation newest-first, each with its own delete affordance. Slides
 * in over the chat itself (ModalNavigationDrawer) rather than living
 * beside it — there's no room for a permanent side rail on a phone.
 */
@Composable
private fun ConversationSidebar(
    conversations: List<ChatConversation>,
    currentId: String,
    onSelect: (String) -> Unit,
    onNewChat: () -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VoidColors.Purple, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("VOID AI", color = VoidColors.TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(VoidColors.Purple.copy(alpha = 0.12f))
                .border(1.dp, VoidColors.Purple.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .clickable { onNewChat() }
                .padding(vertical = 12.dp, horizontal = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = VoidColors.Purple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("NEW CHAT", color = VoidColors.Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        VoidSectionLabel("CONVERSATIONS")
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(conversations, key = { it.id }) { conversation ->
                ConversationRow(
                    conversation = conversation,
                    isSelected = conversation.id == currentId,
                    onClick = { onSelect(conversation.id) },
                    onDelete = { onDelete(conversation.id) }
                )
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ChatConversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) VoidColors.Surface2 else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = if (isSelected) VoidColors.Purple else VoidColors.TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = conversation.title.ifBlank { "New chat" },
            color = if (isSelected) VoidColors.TextPrimary else VoidColors.TextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.DeleteOutline,
            contentDescription = "Delete conversation",
            tint = VoidColors.TextSecondary,
            modifier = Modifier.size(15.dp).clickable { onDelete() }
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, onCopy: (String) -> Unit) {
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
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isUser) "YOU" else "VOID AI",
                    color = if (isUser) VoidColors.Cyan else VoidColors.Purple,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                if (!isUser) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy response",
                        tint = VoidColors.TextSecondary,
                        modifier = Modifier.size(13.dp).clickable { onCopy(message.content) }
                    )
                }
            }
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
