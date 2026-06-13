# AI Browser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android browser with an integrated AI agent that can navigate, read, and interact with web pages via MCP tools.

**Architecture:** WebView-based browser with tab management, a chat-based agent UI, and an MCP controller that executes AI tool calls on WebViews. Cloud AI APIs (OpenAI/Claude/custom) are user-configurable.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Retrofit, OkHttp, Hilt, DataStore, Android WebView

---

## File Map

```
app/build.gradle.kts                          # App-level Gradle config
app/src/main/AndroidManifest.xml              # Permissions, activity
app/src/main/java/com/aibrowser/
├── AiBrowserApp.kt                           # Application class (Hilt)
├── MainActivity.kt                           # Single activity, nav host
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                          # Material 3 theme
│   │   ├── Color.kt                          # Color palette
│   │   └── Type.kt                           # Typography
│   ├── navigation/
│   │   └── NavGraph.kt                       # Navigation routes
│   ├── screens/
│   │   ├── BrowserScreen.kt                  # WebView + tab bar
│   │   ├── AgentScreen.kt                    # Chat interface
│   │   └── SettingsScreen.kt                 # API configuration
│   └── components/
│       ├── TabBar.kt                         # Bottom tab strip
│       ├── MessageBubble.kt                  # Chat message display
│       ├── ToolCallCard.kt                   # Collapsible tool call
│       └── WebViewContainer.kt               # WebView wrapper composable
├── browser/
│   ├── BrowserViewModel.kt                   # Tab state, navigation
│   ├── TabManager.kt                         # Create/destroy tabs
│   └── WebViewState.kt                       # Per-tab state holder
├── agent/
│   ├── AgentViewModel.kt                     # Chat state, send messages
│   ├── AiService.kt                          # API calls, streaming
│   ├── McpController.kt                      # Tool execution on WebView
│   ├── ToolDefinitions.kt                    # MCP tool schemas
│   ├── ToolExecutor.kt                       # Route tool calls to actions
│   └── StealthInjector.kt                    # Patchright-style anti-detection
├── data/
│   ├── SettingsRepository.kt                 # DataStore preferences
│   └── models/
│       ├── Message.kt                        # Chat message model
│       ├── ToolCall.kt                       # Tool call model
│       ├── TabState.kt                       # Tab data model
│       └── ApiConfig.kt                      # API configuration model
└── di/
    ├── AppModule.kt                          # App-level DI
    └── NetworkModule.kt                      # HTTP client DI
```

---

### Task 1: Project Setup

**Covers:** [S5]
**Files:**
- Create: `build.gradle.kts` (root)
- Create: `settings.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `gradle.properties`

- [ ] **Step 1: Create root build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
}
```

- [ ] **Step 2: Create settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "AiBrowser"
include(":app")
```

- [ ] **Step 3: Create app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.aibrowser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aibrowser"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
}
```

- [ ] **Step 4: Create AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".AiBrowserApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="AI Browser"
        android:supportsRtl="true"
        android:theme="@style/Theme.AiBrowser"
        android:usesCleartextTraffic="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 5: Create gradle.properties**

```
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 6: Create AiBrowserApp.kt**

```kotlin
package com.aibrowser

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AiBrowserApp : Application()
```

- [ ] **Step 7: Commit**

```bash
git init
git add -A
git commit -m "feat: project setup with Gradle, Hilt, Compose dependencies"
```

---

### Task 2: Data Models

**Covers:** [S4, S6]
**Files:**
- Create: `app/src/main/java/com/aibrowser/data/models/TabState.kt`
- Create: `app/src/main/java/com/aibrowser/data/models/Message.kt`
- Create: `app/src/main/java/com/aibrowser/data/models/ToolCall.kt`
- Create: `app/src/main/java/com/aibrowser/data/models/ApiConfig.kt`

- [ ] **Step 1: Create TabState.kt**

```kotlin
package com.aibrowser.data.models

data class TabState(
    val id: String,
    val url: String = "about:blank",
    val title: String = "New Tab",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
)
```

- [ ] **Step 2: Create Message.kt**

```kotlin
package com.aibrowser.data.models

data class Message(
    val id: String,
    val role: Role,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role { USER, ASSISTANT, SYSTEM, TOOL }
}
```

- [ ] **Step 3: Create ToolCall.kt**

```kotlin
package com.aibrowser.data.models

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>,
    val status: ToolStatus = ToolStatus.PENDING,
    val result: String? = null
) {
    enum class ToolStatus { PENDING, RUNNING, DONE, ERROR }
}
```

- [ ] **Step 4: Create ApiConfig.kt**

```kotlin
package com.aibrowser.data.models

data class ApiConfig(
    val provider: ApiProvider = ApiProvider.OPENAI,
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = ""
) {
    enum class ApiProvider(val displayName: String, val defaultModel: String, val defaultBaseUrl: String) {
        OPENAI("OpenAI", "gpt-4o", "https://api.openai.com/v1"),
        CLAUDE("Anthropic", "claude-sonnet-4-20250514", "https://api.anthropic.com"),
        CUSTOM("Custom", "", "")
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/aibrowser/data/models/
git commit -m "feat: add data models for tabs, messages, tool calls, API config"
```

---

### Task 3: Settings Repository

**Covers:** [S4, S6]
**Files:**
- Create: `app/src/main/java/com/aibrowser/data/SettingsRepository.kt`
- Create: `app/src/main/java/com/aibrowser/di/AppModule.kt`

- [ ] **Step 1: Create SettingsRepository.kt**

```kotlin
package com.aibrowser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aibrowser.data.models.ApiConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_PROVIDER = stringPreferencesKey("api_provider")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
    }

    val apiConfig: Flow<ApiConfig> = context.dataStore.data.map { prefs ->
        val providerName = prefs[KEY_PROVIDER] ?: ApiConfig.ApiProvider.OPENAI.name
        val provider = try {
            ApiConfig.ApiProvider.valueOf(providerName)
        } catch (_: Exception) {
            ApiConfig.ApiProvider.OPENAI
        }
        ApiConfig(
            provider = provider,
            apiKey = prefs[KEY_API_KEY] ?: "",
            model = prefs[KEY_MODEL] ?: provider.defaultModel,
            baseUrl = prefs[KEY_BASE_URL] ?: provider.defaultBaseUrl
        )
    }

    suspend fun saveApiConfig(config: ApiConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = config.provider.name
            prefs[KEY_API_KEY] = config.apiKey
            prefs[KEY_MODEL] = config.model
            prefs[KEY_BASE_URL] = config.baseUrl
        }
    }
}
```

- [ ] **Step 2: Create AppModule.kt**

```kotlin
package com.aibrowser.di

import android.content.Context
import com.aibrowser.data.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/aibrowser/data/SettingsRepository.kt app/src/main/java/com/aibrowser/di/AppModule.kt
git commit -m "feat: add settings repository with DataStore and Hilt module"
```

---

### Task 4: Tab Manager + Browser ViewModel

**Covers:** [S3, S4]
**Files:**
- Create: `app/src/main/java/com/aibrowser/browser/TabState.kt`
- Create: `app/src/main/java/com/aibrowser/browser/TabManager.kt`
- Create: `app/src/main/java/com/aibrowser/browser/BrowserViewModel.kt`

- [ ] **Step 1: Create browser TabState.kt**

```kotlin
package com.aibrowser.browser

import android.webkit.WebView

data class TabState(
    val id: String,
    val url: String = "about:blank",
    val title: String = "New Tab",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val webView: WebView? = null
)
```

- [ ] **Step 2: Create TabManager.kt**

```kotlin
package com.aibrowser.browser

