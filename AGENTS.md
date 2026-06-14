# AiBrowser - Android App

## Build

```bash
ANDROID_HOME=$HOME/android-sdk gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Build Environment (Termux on aarch64/arm64)

### Prerequisites

| Package | Install |
|---------|---------|
| JDK 21 | `pkg install openjdk-21` |
| Gradle | `pkg install gradle` (or use the wrapper) |
| Android SDK | `$HOME/android-sdk` with platforms 33-36, build-tools 34.0.4+ |
| aapt2 | Comes with build-tools; also via `pkg install aapt2` (aarch64 native) |

Set `local.properties`:
```
sdk.dir=/data/data/com.termux/files/home/android-sdk
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

### Common Build Failures & Fixes

**1. `Syntax error: "(" unexpected` / `Daemon startup failed` on aapt2**

-> aapt2 is x86-64 and can't run on aarch64. Fix: set `android.aapt2FromMavenOverride` to an aarch64 aapt2 (see above).

**2. `HiltJavaCompile: Injection of an @HiltViewModel class is prohibited`**

-> An `@HiltViewModel` cannot be injected as a dependency (it must be created via `ViewModelProvider`). Find the class that's injecting a ViewModel and make it inject the underlying service/manager instead.

```
// BAD: @Singleton class Foo @Inject constructor(val vm: BrowserViewModel)
// GOOD: @Singleton class Foo @Inject constructor(val tabManager: TabManager)
```

**3. `Unresolved reference 'Xxx'` for Material Icons**

-> Some icons (e.g. `Chat`) are only in `material-icons-extended`. Add the dependency:

```kotlin
implementation("androidx.compose.material:material-icons-extended")
```

**4. `Unresolved reference 'HttpLoggingInterceptorLevel'`**

-> Correct import is `HttpLoggingInterceptor.Level.BODY`, not a separate class.

**5. `Unresolved reference 'userAgent'` in WebView settings**

-> Property is `settings.userAgentString`, not `settings.userAgent`.

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

If you hit aapt2 transform cache corruption, clear the cached aapt2 transforms and rebuild:

```bash
rm -rf ~/.gradle/caches/*/transforms/*aapt*
gradle assembleDebug --no-build-cache
```
