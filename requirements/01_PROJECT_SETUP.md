# VoxFlow — Spec 01: Project Setup & Scaffold
> Read this for TASK 1 and TASK 2 only.

---

## TASK 1 — Gradle Project Scaffold

Create a Compose Multiplatform project targeting Desktop (JVM).
Single module for MVP — do not split into submodules.
Package name: `com.voxflow`

### build.gradle.kts (root)

```kotlin
plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("org.jetbrains.compose") version "1.6.11"
    id("app.cash.sqldelight") version "2.0.2"
}

group = "com.voxflow"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Koin DI
    implementation("io.insert-koin:koin-core:3.5.6")
    implementation("io.insert-koin:koin-compose:1.1.5")

    // Compose Navigation
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.7.0-alpha07")

    // Ktor Client
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-client-websockets:2.3.12")
    implementation("io.ktor:ktor-client-logging:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

    // SQLDelight
    implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
    implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.insert-koin:koin-test:3.5.6")
}

sqldelight {
    databases {
        create("VoxFlowDB") {
            packageName.set("com.voxflow.db")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.voxflow.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "VoxFlow"
            packageVersion = "1.0.0"
            description = "Real-time AI voice & chat translation"
        }
    }
}
```

### Source folder structure to create

```
src/
└── main/
    └── kotlin/
        └── com/voxflow/
            ├── main.kt
            ├── App.kt
            ├── di/
            │   └── AppModule.kt
            ├── navigation/
            │   └── NavGraph.kt
            ├── data/
            │   ├── db/
            │   │   └── VoxFlow.sq          ← SQLDelight schema
            │   ├── model/
            │   │   ├── ConversationMessage.kt
            │   │   ├── Session.kt
            │   │   └── Language.kt
            │   └── repository/
            │       ├── SessionRepository.kt
            │       └── MessageRepository.kt
            ├── audio/
            │   ├── AudioCapture.kt
            │   └── AudioPlayer.kt
            ├── network/
            │   ├── DeepgramClient.kt
            │   ├── LlmClient.kt
            │   └── TtsClient.kt
            ├── pipeline/
            │   └── TranslationPipeline.kt
            └── ui/
                ├── theme/
                │   ├── Theme.kt
                │   └── Type.kt
                ├── screens/
                │   ├── SetupScreen.kt
                │   ├── HomeScreen.kt
                │   ├── CallScreen.kt
                │   ├── ChatScreen.kt
                │   └── HistoryScreen.kt
                └── components/
                    ├── TranscriptPanel.kt
                    ├── MessageBubble.kt
                    ├── LanguagePicker.kt
                    ├── WaveformIndicator.kt
                    └── StatusBadge.kt
```

### main.kt

```kotlin
package com.voxflow

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import org.koin.core.context.startKoin
import com.voxflow.di.AppModule

fun main() = application {
    startKoin { modules(AppModule.all) }

    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "VoxFlow — Real-Time Translation",
        state = windowState
    ) {
        App()
    }
}
```

**Acceptance:** `./gradlew run` opens a blank window. No compilation errors.

---

## TASK 2 — Koin DI Module + Navigation Shell

### AppModule.kt

```kotlin
package com.voxflow.di

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import com.voxflow.network.*
import com.voxflow.audio.*
import com.voxflow.pipeline.*
import com.voxflow.data.repository.*

object AppModule {
    private val networkModule = module {
        single {
            HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true; isLenient = true })
                }
                install(WebSockets)
                install(Logging) { level = LogLevel.INFO }
            }
        }
        single { DeepgramClient(get()) }
        single { LlmClient(get()) }
        single { TtsClient(get()) }
    }

    private val audioModule = module {
        single { AudioCapture() }
        single { AudioPlayer() }
    }

    private val pipelineModule = module {
        single { TranslationPipeline(get(), get(), get(), get()) }
    }

    private val repositoryModule = module {
        // DatabaseFactory provided after TASK 3
        single { SessionRepository(get()) }
        single { MessageRepository(get()) }
    }

    val all = listOf(networkModule, audioModule, pipelineModule, repositoryModule)
}
```

### NavGraph.kt

Define these routes as a sealed class:

```kotlin
sealed class Screen(val route: String) {
    object Setup   : Screen("setup")    // First-run API config
    object Home    : Screen("home")     // Language picker + mode selector
    object Call    : Screen("call")     // Real-time voice translation
    object Chat    : Screen("chat")     // Text chat translation
    object History : Screen("history")  // Past sessions
}
```

NavHost in App.kt: start destination is `Setup` if config missing, else `Home`.
Config stored at: `~/.voxflow/config.json`
Config fields:
```json
{
  "deepgram_api_key": "",
  "amd_droplet_base_url": "",
  "elevenlabs_api_key": "",
  "source_language": "en",
  "target_language": "ur"
}
```

Read config on startup. If any key is empty string → navigate to Setup.
Use `kotlinx.serialization` to read/write JSON. File path via `System.getProperty("user.home")`.

**Acceptance:** App opens, navigates to Setup if config missing. Routes compile. Koin starts without errors.
