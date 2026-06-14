# AiBrowser

An Android browser with an integrated AI agent that can autonomously navigate, read, and interact with web pages.

The agent controls the browser via tool calls (`browser_navigate`, `browser_click`, `browser_snapshot`, `browser_type`, etc.) — the same pattern as Playwright MCP or Browser Use, but executing directly against the app's own WebView on the device, with no external server needed.

## Features

- Multi-tab WebView browser with address bar, back/forward/refresh
- Integrated AI agent with per-tab conversation history
- 17 browser automation tools (navigate, click, type, snapshot, fill forms, select, hover, wait, tab management, drag, evaluate JS, and more)
- Voice input mode with TTS response read-back
- AI provider support: OpenAI, Anthropic Claude, or any OpenAI-compatible API
- Stealth injection (hides `navigator.webdriver`, spoofs plugins/languages/WebGL, etc.)
- External app intent blocking (redirects `twitter://`, `intent://` etc. to `https://`)
- Configurable system prompt and TTS prompt
- Dark/light theme (Material 3)

> **Bring your own API key.** The app connects to an LLM provider of your choice — no built-in AI, no bundled model, no cloud service. You provide the API key and endpoint in Settings.

## Architecture

```
User input → AgentViewModel → AiService (LLM API)
                                    ↓
                             Streaming response
                           (tokens / tool calls)
                                    ↓
AgentViewModel → executeToolCalls → McpController → ToolExecutor
                                                        ↓
                                                 WebView (evaluateJavascript)
                                                        ↓
                                                 DOM interaction (click, type, snapshot, navigate, ...)

AgentViewModel saves per-tab state (messages, loading, actions) → TabState
                                                                            ↓
                                                          BrowserScreen observes TabState via BrowserViewModel
```

**Key components:**

- **AiService** — HTTP client for LLM APIs (OpenAI/Claude/Custom). Handles streaming, SSE parsing, retry with exponential backoff.
- **AgentViewModel** — HiltViewModel managing the send → stream → execute tools → follow-up loop. Each tab has its own message history and execution state.
- **ToolExecutor** — Executes MCP tool calls against the active WebView via `evaluateJavascript()`. Includes accessibility tree snapshot generation and element interaction.
- **StealthInjector** — Patchright-inspired anti-detection script injected on every page load.
- **TabManager** — Manages WebView instances per tab with JS/DOM storage/mixed content configuration.
- **WebViewContainer** — Compose AndroidView that wraps each tab's WebView, handles page callbacks, intent blocking, and stealth injection.

## Usage

1. Open the **Settings** screen (gear icon) and configure your LLM provider (API key, model, base URL).
2. Go to the **Browser** screen and navigate to a page, or create a new tab.
3. Tap the **Chat** FAB to open the Agent screen. Type a task like "Search for the best restaurants in Tokyo" or "Fill out this form with sample data".
4. Watch the agent navigate pages, click elements, type text, and take snapshots to understand page content.
5. Use the **Microphone** FAB for voice input and get spoken responses via TTS.

## Building on Termux (aarch64/arm64)

### Prerequisites

```
pkg install openjdk-21 gradle aapt2
```

Install Android SDK at `$HOME/android-sdk` with platforms 33-36 and build-tools 34.0.4+.

Set `local.properties`:
```
sdk.dir=/data/data/com.termux/files/home/android-sdk
```

### Build

```bash
ANDROID_HOME=$HOME/android-sdk gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Notes

- **compileSdk must be 34.** The aarch64 aapt2 from SDK build-tools cannot load platform jars >= 35.
- The `gradle.properties` file includes `android.aapt2FromMavenOverride` pointing to the SDK's aarch64-native aapt2. AGP bundles an x86_64 aapt2 that does not run on aarch64.
- If you hit aapt2 transform cache corruption: `rm -rf ~/.gradle/caches/*/transforms/*aapt* && gradle assembleDebug --no-build-cache`

### Tech stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.1.20 |
| AGP | 8.9.1 |
| Gradle | 9.5.1 |
| Compose BOM | 2024.12.01 |
| Hilt | 2.54 |
| OkHttp | 4.12.0 |
| DataStore | 1.1.1 |
| Min SDK | 26 |
| Target SDK | 34 |
