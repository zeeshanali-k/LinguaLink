package com.devscion.lingualink.data.config

import kotlinx.serialization.Serializable

interface ConfigManager {

    @Serializable
    data class AppConfig(
        val llmBaseUrl: String = "https://api.fireworks.ai/inference",
        val llmApiKey: String = "",
        val llmModel: String = "accounts/fireworks/models/llama-v3p1-8b-instruct",
        val deepgramApiKey: String = "",
        val sourceLanguage: String = "en",
        val targetLanguage: String = "ur"
    )

    fun load(): AppConfig?
    fun save(config: AppConfig)
    fun isConfigured(): Boolean
}
