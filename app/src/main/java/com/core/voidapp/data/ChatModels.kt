package com.core.voidapp.data

/**
 * AI Chat domain models. Deliberately separate from data/ai/ — those
 * classes handle provider config and networking, these are just the
 * shapes that live in VoidRepository/Room alongside everything else.
 */

enum class ChatRole { USER, ASSISTANT }

data class ChatConversation(
    val id: String,
    val title: String = "VOID AI",
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
