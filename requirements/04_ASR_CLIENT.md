# VoxFlow — Spec 04: Deepgram ASR Client
> Read this for TASK 6 only.

---

## TASK 6 — Deepgram Streaming WebSocket Client

Deepgram's streaming API accepts raw PCM audio over WebSocket and returns transcripts in real-time as JSON events.

### API Reference
- WebSocket URL: `wss://api.deepgram.com/v1/listen`
- Auth: `Authorization: Token <API_KEY>` header
- Query params to append to URL:
  - `encoding=linear16` — matches our PCM_SIGNED 16-bit format
  - `sample_rate=16000`
  - `channels=1`
  - `language=<BCP-47 code>` — e.g. `en-US`, `ur`, `ar`
  - `punctuate=true`
  - `interim_results=true` — get partial transcripts while speaking
  - `endpointing=300` — detect end of speech after 300ms silence

### Deepgram Response JSON Shape

```json
{
  "type": "Results",
  "channel_index": [0, 1],
  "duration": 1.04,
  "start": 0.0,
  "is_final": true,
  "speech_final": true,
  "channel": {
    "alternatives": [
      {
        "transcript": "hello how are you",
        "confidence": 0.99,
        "words": []
      }
    ]
  }
}
```

Only process events where `is_final = true` for sending to LLM. Show `is_final = false` results as a live "typing" preview in the UI.

---

### network/DeepgramClient.kt

```kotlin
package com.voxflow.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable

@Serializable
data class TranscriptResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val speechFinal: Boolean
)

class DeepgramClient(private val httpClient: HttpClient) {

    // Streams TranscriptResult objects.
    // audioChunks: Flow of raw PCM ByteArrays from AudioCapture
    // apiKey: Deepgram API key
    // languageCode: Deepgram language code e.g. "en-US", "ur"
    fun streamTranscription(
        audioChunks: Flow<ByteArray>,
        apiKey: String,
        languageCode: String
    ): Flow<TranscriptResult> = callbackFlow {

        val url = buildDeepgramUrl(languageCode)

        httpClient.webSocket(
            urlString = url,
            request = {
                headers.append("Authorization", "Token $apiKey")
            }
        ) {
            // Launch sender: read audio chunks and send as binary frames
            val senderJob = launch {
                audioChunks.collect { chunk ->
                    send(Frame.Binary(fin = true, data = chunk))
                }
                // Signal end of stream to Deepgram
                send(Frame.Text("""{"type":"CloseStream"}"""))
            }

            // Receive transcript results
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val json = frame.readText()
                        parseTranscriptResult(json)?.let { result ->
                            trySend(result)
                        }
                    }
                    is Frame.Close -> break
                    else -> { /* ignore */ }
                }
            }

            senderJob.cancelAndJoin()
        }

        close()
    }

    private fun buildDeepgramUrl(languageCode: String): String {
        return "wss://api.deepgram.com/v1/listen" +
            "?encoding=linear16" +
            "&sample_rate=16000" +
            "&channels=1" +
            "&language=$languageCode" +
            "&punctuate=true" +
            "&interim_results=true" +
            "&endpointing=300"
    }

    private fun parseTranscriptResult(json: String): TranscriptResult? {
        return try {
            val obj = Json.parseToJsonElement(json).jsonObject
            val type = obj["type"]?.jsonPrimitive?.content
            if (type != "Results") return null

            val alternatives = obj["channel"]
                ?.jsonObject?.get("alternatives")
                ?.jsonArray ?: return null

            val best = alternatives.firstOrNull()?.jsonObject ?: return null
            val transcript = best["transcript"]?.jsonPrimitive?.content ?: return null
            if (transcript.isBlank()) return null

            val confidence = best["confidence"]?.jsonPrimitive?.float ?: 0f
            val isFinal = obj["is_final"]?.jsonPrimitive?.boolean ?: false
            val speechFinal = obj["speech_final"]?.jsonPrimitive?.boolean ?: false

            TranscriptResult(transcript, confidence, isFinal, speechFinal)
        } catch (e: Exception) {
            null // Silently ignore malformed frames
        }
    }
}
```

**Acceptance:**
- Unit test `parseTranscriptResult()` with a mock JSON string — confirm it returns correct `TranscriptResult`.
- Manual integration test (gated by env var `DEEPGRAM_API_KEY`): stream 5 seconds of mic audio, print results. At least 1 `isFinal=true` result received.
- Confirm partial results (`isFinal=false`) are also emitted.
