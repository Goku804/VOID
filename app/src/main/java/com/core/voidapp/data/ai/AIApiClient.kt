package com.core.voidapp.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * The only place in the app that knows the wire format of a specific AI
 * provider. The Chat UI and ContextBuilder never see HTTP or JSON —
 * they call AIRepository, which calls this. Swapping/adding a provider
 * means adding a branch here, nothing else.
 *
 * Deliberately built on plain HttpURLConnection + org.json (both part of
 * the Android SDK) rather than adding a new networking dependency.
 */
object AIApiClient {

    private const val TIMEOUT_MS = 30_000

    /** Hard ceiling on tool round-trips per user turn — stops a runaway tool loop from hammering the provider forever. */
    private const val MAX_TOOL_ITERATIONS = 6

    /**
     * The multi-API-key fallback chain: config.apiKeys is tried in order,
     * one full request at a time. If a key fails for ANY reason (quota
     * exhausted, invalid, rate-limited, transient network error), the very
     * next key in the list takes over the same request — the caller never
     * sees the intermediate failures, only the first success or, if every
     * key in the chain failed, the last error encountered. Falls back to
     * config.apiKey alone if apiKeys is empty (older saved configs).
     */
    private fun keyChain(config: AIConfig): List<String> =
        config.apiKeys.ifEmpty { listOf(config.apiKey) }.filter { it.isNotBlank() }

    private fun <T> withKeyFallback(config: AIConfig, action: (AIConfig) -> T): T {
        val keys = keyChain(config)
        if (keys.isEmpty()) throw AIException("No API key configured.")
        var lastError: AIException = AIException("All configured API keys failed.")
        for (key in keys) {
            try {
                return action(config.copy(apiKey = key))
            } catch (e: AIException) {
                lastError = e
                // Move on to the next key in the chain.
            }
        }
        throw lastError
    }

    private suspend fun <T> withKeyFallbackSuspend(config: AIConfig, action: suspend (AIConfig) -> T): T {
        val keys = keyChain(config)
        if (keys.isEmpty()) throw AIException("No API key configured.")
        var lastError: AIException = AIException("All configured API keys failed.")
        for (key in keys) {
            try {
                return action(config.copy(apiKey = key))
            } catch (e: AIException) {
                lastError = e
                // Move on to the next key in the chain.
            }
        }
        throw lastError
    }

    /** Sends the full conversation (role, content) pairs and returns the assistant's reply text. Throws AIException on failure. */
    fun sendMessage(config: AIConfig, systemPrompt: String, history: List<Pair<String, String>>): String =
        withKeyFallback(config) { single ->
            when (single.provider) {
                AIProvider.ANTHROPIC -> sendAnthropic(single, systemPrompt, history)
                AIProvider.OPENAI, AIProvider.GROQ, AIProvider.OPENAI_COMPATIBLE -> sendOpenAiCompatible(single, systemPrompt, history)
            }
        }

    /**
     * Same as sendMessage, but offers the model a set of tools it can call
     * (create/edit/delete/analyze VOID data) and loops — executing each
     * tool call via [executeTool] and feeding the result back — until the
     * model answers with plain text or the iteration ceiling is hit.
     * [executeTool] is called on whichever dispatcher this suspend
     * function is running on (network calls are the only part pushed onto
     * Dispatchers.IO), so it's safe for it to touch in-memory app state
     * directly.
     *
     * Also covered by the multi-key fallback chain: if the whole tool
     * round-trip fails on the current key (e.g. it runs out of quota
     * mid-conversation), it's retried from the top with the next key.
     */
    suspend fun sendMessageWithTools(
        config: AIConfig,
        systemPrompt: String,
        history: List<Pair<String, String>>,
        tools: List<AIToolDef>,
        executeTool: suspend (name: String, input: JSONObject) -> String
    ): AIToolLoopResult = withKeyFallbackSuspend(config) { single ->
        when (single.provider) {
            AIProvider.ANTHROPIC -> sendAnthropicWithTools(single, systemPrompt, history, tools, executeTool)
            AIProvider.OPENAI, AIProvider.GROQ, AIProvider.OPENAI_COMPATIBLE -> sendOpenAiWithTools(single, systemPrompt, history, tools, executeTool)
        }
    }

    /** Lightweight reachability + auth check — reports success/failure only, never exposes the key. Exercises the full key chain, same as a real send. */
    fun testConnection(config: AIConfig): Result<Unit> = try {
        sendMessage(config, "You are a connection test. Reply with OK.", listOf("user" to "ping"))
        Result.success(Unit)
    } catch (e: AIException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(AIException("Unexpected error during test."))
    }

    /**
     * Fetches the provider's real list of available model IDs, so the user
     * can pick from what actually exists instead of typing a model name
     * blind. Works for any provider that exposes a /models listing
     * endpoint — which OpenAI, Groq, and Anthropic all do.
     */
    fun fetchModels(config: AIConfig): Result<List<String>> = try {
        val ids = withKeyFallback(config) { single ->
            when (single.provider) {
                AIProvider.ANTHROPIC -> fetchAnthropicModels(single)
                AIProvider.OPENAI, AIProvider.GROQ, AIProvider.OPENAI_COMPATIBLE -> fetchOpenAiCompatibleModels(single)
            }
        }
        Result.success(ids)
    } catch (e: AIException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(AIException("Could not load model list."))
    }

