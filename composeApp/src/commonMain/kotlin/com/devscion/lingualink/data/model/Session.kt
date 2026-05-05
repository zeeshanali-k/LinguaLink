package com.devscion.lingualink.data.model

data class Session(
    val id: Long = 0,
    val sessionType: SessionType,
    val sourceLanguage: String,
    val targetLanguage: String,
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val durationSeconds: Long? = null
)

enum class SessionType { CALL, CHAT }
