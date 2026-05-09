package com.devscion.lingualink.data.repository

import com.devscion.lingualink.data.model.Session
import com.devscion.lingualink.data.model.SessionType
import com.devscion.lingualink.db.LinguaLinkDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmSessionRepository(private val db: LinguaLinkDB) : SessionRepository {

    override suspend fun createSession(type: SessionType, sourceLang: String, targetLang: String): Long =
        withContext(Dispatchers.IO) {
            db.linguaLinkQueries.insertSession(
                session_type = type.name.lowercase(),
                source_language = sourceLang,
                target_language = targetLang,
                started_at = System.currentTimeMillis()
            )
            db.linguaLinkQueries.lastInsertRowId().executeAsOne()
        }

    override suspend fun closeSession(id: Long, durationSeconds: Long) = withContext(Dispatchers.IO) {
        db.linguaLinkQueries.closeSession(
            ended_at = System.currentTimeMillis(),
            duration_seconds = durationSeconds,
            id = id
        )
    }

    override suspend fun getAllSessions(): List<Session> = withContext(Dispatchers.IO) {
        db.linguaLinkQueries.selectAllSessions().executeAsList().map {
            Session(
                id = it.id,
                sessionType = if (it.session_type == "call") SessionType.CALL else SessionType.CHAT,
                sourceLanguage = it.source_language,
                targetLanguage = it.target_language,
                startedAt = it.started_at,
                endedAt = it.ended_at,
                durationSeconds = it.duration_seconds
            )
        }
    }

    override suspend fun getSessionById(id: Long): Session? = withContext(Dispatchers.IO) {
        db.linguaLinkQueries.selectSessionById(id).executeAsOneOrNull()?.let {
            Session(
                id = it.id,
                sessionType = if (it.session_type == "call") SessionType.CALL else SessionType.CHAT,
                sourceLanguage = it.source_language,
                targetLanguage = it.target_language,
                startedAt = it.started_at,
                endedAt = it.ended_at,
                durationSeconds = it.duration_seconds
            )
        }
    }

    override suspend fun deleteSession(id: Long) = withContext(Dispatchers.IO) {
        db.linguaLinkQueries.deleteSession(id)
    }
}
