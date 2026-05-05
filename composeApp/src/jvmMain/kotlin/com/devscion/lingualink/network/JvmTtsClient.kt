package com.devscion.lingualink.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
private data class TtsRequest(
    val text: String,
    val model_id: String = "eleven_turbo_v2",
    val voice_settings: VoiceSettings = VoiceSettings()
)

@Serializable
private data class VoiceSettings(
    val stability: Double = 0.5,
    val similarity_boost: Double = 0.75
)

class JvmTtsClient(private val httpClient: HttpClient) : TtsClient {

    private var apiKey: String = ""

    override fun configure(key: String) { apiKey = key }

    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override suspend fun synthesize(text: String, voiceId: String): ByteArray? {
        if (apiKey.isBlank() || text.isBlank()) return null
        return try {
            httpClient.post("https://api.elevenlabs.io/v1/text-to-speech/$voiceId") {
                header("xi-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(TtsRequest(text = text.take(500)))
            }.body<ByteArray>()
        } catch (e: Exception) {
            println("TTS error (non-fatal): ${e.message}")
            null
        }
    }
}
