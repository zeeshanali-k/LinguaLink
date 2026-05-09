package com.devscion.lingualink.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
private data class SpeakRequest(val text: String)

class JvmTtsClient(private val httpClient: HttpClient) : TtsClient {

    private var apiKey: String = ""

    override fun configure(apiKey: String) {
        this.apiKey = apiKey.trim()
    }

    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override suspend fun synthesize(text: String, voiceModel: String): ByteArray? {
        if (apiKey.isBlank() || text.isBlank() || voiceModel.isBlank()) {
            println("[TTS] skipped: apiKeySet=${apiKey.isNotBlank()}, textSet=${text.isNotBlank()}, voiceSet=${voiceModel.isNotBlank()}")
            return null
        }
        return try {
            val response = httpClient.post("https://api.deepgram.com/v1/speak") {
                // Request WAV (linear16 PCM) — JVM's javax.sound.sampled handles WAV natively,
                // but does NOT decode MP3 (the Deepgram default) without an SPI plugin.
                parameter("model", voiceModel)
                parameter("encoding", "linear16")
                parameter("container", "wav")
                parameter("sample_rate", "24000")
                header("Authorization", "Token $apiKey")
                contentType(ContentType.Application.Json)
                setBody(SpeakRequest(text = text.take(2000)))
            }
            val bytes = response.body<ByteArray>()
            if (!response.status.isSuccess()) {
                println("[TTS] HTTP ${response.status.value}: ${bytes.decodeToString().take(300)}")
                null
            } else {
                println("[TTS] OK: ${bytes.size}B contentType=${response.contentType()}")
                bytes
            }
        } catch (e: Exception) {
            println("[TTS] error: ${e::class.simpleName}: ${e.message}")
            null
        }
    }
}
