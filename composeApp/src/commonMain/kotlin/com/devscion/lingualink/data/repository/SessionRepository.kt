package com.devscion.lingualink.data.repository

import com.devscion.lingualink.data.model.Session
import com.devscion.lingualink.data.model.SessionType

interface SessionRepository {
    suspend fun createSession(type: SessionType, sourceLang: String, targetLang: String): Long
    suspend fun closeSession(id: Long, durationSeconds: Long)
    suspend fun getAllSessions(): List<Session>
    suspend fun deleteSession(id: Long)
}
