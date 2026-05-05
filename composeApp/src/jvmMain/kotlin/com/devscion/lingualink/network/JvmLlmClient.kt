package com.devscion.lingualink.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ChatRequest(
    val model: String = "default",
    val messages: List<ChatMessageDto>,
    val max_tokens: Int = 512,
    val temperature: Double = 0.2,
    val stream: Boolean = false
)

@Serializable
private data class ChatMessageDto(val role: String, val content: String)

@Serializable
private data class ChatResponse(val choices: List<Choice>) {
    @Serializable
    data class Choice(val message: ChatMessageDto)
}

class JvmLlmClient(private val httpClient: HttpClient) : LlmClient {

    private var baseUrl: String = ""

    override fun configure(dropletBaseUrl: String) {
        baseUrl = dropletBaseUrl.trimEnd('/')
    }

    override fun isConfigured(): Boolean = baseUrl.isNotBlank()

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
        conversationHistory: List<ChatMessage>
    ): String {
        if (!isConfigured()) throw IllegalStateException("LlmClient not configured")
        val messages = buildList {
            add(ChatMessageDto("system", buildTranslationPrompt(sourceLang, targetLang)))
            addAll(conversationHistory.takeLast(6).map { ChatMessageDto(it.role, it.content) })
            add(ChatMessageDto("user", text))
        }
        val response: ChatResponse = httpClient.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(messages = messages))
        }.body()
        return response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw IllegalStateException("Empty response from LLM")
    }

    override suspend fun chat(userMessage: String, systemPrompt: String, history: List<ChatMessage>): String {
        if (!isConfigured()) throw IllegalStateException("LlmClient not configured")
        val messages = buildList {
            add(ChatMessageDto("system", systemPrompt))
            addAll(history.takeLast(10).map { ChatMessageDto(it.role, it.content) })
            add(ChatMessageDto("user", userMessage))
        }
        val response: ChatResponse = httpClient.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(messages = messages))
        }.body()
        return response.choices.firstOrNull()?.message?.content?.trim() ?: ""
    }

    private fun buildTranslationPrompt(sourceLang: String, targetLang: String) = """
        You are a real-time translation assistant.
        Translate the user's message from $sourceLang to $targetLang.
        Rules:
        - Output ONLY the translated text. No explanations, no labels, no quotes.
        - Preserve the original tone: formal stays formal, casual stays casual.
        - Preserve all proper nouns and technical terms unless they have a natural translation.
        - If the input is already in $targetLang, return it unchanged.
        - Use the conversation history to resolve pronouns and context correctly.
        - Handle incomplete sentences naturally — real speech is not always grammatically complete.
    """.trimIndent()
}
