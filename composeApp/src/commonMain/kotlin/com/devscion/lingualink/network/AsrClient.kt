package com.devscion.lingualink.network

import kotlinx.coroutines.flow.Flow

data class TranscriptResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val speechFinal: Boolean
)

interface AsrClient {
    fun streamTranscription(
        audioChunks: Flow<ByteArray>,
        apiKey: String,
        languageCode: String
    ): Flow<TranscriptResult>
}
