# AiBrowser - Android App

## Build

```bash
ANDROID_HOME=$HOME/android-sdk gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## Build Environment (Termux on aarch64/arm64)

### Prerequisites

| Package | Install |
|---------|---------|
| JDK 21 | `pkg install openjdk-21` |
| Gradle | `pkg install gradle` (or use the wrapper) |
| Android SDK | `$HOME/android-sdk` with platforms 33-36, build-tools 34.0.4+ |
| aapt2 | Comes with build-tools; also via `pkg install aapt2` (aarch64 native) |
| CMake | `pkg install cmake` (for MNN native build) |
| clang | `pkg install clang` (for MNN native build) |

Set `local.properties`:
```
sdk.dir=/data/data/com.termux/files/home/android-sdk
cmake.dir=/data/data/com.termux/files/usr
```

### Critical: aapt2 Architecture

AGP bundles an **x86_64 aapt2** for Linux, which does NOT run on aarch64 Termux. Set `gradle.properties` to override with an aarch64 build-tools aapt2:

```
android.aapt2FromMavenOverride=/data/data/com.termux/files/home/android-sdk/build-tools/34.0.4/aapt2
```

The aapt2 in `build-tools/34.0.4` is natively compiled for **aarch64 (Android)** — it runs directly on Termux without qemu or proot. It cannot load platform jars >= 35, so **compileSdk must be 34** (not 35+).

### Building with compileSdk 34

```kotlin
// app/build.gradle.kts
android {
    compileSdk = 34
    defaultConfig {
        targetSdk = 34
    }
}
```

Some dependencies must be pinned to versions compatible with SDK 34:

| Library | Max SDK 34 version |
|---------|-------------------|
| `androidx.core:core-ktx` | 1.12.0 |
| `androidx.compose:compose-bom` | 2024.12.01 (works) |
| `androidx.activity:activity-compose` | 1.9.3 (works) |
| `androidx.lifecycle:lifecycle-*` | 2.8.7 (works) |

### Kotlin / Gradle Version Compatibility

| Component | Version |
|-----------|---------|
| Kotlin | 2.1.20 |
| AGP | 8.9.1 |
| Gradle | 9.5.1 |
| Hilt | 2.54 |

Use the `compilerOptions` DSL (not deprecated `kotlinOptions`):

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```

### Clean build

```bash
rm -rf ~/.gradle/caches/*/transforms/*aapt*
gradle assembleDebug --no-build-cache
```

---

## MNN Native Libraries

### Source

The official `libMNN.so` and `libmnnllmapp.so` are extracted from the **MNN Chat APK** (`mnn_chat_0_8_3.apk`), not built from source. These are placed in:

```
app/src/main/jniLibs/arm64-v8a/
├── libMNN.so          (7.5 MB) — MNN inference engine
├── libmnnllmapp.so    (1.4 MB) — JNI bridge (initNative, submitNative, etc.)
└── libc++_shared.so   (1.3 MB) — C++ runtime (bundled from official APK)
```

The JNI functions in `libmnnllmapp.so` map to class `com.alibaba.mnnllm.android.llm.LlmSession` (our `LlmSession.kt` wrapper in that package).

### Why not build from source?

NDK toolchain ships x86_64 binaries (`clang`, `ld`) that won't run on aarch64 Termux. Native Termux clang can compile MNN (used for our `libmnnbridge.so`), but the official APK libraries are tested and stable. We bundle those.

### Custom JNI bridge (`libmnnbridge.so`)

Located at `app/src/main/cpp/` — built natively with Termux cmake/clang when needed for additional JNI functions. Currently **not used** in favor of the official `libmnnllmapp.so`.

CMake build (from `app/src/main/cpp/build/`):
```bash
cmake .. -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++
make -j4
cp libmnnbridge.so ../../jniLibs/arm64-v8a/
```

---

## Architecture Overview

```
┌────────────────────────────────────────────────────┐
│                     MainActivity                   │
│  @AndroidEntryPoint — Hilt-injected dependencies   │
│  Sets up NavGraph with ViewModels + Providers      │
└──────────────────┬─────────────────────────────────┘
                   │
    ┌──────────────┼──────────────┐
    ▼              ▼              ▼
BrowserScreen   AgentScreen   SettingsScreen
    │              │              │
    │         ┌────┴────┐    ┌───┴───┐
    │         │AgentVM   │    │4 Tabs │
    │         │┌───────┐ │    │Cloud  │
    └────────►││AiSvc  │ │    │MNN    │
              ││┌─────┐│ │    │Market │
              │││MNN  ││ │    │Behavior│
              │││Prov ││ │    └───────┘
              ││└──┬──┘│ │
              ││   │   │ │
              │└───┼───┘ │
              └────┼────-┘
                   │
         ┌─────────┴──────────┐
         │   MnnSession.kt    │ (our wrapper)
         │   ┌─────────────┐  │
         │   │ LlmSession  │  │ (official JNI class)
         │   │  (JNI)      │  │
         │   └──────┬──────┘  │
         └──────────┼────────-┘
                    │
         ┌──────────▼──────────┐
         │  libmnnllmapp.so    │
         │  ┌────────────────┐ │
         │  │ mls::LlmSession│ │
         │  │ ┌────────────┐ │ │
         │  │ │ Llm::load()│ │ │
         │  │ │ ::generate │ │ │
         │  │ └────────────┘ │ │
         │  └────────────────┘ │
         └──────────┬──────────┘
                    │
         ┌──────────▼──────────┐
         │    libMNN.so        │
         │  MNN inference engine│
         └────────────────────-┘
```

