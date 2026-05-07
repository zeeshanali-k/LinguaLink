package com.devscion.lingualink.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

class JvmDeepgramClient(private val httpClient: HttpClient) : AsrClient {

    override fun streamTranscription(
        audioChunks: Flow<ByteArray>,
        apiKey: String,
        languageCode: String
    ): Flow<TranscriptResult> = flow {

        println("[ASR] streamTranscription")
        httpClient.webSocket(
            urlString = buildDeepgramUrl(languageCode),
            request = { headers.append("Authorization", "Token $apiKey") }
        ) {
            // Send audio chunks in a child coroutine (DefaultClientWebSocketSession is a CoroutineScope)
            var chunksSent = 0
            val senderJob = launch {
                try {
                    audioChunks.collect { chunk ->
                        println("[ASR] Chunk received: ${chunk.size}")
                        send(Frame.Binary(fin = true, data = chunk))
                        chunksSent++
                        if (chunksSent % 50 == 0) println("[ASR] sent $chunksSent audio chunks to Deepgram")
                    }
                    println("[ASR] Close Stream Sending")
                    send(Frame.Text("""{"type":"CloseStream"}"""))
                } catch (e: Exception) {
                    println("[ASR] sender error: ${e.message}")
                }
            }

            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            println("[ASR] received: ${text.take(300)}")
                            parseTranscriptResult(text)?.let { emit(it) }
                        }
                        is Frame.Close -> {
                            println("[ASR] WebSocket closed by server")
                            break
                        }
                        else -> {
                            println("[ASR] WebSocket other frame received-> $frame")
                        }
                    }
                }
            } finally {
                senderJob.cancel()
            }
        }
    }

    private fun buildDeepgramUrl(languageCode: String) =
        "wss://api.deepgram.com/v1/listen" +
        "?model=nova-3&encoding=linear16&sample_rate=16000&channels=1" +
        "&language=$languageCode&punctuate=true&interim_results=true&endpointing=300"

    private fun parseTranscriptResult(json: String): TranscriptResult? = try {
        val obj = Json.parseToJsonElement(json).jsonObject
        if (obj["type"]?.jsonPrimitive?.content != "Results") return null
        val best = obj["channel"]?.jsonObject
            ?.get("alternatives")?.jsonArray
            ?.firstOrNull()?.jsonObject ?: return null
        val transcript = best["transcript"]?.jsonPrimitive?.content ?: return null
        if (transcript.isBlank()) return null
        TranscriptResult(
            text = transcript,
            confidence = best["confidence"]?.jsonPrimitive?.float ?: 0f,
            isFinal = obj["is_final"]?.jsonPrimitive?.boolean ?: false,
            speechFinal = obj["speech_final"]?.jsonPrimitive?.boolean ?: false
        )
    } catch (_: Exception) { null }
}
