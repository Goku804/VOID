package com.core.voidapp.data.ai

import android.content.Context
import com.core.voidapp.data.ChatRole
import com.core.voidapp.data.VoidRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The only thing the Chat UI talks to.
 *
 * Architecture: Chat UI -> AIRepository -> AIContextBuilder (real data) +
 * AIApiClient (network) -> configured provider. The Planning Engine
 * remains the deterministic source of truth for scheduling — this layer
 * only asks it for a ranking and hands that to the AI to explain, never
 * lets the AI substitute its own guess.
 */
object AIRepository {

    private const val SYSTEM_PROMPT_PREFIX = """You are VOID AI, an academic planning assistant built into the VOID app.
Use ONLY the real data provided below under CONTEXT to answer. Never invent subjects, exams, dates, grades, or schedule items that are not present in CONTEXT.
If information needed to answer is missing from CONTEXT, say so plainly instead of guessing.
CONTEXT includes a PLANNING ENGINE section listing deterministic, rule-based priorities computed outside of you — defer to its ordering and explain/build on it, rather than inventing a competing schedule of your own.
Be concise, direct, and practical. This is a study-planning tool, not a general-purpose chatbot."""

    private const val TOOLS_PROMPT_SUFFIX = """You have tool access to directly create, edit, delete, and analyze data inside VOID: subjects, tasks, exams, Circle Plan slots, and recorded scores. When the user asks you to add, change, remove, complete, or look up something concrete, actually call the relevant tool rather than just describing what you would do — then confirm what happened in plain language. Only call a delete tool when the user's message clearly asked for that item to be removed. Call get_full_snapshot before answering anything that needs full detail, totals, or a comparison across everything, since the CONTEXT block below is a summary, not the whole picture."""

    fun isConfigured(context: Context): Boolean = AIConfigStore.isConfigured(context)

    /** Persists the user's message, then requests and persists the AI's reply. */
    suspend fun send(context: Context, conversationId: String, userMessage: String): AIResult {
        VoidRepository.addChatMessage(conversationId, ChatRole.USER, userMessage)
        return callAI(context, conversationId)
    }

    /** Re-requests a reply for the existing last user message — used by the Retry button. Does not duplicate the user message. */
    suspend fun retry(context: Context, conversationId: String): AIResult = callAI(context, conversationId)

    suspend fun testConnection(config: AIConfig): AIResult = withContext(Dispatchers.IO) {
        AIApiClient.testConnection(config).fold(
            onSuccess = { AIResult.Success("Connected.") },
            onFailure = { AIResult.Failure(it.message ?: "Connection failed.") }
        )
    }

    /** Fetches the real list of model IDs available to this key/provider. */
    suspend fun fetchModels(config: AIConfig): Result<List<String>> = withContext(Dispatchers.IO) {
        AIApiClient.fetchModels(config)
    }

    private suspend fun callAI(context: Context, conversationId: String): AIResult {
        val config = AIConfigStore.load(context)
            ?: return AIResult.Failure("AI is not configured yet. Add an API key in Settings \u2192 Integrations \u2192 AI.")

        val lastUserMessage = VoidRepository.messagesFor(conversationId).lastOrNull { it.role == ChatRole.USER }?.content ?: ""
        val contextBlock = AIContextBuilder.build(lastUserMessage)
        val toolsEnabled = AIConfigStore.areToolsEnabled(context)
        val systemPrompt = buildString {
            append(SYSTEM_PROMPT_PREFIX)
            if (toolsEnabled) {
                append("\n\n")
                append(TOOLS_PROMPT_SUFFIX)
            }
            append("\n\nCONTEXT:\n")
            append(contextBlock)
        }
        val history = VoidRepository.messagesFor(conversationId).map {
            (if (it.role == ChatRole.USER) "user" else "assistant") to it.content
        }
        val tools = if (toolsEnabled) AITools.definitions() else emptyList()

        return try {
            // Tool execution happens inline on whichever dispatcher this
            // runs on (Main, from the Chat screen's coroutine scope) — only
            // the network calls themselves are pushed onto Dispatchers.IO
            // inside AIApiClient. That keeps VoidRepository's in-memory
            // state mutations off a background thread.
            val result = AIApiClient.sendMessageWithTools(config, systemPrompt, history, tools) { name, input ->
                AIToolExecutor.execute(name, input)
            }
            val reply = if (result.actionsLog.isEmpty()) {
                result.finalText
            } else {
                val log = result.actionsLog.joinToString("\n") { "\u2022 $it" }
                "[${result.actionsLog.size} action(s) performed]\n$log\n\n${result.finalText}"
            }
            VoidRepository.addChatMessage(conversationId, ChatRole.ASSISTANT, reply)
            AIResult.Success(reply)
        } catch (e: AIException) {
            AIResult.Failure(e.message ?: "AI request failed.")
        } catch (e: Exception) {
            AIResult.Failure("AI unavailable. Check your API configuration or connection.")
        }
    }
}
