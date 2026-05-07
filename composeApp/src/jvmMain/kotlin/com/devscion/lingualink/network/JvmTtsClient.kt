package com.devscion.lingualink.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
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
        if (apiKey.isBlank() || text.isBlank() || voiceModel.isBlank()) return null
        return try {
            httpClient.post("https://api.deepgram.com/v1/speak") {
                parameter("model", voiceModel)
                header("Authorization", "Token $apiKey")
                contentType(ContentType.Application.Json)
                setBody(SpeakRequest(text = text.take(2000)))
            }.body<ByteArray>()
        } catch (e: Exception) {
            println("TTS error (non-fatal): ${e.message}")
            null
        }
    }
}
