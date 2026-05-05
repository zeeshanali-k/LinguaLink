package com.devscion.lingualink.data.config

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class JvmConfigManager : ConfigManager {

    private val configFile = File(System.getProperty("user.home"), ".lingualink/config.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override fun load(): ConfigManager.AppConfig? = try {
        if (configFile.exists()) json.decodeFromString(configFile.readText()) else null
    } catch (e: Exception) { null }

    override fun save(config: ConfigManager.AppConfig) {
        configFile.parentFile.mkdirs()
        configFile.writeText(json.encodeToString(config))
    }

    override fun isConfigured(): Boolean {
        val cfg = load() ?: return false
        return cfg.amdDropletBaseUrl.isNotBlank() && cfg.deepgramApiKey.isNotBlank()
    }
}
