# VoxFlow — Spec 05: LLM Client (AMD Droplet)
> Read this for TASK 7 only.

---

## TASK 7 — AMD GPU Droplet LLM Client

The AMD droplet exposes an **OpenAI-compatible REST API**. Treat it exactly like the OpenAI chat completions endpoint. No special SDK needed — Ktor HTTP + JSON is enough.

### Expected Endpoint
```
POST http://<AMD_DROPLET_IP>:<PORT>/v1/chat/completions
Content-Type: application/json
```

The base URL comes from config: `amd_droplet_base_url` e.g. `http://192.168.1.100:8000`

---

### network/LlmClient.kt

```kotlin
package com.voxflow.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatRequest(
    val model: String = "default",   // Use "default" — droplet decides model
    val messages: List<ChatMessage>,
    val max_tokens: Int = 512,
    val temperature: Double = 0.2,   // Low temp = consistent translations
    val stream: Boolean = false
)

@Serializable
data class ChatResponse(
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(val message: ChatMessage)
}

class LlmClient(private val httpClient: HttpClient) {

    private var baseUrl: String = ""

    fun configure(dropletBaseUrl: String) {
        baseUrl = dropletBaseUrl.trimEnd('/')
    }

    // Translate text from sourceLang to targetLang
    // Maintains conversation context via history
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): String {
        val systemPrompt = buildTranslationPrompt(sourceLang, targetLang)
        val messages = buildList {
            add(ChatMessage("system", systemPrompt))
            addAll(conversationHistory.takeLast(6)) // Keep last 3 exchanges for context
            add(ChatMessage("user", text))
        }

        val response: ChatResponse = httpClient.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(messages = messages))
        }.body()

        return response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw IllegalStateException("Empty response from LLM")
    }

    // General follow-up question within session context (for future use)
    suspend fun chat(
        userMessage: String,
        systemPrompt: String,
        history: List<ChatMessage> = emptyList()
    ): String {
        val messages = buildList {
            add(ChatMessage("system", systemPrompt))
            addAll(history.takeLast(10))
            add(ChatMessage("user", userMessage))
        }
        val response: ChatResponse = httpClient.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(messages = messages))
        }.body()
        return response.choices.firstOrNull()?.message?.content?.trim() ?: ""
    }

    private fun buildTranslationPrompt(sourceLang: String, targetLang: String): String = """
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

    fun isConfigured(): Boolean = baseUrl.isNotBlank()
}
```

**Acceptance:**
- `buildTranslationPrompt()` unit test: verify it contains source and target language strings.
- Integration test (gated by env var `AMD_DROPLET_URL`): translate "Hello, how are you?" from `en` to `ur`. Confirm non-empty response.
- `configure()` must be called before `translate()` — throw `IllegalStateException("LlmClient not configured")` if `baseUrl` is blank.

---

# VoxFlow — Spec 06: TTS Client (ElevenLabs)
> Read this for TASK 8 only.

---

## TASK 8 — ElevenLabs TTS Client

ElevenLabs REST API converts text to speech. Returns raw audio bytes (mp3).

### API Details
```
POST https://api.elevenlabs.io/v1/text-to-speech/{voice_id}
xi-api-key: <API_KEY>
Content-Type: application/json

Body: { "text": "...", "model_id": "eleven_turbo_v2", "voice_settings": { "stability": 0.5, "similarity_boost": 0.75 } }

Response: binary audio/mpeg bytes
```

### Voice IDs to use (multilingual voices)
```kotlin
val VOICE_USER_A = "pNInz6obpgDQGcFmaJgB"   // Adam — clear, neutral English
val VOICE_USER_B = "ErXwobaYiN019PkySvjV"   // Antoni — alternative voice
// ElevenLabs Turbo v2 supports multilingual output — same voice ID works for all languages
```

---

### network/TtsClient.kt

```kotlin
package com.voxflow.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class TtsRequest(
    val text: String,
    val model_id: String = "eleven_turbo_v2",
    val voice_settings: VoiceSettings = VoiceSettings()
)

@Serializable
data class VoiceSettings(
    val stability: Double = 0.5,
    val similarity_boost: Double = 0.75
)

class TtsClient(private val httpClient: HttpClient) {

    private var apiKey: String = ""

    fun configure(key: String) {
        apiKey = key
    }

    // Returns raw MP3 audio bytes, ready for AudioPlayer.playAudioStream()
    // voiceId: use VOICE_USER_A or VOICE_USER_B constants
    // Returns null if TTS is unavailable (graceful degradation)
    suspend fun synthesize(
        text: String,
        voiceId: String = VOICE_USER_A
    ): ByteArray? {
        if (apiKey.isBlank()) return null   // TTS is optional — degrade gracefully
        if (text.isBlank()) return null

        return try {
            httpClient.post(
                "https://api.elevenlabs.io/v1/text-to-speech/$voiceId"
            ) {
                header("xi-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(TtsRequest(text = text.take(500)))  // ElevenLabs 500 char limit per request
            }.body<ByteArray>()
        } catch (e: Exception) {
            println("TTS error (non-fatal): ${e.message}")
            null
        }
    }

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    companion object {
        const val VOICE_USER_A = "pNInz6obpgDQGcFmaJgB"
        const val VOICE_USER_B = "ErXwobaYiN019PkySvjV"
    }
}
```

**Important — TTS is optional:**
If `elevenlabs_api_key` is empty in config, TTS simply doesn't play. The transcript and translation still show in the UI. Never crash or block the pipeline if TTS fails.

**Acceptance:**
- `synthesize()` returns `null` gracefully when `apiKey` is blank.
- Integration test (gated by env var `ELEVENLABS_API_KEY`): synthesize "Hello world", confirm byte array length > 0.
- AudioPlayer successfully plays the returned bytes without errors.
