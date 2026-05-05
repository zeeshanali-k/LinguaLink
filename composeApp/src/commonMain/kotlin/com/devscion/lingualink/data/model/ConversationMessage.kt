package com.devscion.lingualink.data.model

data class ConversationMessage(
    val id: Long = 0,
    val sessionId: Long,
    val speaker: Speaker,
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val confidence: Float? = null,
    val createdAt: Long = 0L
)

enum class Speaker { USER_A, USER_B }