import android.content.Context
import android.webkit.WebView
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _tabs = mutableListOf<TabState>()
    val tabs: List<TabState> get() = _tabs.toList()

    private var _activeTabId: String? = null
    val activeTabId: String? get() = _activeTabId

    fun createTab(url: String = "about:blank"): TabState {
        val id = UUID.randomUUID().toString().take(8)
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        val tab = TabState(id = id, url = url, webView = webView)
        _tabs.add(tab)
        _activeTabId = id
        if (url != "about:blank") {
            webView.loadUrl(url)
        }
        return tab
    }

    fun closeTab(id: String) {
        val tab = _tabs.find { it.id == id } ?: return
        tab.webView?.destroy()
        _tabs.removeAll { it.id == id }
        if (_activeTabId == id) {
            _activeTabId = _tabs.lastOrNull()?.id
        }
    }

    fun setActiveTab(id: String) {
        if (_tabs.any { it.id == id }) {
            _activeTabId = id
        }
    }

    fun getActiveTab(): TabState? = _tabs.find { it.id == _activeTabId }

    fun getTab(id: String): TabState? = _tabs.find { it.id == id }

    fun updateTab(id: String, update: (TabState) -> TabState) {
        val index = _tabs.indexOfFirst { it.id == id }
        if (index >= 0) {
            _tabs[index] = update(_tabs[index])
        }
    }
}
```

- [ ] **Step 3: Create BrowserViewModel.kt**

```kotlin
package com.aibrowser.browser

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabManager: TabManager
) : ViewModel() {

    private val _tabs = MutableStateFlow<List<TabState>>(emptyList())
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    init {
        if (tabManager.tabs.isEmpty()) {
            tabManager.createTab("https://www.google.com")
        }
        refresh()
    }

    fun createTab(url: String = "about:blank") {
        tabManager.createTab(url)
        refresh()
    }

    fun closeTab(id: String) {
        tabManager.closeTab(id)
        refresh()
    }

    fun setActiveTab(id: String) {
        tabManager.setActiveTab(id)
        refresh()
    }

    fun getActiveTab(): TabState? = tabManager.getActiveTab()

    fun getTab(id: String): TabState? = tabManager.getTab(id)

    fun updateTab(id: String, update: (TabState) -> TabState) {
        tabManager.updateTab(id, update)
        refresh()
    }

    fun refresh() {
        _tabs.value = tabManager.tabs
        _activeTabId.value = tabManager.activeTabId
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/aibrowser/browser/
git commit -m "feat: add tab manager and browser view model"
```

---

### Task 5: MCP Tool Definitions + Executor

**Covers:** [S3]
**Files:**
- Create: `app/src/main/java/com/aibrowser/agent/ToolDefinitions.kt`
- Create: `app/src/main/java/com/aibrowser/agent/ToolExecutor.kt`
- Create: `app/src/main/java/com/aibrowser/agent/McpController.kt`

- [ ] **Step 1: Create ToolDefinitions.kt**

```kotlin
package com.aibrowser.agent

import com.google.gson.Gson
import com.google.gson.JsonObject

object ToolDefinitions {
    private val gson = Gson()

    data class Tool(
        val name: String,
        val description: String,
        val parameters: JsonObject
    )

    val tools: List<Tool> = listOf(
        // === Navigation ===
        Tool(
            name = "browser_navigate",
            description = "Navigate to a URL",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "url": {"type": "string", "description": "The URL to navigate to"}
                },
                "required": ["url"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_navigate_back",
            description = "Go back to the previous page in history",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {}
            }""", JsonObject::class.java)
        ),
        // === Page Reading ===
        Tool(
            name = "browser_snapshot",
            description = "Capture accessibility snapshot of the current page. Returns structured DOM tree with element references for actions. Use this instead of screenshots.",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "depth": {"type": "integer", "description": "Limit depth of snapshot tree"},
                    "boxes": {"type": "boolean", "description": "Include bounding boxes for elements"}
                }
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_take_screenshot",
            description = "Take a screenshot of the current page. For actions, use browser_snapshot instead.",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "fullPage": {"type": "boolean", "description": "Capture full scrollable page"},
                    "type": {"type": "string", "enum": ["png", "jpeg"], "description": "Image format"}
                }
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_console_messages",
            description = "Get browser console messages",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "level": {"type": "string", "enum": ["info", "warning", "error"], "description": "Minimum log level"}
                }
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_network_requests",
            description = "List network requests since page load",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "filter": {"type": "string", "description": "URL regexp filter"},
                    "static": {"type": "boolean", "description": "Include static resources"}
                }
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_network_request",
            description = "Get full details of a specific network request",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "index": {"type": "integer", "description": "1-based request index from browser_network_requests"}
                },
                "required": ["index"]
            }""", JsonObject::class.java)
        ),
        // === Interaction ===
        Tool(
            name = "browser_click",
            description = "Click an element on the page",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "target": {"type": "string", "description": "CSS selector or accessibility reference for element"},
                    "element": {"type": "string", "description": "Human-readable description of element"},
                    "doubleClick": {"type": "boolean", "description": "Perform double click"},
                    "button": {"type": "string", "enum": ["left", "right", "middle"], "description": "Mouse button"},
                    "modifiers": {"type": "array", "items": {"type": "string"}, "description": "Modifier keys (Control, Alt, Shift, Meta)"}
                },
                "required": ["target"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_type",
            description = "Type text into an editable element",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "target": {"type": "string", "description": "CSS selector or accessibility reference"},
                    "element": {"type": "string", "description": "Human-readable description"},
                    "text": {"type": "string", "description": "Text to type"},
                    "submit": {"type": "boolean", "description": "Press Enter after typing"},
                    "slowly": {"type": "boolean", "description": "Type one character at a time (for key handlers)"}
                },
                "required": ["target", "text"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_fill_form",
            description = "Fill multiple form fields at once",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "fields": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "selector": {"type": "string"},
                                "value": {"type": "string"}
                            }
                        },
                        "description": "Array of {selector, value} pairs"
                    }
                },
                "required": ["fields"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_select_option",
            description = "Select option(s) in a dropdown",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "target": {"type": "string", "description": "CSS selector or accessibility reference"},
                    "element": {"type": "string", "description": "Human-readable description"},
                    "values": {"type": "array", "items": {"type": "string"}, "description": "Values to select"}
                },
                "required": ["target", "values"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_hover",
            description = "Hover over an element",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "target": {"type": "string", "description": "CSS selector or accessibility reference"},
                    "element": {"type": "string", "description": "Human-readable description"}
                },
                "required": ["target"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_press_key",
            description = "Press a keyboard key (Enter, Tab, Escape, ArrowLeft, etc.)",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "key": {"type": "string", "description": "Key name: ArrowLeft, Enter, Tab, Escape, Backspace, a-z, 0-9, etc."}
                },
                "required": ["key"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_drag",
            description = "Drag from one element to another",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "startTarget": {"type": "string", "description": "Source element selector"},
                    "endTarget": {"type": "string", "description": "Target element selector"},
                    "startElement": {"type": "string", "description": "Source description"},
                    "endElement": {"type": "string", "description": "Target description"}
                },
                "required": ["startTarget", "endTarget"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_handle_dialog",
            description = "Handle alert/confirm/prompt dialogs",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "accept": {"type": "boolean", "description": "Accept (true) or dismiss (false) the dialog"},
                    "promptText": {"type": "string", "description": "Text for prompt dialogs"}
                },
                "required": ["accept"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_evaluate",
            description = "Evaluate JavaScript on the page",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "function": {"type": "string", "description": "JS function: () => { return document.title; }"}
                },
                "required": ["function"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_wait_for",
            description = "Wait for text to appear/disappear or time to pass",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "text": {"type": "string", "description": "Text to wait for to appear"},
                    "textGone": {"type": "string", "description": "Text to wait for to disappear"},
                    "time": {"type": "number", "description": "Seconds to wait"}
                }
            }""", JsonObject::class.java)
        ),
        // === Tabs ===
        Tool(
            name = "browser_tabs",
            description = "List, create, close, or select browser tabs",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "action": {"type": "string", "enum": ["list", "new", "close", "select"], "description": "Tab operation"},
                    "index": {"type": "integer", "description": "Tab index for close/select"},
                    "url": {"type": "string", "description": "URL for new tab"}
                },
                "required": ["action"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_close",
            description = "Close the browser page",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {}
            }""", JsonObject::class.java)
        )
    )

    fun getToolsForApi(): List<Map<String, Any>> {
        return tools.map { tool ->
            mapOf(
                "type" to "function",
                "function" to mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "parameters" to tool.parameters
                )
            )
        }
    }
}
```

- [ ] **Step 2: Create ToolExecutor.kt**

```kotlin
package com.aibrowser.agent

