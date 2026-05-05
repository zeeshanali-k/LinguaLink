# VoxFlow — Spec 02: Data Layer
> Read this for TASK 3 and TASK 4 only.

---

## TASK 3 — SQLDelight Schema + Database Factory

### src/main/sqldelight/com/voxflow/db/VoxFlow.sq

```sql
-- Translation sessions (one per call or chat)
CREATE TABLE sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_type TEXT NOT NULL,        -- "call" | "chat"
    source_language TEXT NOT NULL,     -- e.g. "en"
    target_language TEXT NOT NULL,     -- e.g. "ur"
    started_at INTEGER NOT NULL,       -- Unix timestamp ms
    ended_at INTEGER,                  -- null if ongoing
    duration_seconds INTEGER           -- filled on session end
);

-- Individual messages within a session
CREATE TABLE messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    speaker TEXT NOT NULL,             -- "user_a" | "user_b"
    original_text TEXT NOT NULL,       -- raw transcript or typed text
    translated_text TEXT NOT NULL,     -- LLM output
    source_language TEXT NOT NULL,
    target_language TEXT NOT NULL,
    confidence REAL,                   -- ASR confidence 0.0-1.0, null for chat
    created_at INTEGER NOT NULL        -- Unix timestamp ms
);

-- Queries
selectAllSessions:
SELECT * FROM sessions ORDER BY started_at DESC;

selectSessionById:
SELECT * FROM sessions WHERE id = ?;

selectMessagesBySession:
SELECT * FROM messages WHERE session_id = ? ORDER BY created_at ASC;

insertSession:
INSERT INTO sessions(session_type, source_language, target_language, started_at)
VALUES (?, ?, ?, ?);

closeSession:
UPDATE sessions SET ended_at = ?, duration_seconds = ? WHERE id = ?;

insertMessage:
INSERT INTO messages(session_id, speaker, original_text, translated_text, source_language, target_language, confidence, created_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

deleteSession:
DELETE FROM sessions WHERE id = ?;
```

### DatabaseFactory.kt

```kotlin
package com.voxflow.data.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.voxflow.db.VoxFlowDB
import java.io.File

object DatabaseFactory {
    fun create(): VoxFlowDB {
        val dbDir = File(System.getProperty("user.home"), ".voxflow").also { it.mkdirs() }
        val dbFile = File(dbDir, "voxflow.db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        // Create tables if they don't exist
        VoxFlowDB.Schema.create(driver)

        return VoxFlowDB(driver)
    }
}
```

Add to Koin `repositoryModule`:
```kotlin
single { DatabaseFactory.create() }
```

**Acceptance:** `./gradlew generateVoxFlowDBInterface` runs cleanly. DB file created at `~/.voxflow/voxflow.db` on first run.

---

## TASK 4 — Kotlin Data Models + Repositories

### data/model/Language.kt

```kotlin
package com.voxflow.data.model

data class Language(
    val code: String,   // BCP-47 e.g. "en", "ur", "zh", "ar", "fr"
    val name: String,   // Display name e.g. "English"
    val nativeName: String,  // e.g. "اردو"
    val deepgramCode: String // Deepgram language code — may differ from BCP-47
)

val SupportedLanguages = listOf(
    Language("en", "English", "English", "en-US"),
    Language("ur", "Urdu", "اردو", "ur"),
    Language("ar", "Arabic", "العربية", "ar"),
    Language("zh", "Chinese", "中文", "zh-CN"),
    Language("fr", "French", "Français", "fr"),
    Language("de", "German", "Deutsch", "de"),
    Language("es", "Spanish", "Español", "es"),
    Language("hi", "Hindi", "हिन्दी", "hi"),
    Language("ja", "Japanese", "日本語", "ja"),
    Language("ko", "Korean", "한국어", "ko"),
    Language("pt", "Portuguese", "Português", "pt"),
    Language("ru", "Russian", "Русский", "ru"),
    Language("tr", "Turkish", "Türkçe", "tr")
)

fun languageByCode(code: String) = SupportedLanguages.find { it.code == code }
    ?: SupportedLanguages.first()
```

### data/model/ConversationMessage.kt

```kotlin
package com.voxflow.data.model

data class ConversationMessage(
    val id: Long = 0,
    val sessionId: Long,
    val speaker: Speaker,
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val confidence: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Speaker { USER_A, USER_B }
```

### data/model/Session.kt

```kotlin
package com.voxflow.data.model

data class Session(
    val id: Long = 0,
    val sessionType: SessionType,
    val sourceLanguage: String,
    val targetLanguage: String,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val durationSeconds: Long? = null
)

enum class SessionType { CALL, CHAT }
```

### data/repository/SessionRepository.kt

```kotlin
package com.voxflow.data.repository

import com.voxflow.db.VoxFlowDB
import com.voxflow.data.model.Session
import com.voxflow.data.model.SessionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionRepository(private val db: VoxFlowDB) {

    suspend fun createSession(
        type: SessionType,
        sourceLang: String,
        targetLang: String
    ): Long = withContext(Dispatchers.IO) {
        db.sessionsQueries.insertSession(
            session_type = type.name.lowercase(),
            source_language = sourceLang,
            target_language = targetLang,
            started_at = System.currentTimeMillis()
        )
        db.sessionsQueries.selectAllSessions().executeAsList().first().id
    }

    suspend fun closeSession(id: Long, durationSeconds: Long) = withContext(Dispatchers.IO) {
        db.sessionsQueries.closeSession(
            ended_at = System.currentTimeMillis(),
            duration_seconds = durationSeconds,
            id = id
        )
    }

    suspend fun getAllSessions(): List<Session> = withContext(Dispatchers.IO) {
        db.sessionsQueries.selectAllSessions().executeAsList().map {
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

    suspend fun deleteSession(id: Long) = withContext(Dispatchers.IO) {
        db.sessionsQueries.deleteSession(id)
    }
}
```

### data/repository/MessageRepository.kt

```kotlin
package com.voxflow.data.repository

import com.voxflow.db.VoxFlowDB
import com.voxflow.data.model.ConversationMessage
import com.voxflow.data.model.Speaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessageRepository(private val db: VoxFlowDB) {

    suspend fun insertMessage(msg: ConversationMessage): Long = withContext(Dispatchers.IO) {
        db.messagesQueries.insertMessage(
            session_id = msg.sessionId,
            speaker = msg.speaker.name.lowercase(),
            original_text = msg.originalText,
            translated_text = msg.translatedText,
            source_language = msg.sourceLanguage,
            target_language = msg.targetLanguage,
            confidence = msg.confidence?.toDouble(),
            created_at = msg.createdAt
        )
        db.messagesQueries.selectMessagesBySession(msg.sessionId).executeAsList().last().id
    }

    suspend fun getMessagesBySession(sessionId: Long): List<ConversationMessage> =
        withContext(Dispatchers.IO) {
            db.messagesQueries.selectMessagesBySession(sessionId).executeAsList().map {
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
```

**Acceptance:** Repositories compile. Write a unit test that inserts a session + message and reads them back using an in-memory SQLite driver (`JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)`). Test passes.
