# LinguaLink

**Real-time AI voice & text translation — desktop app built with Kotlin Multiplatform and AMD-powered LLMs.**

LinguaLink breaks language barriers in live conversations. Speak into your microphone in any of 13 supported languages and hear the translated response within seconds — powered by a streaming ASR → LLM → TTS pipeline running entirely on the desktop with no browser required.

<a href="assets/demo.mp4">Watch the demo</a>

---

## Features

- **Live voice translation** — real-time microphone capture, streamed to Deepgram ASR, translated by an AMD-hosted LLM, then spoken back via Deepgram TTS
- **Text chat translation** — typed messages translated instantly with full conversational context
- **13 supported languages** — Arabic, Chinese, English, French, German, Hindi, Japanese, Korean, Portuguese, Russian, Spanish, Turkish, Urdu
- **Session history** — every call and chat is persisted locally with a full transcript and per-message TTS replay
- **Two-speaker mode** — USER_A / USER_B roles so two people can hold a live bilingual conversation on one device
- **Language swap** — switch source and target mid-session without losing context
- **Offline-first storage** — SQLite database stores sessions and messages locally via SQLDelight
- **Polished design system** — custom glass morphism tokens, animated orb visualizer, responsive compact/wide layouts

---

## How it works

The pipeline is a straight coroutine chain:

```
Microphone  (javax.sound.sampled)
     ↓  ByteArray chunks (16 kHz PCM, 100 ms each)
Deepgram ASR  (WebSocket streaming — nova-3 model)
     ↓  TranscriptResult  (interim + final)
Fireworks AI LLM  (OpenAI-compatible REST — Llama 3.1 8B, AMD-hosted)
     ↓  Translated text
Deepgram TTS  (REST — Aura-2 voices, WAV output)
     ↓  ByteArray audio
Speaker  (javax.sound.sampled)
```

### Voice call flow

1. User taps **Start Listening**.
2. `JvmAudioCapture` opens a `TargetDataLine` (16 kHz, 16-bit mono PCM) and emits 100 ms chunks.
3. `JvmDeepgramClient` streams those chunks over a WebSocket to Deepgram `nova-3`.
4. Interim transcript results update the orb subtitle in real time.
5. On `speech_final=true`, the full utterance goes to `JvmLlmClient`.
6. Llama 3.1 8B on Fireworks AI returns the translation.
7. `JvmTtsClient` sends the translation to Deepgram Aura-2 and receives a WAV response.
8. `JvmAudioPlayer` plays the WAV through the system speaker.
9. The message (original + translation) is persisted to SQLite and shown in the transcript.

### Text chat flow

User types a message → `TranslationPipeline.translateText()` skips ASR and goes straight to the LLM → translated text is synthesized and played → message saved and displayed.

---

## Tech stack

### Core

| Concern | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.3.20 |
| UI framework | Compose Multiplatform (Desktop) | 1.10.3 |
| Dependency injection | Koin | 4.2.1 |
| Async / concurrency | kotlinx-coroutines | 1.10.2 |
| Serialization | kotlinx-serialization-json | 1.7.3 |
| Navigation | Compose Navigation (typed routes) | 2.9.2 |
| Local database | SQLDelight + SQLite (JdbcSqliteDriver) | 2.0.2 |
| HTTP / WebSocket client | Ktor Client (OkHttp engine) | 3.1.3 |
| Audio capture & playback | javax.sound.sampled (JVM standard library) | — |
| Material Design | Material3 for Compose | 1.10.0-alpha05 |
| Lifecycle-aware ViewModels | AndroidX Lifecycle Compose | 2.10.0 |
| Build system | Gradle (Kotlin DSL) | — |

### External APIs

| Service | Purpose | Protocol |
|---|---|---|
| **Deepgram** (ASR) | Streaming speech-to-text | WebSocket — `wss://api.deepgram.com/v1/listen` |
| **Fireworks AI** (AMD) | LLM translation (Llama 3.1 8B) | REST — `POST /v1/chat/completions` |
| **Deepgram** (TTS) | Text-to-speech synthesis | REST — `POST /v1/speak` |

### AMD integration

LinguaLink uses **AMD's Fireworks AI** endpoint for all LLM inference. The default model is `accounts/fireworks/models/llama-v3p1-8b-instruct` — fast, multilingual, and cost-efficient (~$0.20/1M tokens). Any OpenAI-compatible model served on Fireworks (e.g. `llama-v3p1-70b-instruct`, `qwen2p5-72b-instruct`) can be swapped in from the in-app Setup screen without rebuilding.

