package com.devscion.lingualink.network

interface TtsClient {
    fun configure(apiKey: String)
    fun isConfigured(): Boolean
    suspend fun synthesize(text: String, voiceId: String = VOICE_USER_A): ByteArray?

    companion object {
        const val VOICE_USER_A = "pNInz6obpgDQGcFmaJgB"
        const val VOICE_USER_B = "ErXwobaYiN019PkySvjV"
    }
}