---

## Key Source Files

### Agent / Inference

| File | Purpose |
|------|---------|
| `agent/AiService.kt` | Central LLM routing: cloud API vs MNN local. Parses streaming responses |
| `agent/MnnLlmProvider.kt` | MNN local inference: `buildPrompt()`, `sendMessage()`, `streamTest()`, session lifecycle |
| `agent/mnn/MnnSession.kt` | Wrapper around official JNI: `load()`, `generate()`, `release()` |
| `alibaba/.../LlmSession.kt` | Official JNI class with `external` methods matching `libmnnllmapp.so` |
| `agent/AgentViewModel.kt` | Agent orchestration: message loop, tool execution, follow-ups, export/import |
| `agent/ToolDefinitions.kt` | 20 browser tools as JSON Schema (browser_navigate, browser_snapshot, etc.) |
| `agent/ToolExecutor.kt` | Executes tools via WebView JS injection + SAF file I/O |
| `agent/McpController.kt` | Thin wrapper around ToolExecutor |
| `agent/StealthInjector.kt` | Injects data-ref attributes into DOM for snapshot + click targeting |

### Browser

| File | Purpose |
|------|---------|
| `browser/BrowserViewModel.kt` | WebView control (navigation, tabs, voice) |
| `browser/TabManager.kt` | Multi-tab state management |
| `browser/TabState.kt` | Per-tab data (URL, messages, loading state, WebView) |

### Data Models

| File | Purpose |
|------|---------|
| `data/models/Message.kt` | Chat message (id, role, content, toolCalls[], thinking, timestamp) |
| `data/models/ToolCall.kt` | Tool call (id, name, arguments, status, result) |
| `data/models/ApiConfig.kt` | Provider enum: OPENAI, CLAUDE, CUSTOM, LOCAL_MNN |
| `data/models/BehaviorConfig.kt` | System prompt, scroll behavior, TTS config |
| `data/SettingsRepository.kt` | DataStore persistence for all settings (API keys, MNN config, downloads) |

### MNN Marketplace

| File | Purpose |
|------|---------|
| `agent/mnn/market/MarketModels.kt` | `MarketModel`, `DownloadedMnnModel`, `ModelMarket` data classes |
| `settings/MnnMarketplaceTab.kt` | Model browse + download UI, download logic with resume |
| `settings/MnnSettingsTab.kt` | MNN config UI: model selection, backend, samplers, test inference |

### UI

| File | Purpose |
|------|---------|
| `ui/screens/AgentScreen.kt` | Chat UI with message list, input bar, export/import buttons |
| `ui/screens/BrowserScreen.kt` | WebView + tab bar + agent status overlay |
| `ui/screens/SettingsScreen.kt` | 4-tab settings: Cloud LLM, MNN Local AI, Marketplace, Behavior |
| `ui/components/MessageBubble.kt` | Renders chat messages (markdown, thinking, tool calls, TTS) |
| `ui/components/ToolCallCard.kt` | Renders tool call status + results in messages |
| `ui/navigation/NavGraph.kt` | Navigation graph (Browser → Agent → Settings) |

### DI / App

| File | Purpose |
|------|---------|
| `di/AppModule.kt` | Hilt module: OkHttpClient, SettingsRepository |
| `MainActivity.kt` | Hilt entry point, injects providers, sets up NavGraph |
| `AiBrowserApp.kt` | Application class, notification channels |

---

## MNN Model Loading Flow

```
User clicks "Run Test" or sends message
  → MnnLlmProvider.sendMessage()
    → ensureSession(modelPath, settings)
      → MnnSession.load(settings)
        → Reads config.json from model directory
        → Merges MnnSettings (temperature, topP, topK, etc.) into config
        → Calls jni.initNative(configFile.absolutePath, null, configJson, extra)
          → libmnnllmapp.so → mls::LlmSession constructor
            → Llm::createLLM(path) — reads config.json from disk
            → set_config(merged_config) — applies our settings
            → llm_->load() — loads model into memory (10-30s first time)
```