    private fun fetchOpenAiCompatibleModels(config: AIConfig): List<String> {
        val base = config.baseUrl.ifBlank { AIProvider.OPENAI.defaultBaseUrl }.trimEnd('/')
        val conn = (URL("$base/models").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }
        val json = executeGet(conn)
        val data = json.getJSONArray("data")
        return (0 until data.length()).map { data.getJSONObject(it).getString("id") }.sorted()
    }

    private fun fetchAnthropicModels(config: AIConfig): List<String> {
        val base = config.baseUrl.ifBlank { AIProvider.ANTHROPIC.defaultBaseUrl }.trimEnd('/')
        val conn = (URL("$base/models").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("x-api-key", config.apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
        }
        val json = executeGet(conn)
        val data = json.getJSONArray("data")
        return (0 until data.length()).map { data.getJSONObject(it).getString("id") }.sorted()
    }

    private fun executeGet(conn: HttpURLConnection): JSONObject {
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                val providerMessage = runCatching {
                    JSONObject(text).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw AIException(providerMessage?.takeIf { it.isNotBlank() } ?: "Request failed (HTTP $code).")
            }
            return JSONObject(text)
        } catch (e: AIException) {
            throw e
        } catch (e: IOException) {
            throw AIException("Could not reach the AI provider. Check your connection.")
        } catch (e: Exception) {
            throw AIException("Unexpected error talking to the AI provider.")
        } finally {
            conn.disconnect()
        }
    }

    private fun sendAnthropic(config: AIConfig, systemPrompt: String, history: List<Pair<String, String>>): String {
        val base = config.baseUrl.ifBlank { AIProvider.ANTHROPIC.defaultBaseUrl }.trimEnd('/')
        val conn = openConnection("$base/messages") {
            setRequestProperty("x-api-key", config.apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
        }

        val messages = JSONArray()
        history.forEach { (role, content) -> messages.put(JSONObject().put("role", role).put("content", content)) }

        val body = JSONObject()
            .put("model", config.model.ifBlank { AIProvider.ANTHROPIC.defaultModel })
            .put("max_tokens", 1024)
            .put("system", systemPrompt)
            .put("messages", messages)

        return execute(conn, body) { response ->
            val content = response.getJSONArray("content")
            val text = StringBuilder()
            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                if (block.optString("type") == "text") text.append(block.optString("text"))
            }
            text.toString()
        }
    }

    private fun sendOpenAiCompatible(config: AIConfig, systemPrompt: String, history: List<Pair<String, String>>): String {
        val base = config.baseUrl.ifBlank { AIProvider.OPENAI.defaultBaseUrl }.trimEnd('/')
        val conn = openConnection("$base/chat/completions") {
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        history.forEach { (role, content) -> messages.put(JSONObject().put("role", role).put("content", content)) }

        val body = JSONObject()
            .put("model", config.model.ifBlank { AIProvider.OPENAI.defaultModel })
            .put("messages", messages)

        return execute(conn, body) { response ->
            val choices = response.getJSONArray("choices")
            choices.getJSONObject(0).getJSONObject("message").optString("content")
        }
    }

    private fun openConnection(url: String, configure: HttpURLConnection.() -> Unit): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json")
            configure()
        }