---

## Architecture

### Source set split

| Source set | Contents |
|---|---|
| `commonMain` | Interfaces, ViewModels, `TranslationPipeline`, UI screens (Compose), navigation, Koin common module |
| `jvmMain` | JVM implementations: audio I/O, Ktor clients, SQLDelight DB, ConfigManager, Koin platform module, `main.kt` |

### Interface → JVM implementation map

| Interface (commonMain) | JVM implementation (jvmMain) |
|---|---|
| `AudioCapture` | `JvmAudioCapture` (TargetDataLine) |
| `AudioPlayer` | `JvmAudioPlayer` (SourceDataLine + WAV decode) |
| `AsrClient` | `JvmDeepgramClient` (Ktor WebSocket) |
| `LlmClient` | `JvmLlmClient` (Ktor HTTP, OpenAI-compatible) |
| `TtsClient` | `JvmTtsClient` (Ktor HTTP, Deepgram /v1/speak) |
| `SessionRepository` | `JvmSessionRepository` (SQLDelight) |
| `MessageRepository` | `JvmMessageRepository` (SQLDelight) |
| `ConfigManager` | `JvmConfigManager` (~/.lingualink/config.json) |

### Pipeline state machine

```
Idle → Listening → Transcribing(partial) → Translating(text) → Speaking(text) → Idle
                                                                       ↓
                                                                   Error(msg)
```

Exposed as `StateFlow<PipelineState>` — the UI observes it and updates the orb and status chips reactively.

---

## Project structure

```
LinguaLink/
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/com/devscion/lingualink/
│       │   ├── App.kt                        # Root Composable + NavHost
│       │   ├── audio/                        # AudioCapture, AudioPlayer interfaces
│       │   ├── data/
│       │   │   ├── config/ConfigManager.kt
│       │   │   ├── model/                    # ConversationMessage, Session, Language
│       │   │   └── repository/               # SessionRepository, MessageRepository
│       │   ├── di/AppModule.kt               # Koin commonModule
│       │   ├── navigation/Screen.kt          # Typed + string routes
│       │   ├── network/                      # AsrClient, LlmClient, TtsClient interfaces
│       │   ├── pipeline/
│       │   │   ├── TranslationPipeline.kt    # ASR → LLM → TTS orchestration
│       │   │   └── PipelineState.kt
│       │   └── ui/
│       │       ├── screens/                  # SetupScreen, HomeScreen, CallScreen,
│       │       │                             # ChatScreen, HistoryScreen,
│       │       │                             # SessionDetailsScreen, SettingsScreen
│       │       └── theme/                    # Design tokens, glass morphism, orb, transitions
│       │
│       └── jvmMain/kotlin/com/devscion/lingualink/
│           ├── main.kt                       # Koin init + Compose Desktop window
│           ├── audio/
│           │   ├── AudioConstants.kt         # PCM format (16 kHz, 16-bit mono)
│           │   ├── JvmAudioCapture.kt
│           │   └── JvmAudioPlayer.kt
│           ├── data/
│           │   ├── config/JvmConfigManager.kt
│           │   ├── db/DatabaseFactory.kt
│           │   └── repository/
│           ├── di/JvmModule.kt
│           └── network/
│               ├── JvmDeepgramClient.kt
│               ├── JvmLlmClient.kt
│               └── JvmTtsClient.kt
│
├── gradle/libs.versions.toml                 # Version catalog
├── requirements/                             # Original feature specifications
└── fireworks-ai-setup-instruction.md         # AMD Fireworks AI integration guide
```

---

## Prerequisites

### Tools

