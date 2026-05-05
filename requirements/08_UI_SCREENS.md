# VoxFlow — Spec 08: UI Theme + All Screens
> Read this for TASK 10 through TASK 16.

---

## TASK 10 — Theme

### Design Tokens
| Token | Value | Usage |
|---|---|---|
| Background | `#0A0C14` | App background |
| Surface | `#12151F` | Cards, panels |
| SurfaceVariant | `#1A1E2E` | Input fields, secondary panels |
| Primary | `#4F8EF7` | Buttons, active states |
| Accent | `#7C4DFF` | Speaking / active mic indicator |
| UserA | `#4F8EF7` | User A message bubble |
| UserB | `#1DB97F` | User B message bubble |
| Error | `#FF4D4D` | Error state |
| Success | `#1DB97F` | Connected / OK state |
| TextPrimary | `#F0F2FF` | Main text |
| TextSecondary | `#8A8FA8` | Labels, subtitles |
| TranscriptFont | JetBrains Mono 14sp | Transcript text |
| UIFont | Roboto | All other UI |

### ui/theme/Theme.kt

```kotlin
package com.voxflow.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object VoxFlowColors {
    val Background     = Color(0xFF0A0C14)
    val Surface        = Color(0xFF12151F)
    val SurfaceVariant = Color(0xFF1A1E2E)
    val Primary        = Color(0xFF4F8EF7)
    val PrimaryVariant = Color(0xFF2D5FC4)
    val Accent         = Color(0xFF7C4DFF)
    val UserA          = Color(0xFF4F8EF7)
    val UserB          = Color(0xFF1DB97F)
    val Error          = Color(0xFFFF4D4D)
    val Success        = Color(0xFF1DB97F)
    val TextPrimary    = Color(0xFFF0F2FF)
    val TextSecondary  = Color(0xFF8A8FA8)
    val Divider        = Color(0xFF252A3D)
}

private val VoxFlowColorScheme = darkColorScheme(
    primary          = VoxFlowColors.Primary,
    onPrimary        = Color.White,
    background       = VoxFlowColors.Background,
    onBackground     = VoxFlowColors.TextPrimary,
    surface          = VoxFlowColors.Surface,
    onSurface        = VoxFlowColors.TextPrimary,
    surfaceVariant   = VoxFlowColors.SurfaceVariant,
    onSurfaceVariant = VoxFlowColors.TextSecondary,
    error            = VoxFlowColors.Error,
    onError          = Color.White
)

@Composable
fun VoxFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoxFlowColorScheme,
        typography = VoxFlowTypography,
        content = content
    )
}
```

### ui/theme/Type.kt

```kotlin
package com.voxflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TranscriptFontFamily = FontFamily.Monospace  // Falls back to system monospace

val VoxFlowTypography = Typography(
    headlineLarge  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 16.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 16.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 12.sp)
)
```

**Acceptance:** App renders with dark background `#0A0C14`. No white flash on startup.

---

## TASK 11 — SetupScreen

First-run config screen. Shown when `~/.voxflow/config.json` is missing or any required key is blank.

```kotlin
// ui/screens/SetupScreen.kt
package com.voxflow.ui.screens

// Layout described below — implement as a single Column composable
// centered horizontally, max width 560dp
```

### Layout
```
┌─────────────────────────────────────────────┐
│              🎙  VoxFlow                     │
│       Real-time AI Voice Translation         │
│                                             │
│  AMD Droplet Base URL *                     │
│  ┌─────────────────────────────────────┐   │
│  │ http://192.168.x.x:8000             │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  Deepgram API Key *                         │
│  ┌─────────────────────────────────────┐   │
│  │ ●●●●●●●●●●●●●●●●●●●●●●●●           │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ElevenLabs API Key  (optional — TTS)       │
│  ┌─────────────────────────────────────┐   │
│  │ Leave blank to disable voice output │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  [ Test Connection ]   [ Save & Continue ]  │
│                                             │
│  ✓ Connected to droplet                     │
└─────────────────────────────────────────────┘
```

