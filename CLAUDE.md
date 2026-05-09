# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LinguaLink is a **Kotlin Multiplatform real-time voice & text translation desktop application**. The primary target is JVM Desktop (Compose for Desktop). Future web/mobile targets are supported via the interface architecture.

## Build & Run Commands

```bash
# Run desktop application (primary target)
./gradlew :composeApp:run

# Compile check (JVM) without running
./gradlew :composeApp:compileKotlinJvm

# Generate SQLDelight DB interface
./gradlew :composeApp:generateCommonMainLinguaLinkDBInterface

# Package desktop app (creates Dmg/Msi/Deb installers)
./gradlew :composeApp:packageDistributionForCurrentOS
```

## Architecture

### Module Structure

| Module | Purpose |
|--------|---------|
| `composeApp/` | Desktop app (JVM primary) |

### Source Set Split

| Source Set | Contents |
|------------|----------|
| `commonMain` | Interfaces, models, pipeline, ViewModels, UI screens (Compose), navigation, DI common module |
| `jvmMain` | JVM implementations: AudioCapture/Player, Ktor clients, SQLDelight DB, ConfigManager, Koin platform module, main.kt |

### Key Interfaces (commonMain) → JVM Implementations (jvmMain)

| Interface | JVM Implementation |
|-----------|-------------------|
| `AudioCapture` | `JvmAudioCapture` (javax.sound.sampled) |
| `AudioPlayer` | `JvmAudioPlayer` (javax.sound.sampled) |
| `AsrClient` | `JvmDeepgramClient` (Ktor WebSocket) |
| `LlmClient` | `JvmLlmClient` (Ktor HTTP, OpenAI-compatible) |
| `TtsClient` | `JvmTtsClient` (Ktor HTTP, Deepgram `/v1/speak` aura-2 voices) |
| `SessionRepository` | `JvmSessionRepository` (SQLDelight) |
| `MessageRepository` | `JvmMessageRepository` (SQLDelight) |
| `ConfigManager` | `JvmConfigManager` (java.io.File → ~/.lingualink/config.json) |

### Dependency Flow

```
main.kt → startKoin(commonModule + jvmPlatformModule) → App()
App() → NavHost → Screens → koinViewModel<VM>()
TranslationPipeline: AsrClient → LlmClient → TtsClient → AudioPlayer
```

### Navigation (Typed Routes — Navigation 2.9.2)

- `Screen.Setup.route` — first-run API config
- `Screen.Home.route` — language picker + mode selector
- `CallRoute(sessionId, sourceLang, targetLang)` — voice translation
- `ChatRoute(sessionId, sourceLang, targetLang)` — text chat translation
- `Screen.History.route` — past sessions browser

### Config

Config persists to `~/.lingualink/config.json` via `JvmConfigManager`.
Fields: `llmBaseUrl`, `llmApiKey`, `llmModel`, `deepgramApiKey`, `sourceLanguage`, `targetLanguage`.
LLM defaults target the AMD-hosted **Fireworks AI** endpoint (`https://api.fireworks.ai/inference`, model `accounts/fireworks/models/llama-v3p1-8b-instruct`).
The single `deepgramApiKey` powers both ASR (`/v1/listen`) and TTS (`/v1/speak`).
App navigates to SetupScreen if `llmBaseUrl`, `llmApiKey`, `llmModel`, or `deepgramApiKey` is blank.
On startup, `App.kt` re-applies saved config to `LlmClient` and `TtsClient` via a `LaunchedEffect`.

### Database

SQLDelight schema at `src/jvmMain/sqldelight/com/devscion/lingualink/db/LinguaLink.sq`.
DB file created at `~/.lingualink/lingualink.db` on first run.

### External Service Integrations

| Service | Client | Protocol |
|---------|--------|----------|
| Deepgram (ASR) | `JvmDeepgramClient` | WebSocket streaming (wss://api.deepgram.com/v1/listen) |
| Fireworks AI (LLM translation, AMD-hosted) | `JvmLlmClient` | OpenAI-compatible REST POST /v1/chat/completions with `Authorization: Bearer` |
| Deepgram (TTS) | `JvmTtsClient` | REST POST /v1/speak?model=aura-2-* with `Authorization: Token` (returns MP3) |

## Key Technology Choices

- **UI:** Jetbrains Compose Multiplatform with Material3
- **DI:** Koin 4.2.1
- **Networking:** Ktor Client 3.1.3 with OkHttp engine + WebSocket support
- **Database:** SQLDelight 2.0.2 (JVM JdbcSqliteDriver)
- **Audio:** `javax.sound.sampled` (standard JVM library)
- **Serialization:** `kotlinx.serialization`
- **Coroutines:** `kotlinx.coroutines` 1.10.2
- **Navigation:** `org.jetbrains.androidx.navigation:navigation-compose` 2.9.2 (typed routes)

## Gradle Notes

- JVM memory: 3 GB heap for both Kotlin daemon and Gradle (set in `gradle.properties`)
- Version catalog: `gradle/libs.versions.toml`
- Kotlin version: 2.3.20, Compose Multiplatform: 1.10.3
- Only `jvm()` target active; web/mobile targets to be added later with platform-specific implementations
