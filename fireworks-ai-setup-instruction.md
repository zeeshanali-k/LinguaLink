# Setting Up Fireworks AI (AMD-Hosted) for LinguaLink

This guide walks you through getting an API key from Fireworks AI's AMD-hosted inference platform, picking a model, verifying it works, and plugging the credentials into the **LinguaLink** desktop app.

Fireworks AI runs on AMD GPUs and exposes an **OpenAI-compatible REST API**, so LinguaLink talks to it with the same `JvmLlmClient` it would use for any OpenAI-style endpoint — only the base URL, API key, and model name change.

---

## Why Fireworks AI?

- No infrastructure to provision — models are deployed and ready
- Pay-per-token billing (no idle GPU cost like a self-hosted droplet)
- OpenAI-compatible `/v1/chat/completions` endpoint — drop-in for the LinguaLink LLM client
- AMD-accelerated inference

---

## Step 1 — Create a Fireworks AI Account

1. Go to [fireworks.ai](https://fireworks.ai)
2. Sign up (Google / GitHub / email)
3. Verify your email if prompted
4. Add a payment method in **Billing** — Fireworks gives free credits on signup, but a card on file is required to use most models

---

## Step 2 — Generate an API Key

1. In the Fireworks dashboard, open **Settings → API Keys** (or visit [fireworks.ai/account/api-keys](https://fireworks.ai/account/api-keys))
2. Click **Create API Key**
3. Give it a name (e.g., `lingualink-dev`)
4. **Copy the key immediately** — it starts with `fw_…` and is only shown once

> Treat this key like a password. Anyone with it can run inference on your account and rack up charges.

---

## Step 3 — Choose a Model

Browse the **Model Library** at [fireworks.ai/models](https://fireworks.ai/models). For real-time translation, you want a model that is:

- Fast (low time-to-first-token)
- Multilingual
- Instruction-tuned

**Recommended starting points:**

| Model | Path | Best For |
|-------|------|----------|
| Llama 3.1 8B Instruct | `accounts/fireworks/models/llama-v3p1-8b-instruct` | **Default** — fast, multilingual, low cost |
| Llama 3.1 70B Instruct | `accounts/fireworks/models/llama-v3p1-70b-instruct` | Higher quality, slower, pricier |
| Qwen 2.5 72B Instruct | `accounts/fireworks/models/qwen2p5-72b-instruct` | Strong on Chinese / Asian languages |
| Mixtral 8x7B Instruct | `accounts/fireworks/models/mixtral-8x7b-instruct` | Solid all-rounder |

> Copy the **full model path** including `accounts/fireworks/models/…` — that's exactly what the LinguaLink Setup screen expects.

---

## Step 4 — Verify the Endpoint with `curl`

From your local machine, run a quick sanity check (replace `YOUR_API_KEY` and pick a model):

```bash
curl https://api.fireworks.ai/inference/v1/chat/completions \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -d '{
    "model": "accounts/fireworks/models/llama-v3p1-8b-instruct",
    "max_tokens": 256,
    "temperature": 0.2,
    "messages": [
      {"role": "system", "content": "You are a translator. Translate to Spanish. Output only the translation."},
      {"role": "user", "content": "Hello, how are you?"}
    ]
  }'
```

Expected response (truncated):

```json
{
  "id": "...",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hola, ¿cómo estás?"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": { ... }
}
```

If you get a `401` — the key is wrong. A `404` — the model path is wrong.

---

## Step 5 — Plug Into LinguaLink

1. Launch the app:
   ```bash
   ./gradlew :composeApp:run
   ```
2. On the **Setup** screen, enter:
   - **Fireworks AI base URL:** `https://api.fireworks.ai/inference` *(prefilled by default)*
   - **Fireworks AI API key:** `fw_…` (paste from Step 2)
   - **Fireworks model:** `accounts/fireworks/models/llama-v3p1-8b-instruct` *(or whichever you picked)*
   - **Deepgram API key:** your Deepgram key
   - *(TTS audio playback uses the same Deepgram key — no separate field.)*
3. Click **Test connection** — sends a `ping` request to the chosen model and expects a non-empty reply
4. Click **Save & continue** — config is persisted to `~/.lingualink/config.json` and the app routes to Home

After this point, the app reapplies the saved Fireworks config to the LLM client automatically on every launch — no need to re-enter on restart.

---

## What the App Sends

`JvmLlmClient` issues this request for every translation:

```
POST https://api.fireworks.ai/inference/v1/chat/completions
Headers:
  Authorization: Bearer <your-fw-key>
  Content-Type: application/json
  Accept: application/json
Body:
  {
    "model": "accounts/fireworks/models/llama-v3p1-8b-instruct",
    "messages": [
      {"role": "system", "content": "<translation prompt>"},
      ...conversation history (last 6 turns)...,
      {"role": "user", "content": "<text to translate>"}
    ],
    "max_tokens": 512,
    "temperature": 0.2,
    "top_p": 1.0,
    "stream": false
  }
```

The system prompt is built from the current source/target languages and instructs the model to output the translation only.

---

## Edge Cases & Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|--------------|-----|
| Test Connection: `401 Unauthorized` | API key wrong or revoked | Regenerate key, paste again |
| Test Connection: `404 Not Found` | Model path typo | Copy the exact path from the Fireworks model page |
| Test Connection: `429 Rate Limited` | Too many requests on free tier | Wait or upgrade billing tier |
| Translations slow | Large model picked | Switch to `llama-v3p1-8b-instruct` for speed |
| Translation has extra commentary | Model not following system prompt | Try a different instruct-tuned model |
| Empty response | Model output filtered or `max_tokens=0` | Increase max tokens or change model |
| `Connection refused` | Wrong base URL | Should be `https://api.fireworks.ai/inference` (no trailing slash, no `/v1`) |

---

## Cost Awareness

Fireworks bills **per million input + output tokens**. For real-time translation:

- Each translation roundtrip is ~50–200 tokens total
- A 30-minute conversation might use 5k–15k tokens
- Llama 3.1 8B at ~$0.20 per 1M tokens = pennies per session

But: a runaway loop (e.g., the app stuck retrying) can burn tokens fast. Set up **billing alerts** in the Fireworks dashboard.

---

## What You Need to Configure in LinguaLink

| Field | Value |
|-------|-------|
| Fireworks AI base URL | `https://api.fireworks.ai/inference` |
| Fireworks AI API key | `fw_…` (from Step 2) |
| Fireworks model | `accounts/fireworks/models/llama-v3p1-8b-instruct` (or another from Step 3) |
| Deepgram API key | (required — used for both ASR `/v1/listen` and TTS `/v1/speak`) |

That's everything. Once Setup is saved, the app is ready to translate.
