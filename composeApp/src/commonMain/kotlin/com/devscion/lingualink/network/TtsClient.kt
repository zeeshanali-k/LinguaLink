package com.devscion.lingualink.network

import com.devscion.lingualink.data.model.Speaker

interface TtsClient {
    fun configure(apiKey: String)
    fun isConfigured(): Boolean
    suspend fun synthesize(text: String, voiceModel: String): ByteArray?
}

/**
 * Returns the Deepgram Aura-2 model name for a given speaker + target language.
 * Languages without a native Aura-2 voice (ar, zh, hi, ko, pt, ru, tr, ur) fall
 * back to an English voice so TTS always plays rather than being silently skipped.
 *
 * Source: https://developers.deepgram.com/docs/tts-models (aura-2 family).
 */
fun deepgramVoiceFor(speaker: Speaker, targetLang: String): String {
    val isUserA = speaker == Speaker.USER_A
    return when (targetLang) {
        "en" -> if (isUserA) "aura-2-apollo-en" else "aura-2-thalia-en"
        "es" -> if (isUserA) "aura-2-nestor-es" else "aura-2-celeste-es"
        "fr" -> if (isUserA) "aura-2-hector-fr" else "aura-2-agathe-fr"
        "de" -> if (isUserA) "aura-2-julius-de" else "aura-2-viktoria-de"
        "it" -> if (isUserA) "aura-2-dionisio-it" else "aura-2-livia-it"
        "ja" -> if (isUserA) "aura-2-fujin-ja" else "aura-2-uzume-ja"
        "nl" -> if (isUserA) "aura-2-sander-nl" else "aura-2-beatrix-nl"
        // No native Aura-2 voice for ar, zh, hi, ko, pt, ru, tr, ur —
        // fall back to English so audio always plays.
        else -> if (isUserA) "aura-2-apollo-en" else "aura-2-thalia-en"
    }
}
