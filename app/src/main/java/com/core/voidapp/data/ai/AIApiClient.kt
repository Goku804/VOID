package com.core.voidapp.data.ai

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

    /** Sends the full conversation (role, content) pairs and returns the assistant's reply text. Throws AIException on failure. */
    fun sendMessage(config: AIConfig, systemPrompt: String, history: List<Pair<String, String>>): String =
        when (config.provider) {
            AIProvider.ANTHROPIC -> sendAnthropic(config, systemPrompt, history)
            AIProvider.OPENAI, AIProvider.OPENAI_COMPATIBLE -> sendOpenAiCompatible(config, systemPrompt, history)
        }

    /** Lightweight reachability + auth check — reports success/failure only, never exposes the key. */
    fun testConnection(config: AIConfig): Result<Unit> = try {
        sendMessage(config, "You are a connection test. Reply with OK.", listOf("user" to "ping"))
        Result.success(Unit)
    } catch (e: AIException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(AIException("Unexpected error during test."))
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

    private fun execute(conn: HttpURLConnection, body: JSONObject, parse: (JSONObject) -> String): String {
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

            val reply = parse(JSONObject(text))
            if (reply.isBlank()) throw AIException("Empty response from AI provider.")
            return reply
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
}