### Behavior
- "Test Connection": calls `LlmClient.chat("ping", "Reply with the single word: pong")`. Shows ✓ green "Connected" or ✗ red error message below the button.
- "Save & Continue": writes config, calls `LlmClient.configure()` and `TtsClient.configure()`, navigates to `Screen.Home`.
- Disabled until AMD URL and Deepgram key are non-empty.
- Password visual for API key fields (`visualTransformation = PasswordVisualTransformation()`).
- No back navigation from this screen.

### ViewModel: SetupViewModel
```kotlin
class SetupViewModel(
    private val llmClient: LlmClient,
    private val ttsClient: TtsClient
) : ViewModel() {
    var dropletUrl   by mutableStateOf("")
    var deepgramKey  by mutableStateOf("")
    var elevenLabsKey by mutableStateOf("")
    var testStatus   by mutableStateOf<String?>(null)  // null | "ok" | "error: ..."
    var isTesting    by mutableStateOf(false)
    var isSaving     by mutableStateOf(false)

    val canSave get() = dropletUrl.isNotBlank() && deepgramKey.isNotBlank()

    fun testConnection(scope: CoroutineScope) {
        isTesting = true
        testStatus = null
        scope.launch {
            try {
                llmClient.configure(dropletUrl)
                val response = llmClient.chat("ping", "Reply with the single word: pong")
                testStatus = if (response.isNotBlank()) "ok" else "error: empty response"
            } catch (e: Exception) {
                testStatus = "error: ${e.message}"
            } finally {
                isTesting = false
            }
        }
    }

    fun save(scope: CoroutineScope, onSuccess: () -> Unit) {
        isSaving = true
        scope.launch {
            val config = AppConfig(dropletUrl, deepgramKey, elevenLabsKey)
            ConfigManager.save(config)
            llmClient.configure(dropletUrl)
            ttsClient.configure(elevenLabsKey)
            isSaving = false
            onSuccess()
        }
    }
}
```

### ConfigManager (create in di/ or data/)
```kotlin
object ConfigManager {
    private val configFile = File(
        System.getProperty("user.home"), ".voxflow/config.json"
    )

    @Serializable
    data class AppConfig(
        val amd_droplet_base_url: String = "",
        val deepgram_api_key: String = "",
        val elevenlabs_api_key: String = "",
        val source_language: String = "en",
        val target_language: String = "ur"
    )

    fun load(): AppConfig? = try {
        if (configFile.exists())
            Json.decodeFromString(configFile.readText())
        else null
    } catch (e: Exception) { null }

    fun save(config: AppConfig) {
        configFile.parentFile.mkdirs()
        configFile.writeText(Json.encodeToString(config))
    }

    fun isConfigured(): Boolean {
        val cfg = load() ?: return false
        return cfg.amd_droplet_base_url.isNotBlank() && cfg.deepgram_api_key.isNotBlank()
    }
}
```

**Acceptance:** Config saves to `~/.voxflow/config.json`. Test connection shows ✓ or error. App navigates to HomeScreen after save.

---

## TASK 12 — HomeScreen

Language selection and mode entry point.

```kotlin
// ui/screens/HomeScreen.kt
```

### Layout
```
┌──────────────────────────────────────────────────┐
│  VoxFlow                          [ ⚙ Settings ] │
│──────────────────────────────────────────────────│
│                                                   │
│            TRANSLATION DIRECTION                  │
│                                                   │
│  ┌──────────────┐   [ ⇄ ]   ┌──────────────┐    │
│  │   English ▼  │           │    Urdu  ▼   │    │
│  └──────────────┘           └──────────────┘    │
│                                                   │
│  ┌─────────────────────────────────────────────┐ │
│  │  🎙  Voice Call Translation                  │ │
│  │  Real-time mic → translated speech           │ │
│  │                              [ Start Call ]  │ │
│  └─────────────────────────────────────────────┘ │
│                                                   │
│  ┌─────────────────────────────────────────────┐ │
│  │  💬  Chat Translation                        │ │
│  │  Type messages, get instant translation      │ │
│  │                              [ Open Chat  ]  │ │
│  └─────────────────────────────────────────────┘ │
│                                                   │
│                         [ 📋 View History ]       │
└──────────────────────────────────────────────────┘
```