Key: The model stays loaded via `ensureSession()` until the 5-minute idle timer fires.

---

## Tool Calling (MNN Local)

The MNN native library (`libMNN.so`) does NOT expose tool calling through its public API. Tool definitions are formatted in `MnnLlmProvider.buildPrompt()` and injected into the system prompt. The model outputs tool calls which our `ToolCallParser` extracts.

### Prompt Formats (per model_type)

**Qwen3.5 (qwen3_5):**
```
<|im_start|>system
# Tools
You have access to the following functions:
<tools>
{openai_tool_schema_json}
</tools>
...format instructions...<|im_end|>
```

Models output: `<tool_call><function=name><parameter=key>value</parameter></function></tool_call>`

**Claude-distilled Qwen3.5 (qwen3_5 with different chat template):**
Same input format, outputs: `<tool_call>\n{"name": "x", "arguments": {...}}\n</tool_call>`

**LFM2 (lfm2):**
```
List of tools: [{openai_tool_schema_json}, ...]
When you need to call a function, respond with a JSON object: {"name": "function_name", "arguments": {...}}
```

Models output: `<|tool_call_start|>[{"name": "x", "arguments": {...}}]<|tool_call_end|>`

### ToolCallParser

Handles 4 output formats:
1. `<tool_call><function=NAME>...</function></tool_call>` — Qwen XML
2. `<tool_call>{"name": "...", "arguments": {...}}</tool_call>` — Claude-distilled JSON-in-XML
3. `<|tool_call_start|>[{"name": "..."}]<|tool_call_end|>` — LFM2 array
4. `{"name": "...", "arguments": {...}}` — bare JSON (fallback)

Also handles:
- `<think>...</think>` block parsing — thinking content extracted and passed via `StreamEvent.Done(thinking=...)`
- `<tool_response>...</tool_response>` — tool results wrapped as user messages

---

## Model Marketplace & Downloads

### Download Flow

1. Load `model_market.json` from assets (~40 MNN models)
2. Filter by vendor, tags, search query
3. User clicks Download → `downloadModel()`:
   - Fetches file tree from `huggingface.co/api/models/{repo}/tree/main?recursive=true`
   - Downloads each file from `huggingface.co/{repo}/resolve/main/{path}`
   - Redirect-following OkHttpClient handles LFS transparently
   - `.incomplete` temp files with atomic `renameTo()` on completion
   - Resume via `Range` header and `RandomAccessFile.seek()`
   - Progress persisted to DataStore every ~5MB
   - Speed computed in UI from delta over ~200ms intervals
4. After download → model path saved to SettingsRepository, appears in MNN tab

### Storage

- Model files: `{filesDir}/mnn_models/hf/{org}--{model}/`
- Download state: SettingsRepository `mnn_downloads` key (JSON list of `DownloadedMnnModel`)

---

## Session Lifecycle

| State | Behavior |
|-------|----------|
| **First prompt** | `MnnSession.load()` — loads model from disk (10-30s) |
| **Subsequent prompts** | Session reused via `ensureSession()` — fast (~1-2s per turn) |
| **During LLM generation** | Timer paused (`pauseKeepAlive()`) |
| **During tool execution** | Timer running — reset via `AgentViewModel.keepAlive()` |
| **Idle 5 minutes** | Timer fires → `releaseSession()` — model unloaded, memory freed |
| **New prompt after idle** | Fresh `load()` — model reloaded from disk |

---

## Settings (DataStore Keys)

### MNN Settings

| Key | Type | Default | UI |
|-----|------|---------|-----|
| `mnn_model_path` | String | `""` | TextField |
| `mnn_backend` | String | `"cpu"` | FilterChip (cpu/opencl) |
| `mnn_use_mmap` | Boolean | `false` | Switch |
| `mnn_prompt_cache` | Boolean | `true` | Switch |
| `mnn_max_tokens` | Int | `2048` | NumberField |
| `mnn_temperature` | Float (string) | `0.7` | Slider 0.0-2.0 |
| `mnn_top_p` | Float (string) | `0.95` | Slider 0.0-1.0 |
| `mnn_top_k` | Int | `20` | Slider 1-100 |
| `mnn_precision` | String | `"low"` | FilterChip (low/high) |
| `mnn_threads` | Int | `4` | NumberField |
| `mnn_downloads` | JSON string | `"[]"` | Marketplace state |

### API Settings

| Key | Type | Default |
|-----|------|---------|
| `api_provider` | String | `"OPENAI"` |
| `api_key` | String | `""` |
| `model` | String | `""` |
| `base_url` | String | `""` |

### Behavior Settings

