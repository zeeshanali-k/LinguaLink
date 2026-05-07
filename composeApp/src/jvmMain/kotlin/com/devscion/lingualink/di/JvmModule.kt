package com.devscion.lingualink.di

import com.devscion.lingualink.audio.AudioCapture
import com.devscion.lingualink.audio.AudioPlayer
import com.devscion.lingualink.audio.JvmAudioCapture
import com.devscion.lingualink.audio.JvmAudioPlayer
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.data.config.JvmConfigManager
import com.devscion.lingualink.data.db.DatabaseFactory
import com.devscion.lingualink.data.repository.JvmMessageRepository
import com.devscion.lingualink.data.repository.JvmSessionRepository
import com.devscion.lingualink.data.repository.MessageRepository
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.network.AsrClient
import com.devscion.lingualink.network.JvmDeepgramClient
import com.devscion.lingualink.network.JvmLlmClient
import com.devscion.lingualink.network.JvmTtsClient
import com.devscion.lingualink.network.LlmClient
import com.devscion.lingualink.network.TtsClient
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val jvmPlatformModule = module {
    single<ConfigManager> { JvmConfigManager() }

    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(WebSockets)
            install(Logging) { level = LogLevel.ALL }
        }
    }

    single<AsrClient> { JvmDeepgramClient(get()) }
    single<LlmClient> { JvmLlmClient(get()) }
    single<TtsClient> { JvmTtsClient(get()) }

    single<AudioCapture> { JvmAudioCapture() }
    single<AudioPlayer> { JvmAudioPlayer() }

    single { DatabaseFactory.create() }
    single<SessionRepository> { JvmSessionRepository(get()) }
    single<MessageRepository> { JvmMessageRepository(get()) }
}