### Behavior
- Language dropdowns: show `Language.name` as label, store `Language.code`. Use `SupportedLanguages` list.
- Source and target must be different — show `"Source and target language must differ"` snackbar if equal.
- ⇄ button: swaps source ↔ target immediately, saves to config.
- "Start Call" → `sessionRepo.createSession(CALL, sourceLang, targetLang)` → navigate to `Screen.Call(sessionId, sourceLang, targetLang)`.
- "Open Chat" → same but `CHAT` → navigate to `Screen.Chat(...)`.
- ⚙ Settings → navigate to `Screen.Setup`.
- Language selections persist to `ConfigManager` on change.

### HomeViewModel
```kotlin
class HomeViewModel(
    private val sessionRepo: SessionRepository,
    private val pipeline: TranslationPipeline
) : ViewModel() {
    var sourceLang by mutableStateOf("en")
    var targetLang by mutableStateOf("ur")

    fun swapLanguages() {
        val tmp = sourceLang; sourceLang = targetLang; targetLang = tmp
        pipeline.swapLanguages()
        persistLanguages()
    }

    fun createCallSession(scope: CoroutineScope, onCreated: (Long) -> Unit) {
        scope.launch {
            val id = sessionRepo.createSession(SessionType.CALL, sourceLang, targetLang)
            onCreated(id)
        }
    }

    fun createChatSession(scope: CoroutineScope, onCreated: (Long) -> Unit) {
        scope.launch {
            val id = sessionRepo.createSession(SessionType.CHAT, sourceLang, targetLang)
            onCreated(id)
        }
    }

    private fun persistLanguages() {
        val cfg = ConfigManager.load() ?: return
        ConfigManager.save(cfg.copy(source_language = sourceLang, target_language = targetLang))
    }
}
```

**Acceptance:** Language dropdowns work. Swap button reverses languages. Both mode buttons create a DB session and navigate correctly.

---

## TASK 13 — CallScreen

Real-time voice translation — the core feature.

```kotlin
// ui/screens/CallScreen.kt
// Receives: sessionId: Long, sourceLang: String, targetLang: String
```

### Layout
```
┌────────────────────────────────────────────────────────────┐
│ ←  🎙 Voice Translation      [ EN → UR ]     [ ⇄ Swap ]  │
│────────────────────────────────────────────────────────────│
│                                                             │
│  ┌────────── YOU (EN) ──────────┐ ┌──── TRANSLATION (UR) ─┐│
│  │                              │ │                        ││
│  │   ░░░░░░░░░░▓▓▓▓▓▓░░░░░░    │ │  آپ کیسے ہیں؟         ││
│  │   [waveform indicator]       │ │                        ││
│  │                              │ │  How are you? (orig)   ││
│  │  "How are you doing today?"  │ │  [confidence: 98%]     ││
│  │   [live partial transcript]  │ │                        ││
│  │                              │ │  🔊 Playing...         ││
│  │                              │ │                        ││
│  └──────────────────────────────┘ └────────────────────────┘│
│                                                             │
│  ┌──────────────── CONVERSATION HISTORY ──────────────────┐ │
│  │  [YOU]    Hello, nice to meet you.                     │ │
│  │  [TRANS]  ہیلو، آپ سے مل کر خوشی ہوئی۔               │ │
│  │  [YOU]    How are you doing today?                     │ │
│  │  [TRANS]  آپ آج کیسے ہیں؟                              │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│      [ 🎙 HOLD TO SPEAK ]     [ ⏹ End Session ]            │
└────────────────────────────────────────────────────────────┘
```

### Key Components