| Key | Type | Default |
|-----|------|---------|
| `system_prompt` | String | Browser automation assistant prompt |
| `scroll_into_view` | Boolean | `true` |
| `tts_prompt` | String | TTS cleaning prompt |
| `notes_directory_uri` | String | SAF tree URI for file tools |

---

## Chat Export/Import

### Export Format

```json
{
  "messages": [
    { "id": "system", "role": "SYSTEM", "content": "...", "timestamp": 1781969622829, "toolCalls": [] },
    { "id": "...", "role": "USER", "content": "Open google", ... },
    { "id": "...", "role": "ASSISTANT", "content": "...", "toolCalls": [...], "thinking": "..." },
    { "id": "...", "role": "TOOL", "content": "...", "toolCallId": "..." }
  ],
  "config": {
    "modelPath": "/data/user/0/.../Qwen3.5-2B-...",
    "modelType": "qwen3_5",
    "backend": "cpu",
    "settings": {
      "useMmap": false, "promptCache": true, "temperature": 0.7,
      "topP": 0.95, "topK": 20, "precision": "low", "threads": 4
    },
    "modelConfig": { /* content of config.json */ },
    "llmConfig": { /* content of llm_config.json */ },
    "exportTimestamp": 1781969622829
  }
}
```

Files saved to the configured notes directory as `msglog_YYYYMMDDHHmmss.json`. Import restores both messages and model settings.

---

## Common Issues

### 1. Model won't load ("Module load failed")

- Verify all files downloaded correctly (SHA-256 checksums shown in error)
- Model directory must contain `config.json` with `llm_model`, `llm_weight` fields
- First param to `initNative()` must be the **file path** to `config.json`, NOT the directory
- Try disabling mmap and reducing threads to 1

### 2. Model doesn't call tools

- Check model type: `qwen3_5` supports `<tool_call>` XML; `lfm2` supports JSON
- `Qwen3.5-0.8B-MNN` (base) uses `<function=name><parameter=key>` XML
- `Qwen3.5-2B-Claude-4.6-Opus-Reasoning-Distilled` uses `{"name": "...", "arguments": {...}}` JSON inside `<tool_call>` tags
- `QuickAdapter-Chikasha-1.5B` uses `{"name": "...", "arguments": {...}}` JSON without XML wrapper
- Check the msglog export to see what the model actually output

### 3. Native crashes (SIGBUS/SIGSEGV in libMNN.so)

- Added `android:memtagMode="async"` to manifest to disable synchronous MTE checks
- Reduce thread count to 1
- Disable mmap
- Delete `visual.mnn` + `visual.mnn.weight` to skip vision model loading (saves 63MB+)
- Try smaller models (MobileLLM-125M, Smolm2-135M)

### 4. App crashes on resume/restart

- `keepAliveJob` creates new `CoroutineScope(Dispatchers.IO)` each time — use a shared scope
- Timer fires after 5 min idle and releases the model session
- Tool execution resets the timer via `AgentViewModel.aiService.keepAlive()`

### 5. Database errors (DataStore crash at startup)

```
Exception 'android.app.servertransaction.ActivityResultItem' was not found
```
This error is harmless — appears in logs but doesn't affect functionality.

### 6. download speed shows 0 MB/s or random values

- Speed is computed in the UI composable from `(currentBytes - prevBytes) / elapsed`
- If progress updates fire on every 8KB chunk (~0.1ms intervals), speed appears as 0
- The download code should rate-limit to ~200ms intervals for meaningful speed

---

## Build Troubleshooting

| Error | Fix |
|-------|-----|
| `Syntax error: "(" unexpected` on aapt2 | Set `android.aapt2FromMavenOverride` to aarch64 aapt2 path |
| `HiltJavaCompile: Injection of an @HiltViewModel class is prohibited` | Inject the underlying service/manager, not the ViewModel |
| `Unresolved reference 'Chat'` for Material Icons | Add `material-icons-extended` dependency |
| `Unresolved reference 'HttpLoggingInterceptorLevel'` | Use `HttpLoggingInterceptor.Level.BODY` |
| CMake fails with x86_64 compiler | Use native Termux cmake/clang with `cmake.dir` in local.properties |
| NDK toolchain can't find compiler | NDK ships x86_64 binaries — use system clang or prebuilt .so from APK |
| `No space left on device` | Delete unused models from `filesDir/mnn_models/hf/` |

---

## Useful ADB Commands

```bash
# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Logcat (filter our app)
adb logcat -s "MnnJNI:*" "MnnSession:*" "MnnLlmProvider:*" "AndroidRuntime:E"

# Check crashes
adb logcat -d | grep -A 30 "FATAL EXCEPTION\|Fatal signal.*com.aibrowser"

# Clear logs
adb logcat -c

# Check storage
adb shell df -h /data

# List model files
adb shell ls -la /data/user/0/com.aibrowser/files/mnn_models/hf/
```