    /** Sends the request body and returns the raw parsed JSON response. Throws AIException on any failure. */
    private fun postJson(conn: HttpURLConnection, body: JSONObject): JSONObject {
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                val providerMessage = runCatching {
                    JSONObject(text).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw AIException(providerMessage?.takeIf { it.isNotBlank() } ?: "Request failed (HTTP $code).")
            }

            return JSONObject(text)
        } catch (e: AIException) {
            throw e
        } catch (e: IOException) {
            throw AIException("Could not reach the AI provider. Check your connection.")
        } catch (e: Exception) {
            throw AIException("Unexpected error talking to the AI provider.")
        } finally {
            conn.disconnect()
        }
    }

    private fun execute(conn: HttpURLConnection, body: JSONObject, parse: (JSONObject) -> String): String {
        val reply = parse(postJson(conn, body))
        if (reply.isBlank()) throw AIException("Empty response from AI provider.")
        return reply
    }

    /** Trims a tool call's input to something short enough to show in an action log line. */
    private fun summarizeInput(input: JSONObject): String {
        val s = input.toString()
        return if (s.length > 140) s.take(140) + "\u2026" else s
    }

    private suspend fun sendAnthropicWithTools(
        config: AIConfig,
        systemPrompt: String,
        history: List<Pair<String, String>>,
        tools: List<AIToolDef>,
        executeTool: suspend (String, JSONObject) -> String
    ): AIToolLoopResult {
        val base = config.baseUrl.ifBlank { AIProvider.ANTHROPIC.defaultBaseUrl }.trimEnd('/')
        val messages = JSONArray()
        history.forEach { (role, content) -> messages.put(JSONObject().put("role", role).put("content", content)) }

        val toolsJson = JSONArray()
        tools.forEach { toolsJson.put(JSONObject().put("name", it.name).put("description", it.description).put("input_schema", it.inputSchema)) }

        val actionsLog = mutableListOf<String>()

        repeat(MAX_TOOL_ITERATIONS) {
            val body = JSONObject()
                .put("model", config.model.ifBlank { AIProvider.ANTHROPIC.defaultModel })
                .put("max_tokens", 1024)
                .put("system", systemPrompt)
                .put("messages", messages)
            if (toolsJson.length() > 0) body.put("tools", toolsJson)

            val response = withContext(Dispatchers.IO) {
                val conn = openConnection("$base/messages") {
                    setRequestProperty("x-api-key", config.apiKey)
                    setRequestProperty("anthropic-version", "2023-06-01")
                }
                postJson(conn, body)
            }

            val contentArr = response.getJSONArray("content")
            val textParts = StringBuilder()
            val toolUses = mutableListOf<JSONObject>()
            for (i in 0 until contentArr.length()) {
                val block = contentArr.getJSONObject(i)
                when (block.optString("type")) {
                    "text" -> textParts.append(block.optString("text"))
                    "tool_use" -> toolUses += block
                }
            }

            if (toolUses.isEmpty()) {
                return AIToolLoopResult(textParts.toString().ifBlank { "OK." }, actionsLog)
            }

            // The assistant turn must be echoed back verbatim (including
            // its tool_use blocks) so the tool_result below can reference
            // the same tool_use_id — Anthropic requires this pairing.
            messages.put(JSONObject().put("role", "assistant").put("content", contentArr))

            val resultsContent = JSONArray()
            toolUses.forEach { block ->
                val toolName = block.getString("name")
                val toolInput = block.optJSONObject("input") ?: JSONObject()
                val toolUseId = block.getString("id")
                val resultText = executeTool(toolName, toolInput)
                actionsLog += "$toolName(${summarizeInput(toolInput)}) \u2192 $resultText"
                resultsContent.put(
                    JSONObject()
                        .put("type", "tool_result")
                        .put("tool_use_id", toolUseId)
                        .put("content", resultText)
                )
            }
            messages.put(JSONObject().put("role", "user").put("content", resultsContent))
        }

        return AIToolLoopResult(
            "I performed several actions but hit the step limit before finishing my reply. Check the app to confirm everything is as expected.",
            actionsLog
        )
    }

    private suspend fun sendOpenAiWithTools(
        config: AIConfig,
        systemPrompt: String,
        history: List<Pair<String, String>>,
        tools: List<AIToolDef>,
        executeTool: suspend (String, JSONObject) -> String
    ): AIToolLoopResult {
        val base = config.baseUrl.ifBlank { AIProvider.OPENAI.defaultBaseUrl }.trimEnd('/')
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        history.forEach { (role, content) -> messages.put(JSONObject().put("role", role).put("content", content)) }

        val toolsJson = JSONArray()
        tools.forEach {
            toolsJson.put(
                JSONObject().put("type", "function").put(
                    "function",
                    JSONObject().put("name", it.name).put("description", it.description).put("parameters", it.inputSchema)
                )
            )
        }

        val actionsLog = mutableListOf<String>()

        repeat(MAX_TOOL_ITERATIONS) {
            val body = JSONObject()
                .put("model", config.model.ifBlank { AIProvider.OPENAI.defaultModel })
                .put("messages", messages)
            if (toolsJson.length() > 0) body.put("tools", toolsJson)

            val response = withContext(Dispatchers.IO) {
                val conn = openConnection("$base/chat/completions") {
                    setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                }
                postJson(conn, body)
            }

            val message = response.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            val toolCalls = message.optJSONArray("tool_calls")
            val text = message.optString("content", "")

            if (toolCalls == null || toolCalls.length() == 0) {
                return AIToolLoopResult(text.ifBlank { "OK." }, actionsLog)
            }

            val assistantMsg = JSONObject().put("role", "assistant").put("tool_calls", toolCalls)
            assistantMsg.put("content", if (text.isNotBlank()) text else JSONObject.NULL)
            messages.put(assistantMsg)

            for (i in 0 until toolCalls.length()) {
                val call = toolCalls.getJSONObject(i)
                val fn = call.getJSONObject("function")
                val toolName = fn.getString("name")
                val toolInput = runCatching { JSONObject(fn.optString("arguments", "{}")) }.getOrDefault(JSONObject())
                val callId = call.getString("id")
                val resultText = executeTool(toolName, toolInput)
                actionsLog += "$toolName(${summarizeInput(toolInput)}) \u2192 $resultText"
                messages.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", resultText))
            }
        }

        return AIToolLoopResult(
            "I performed several actions but hit the step limit before finishing my reply. Check the app to confirm everything is as expected.",
            actionsLog
        )
    }
}
