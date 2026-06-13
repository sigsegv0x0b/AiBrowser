# AI Browser — Design Spec

## [S1] Problem

Build an Android browser with integrated AI agent that can autonomously navigate, interact with, and read web pages. The user can switch between viewing the agent's actions (Agent tab) and the live browser state (Web tab), with the ability to intervene.

## [S2] Solution Overview

Kotlin + Jetpack Compose Android app using WebView for browsing and a chat-based agent interface. The browser acts as both browser AND MCP server — the AI controls the same app's tabs, DOM, and navigation. Cloud AI APIs (OpenAI, Claude, custom) are user-configurable.

## [S3] Architecture

### Components

| Component | Responsibility |
|-----------|---------------|
| `MainActivity` | Hosts tab navigation (Web/Agent) |
| `BrowserViewModel` | Manages tabs, current tab, state |
| `WebViewScreen` | Renders current WebView with tab bar |
| `AgentScreen` | Chat UI with message list + input |
| `McpController` | Executes tool calls on WebViews |
| `AiService` | Manages API calls, streaming, tool routing |
| `StealthInjector` | Patchright-style anti-detection (removes navigator.webdriver, automation flags) |
| `SettingsScreen` | API key, provider, model configuration |
| `TabManager` | Creates/destroys WebView tabs |

### Data Flow

1. User types prompt in Agent tab
2. `AiService` sends prompt + MCP tool definitions to configured API
3. API returns tool calls (e.g., `navigate`, `click`, `read_page`)
4. `McpController` executes each tool on the active WebView
5. Tool results sent back to API for next step
6. Agent tab streams each step; Web tab shows live state

### MCP Tools (Playwright-compatible)

| Tool | Description |
|------|-------------|
| `browser_navigate(url)` | Navigate to URL |
| `browser_navigate_back()` | Go back in history |
| `browser_snapshot()` | Accessibility tree (structured DOM for actions) |
| `browser_take_screenshot(fullPage)` | Capture screenshot |
| `browser_console_messages(level)` | Get console logs |
| `browser_network_requests(filter)` | List network requests |
| `browser_click(target)` | Click element |
| `browser_type(target, text)` | Type text into element |
| `browser_fill_form(fields)` | Fill multiple form fields |
| `browser_select_option(target, values)` | Select dropdown option |
| `browser_hover(target)` | Hover over element |
| `browser_press_key(key)` | Press keyboard key |
| `browser_drag(start, end)` | Drag and drop |
| `browser_handle_dialog(accept)` | Handle alert/confirm/prompt |
| `browser_evaluate(function)` | Run JavaScript |
| `browser_wait_for(text)` | Wait for text/time |
| `browser_tabs(action)` | List/create/close/select tabs |
| `browser_close()` | Close browser |

### Stealth Features (Patchright-inspired)

| Feature | Purpose |
|---------|---------|
| Remove `navigator.webdriver` | Prevent automation detection |
| Remove WebView marker from User-Agent | Look like real Chrome |
| Inject stealth JS on page load | Consistent fingerprints |
| Disable console API leaks | Prevent timing attacks |
| Add Chrome runtime object | Match real Chrome behavior |
| Consistent plugins/languages | Match desktop Chrome |
| WebGL vendor spoofing | Prevent GPU fingerprinting |
| Reduce timer precision | Prevent timing fingerprinting |

## [S4] UI Design

### Tab Bar (Bottom)
```
[ Web Tab 1 | Web Tab 2 | + ]    [Agent]
```
- `Agent` button switches to agent view
- Web tabs show page title (truncated)
- `+` adds new tab

### Agent Tab
```
┌─────────────────────────┐
│  Agent                   │
├─────────────────────────┤
│  User: search for pizza  │
│                          │
│  Agent: navigate to...   │
│  > navigate("google...") │
│  ✓ Done                  │
│                          │
│  Agent: I found 3...     │
├─────────────────────────┤
│  [Type a command...] [→] │
└─────────────────────────┘
```

- Each tool call shows as collapsible card with status (pending/running/done/error)
- User can tap a tool call to see details
- Input field with send button

### Settings
```
API Provider: [OpenAI ▼]
API Key:      [sk-...]
Model:        [gpt-4o ▼]
Base URL:     [https://api.openai.com/v1]
```

## [S5] Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Networking:** Retrofit + OkHttp for API calls, SSE for streaming
- **DI:** Hilt
- **Storage:** DataStore for settings
- **WebView:** Android built-in WebView
- **Min SDK:** 26 (Android 8.0)

## [S6] Project Structure

```
app/src/main/java/com/aibrowser/
├── MainActivity.kt
├── ui/
│   ├── theme/
│   ├── screens/
│   │   ├── WebViewScreen.kt
│   │   ├── AgentScreen.kt
│   │   └── SettingsScreen.kt
│   └── components/
│       ├── TabBar.kt
│       ├── MessageBubble.kt
│       └── ToolCallCard.kt
├── browser/
│   ├── TabManager.kt
│   └── WebViewPool.kt
├── agent/
│   ├── McpController.kt
│   ├── AiService.kt
│   ├── ToolDefinitions.kt
│   └── ToolExecutor.kt
├── data/
│   ├── SettingsRepository.kt
│   └── models/
└── di/
    └── AppModule.kt
```

## [S7] Error Handling

- Network errors → retry with backoff, show toast
- API errors → display in agent chat
- Invalid tool calls → log + skip, notify AI
- WebView crashes → recreate tab, restore URL
- Invalid API key → prompt user in settings

## [S8] Testing Strategy

- Unit tests: `McpController`, `AiService`, `ToolDefinitions`
- Integration tests: tool execution on mock WebView
- UI tests: agent chat flow, tab switching
- Manual: end-to-end agent browsing scenarios

## [S9] Future Enhations (Out of Scope for MVP)

- Screenshot-based vision (send page screenshot to vision model)
- Voice commands
- Browser history + bookmarks
- Extensions system
- Local LLM support (Ollama integration)
- Tab groups / sessions
- CDP-Patches (full Chromium DevTools Protocol patches like Patchright)
- Proxy rotation support
- Cookie/session persistence
- Custom user agent profiles