| Tool | Minimum version | Notes |
|---|---|---|
| JDK | 17 | JDK 17 or 21 recommended. [Temurin/Adoptium](https://adoptium.net/) or Zulu. |
| Gradle | 8.x | Bundled via Gradle Wrapper — no separate install needed. |
| IntelliJ IDEA | 2024.1+ | Community Edition works. Android Studio Meerkat+ also works. |
| Kotlin Multiplatform plugin | 0.8.4+ | *Settings → Plugins → search "Kotlin Multiplatform"* |

> **macOS:** Built-in JDK from Xcode toolchain is not sufficient — install a standalone JDK 17+.  
> **Linux:** Requires ALSA for audio I/O: `sudo apt install libasound2-dev`  
> **Windows:** JDK 17+ on Windows 10+ works out of the box.

### API keys

| Key | Where to get it |
|---|---|
| **Deepgram API key** | [console.deepgram.com](https://console.deepgram.com) — free tier available |
| **Fireworks AI API key** | [app.fireworks.ai](https://app.fireworks.ai) |

---

## Getting started

### 1. Clone the repo

```bash
git clone https://github.com/your-org/LinguaLink.git
cd LinguaLink
```

### 2. Verify your JDK

```bash
java -version
# Expected: openjdk version "17.x.x" or "21.x.x"
```

### 3. Open in IntelliJ IDEA

1. **File → Open** → select the `LinguaLink` folder.
2. Wait for Gradle sync to complete (first sync downloads ~500 MB of dependencies).
3. If prompted to install the **Kotlin Multiplatform** plugin, accept and restart.
4. Set the project SDK to JDK 17+: **File → Project Structure → SDK**.

### 4. Run the app

**Terminal (macOS / Linux):**
```bash
./gradlew :composeApp:run
```

**Terminal (Windows):**
```bat
.\gradlew.bat :composeApp:run
```

**IntelliJ IDEA:**  
Gradle tool window → `composeApp → Tasks → compose desktop → run` → double-click.

The app opens a 1280×800 window.

### 5. First-run setup

On first launch the **Setup screen** appears. Fill in:

| Field | Value |
|---|---|
| Fireworks AI Base URL | `https://api.fireworks.ai/inference` |
| Fireworks AI API Key | `fw_…` (your key) |
| LLM Model | `accounts/fireworks/models/llama-v3p1-8b-instruct` |
| Deepgram API Key | `dg_…` (your key) |

Settings are saved to `~/.lingualink/config.json` and reloaded automatically on the next launch. You can change them anytime via **Settings → Edit API Config**.

---

## Build commands

```bash
# Run the desktop app
./gradlew :composeApp:run

# Compile-check only (no window, faster feedback)
./gradlew :composeApp:compileKotlinJvm

# Regenerate SQLDelight DB interface after schema changes
./gradlew :composeApp:generateCommonMainLinguaLinkDBInterface

# Package a native installer for the current OS (DMG / MSI / DEB)
./gradlew :composeApp:packageDistributionForCurrentOS
```

---

## Supported languages

| Code | Language | Native name |
|---|---|---|
| `ar` | Arabic | العربية |
| `zh` | Chinese | 中文 |
| `en` | English | English |
| `fr` | French | Français |
| `de` | German | Deutsch |
| `hi` | Hindi | हिन्दी |
| `ja` | Japanese | 日本語 |
| `ko` | Korean | 한국어 |
| `pt` | Portuguese | Português |
| `ru` | Russian | Русский |
| `es` | Spanish | Español |
| `tr` | Turkish | Türkçe |
| `ur` | Urdu | اردو |

---

## Configuration reference

Config is persisted to `~/.lingualink/config.json`:

```json
{
  "llmBaseUrl":      "https://api.fireworks.ai/inference",
  "llmApiKey":       "fw_...",
  "llmModel":        "accounts/fireworks/models/llama-v3p1-8b-instruct",
  "deepgramApiKey":  "dg_...",
  "sourceLanguage":  "en",
  "targetLanguage":  "ur"
}
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| App returns to Setup screen on every launch | Ensure all four fields (Base URL, LLM API Key, Model, Deepgram Key) are saved and non-blank. |
| No transcripts during a call | Check mic permissions. macOS: *System Settings → Privacy & Security → Microphone*. |
| TTS audio not playing | Confirm Deepgram API key is valid and the system default audio output device is set. |
| Gradle sync fails | Ensure JDK 17+ is set as the project SDK (*File → Project Structure → SDK*). |
| Audio error on Linux | `sudo apt install libasound2-dev` |

---

## License

MIT — see [LICENSE](LICENSE).

---

## Built with

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)
- [Deepgram](https://deepgram.com) — streaming ASR and TTS
- [Fireworks AI](https://fireworks.ai) — AMD-hosted LLM inference
- [Ktor](https://ktor.io) — HTTP and WebSocket client
- [SQLDelight](https://cashapp.github.io/sqldelight/) — type-safe SQLite
- [Koin](https://insert-koin.io) — dependency injection

---

*Built at the AMD AI Hackathon.*
