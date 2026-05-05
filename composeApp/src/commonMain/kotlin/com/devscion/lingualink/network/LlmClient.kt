package com.devscion.lingualink.network

interface LlmClient {
    fun configure(dropletBaseUrl: String)
    fun isConfigured(): Boolean
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): String
    suspend fun chat(userMessage: String, systemPrompt: String, history: List<ChatMessage> = emptyList()): String
}

data class ChatMessage(val role: String, val content: String)