**WaveformIndicator** — animated bars showing mic input level:
```kotlin
// ui/components/WaveformIndicator.kt
@Composable
fun WaveformIndicator(level: Float, isActive: Boolean) {
    // Draw 20 vertical bars using Canvas
    // Bar height = level * maxHeight + small random jitter for natural look
    // Bar color: Primary when active, TextSecondary when idle
    // Animate with animateFloatAsState for smooth transitions
    Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
        val barCount = 20
        val barWidth = size.width / (barCount * 2f)
        val maxBarHeight = size.height
        repeat(barCount) { i ->
            val jitter = if (isActive) (Math.random() * 0.3).toFloat() else 0f
            val barHeight = if (isActive) (level + jitter).coerceIn(0.05f, 1f) * maxBarHeight
                           else maxBarHeight * 0.05f
            val x = i * barWidth * 2f + barWidth / 2
            val color = if (isActive) VoxFlowColors.Primary else VoxFlowColors.TextSecondary
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - barHeight) / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}
```

**StatusBadge** — shows current pipeline state:
```kotlin
// ui/components/StatusBadge.kt
@Composable
fun StatusBadge(state: PipelineState) {
    val (text, color) = when (state) {
        is PipelineState.Idle        -> "Ready" to VoxFlowColors.TextSecondary
        is PipelineState.Listening   -> "Listening..." to VoxFlowColors.Success
        is PipelineState.Transcribing -> "Transcribing..." to VoxFlowColors.Primary
        is PipelineState.Translating -> "Translating..." to VoxFlowColors.Accent
        is PipelineState.Speaking    -> "Speaking..." to VoxFlowColors.Accent
        is PipelineState.Error       -> "Error" to VoxFlowColors.Error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}
```

### CallViewModel
```kotlin
class CallViewModel(
    private val pipeline: TranslationPipeline,
    private val audioCapture: AudioCapture,
    private val messageRepo: MessageRepository,
    private val sessionRepo: SessionRepository
) : ViewModel() {

    val pipelineState = pipeline.state
    val audioLevel    = pipeline.audioLevel
    val messages      = mutableStateListOf<ConversationMessage>()

    private var sessionId = -1L
    private var sessionStartTime = 0L

    fun initialize(
        sessionId: Long,
        sourceLang: String,
        targetLang: String,
        config: ConfigManager.AppConfig,
        scope: CoroutineScope
    ) {
        this.sessionId = sessionId
        sessionStartTime = System.currentTimeMillis()

        pipeline.configure(
            sessionId = sessionId,
            sourceLang = sourceLang,
            targetLang = targetLang,
            deepgramApiKey = config.deepgram_api_key
        )

        // Load existing messages for this session
        scope.launch {
            messages.addAll(messageRepo.getMessagesBySession(sessionId))
        }

        // Collect new messages from pipeline and persist
        scope.launch {
            pipeline.messages.collect { msg ->
                messageRepo.insertMessage(msg)
                messages.add(msg)
            }
        }
    }

    fun startListening() {
        pipeline.startVoiceTranslation(audioCapture)
    }

    fun stopListening() {
        pipeline.stopVoiceTranslation(audioCapture)
    }

    fun swapLanguages() {
        pipeline.swapLanguages()
    }

    fun endSession(scope: CoroutineScope, onEnded: () -> Unit) {
        stopListening()
        val durationSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000
        scope.launch {
            sessionRepo.closeSession(sessionId, durationSeconds)
            onEnded()
        }
    }
}
```

### Push-to-Talk behavior
- "HOLD TO SPEAK" button: `onPointerEvent(PointerEventType.Press) { startListening() }` + `onPointerEvent(PointerEventType.Release) { stopListening() }`
- Alternative: Toggle mode — first tap starts, second tap stops. Add a `var isListening by remember` toggle. Both modes should be attempted; pick whichever compiles cleanly on desktop Compose.

**Acceptance:** App opens CallScreen, mic activates, partial transcript appears live in left panel. Final transcript triggers translation that appears in right panel. History list updates. End Session closes session in DB and navigates back.

---

## TASK 14 — ChatScreen

Text-based translation. Same pipeline, keyboard input instead of mic.

