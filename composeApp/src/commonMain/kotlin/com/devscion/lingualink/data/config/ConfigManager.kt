package com.devscion.lingualink.data.config

import kotlinx.serialization.Serializable

interface ConfigManager {

    @Serializable
    data class AppConfig(
        val amdDropletBaseUrl: String = "",
        val deepgramApiKey: String = "",
        val elevenlabsApiKey: String = "",
        val sourceLanguage: String = "en",
        val targetLanguage: String = "ur"
    )

    fun load(): AppConfig?
    fun save(config: AppConfig)
    fun isConfigured(): Boolean
}
