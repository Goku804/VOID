package com.core.voidapp.data.ai

/**
 * Supported AI providers. The provider is never hard-coded into the Chat
 * UI or the API client's caller — it's configured by the user under
 * SETTINGS -> INTEGRATIONS and read fresh from AIConfigStore on every call.
 */
enum class AIProvider(val displayName: String, val defaultBaseUrl: String, val defaultModel: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
    ANTHROPIC("Anthropic (Claude)", "https://api.anthropic.com/v1", "claude-3-5-haiku-latest"),
    GROQ("Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
    OPENAI_COMPATIBLE("Other (OpenAI-compatible endpoint)", "", "")
}

/** Everything needed to make one API call. Never persisted anywhere except AIConfigStore. */
data class AIConfig(
    val provider: AIProvider,
    val apiKey: String,
    val model: String,
    val baseUrl: String
)

/** Result of an AI request — no exceptions escape past AIRepository, callers just branch on this. */
sealed class AIResult {
    data class Success(val reply: String) : AIResult()
    data class Failure(val message: String) : AIResult()
}

/** Thrown internally by the API client, always with a message safe to show the user. */
class AIException(message: String) : Exception(message)