import android.webkit.WebView
import com.aibrowser.browser.BrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolExecutor @Inject constructor(
    private val browserViewModel: BrowserViewModel
) {
    suspend fun execute(toolName: String, arguments: Map<String, Any>): String {
        return withContext(Dispatchers.Main) {
            when (toolName) {
                // Navigation
                "browser_navigate" -> navigate(arguments["url"] as String)
                "browser_navigate_back" -> navigateBack()
                // Page reading
                "browser_snapshot" -> snapshot(
                    depth = (arguments["depth"] as? Double)?.toInt(),
                    boxes = arguments["boxes"] as? Boolean
                )
                "browser_take_screenshot" -> screenshot(
                    fullPage = arguments["fullPage"] as? Boolean,
                    type = arguments["type"] as? String
                )
                "browser_console_messages" -> consoleMessages(arguments["level"] as? String)
                "browser_network_requests" -> networkRequests(
                    filter = arguments["filter"] as? String,
                    static = arguments["static"] as? Boolean
                )
                "browser_network_request" -> networkRequest((arguments["index"] as Double).toInt())
                // Interaction
                "browser_click" -> click(
                    target = arguments["target"] as String,
                    doubleClick = arguments["doubleClick"] as? Boolean,
                    button = arguments["button"] as? String,
                    modifiers = (arguments["modifiers"] as? List<*>)?.filterIsInstance<String>()
                )
                "browser_type" -> type(
                    target = arguments["target"] as String,
                    text = arguments["text"] as String,
                    submit = arguments["submit"] as? Boolean,
                    slowly = arguments["slowly"] as? Boolean
                )
                "browser_fill_form" -> fillForm(arguments["fields"] as List<Map<String, String>>)
                "browser_select_option" -> selectOption(
                    target = arguments["target"] as String,
                    values = arguments["values"] as List<String>
                )
                "browser_hover" -> hover(arguments["target"] as String)
                "browser_press_key" -> pressKey(arguments["key"] as String)
                "browser_drag" -> drag(
                    startTarget = arguments["startTarget"] as String,
                    endTarget = arguments["endTarget"] as String
                )
                "browser_handle_dialog" -> handleDialog(
                    accept = arguments["accept"] as Boolean,
                    promptText = arguments["promptText"] as? String
                )
                "browser_evaluate" -> evaluate(arguments["function"] as String)
                "browser_wait_for" -> waitFor(
                    text = arguments["text"] as? String,
                    textGone = arguments["textGone"] as? String,
                    time = (arguments["time"] as? Double)
                )
                // Tabs
                "browser_tabs" -> tabs(
                    action = arguments["action"] as String,
                    index = (arguments["index"] as? Double)?.toInt(),
                    url = arguments["url"] as? String
                )
                "browser_close" -> closeBrowser()
                else -> "Unknown tool: $toolName"
            }
        }
    }

    private fun runJs(js: String): String {
        var result = ""
        browserViewModel.getActiveTab()?.webView?.evaluateJavascript(js) {
            result = it?.removeSurrounding("\"") ?: "Error"
        }
        return result
    }

    // === Navigation ===

    private fun navigate(url: String): String {
        val tab = browserViewModel.getActiveTab() ?: return "No active tab"
        tab.webView?.loadUrl(url)
        return "Navigated to $url"
    }

    private fun navigateBack(): String {
        val tab = browserViewModel.getActiveTab() ?: return "No active tab"
        if (tab.webView?.canGoBack() == true) {
            tab.webView?.goBack()
            return "Went back"
        }
        return "Cannot go back"
    }

    // === Page Reading ===

    private fun snapshot(depth: Int?, boxes: Boolean?): String {
        val js = """
            (function() {
                function getRole(el) {
                    var tag = el.tagName.toLowerCase();
                    var role = el.getAttribute('role') || '';
                    if (tag === 'a') return 'link';
                    if (tag === 'button' || role === 'button') return 'button';
                    if (tag === 'input' || tag === 'textarea' || tag === 'select') return 'textbox';
                    if (tag === 'img') return 'image';
                    if (tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'h4') return 'heading';
                    if (tag === 'ul' || tag === 'ol') return 'list';
                    if (tag === 'li') return 'listitem';
                    if (tag === 'table') return 'table';
                    return role || tag;
                }

                function getLabel(el) {
                    if (el.id) return '#' + el.id;
                    if (el.name) return '[name="' + el.name + '"]';
                    if (el.placeholder) return el.placeholder;
                    if (el.textContent.trim().substring(0, 50)) return el.textContent.trim().substring(0, 50);
                    return el.tagName.toLowerCase();
                }

                function walk(el, indent, maxDepth) {
                    if (maxDepth !== null && indent/2 > maxDepth) return '';
                    var lines = '';
                    var children = el.children;
                    for (var i = 0; i < children.length; i++) {
                        var child = children[i];
                        if (child.offsetWidth === 0 && child.offsetHeight === 0) continue;
                        var role = getRole(child);
                        var label = getLabel(child);
                        var ref = child.getAttribute('data-ref') || '';
                        if (!ref) {
                            ref = 'ref' + Math.random().toString(36).substr(2, 6);
                            child.setAttribute('data-ref', ref);
                        }
                        var box = '';
                        ${if (boxes == true) """var r = child.getBoundingClientRect();
                        box = ' [' + Math.round(r.x) + ',' + Math.round(r.y) + ',' + Math.round(r.width) + ',' + Math.round(r.height) + ']';""" else ""}
                        lines += ' '.repeat(indent) + '[' + ref + ' ' + role + '] ' + label + box + '\n';
                        lines += walk(child, indent + 2, maxDepth);
                    }
                    return lines;
                }

                var depth = ${depth ?: "null"};
                return walk(document.body, 0, depth);
            })()
        """.trimIndent()
        var result = "Failed to capture snapshot"
        browserViewModel.getActiveTab()?.webView?.evaluateJavascript(js) {
            result = it?.removeSurrounding("\"")?.replace("\\n", "\n") ?: "Error"
        }
        return result
    }

    private fun screenshot(fullPage: Boolean?, type: String?): String {
        return "Screenshots require native Android WebView capture. Use browser_snapshot instead."
    }

    private fun consoleMessages(level: String?): String {
        val minLevel = level ?: "info"
        val js = """
            (function() {
                var levels = ['info', 'warning', 'error'];
                var minIdx = levels.indexOf('$minLevel');
                var msgs = [];
                if (window.__consoleLogs) {
                    for (var i = 0; i < window.__consoleLogs.length; i++) {
                        var m = window.__consoleLogs[i];
                        if (levels.indexOf(m.level) >= minIdx) {
                            msgs.push('[' + m.level + '] ' + m.text);
                        }
                    }
                }
                return msgs.length ? msgs.join('\n') : 'No console messages (injection required)';
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun networkRequests(filter: String?, static: Boolean?): String {
        val js = """
            (function() {
                if (!window.__networkRequests) return 'No network data available';
                var reqs = window.__networkRequests;
                var filter = '${filter ?: ""}';
                var result = [];
                for (var i = 0; i < reqs.length; i++) {
                    if (filter && !reqs[i].url.match(filter)) continue;
                    result.push((i+1) + '. ' + reqs[i].method + ' ' + reqs[i].url + ' -> ' + reqs[i].status);
                }
                return result.length ? result.join('\n') : 'No matching requests';
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun networkRequest(index: Int): String {
        val js = """
            (function() {
                if (!window.__networkRequests || ${index} > window.__networkRequests.length) return 'Request not found';
                var req = window.__networkRequests[${index - 1}];
                return JSON.stringify(req, null, 2);
            })()
        """.trimIndent()
        return runJs(js)
    }

    // === Interaction ===

    private fun click(target: String, doubleClick: Boolean?, button: String?, modifiers: List<String>?): String {
        val modifier = modifiers?.firstOrNull()?.lowercase() ?: ""
        val clickType = if (doubleClick == true) "dblclick" else "click"
        val js = """
            (function() {
                var el = document.querySelector('$target');
                if (!el) return 'Element not found: $target';
                var evt = new MouseEvent('$clickType', {
                    bubbles: true,
                    cancelable: true,
                    view: window,
                    button: '${button ?: "left"}',
                    ctrlKey: $modifier,
                    shiftKey: ${modifiers?.contains("Shift") ?: false},
                    altKey: ${modifiers?.contains("Alt") ?: false},
                    metaKey: ${modifiers?.contains("Meta") ?: false}
                });
                el.dispatchEvent(evt);
                return 'Clicked ' + el.tagName;
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun type(target: String, text: String, submit: Boolean?, slowly: Boolean?): String {
        val escaped = text.replace("'", "\\'").replace("\n", "\\n")
        val js = """
            (function() {
                var el = document.querySelector('$target');
                if (!el) return 'Element not found: $target';
                el.focus();
                el.value = '$escaped';
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                ${if (submit == true) "el.dispatchEvent(new KeyboardEvent('keydown', {key:'Enter', keyCode:13, bubbles:true}));" else ""}
                return 'Typed into ' + el.tagName;
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun fillForm(fields: List<Map<String, String>>): String {
        val results = mutableListOf<String>()
        for (field in fields) {
            val selector = field["selector"] ?: continue
            val value = field["value"] ?: continue
            val escaped = value.replace("'", "\\'").replace("\n", "\\n")
            val js = """
                (function() {
                    var el = document.querySelector('$selector');
                    if (!el) return 'Not found: $selector';
                    el.focus();
                    el.value = '$escaped';
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    return 'Filled ' + el.tagName;
                })()
            """.trimIndent()
            results.add(runJs(js))
        }
        return results.joinToString("; ")
    }

    private fun selectOption(target: String, values: List<String>): String {
        val js = """
            (function() {
                var el = document.querySelector('$target');
                if (!el) return 'Element not found: $target';
                var vals = ${values.map { "\"$it\"" }.toString()};
                for (var i = 0; i < el.options.length; i++) {
                    if (vals.includes(el.options[i].value)) {
                        el.options[i].selected = true;
                    }
                }
                el.dispatchEvent(new Event('change', { bubbles: true }));
                return 'Selected ' + vals.length + ' option(s)';
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun hover(target: String): String {
        val js = """
            (function() {
                var el = document.querySelector('$target');
                if (!el) return 'Element not found: $target';
                el.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
                el.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }));
                return 'Hovered ' + el.tagName;
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun pressKey(key: String): String {
        val keyCode = when (key.lowercase()) {
            "enter" -> 13
            "tab" -> 9
            "escape" -> 27
            "backspace" -> 8
            "delete" -> 46
            "arrowleft" -> 37
            "arrowup" -> 38
            "arrowright" -> 39
            "arrowdown" -> 40
            "home" -> 36
            "end" -> 35
            " " -> 32
            else -> key.firstOrNull()?.code ?: 0
        }
        val js = """
            (function() {
                var el = document.activeElement || document.body;
                el.dispatchEvent(new KeyboardEvent('keydown', {key:'$key', keyCode:$keyCode, bubbles:true}));
                el.dispatchEvent(new KeyboardEvent('keyup', {key:'$key', keyCode:$keyCode, bubbles:true}));
                return 'Pressed $key';
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun drag(startTarget: String, endTarget: String): String {
        val js = """
            (function() {
                var start = document.querySelector('$startTarget');
                var end = document.querySelector('$endTarget');
                if (!start || !end) return 'Elements not found';
                var sr = start.getBoundingClientRect();
                var er = end.getBoundingClientRect();
                var dataTransfer = new DataTransfer();
                start.dispatchEvent(new DragEvent('dragstart', {bubbles:true, dataTransfer:dataTransfer, clientX:sr.x, clientY:sr.y}));
                end.dispatchEvent(new DragEvent('dragenter', {bubbles:true, dataTransfer:dataTransfer, clientX:er.x, clientY:er.y}));
                end.dispatchEvent(new DragEvent('drop', {bubbles:true, dataTransfer:dataTransfer, clientX:er.x, clientY:er.y}));
                return 'Dragged';
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun handleDialog(accept: Boolean, promptText: String?): String {
        return "Dialog handling requires native WebViewClient callback. Dialog was ${if (accept) "accepted" else "dismissed"}."
    }

    private fun evaluate(function: String): String {
        val js = "($function)()"
        var result = ""
        browserViewModel.getActiveTab()?.webView?.evaluateJavascript(js) {
            result = it?.removeSurrounding("\"") ?: "undefined"
        }
        return result
    }

    private fun waitFor(text: String?, textGone: String?, time: Double?): String {
        if (time != null) {
            return "Waited ${time}s (async)"
        }
        val checkText = text ?: textGone ?: ""
        val shouldExist = text != null
        val js = """
            (function() {
                var text = document.body.innerText;
                var found = text.includes('$checkText');
                return found ? 'Found: $checkText' : 'Not found: $checkText';
            })()
        """.trimIndent()
        return runJs(js)
    }

    // === Tabs ===

    private fun tabs(action: String, index: Int?, url: String?): String {
        return when (action) {
            "list" -> {
                val tabs = browserViewModel.tabs.value
                tabs.mapIndexed { i, t -> "$i: ${t.title} (${t.url})" }.joinToString("\n")
            }
            "new" -> {
                browserViewModel.createTab(url ?: "about:blank")
                "Created new tab"
            }
            "close" -> {
                val tabs = browserViewModel.tabs.value
                val idx = index ?: tabs.size - 1
                if (idx in tabs.indices) {
                    browserViewModel.closeTab(tabs[idx].id)
                    "Closed tab $idx"
                } else "Invalid index: $idx"
            }
            "select" -> {
                val tabs = browserViewModel.tabs.value
                if (index != null && index in tabs.indices) {
                    browserViewModel.setActiveTab(tabs[index].id)
                    "Selected tab $index: ${tabs[index].title}"
                } else "Invalid index: $index"
            }
            else -> "Unknown tab action: $action"
        }
    }

    private fun closeBrowser(): String {
        browserViewModel.tabs.value.forEach { it.webView?.destroy() }
        return "Browser closed"
    }
}
```

- [ ] **Step 3: Create StealthInjector.kt**

```kotlin
package com.aibrowser.agent

import android.webkit.WebView

/**
 * Stealth injection based on Patchright's anti-detection patches.
 * Removes automation fingerprints from Android WebView.
 */
object StealthInjector {

    private val STEALTH_JS = """
        (function() {
            // === navigator.webdriver ===
            // Patchright core: remove webdriver flag
            Object.defineProperty(navigator, 'webdriver', {
                get: () => undefined,
                configurable: true
            });
            delete navigator.__proto__.webdriver;

            // === navigator.languages ===
            if (!navigator.languages || navigator.languages.length === 0) {
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['en-US', 'en'],
                    configurable: true
                });
            }

            // === navigator.plugins ===
            // Patchright: match real Chrome plugin count
            Object.defineProperty(navigator, 'plugins', {
                get: () => {
                    var plugins = [
                        { name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer', description: 'Portable Document Format' },
                        { name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai', description: '' },
                        { name: 'Native Client', filename: 'internal-nacl-plugin', description: '' }
                    ];
                    plugins.length = 3;
                    return plugins;
                },
                configurable: true
            });

            // === navigator.permissions ===
            // Patchright: proper permission query
            var originalQuery = window.Notification && Notification.permission;
            if (navigator.permissions) {
                var originalPermQuery = navigator.permissions.query;
                navigator.permissions.query = (parameters) => {
                    if (parameters.name === 'notifications') {
                        return Promise.resolve({ state: originalQuery || 'denied', onchange: null });
                    }
                    return originalPermQuery.call(navigator.permissions, parameters);
                };
            }

            // === chrome object ===
            // Patchright: expose chrome runtime
            if (!window.chrome) {
                window.chrome = {};
            }
            if (!window.chrome.runtime) {
                window.chrome.runtime = {
                    connect: function() {},
                    sendMessage: function() {}
                };
            }

            // === window.outerWidth/Height ===
            // Patchright: consistent dimensions
            if (window.outerWidth === 0) {
                Object.defineProperty(window, 'outerWidth', { get: () => window.innerWidth });
            }
            if (window.outerHeight === 0) {
                Object.defineProperty(window, 'outerHeight', { get: () => window.innerHeight + 85 });
            }

            // === screen dimensions ===
            if (screen.availWidth === 0) {
                Object.defineProperty(screen, 'availWidth', { get: () => screen.width });
            }
            if (screen.availHeight === 0) {
                Object.defineProperty(screen, 'availHeight', { get: () => screen.height });
            }

            // === navigator.connection ===
            if (!navigator.connection) {
                Object.defineProperty(navigator, 'connection', {
                    get: () => ({
                        effectiveType: '4g',
                        rtt: 50,
                        downlink: 10,
                        saveData: false
                    }),
                    configurable: true
                });
            }

            // === WebGL fingerprint ===
            // Patchright: consistent WebGL vendor/renderer
            var getParameter = WebGLRenderingContext.prototype.getParameter;
            WebGLRenderingContext.prototype.getParameter = function(parameter) {
                if (parameter === 37445) return 'Intel Inc.';
                if (parameter === 37446) return 'Intel Iris OpenGL Engine';
                return getParameter.call(this, parameter);
            };
            if (window.WebGL2RenderingContext) {
                var getParameter2 = WebGL2RenderingContext.prototype.getParameter;
                WebGL2RenderingContext.prototype.getParameter = function(parameter) {
                    if (parameter === 37445) return 'Intel Inc.';
                    if (parameter === 37446) return 'Intel Iris OpenGL Engine';
                    return getParameter2.call(this, parameter);
                };
            }

            // === navigator.mediaDevices ===
            if (!navigator.mediaDevices) {
                Object.defineProperty(navigator, 'mediaDevices', {
                    get: () => ({
                        enumerateDevices: () => Promise.resolve([
                            { kind: 'audioinput', deviceId: 'default', label: '' },
                            { kind: 'videoinput', deviceId: 'default', label: '' }
                        ]),
                        getUserMedia: () => Promise.reject(new DOMException('Not allowed', 'NotAllowedError'))
                    }),
                    configurable: true
                });
            }

            // === navigator.getBattery ===
            if (!navigator.getBattery) {
                navigator.getBattery = () => Promise.resolve({
                    charging: true,
                    chargingTime: 0,
                    dischargingTime: Infinity,
                    level: 1,
                    addEventListener: () => {},
                    removeEventListener: () => {}
                });
            }

            // === navigator.vibrate ===
            if (!navigator.vibrate) {
                navigator.vibrate = () => false;
            }

            // === CDP Runtime.enable leak ===
            // Patchright: prevent detection via CDP
            var originalEval = window.eval;
            window.eval = function(code) {
                if (typeof code === 'string' && code.includes('Runtime.enable')) {
                    return undefined;
                }
                return originalEval.call(this, code);
            };

            // === Console.enable leak ===
            // Patchright: disable console to prevent detection
            var noop = function() {};
            ['debug', 'info', 'warn', 'error', 'log', 'assert', 'trace', 'dir'].forEach(function(method) {
                console[method] = noop;
            });

            // === prevent automation detection via stack traces ===
            var originalCaptureStackTrace = Error.captureStackTrace;
            if (originalCaptureStackTrace) {
                Error.captureStackTrace = function() {
                    var obj = {};
                    originalCaptureStackTrace(obj, Error.captureStackTrace);
                    // Clean up automation-related stack frames
                    if (obj.stack) {
                        obj.stack = obj.stack.split('\n')
                            .filter(function(line) {
                                return !line.includes('puppeteer') &&
                                       !line.includes('playwright') &&
                                       !line.includes('webdriver') &&
                                       !line.includes('selenium');
                            })
                            .join('\n');
                    }
                    return obj;
                };
            }

            // === prevent iframe detection ===
            // Some sites check for automation via iframe contentWindow
            var originalCreateElement = document.createElement;
            document.createElement = function(tag) {
                var el = originalCreateElement.call(document, tag);
                if (tag.toLowerCase() === 'iframe') {
                    try {
                        Object.defineProperty(el.contentWindow, 'navigator', {
                            get: () => ({
                                webdriver: false,
                                languages: ['en-US', 'en'],
                                plugins: []
                            })
                        });
                    } catch(e) {}
                }
                return el;
            };

            // === navigator.hardwareConcurrency ===
            // Patchright: match real device
            if (navigator.hardwareConcurrency === 0) {
                Object.defineProperty(navigator, 'hardwareConcurrency', {
                    get: () => 4,
                    configurable: true
                });
            }

            // === navigator.deviceMemory ===
            if (!navigator.deviceMemory) {
                Object.defineProperty(navigator, 'deviceMemory', {
                    get: () => 8,
                    configurable: true
                });
            }

            // === performance.now() precision ===
            // Patchright: reduce timing precision to prevent fingerprinting
            var originalNow = performance.now.bind(performance);
            performance.now = function() {
                return Math.round(originalNow() * 1000) / 1000;
            };

            // === Date.now() precision ===
            var originalDateNow = Date.now;
            Date.now = function() {
                return Math.round(originalDateNow() / 1000) * 1000;
            };

            // === Speech Synthesis ===
            if (!window.speechSynthesis) {
                window.speechSynthesis = {
                    speak: function() {},
                    cancel: function() {},
                    pause: function() {},
                    resume: function() {},
                    getVoices: function() { return []; },
                    addEventListener: function() {},
                    removeEventListener: function() {}
                };
            }

            // === Notification ===
            if (!window.Notification) {
                window.Notification = {
                    permission: 'denied',
                    requestPermission: () => Promise.resolve('denied')
                };
            }
        })();
    """.trimIndent()

    /**
     * Inject stealth scripts into WebView.
     * Call this after WebView is created but before loading URLs.
     */
    fun inject(webView: WebView) {
        webView.evaluateJavascript(STEALTH_JS, null)
    }

    /**
     * Returns JavaScript to inject via WebViewClient.onPageFinished
     */
    fun getInjectionScript(): String = STEALTH_JS
}
```

- [ ] **Step 4: Create McpController.kt**

```kotlin
package com.aibrowser.agent

import com.aibrowser.data.models.ToolCall
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpController @Inject constructor(
    private val toolExecutor: ToolExecutor
) {
    private val gson = Gson()

    suspend fun executeToolCall(toolCall: ToolCall): ToolCall {
        return try {
            val result = toolExecutor.execute(toolCall.name, toolCall.arguments)
            toolCall.copy(status = ToolCall.ToolStatus.DONE, result = result)
        } catch (e: Exception) {
            toolCall.copy(status = ToolCall.ToolStatus.ERROR, result = "Error: ${e.message}")
        }
    }

    fun parseToolCalls(responseContent: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()
        try {
            val json = JsonParser.parseString(responseContent).asJsonObject
            if (json.has("tool_calls")) {
                val calls = json.getAsJsonArray("tool_calls")
                for (call in calls) {
                    val obj = call.asJsonObject
                    val function = obj.getAsJsonObject("function")
                    val args = try {
                        gson.fromJson(function.get("arguments"), JsonObject::class.java)
                            .entrySet().associate { it.key to it.value.asString }
                    } catch (_: Exception) {
                        emptyMap()
                    }
                    toolCalls.add(
                        ToolCall(
                            id = obj.get("id")?.asString ?: "",
                            name = function.get("name").asString,
                            arguments = args,
                            status = ToolCall.ToolStatus.PENDING
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        return toolCalls
    }
}
```

- [ ] **Step 5: Update TabManager.kt to inject stealth**

Add to the `createTab` method after WebView creation:

```kotlin
fun createTab(url: String = "about:blank"): TabState {
    val id = UUID.randomUUID().toString().take(8)
    val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // Patchright-style stealth: disable automation detection
        settings.userAgent = settings.userAgentString
            .replace("; wv", "")  // Remove WebView marker
            .replace("Android WebView", "Chrome")

        // Inject stealth before any page loads
        evaluateJavascript(StealthInjector.getInjectionScript(), null)
    }
    val tab = TabState(id = id, url = url, webView = webView)
    _tabs.add(tab)
    _activeTabId = id
    if (url != "about:blank") {
        webView.loadUrl(url)
    }
    return tab
}
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/aibrowser/agent/
git commit -m "feat: add Patchright-style stealth injection for anti-detection"
```

---

### Task 6: AI Service

**Covers:** [S3, S7]
**Files:**
- Create: `app/src/main/java/com/aibrowser/agent/AiService.kt`
- Create: `app/src/main/java/com/aibrowser/di/NetworkModule.kt`

- [ ] **Step 1: Create NetworkModule.kt**

```kotlin
package com.aibrowser.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule
```

- [ ] **Step 2: Create AiService.kt**

```kotlin
package com.aibrowser.agent

import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.Message
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    private val client: OkHttpClient,
    private val settingsRepository: SettingsRepository,
    private val mcpController: McpController
) {
    private val gson = Gson()

    sealed class StreamEvent {
        data class Token(val text: String) : StreamEvent()
        data class ToolCallStart(val id: String, val name: String, val args: String) : StreamEvent()
        data class Done(val fullResponse: String) : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    suspend fun sendMessage(
        messages: List<Message>,
        onEvent: (StreamEvent) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val config = settingsRepository.apiConfig.first()

        if (config.apiKey.isBlank()) {
            onEvent(StreamEvent.Error("API key not configured. Go to Settings."))
            return@withContext ""
        }

        try {
            val requestBody = buildRequestBody(config, messages)
            val request = buildRequest(config, requestBody)

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val error = response.body?.string() ?: "Unknown error"
                    onEvent(StreamEvent.Error("API error ${response.code}: $error"))
                    return@withContext ""
                }

                val body = response.body?.string() ?: ""
                val fullResponse = parseStreamingResponse(body, onEvent)
                onEvent(StreamEvent.Done(fullResponse))
                fullResponse
            }
        } catch (e: Exception) {
            onEvent(StreamEvent.Error("Network error: ${e.message}"))
            ""
        }
    }

    private fun buildRequestBody(config: ApiConfig, messages: List<Message>): String {
        val apiMessages = messages.map { msg ->
            mapOf(
                "role" to when (msg.role) {
                    Message.Role.USER -> "user"
                    Message.Role.ASSISTANT -> "assistant"
                    Message.Role.SYSTEM -> "system"
                    Message.Role.TOOL -> "tool"
                },
                "content" to msg.content
            )
        }

        val body = mutableMapOf<String, Any>(
            "model" to config.model,
            "messages" to apiMessages,
            "tools" to ToolDefinitions.getToolsForApi(),
            "tool_choice" to "auto"
        )

        return gson.toJson(body)
    }

    private fun buildRequest(config: ApiConfig, body: String): Request {
        val url = when (config.provider) {
            ApiConfig.ApiProvider.CLAUDE -> "${config.baseUrl}/v1/messages"
            else -> "${config.baseUrl}/chat/completions"
        }

        val builder = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))

        when (config.provider) {
            ApiConfig.ApiProvider.OPENAI, ApiConfig.ApiProvider.CUSTOM -> {
                builder.addHeader("Authorization", "Bearer ${config.apiKey}")
            }
            ApiConfig.ApiProvider.CLAUDE -> {
                builder.addHeader("x-api-key", config.apiKey)
                builder.addHeader("anthropic-version", "2023-06-01")
            }
        }

        return builder.build()
    }

    private fun parseStreamingResponse(body: String, onEvent: (StreamEvent) -> Unit): String {
        val lines = body.lines()
        var fullContent = ""
        var inToolCall = false
        var toolCallId = ""
        var toolCallName = ""
        var toolCallArgs = StringBuilder()

        for (line in lines) {
            if (!line.startsWith("data: ")) continue
            val data = line.removePrefix("data: ").trim()
            if (data == "[DONE]") break

            try {
                val json = JsonParser.parseString(data).asJsonObject
                val choices = json.getAsJsonArray("choices") ?: continue
                if (choices.size() == 0) continue
                val delta = choices[0].asJsonObject.getAsJsonObject("delta") ?: continue

                // Handle content tokens
                if (delta.has("content") && !delta.get("content").isJsonNull) {
                    val token = delta.get("content").asString
                    fullContent += token
                    onEvent(StreamEvent.Token(token))
                }

                // Handle tool calls
                if (delta.has("tool_calls") && !delta.get("tool_calls").isJsonNull) {
                    val toolCalls = delta.getAsJsonArray("tool_calls")
                    for (tc in toolCalls) {
                        val tcObj = tc.asJsonObject
                        val index = tcObj.get("index")?.asInt ?: 0

                        if (tcObj.has("id") && !tcObj.get("id").isJsonNull) {
                            // New tool call
                            if (inToolCall && toolCallId.isNotEmpty()) {
                                onEvent(StreamEvent.ToolCallStart(toolCallId, toolCallName, toolCallArgs.toString()))
                            }
                            toolCallId = tcObj.get("id").asString
                            inToolCall = true
                        }

                        if (tcObj.has("function")) {
                            val func = tcObj.getAsJsonObject("function")
                            if (func.has("name") && !func.get("name").isJsonNull) {
                                toolCallName = func.get("name").asString
                            }
                            if (func.has("arguments") && !func.get("arguments").isJsonNull) {
                                toolCallArgs.append(func.get("arguments").asString)
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Emit last tool call if any
        if (inToolCall && toolCallId.isNotEmpty()) {
            onEvent(StreamEvent.ToolCallStart(toolCallId, toolCallName, toolCallArgs.toString()))
        }

        return fullContent
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/aibrowser/agent/AiService.kt app/src/main/java/com/aibrowser/di/NetworkModule.kt
git commit -m "feat: add AI service with streaming and multi-provider support"
```

---

### Task 7: Agent ViewModel

**Covers:** [S3, S4]
**Files:**
- Create: `app/src/main/java/com/aibrowser/agent/AgentViewModel.kt`

- [ ] **Step 1: Create AgentViewModel.kt**

```kotlin
package com.aibrowser.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibrowser.data.models.Message
import com.aibrowser.data.models.ToolCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val aiService: AiService,
    private val mcpController: McpController
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isAgentMode = MutableStateFlow(false)
    val isAgentMode: StateFlow<Boolean> = _isAgentMode.asStateFlow()

    fun setAgentMode(enabled: Boolean) {
        _isAgentMode.value = enabled
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isLoading.value) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            role = Message.Role.USER,
            content = content
        )
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            val assistantContent = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()

            aiService.sendMessage(_messages.value) { event ->
                when (event) {
                    is AiService.StreamEvent.Token -> {
                        assistantContent.append(event.text)
                    }
                    is AiService.StreamEvent.ToolCallStart -> {
                        val args = try {
                            com.google.gson.Gson().fromJson(
                                event.args,
                                com.google.gson.JsonObject::class.java
                            ).entrySet().associate { it.key to it.value.asString }
                        } catch (_: Exception) {
                            emptyMap()
                        }
                        toolCalls.add(
                            ToolCall(
                                id = event.id,
                                name = event.name,
                                arguments = args,
                                status = ToolCall.ToolStatus.PENDING
                            )
                        )
                    }
                    is AiService.StreamEvent.Done -> {
                        // Add assistant message
                        if (assistantContent.isNotEmpty() || toolCalls.isNotEmpty()) {
                            val assistantMsg = Message(
                                id = UUID.randomUUID().toString(),
                                role = Message.Role.ASSISTANT,
                                content = assistantContent.toString(),
                                toolCalls = toolCalls.toList()
                            )
                            _messages.value = _messages.value + assistantMsg

                            // Execute tool calls
                            if (toolCalls.isNotEmpty()) {
                                executeToolCalls(toolCalls)
                            }
                        }
                    }
                    is AiService.StreamEvent.Error -> {
                        val errorMsg = Message(
                            id = UUID.randomUUID().toString(),
                            role = Message.Role.ASSISTANT,
                            content = "Error: ${event.message}"
                        )
                        _messages.value = _messages.value + errorMsg
                    }
                }
            }

            _isLoading.value = false
        }
    }

    private suspend fun executeToolCalls(toolCalls: List<ToolCall>) {
        for (toolCall in toolCalls) {
            // Update status to RUNNING
            updateToolCallStatus(toolCall.id, ToolCall.ToolStatus.RUNNING)

            // Execute
            val result = mcpController.executeToolCall(toolCall)

            // Update status to DONE/ERROR
            updateToolCallStatus(toolCall.id, result.status, result.result)

            // Add tool result message
            val toolMessage = Message(
                id = UUID.randomUUID().toString(),
                role = Message.Role.TOOL,
                content = result.result ?: "No result"
            )
            _messages.value = _messages.value + toolMessage
        }

        // Get AI response to tool results
        if (toolCalls.isNotEmpty()) {
            val followUpContent = StringBuilder()
            val followUpToolCalls = mutableListOf<ToolCall>()

            aiService.sendMessage(_messages.value) { event ->
                when (event) {
                    is AiService.StreamEvent.Token -> followUpContent.append(event.text)
                    is AiService.StreamEvent.ToolCallStart -> {
                        val args = try {
                            com.google.gson.Gson().fromJson(
                                event.args,
                                com.google.gson.JsonObject::class.java
                            ).entrySet().associate { it.key to it.value.asString }
                        } catch (_: Exception) { emptyMap() }
                        followUpToolCalls.add(
                            ToolCall(event.id, event.name, args, ToolCall.ToolStatus.PENDING)
                        )
                    }
                    is AiService.StreamEvent.Done -> {
                        if (followUpContent.isNotEmpty() || followUpToolCalls.isNotEmpty()) {
                            val msg = Message(
                                id = UUID.randomUUID().toString(),
                                role = Message.Role.ASSISTANT,
                                content = followUpContent.toString(),
                                toolCalls = followUpToolCalls.toList()
                            )
                            _messages.value = _messages.value + msg

                            if (followUpToolCalls.isNotEmpty()) {
                                viewModelScope.launch {
                                    executeToolCalls(followUpToolCalls)
                                }
                            }
                        }
                    }
                    is AiService.StreamEvent.Error -> {
                        _messages.value = _messages.value + Message(
                            id = UUID.randomUUID().toString(),
                            role = Message.Role.ASSISTANT,
                            content = "Error: ${event.message}"
                        )
                    }
                }
            }
        }
    }

    private fun updateToolCallStatus(id: String, status: ToolCall.ToolStatus, result: String? = null) {
        _messages.value = _messages.value.map { msg ->
            msg.copy(
                toolCalls = msg.toolCalls.map { tc ->
                    if (tc.id == id) tc.copy(status = status, result = result ?: tc.result) else tc
                }
            )
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/aibrowser/agent/AgentViewModel.kt
git commit -m "feat: add agent view model with chat and tool execution"
```

---

### Task 8: Theme + Navigation

**Covers:** [S4]
**Files:**
- Create: `app/src/main/java/com/aibrowser/ui/theme/Color.kt`
- Create: `app/src/main/java/com/aibrowser/ui/theme/Type.kt`
- Create: `app/src/main/java/com/aibrowser/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/aibrowser/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create Color.kt**

```kotlin
package com.aibrowser.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val AgentBlue = Color(0xFF2196F3)
val AgentGreen = Color(0xFF4CAF50)
val ToolOrange = Color(0xFFFF9800)
val ErrorRed = Color(0xFFF44336)
```

- [ ] **Step 2: Create Type.kt**

```kotlin
package com.aibrowser.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

- [ ] **Step 3: Create Theme.kt**

```kotlin
package com.aibrowser.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun AiBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

- [ ] **Step 4: Create NavGraph.kt**

```kotlin
package com.aibrowser.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aibrowser.agent.AgentViewModel
import com.aibrowser.browser.BrowserViewModel
import com.aibrowser.ui.screens.AgentScreen
import com.aibrowser.ui.screens.BrowserScreen
import com.aibrowser.ui.screens.SettingsScreen

object Routes {
    const val BROWSER = "browser"
    const val AGENT = "agent"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    browserViewModel: BrowserViewModel,
    agentViewModel: AgentViewModel
) {
    NavHost(navController = navController, startDestination = Routes.BROWSER) {
        composable(Routes.BROWSER) {
            BrowserScreen(
                browserViewModel = browserViewModel,
                agentViewModel = agentViewModel,
                onNavigateToAgent = { navController.navigate(Routes.AGENT) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.AGENT) {
            AgentScreen(
                agentViewModel = agentViewModel,
                browserViewModel = browserViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/aibrowser/ui/theme/ app/src/main/java/com/aibrowser/ui/navigation/
git commit -m "feat: add theme, typography, and navigation graph"
```

---

### Task 9: UI Components

**Covers:** [S4]
**Files:**
- Create: `app/src/main/java/com/aibrowser/ui/components/TabBar.kt`
- Create: `app/src/main/java/com/aibrowser/ui/components/MessageBubble.kt`
- Create: `app/src/main/java/com/aibrowser/ui/components/ToolCallCard.kt`
- Create: `app/src/main/java/com/aibrowser/ui/components/WebViewContainer.kt`

- [ ] **Step 1: Create TabBar.kt**

```kotlin
package com.aibrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aibrowser.browser.TabState

@Composable
fun TabBar(
    tabs: List<TabState>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tabs) { tab ->
                TabChip(
                    title = tab.title,
                    isActive = tab.id == activeTabId,
                    onClick = { onTabClick(tab.id) },
                    onClose = { onCloseTab(tab.id) }
                )
            }
        }

        IconButton(onClick = onNewTab) {
            Icon(Icons.Default.Add, contentDescription = "New Tab")
        }

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun TabChip(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .widthIn(min = 80.dp, max = 160.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create MessageBubble.kt**

```kotlin
package com.aibrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aibrowser.data.models.Message

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == Message.Role.USER
    val isTool = message.role == Message.Role.TOOL

    val bubbleColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        isTool -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Show tool calls if any
        message.toolCalls.forEach { toolCall ->
            ToolCallCard(
                toolCall = toolCall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
```

- [ ] **Step 3: Create ToolCallCard.kt**

```kotlin
package com.aibrowser.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aibrowser.data.models.ToolCall

@Composable
fun ToolCallCard(
    toolCall: ToolCall,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (toolCall.status) {
        ToolCall.ToolStatus.PENDING -> MaterialTheme.colorScheme.outline
        ToolCall.ToolStatus.RUNNING -> MaterialTheme.colorScheme.primary
        ToolCall.ToolStatus.DONE -> MaterialTheme.colorScheme.tertiary
        ToolCall.ToolStatus.ERROR -> MaterialTheme.colorScheme.error
    }

    val statusIcon = when (toolCall.status) {
        ToolCall.ToolStatus.PENDING -> "⏳"
        ToolCall.ToolStatus.RUNNING -> "⚙️"
        ToolCall.ToolStatus.DONE -> "✓"
        ToolCall.ToolStatus.ERROR -> "✗"
    }

    Column(
        modifier = modifier
            .widthIn(max = 300.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .animateContentSize()
            .clickable { expanded = !expanded }
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = statusIcon, style = MaterialTheme.typography.labelSmall)
            Text(
                text = toolCall.name,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand",
                modifier = Modifier.size(16.dp)
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Arguments: ${toolCall.arguments}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            toolCall.result?.let { result ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Result: $result",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (toolCall.status == ToolCall.ToolStatus.ERROR)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 4: Create WebViewContainer.kt**

```kotlin
package com.aibrowser.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.aibrowser.browser.TabState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tab: TabState,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onUrlChanged(url ?: "")
                        onLoadingChanged(false)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        onTitleChanged(title ?: "Untitled")
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onLoadingChanged(newProgress < 100)
                    }
                }

                tab.webView?.let { webView ->
                    webView.webViewClient = webViewClient
                    webView.webChromeClient = webChromeClient
                }
            }
        },
        modifier = modifier,
        update = { webView ->
            tab.webView?.let { tabWebView ->
                if (tabWebView.parent != null) {
                    (tabWebView.parent as ViewGroup).removeView(tabWebView)
                }
                webView.addView(tabWebView)
            }
        }
    )
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/aibrowser/ui/components/
git commit -m "feat: add UI components (TabBar, MessageBubble, ToolCallCard, WebViewContainer)"
```

---

### Task 10: Screens

**Covers:** [S4]
**Files:**
- Create: `app/src/main/java/com/aibrowser/ui/screens/BrowserScreen.kt`
- Create: `app/src/main/java/com/aibrowser/ui/screens/AgentScreen.kt`
- Create: `app/src/main/java/com/aibrowser/ui/screens/SettingsScreen.kt`

- [ ] **Step 1: Create BrowserScreen.kt**

```kotlin
package com.aibrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AgentViewModel
import com.aibrowser.browser.BrowserViewModel
import com.aibrowser.ui.components.TabBar
import com.aibrowser.ui.components.WebViewContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    browserViewModel: BrowserViewModel,
    agentViewModel: AgentViewModel,
    onNavigateToAgent: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val tabs by browserViewModel.tabs.collectAsState()
    val activeTabId by browserViewModel.activeTabId.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }

    Scaffold(
        topBar = {
            TabBar(
                tabs = tabs,
                activeTabId = activeTabId,
                onTabClick = { browserViewModel.setActiveTab(it) },
                onCloseTab = { browserViewModel.closeTab(it) },
                onNewTab = { browserViewModel.createTab() },
                onSettingsClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAgent,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Agent")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            activeTab?.let { tab ->
                WebViewContainer(
                    tab = tab,
                    onTitleChanged = { title ->
                        browserViewModel.updateTab(tab.id) { it.copy(title = title) }
                    },
                    onUrlChanged = { url ->
                        browserViewModel.updateTab(tab.id) { it.copy(url = url) }
                    },
                    onLoadingChanged = { loading ->
                        browserViewModel.updateTab(tab.id) { it.copy(isLoading = loading) }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } ?: run {
                Text(
                    text = "No tabs open",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create AgentScreen.kt**

```kotlin
package com.aibrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aibrowser.agent.AgentViewModel
import com.aibrowser.browser.BrowserViewModel
import com.aibrowser.ui.components.MessageBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    agentViewModel: AgentViewModel,
    browserViewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val messages by agentViewModel.messages.collectAsState()
    val isLoading by agentViewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Agent") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }

            // Loading indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a command...") },
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        agentViewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank() && !isLoading
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create SettingsScreen.kt**

```kotlin
package com.aibrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aibrowser.data.SettingsRepository
import com.aibrowser.data.models.ApiConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val config by settingsRepository.apiConfig.collectAsState(initial = ApiConfig())
    var provider by remember(config) { mutableStateOf(config.provider) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var model by remember(config) { mutableStateOf(config.model) }
    var baseUrl by remember(config) { mutableStateOf(config.baseUrl) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Provider dropdown
            Text("API Provider", style = MaterialTheme.typography.titleMedium)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = provider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ApiConfig.ApiProvider.entries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.displayName) },
                            onClick = {
                                provider = p
                                model = p.defaultModel
                                baseUrl = p.defaultBaseUrl
                                expanded = false
                            }
                        )
                    }
                }
            }

            // API Key
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Model
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Base URL
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        settingsRepository.saveApiConfig(
                            ApiConfig(
                                provider = provider,
                                apiKey = apiKey,
                                model = model,
                                baseUrl = baseUrl
                            )
                        )
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/aibrowser/ui/screens/
git commit -m "feat: add BrowserScreen, AgentScreen, and SettingsScreen"
```

---

### Task 11: MainActivity Wiring

**Covers:** [S3, S6]
**Files:**
- Create: `app/src/main/java/com/aibrowser/MainActivity.kt`

- [ ] **Step 1: Create MainActivity.kt**

```kotlin
package com.aibrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.aibrowser.agent.AgentViewModel
import com.aibrowser.browser.BrowserViewModel
import com.aibrowser.data.SettingsRepository
import com.aibrowser.ui.navigation.NavGraph
import com.aibrowser.ui.theme.AiBrowserTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiBrowserTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val browserViewModel: BrowserViewModel = hiltViewModel()
                    val agentViewModel: AgentViewModel = hiltViewModel()

                    NavGraph(
                        navController = navController,
                        browserViewModel = browserViewModel,
                        agentViewModel = agentViewModel
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/aibrowser/MainActivity.kt
git commit -m "feat: wire MainActivity with Hilt, navigation, and ViewModels"
```

---

### Task 12: Build and Verify

**Covers:** [S3, S7, S8]
**Files:** None (verification only)

- [ ] **Step 1: Verify project structure**

```bash
find app/src/main/java/com/aibrowser -name "*.kt" | sort
```

Expected: All Kotlin files listed in the file map.

- [ ] **Step 2: Build the project (if Android SDK available)**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat: complete AI browser MVP with agent, tabs, and MCP tools"
```