```kotlin
// ui/screens/ChatScreen.kt
// Receives: sessionId: Long, sourceLang: String, targetLang: String
```

### Layout
```
┌────────────────────────────────────────────────────────────┐
│ ←  💬 Chat Translation        [ EN → UR ]    [ ⇄ Swap ]  │
│────────────────────────────────────────────────────────────│
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [USER A] Hello, how are you?              [EN] 14:32 │  │
│  │          ہیلو، آپ کیسے ہیں؟              [UR]       │  │
│  │                                                      │  │
│  │ [USER B] I am doing well, thank you!      [EN] 14:33 │  │
│  │          میں ٹھیک ہوں، شکریہ!             [UR]       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  Currently translating:  EN → UR                           │
│  ┌──────────────────────────────────────┐ [ Send / Enter ] │
│  │  Type your message here...           │                  │
│  └──────────────────────────────────────┘                  │
│                              [ ⏹ End Session ]             │
└────────────────────────────────────────────────────────────┘
```

### MessageBubble Component
```kotlin
// ui/components/MessageBubble.kt
@Composable
fun MessageBubble(message: ConversationMessage) {
    val isUserA = message.speaker == Speaker.USER_A
    val alignment = if (isUserA) Alignment.Start else Alignment.End
    val bubbleColor = if (isUserA) VoxFlowColors.UserA.copy(alpha = 0.15f)
                      else VoxFlowColors.UserB.copy(alpha = 0.15f)
    val accentColor = if (isUserA) VoxFlowColors.UserA else VoxFlowColors.UserB

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Original text
                Text(
                    text = message.originalText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VoxFlowColors.TextPrimary,
                    fontFamily = TranscriptFontFamily
                )
                Spacer(Modifier.height(4.dp))
                Divider(color = accentColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                Spacer(Modifier.height(4.dp))
                // Translated text
                Text(
                    text = message.translatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    fontFamily = TranscriptFontFamily
                )
                // Confidence (voice only)
                message.confidence?.let { conf ->
                    Text(
                        text = "${(conf * 100).toInt()}% confidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoxFlowColors.TextSecondary
                    )
                }
            }
        }
    }
}
```

### ChatViewModel
```kotlin
class ChatViewModel(
    private val pipeline: TranslationPipeline,
    private val messageRepo: MessageRepository,
    private val sessionRepo: SessionRepository
) : ViewModel() {

    val pipelineState = pipeline.state
    val messages = mutableStateListOf<ConversationMessage>()
    var inputText by mutableStateOf("")
    var isTranslating by mutableStateOf(false)

    private var sessionId = -1L
    private var sessionStartTime = 0L

    fun initialize(sessionId: Long, sourceLang: String, targetLang: String, config: ConfigManager.AppConfig, scope: CoroutineScope) {
        this.sessionId = sessionId
        sessionStartTime = System.currentTimeMillis()
        pipeline.configure(sessionId, sourceLang, targetLang, config.deepgram_api_key)
        scope.launch { messages.addAll(messageRepo.getMessagesBySession(sessionId)) }
        scope.launch {
            pipeline.messages.collect { msg ->
                messageRepo.insertMessage(msg)
                messages.add(msg)
                isTranslating = false
            }
        }
    }

    fun sendMessage() {
        if (inputText.isBlank()) return
        isTranslating = true
        pipeline.translateText(inputText)
        inputText = ""
    }

    fun swapLanguages() = pipeline.swapLanguages()

    fun endSession(scope: CoroutineScope, onEnded: () -> Unit) {
        val duration = (System.currentTimeMillis() - sessionStartTime) / 1000
        scope.launch { sessionRepo.closeSession(sessionId, duration); onEnded() }
    }
}
```

**Send behavior:** Enter key in text field also triggers `sendMessage()`. While `isTranslating=true`, show a subtle loading indicator next to the input field and disable the send button.

**Acceptance:** Type a message → press Send → original and translated text appear as a bubble. Enter key works. Swap button reverses direction. Bubbles scroll automatically to latest message.

---

## TASK 15 — HistoryScreen

