# VoxFlow — Master Spec Index
> AMD AI Hackathon | Real-Time Voice & Chat Translation | Compose Multiplatform Desktop

---

## Agent Instructions

Read this file first. Then read ONLY the spec file for the task you are implementing.
Do not read all spec files at once.

## Rules (Apply to Every Task)
- Kotlin only. No Java files except where JVM audio APIs are unavoidable.
- Do not ask clarifying questions — all decisions are in the spec files.
- After each task: confirm it compiles and passes its acceptance check before proceeding.
- If a library version is unavailable, use nearest stable and leave a `// VERSION:` comment.
- When a choice is unspecified, pick simpler and leave a `// DECISION:` comment.

## Stack Summary
| Concern | Choice |
|---|---|
| UI | Compose Multiplatform (Desktop/JVM) |
| Language | Kotlin 100% |
| DI | Koin |
| Navigation | Compose Navigation (decompose if needed) |
| HTTP + WebSocket | Ktor Client (OkHttp engine) |
| Local DB | SQLDelight (SQLite) |
| Audio Capture/Playback | javax.sound.sampled (JVM Desktop) |
| ASR | Deepgram Streaming WebSocket API |
| Translation LLM | AMD GPU Droplet — OpenAI-compatible REST endpoint |
| TTS | ElevenLabs REST API |
| Build | Gradle Kotlin DSL |

## Spec Files — Read Only What You Need

| File | What It Covers | Tasks |
|---|---|---|
| `01_PROJECT_SETUP.md` | Scaffold, Gradle, modules, Koin, Navigation | TASK 1–2 |
| `02_DATA_LAYER.md` | SQLDelight schema, models, repositories | TASK 3–4 |
| `03_AUDIO_ENGINE.md` | Mic capture, playback, chunking, JVM audio | TASK 5 |
| `04_ASR_CLIENT.md` | Deepgram WebSocket streaming integration | TASK 6 |
| `05_LLM_CLIENT.md` | AMD droplet LLM client (OpenAI-compatible) | TASK 7 |
| `06_TTS_CLIENT.md` | ElevenLabs TTS client + audio playback | TASK 8 |
| `07_TRANSLATION_PIPELINE.md` | Orchestrating ASR → LLM → TTS in real-time | TASK 9 |
| `08_UI_THEME.md` | Design tokens, Theme.kt, Type.kt | TASK 10 |
| `09_UI_SCREENS.md` | All screens and components | TASK 11–16 |
| `10_FINAL_INTEGRATION.md` | Wiring, error handling, demo setup, packaging | TASK 17–18 |

## Prompt to Use With Agent
For each task say:
> "Read voxflow-spec/[FILENAME].md and implement [TASK N]. Do not proceed until it compiles."
