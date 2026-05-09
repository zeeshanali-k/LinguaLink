package com.devscion.lingualink.data.repository

import com.devscion.lingualink.data.model.ConversationMessage
import com.devscion.lingualink.data.model.Speaker
import com.devscion.lingualink.db.LinguaLinkDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmMessageRepository(private val db: LinguaLinkDB) : MessageRepository {

    override suspend fun insertMessage(msg: ConversationMessage): Long = withContext(Dispatchers.IO) {
        var newId = -1L
        db.linguaLinkQueries.transaction {
            db.linguaLinkQueries.insertMessage(
                session_id = msg.sessionId,
                speaker = msg.speaker.name.lowercase(),
                original_text = msg.originalText,
                translated_text = msg.translatedText,
                source_language = msg.sourceLanguage,
                target_language = msg.targetLanguage,
                confidence = msg.confidence?.toDouble(),
                created_at = System.currentTimeMillis()
            )
            newId = db.linguaLinkQueries.lastInsertRowId().executeAsOne()
        }
        newId
    }

    override suspend fun getMessagesBySession(sessionId: Long): List<ConversationMessage> =
        withContext(Dispatchers.IO) {
            db.linguaLinkQueries.selectMessagesBySession(sessionId).executeAsList().map {
                ConversationMessage(
                    id = it.id,
                    sessionId = it.session_id,
                    speaker = if (it.speaker == "user_a") Speaker.USER_A else Speaker.USER_B,
                    originalText = it.original_text,
                    translatedText = it.translated_text,
                    sourceLanguage = it.source_language,
                    targetLanguage = it.target_language,
                    confidence = it.confidence?.toFloat(),
                    createdAt = it.created_at
                )
            }
        }
}