Past sessions browser.

```kotlin
// ui/screens/HistoryScreen.kt
```

### Layout
```
┌────────────────────────────────────────────────────────────┐
│ ←  📋 Session History                                      │
│────────────────────────────────────────────────────────────│
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 🎙 Voice Call  EN → UR    May 3, 2026  4m 32s  [↗][🗑]│  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ 💬 Chat        EN → AR    May 2, 2026  2m 18s  [↗][🗑]│  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ 🎙 Voice Call  FR → EN    May 1, 2026  8m 05s  [↗][🗑]│  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  (empty state: "No sessions yet. Start a call or chat.")    │
└────────────────────────────────────────────────────────────┘
```

### Behavior
- Load all sessions via `SessionRepository.getAllSessions()` on screen open.
- ↗ "Re-open": navigates to the appropriate screen (CallScreen or ChatScreen) with the existing `sessionId`. The screen loads history from DB without re-running the pipeline.
- 🗑 "Delete": shows a confirmation dialog, then `sessionRepo.deleteSession(id)` and removes from list.
- Sessions ordered newest first.
- Row shows: type icon, language pair (e.g. EN → UR), formatted date, duration if available.

**Acceptance:** All past sessions listed. Delete removes from list and DB. Re-open navigates and loads message history.

---

## TASK 16 — App.kt + NavGraph Wiring

Wire all screens together with Compose Navigation.

```kotlin
// App.kt
@Composable
fun App() {
    VoxFlowTheme {
        val navController = rememberNavController()
        val startDest = if (ConfigManager.isConfigured()) Screen.Home.route else Screen.Setup.route

        NavHost(navController = navController, startDestination = startDest) {
            composable(Screen.Setup.route) {
                val vm: SetupViewModel = koinViewModel()
                SetupScreen(vm = vm, onSaved = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Home.route) {
                val vm: HomeViewModel = koinViewModel()
                HomeScreen(
                    vm = vm,
                    onStartCall  = { id, src, tgt -> navController.navigate("call/$id/$src/$tgt") },
                    onOpenChat   = { id, src, tgt -> navController.navigate("chat/$id/$src/$tgt") },
                    onHistory    = { navController.navigate(Screen.History.route) },
                    onSettings   = { navController.navigate(Screen.Setup.route) }
                )
            }

            composable("call/{sessionId}/{sourceLang}/{targetLang}") { backStack ->
                val vm: CallViewModel = koinViewModel()
                CallScreen(
                    sessionId  = backStack.arguments?.getString("sessionId")?.toLong() ?: -1L,
                    sourceLang = backStack.arguments?.getString("sourceLang") ?: "en",
                    targetLang = backStack.arguments?.getString("targetLang") ?: "ur",
                    vm = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("chat/{sessionId}/{sourceLang}/{targetLang}") { backStack ->
                val vm: ChatViewModel = koinViewModel()
                ChatScreen(
                    sessionId  = backStack.arguments?.getString("sessionId")?.toLong() ?: -1L,
                    sourceLang = backStack.arguments?.getString("sourceLang") ?: "en",
                    targetLang = backStack.arguments?.getString("targetLang") ?: "ur",
                    vm = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                val sessionRepo: SessionRepository = koinInject()
                val messageRepo: MessageRepository = koinInject()
                HistoryScreen(
                    sessionRepo = sessionRepo,
                    messageRepo = messageRepo,
                    onReopen    = { id, type, src, tgt ->
                        val route = if (type == SessionType.CALL) "call/$id/$src/$tgt" else "chat/$id/$src/$tgt"
                        navController.navigate(route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
```

Add all ViewModels to Koin `AppModule`:
```kotlin
viewModel { SetupViewModel(get(), get()) }
viewModel { HomeViewModel(get(), get()) }
viewModel { CallViewModel(get(), get(), get(), get()) }
viewModel { ChatViewModel(get(), get(), get()) }
```

**Acceptance:** Full navigation works — Home → Call → Back → Home → Chat → Back → History → Re-open → Back. No crashes on any route.
