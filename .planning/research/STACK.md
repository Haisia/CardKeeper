# Technology Stack

**Project:** CardKeeper — Android Business Card Management App
**Researched:** 2026-03-24
**Overall confidence:** MEDIUM-HIGH (core Jetpack stack HIGH via official docs; ML Kit and Coil versions MEDIUM from training data cross-checked against release patterns)

---

## Recommended Stack

### Language & Build Toolchain

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Kotlin | 2.3.20 | Primary language | Official Android language; Compose requires Kotlin; 2.3.x is current stable (released March 2026) | HIGH |
| Android Gradle Plugin (AGP) | 9.1.0 | Build system | Latest stable (March 2026); required for full API 36 support | HIGH |
| KSP (Kotlin Symbol Processing) | 2.3.20-1.0.31 | Code generation processor | Replaces KAPT for Room + Hilt; faster incremental builds; required for Room 2.7+ KMP mode | MEDIUM — verify KSP release matching Kotlin 2.3.20 at github.com/google/ksp/releases |
| Min SDK | API 26 (Android 8.0) | Target floor | Project constraint; covers ~97% of active devices as of 2025 | HIGH |
| Target/Compile SDK | API 36 | Target ceiling | Match AGP 9.1.0 default; required for Play Store compliance | HIGH |

### UI Framework

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Jetpack Compose BOM | 2026.03.00 | Compose version management | BOM pins all Compose libraries to compatible versions; avoids version mismatch bugs | HIGH |
| androidx.compose.ui | 1.10.5 (via BOM) | Core UI primitives | Current stable; all Compose artifacts move in lockstep on this BOM | HIGH |
| androidx.compose.material3 | 1.4.0 (via BOM) | Material Design 3 components | Official Google design system; provides Card, SearchBar, Chip (for tags), FAB, TopAppBar | HIGH |
| androidx.compose.foundation | 1.10.5 (via BOM) | Layout primitives (LazyColumn, etc.) | Required for card list view | HIGH |
| androidx.compose.ui:ui-tooling | 1.10.5 (via BOM) | Preview support in IDE | Debug only; essential for design iteration | HIGH |

**Why Compose over Views:** Project constraint specifies Compose. It is the current Android UI standard. Faster to build list+detail+form screens than XML Views + ViewBinding.

### Navigation

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| androidx.navigation:navigation-compose | 2.9.7 | Screen routing | Type-safe navigation with `@Serializable` routes (since 2.8.0); no string-based route bugs; replaces old string-argument pattern | HIGH |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 2.0.x | Route argument serialization | Required by Navigation 2.8+ type-safe API | MEDIUM — confirm latest 2.x release |

**Why type-safe nav:** Navigation 2.9 makes routes compile-time verified objects. For a multi-screen app (list → detail → scan → edit), this eliminates runtime crashes from mismatched argument keys.

### Local Database

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| androidx.room:room-runtime | 2.8.4 | SQLite ORM | Project constraint; handles card entities, tags, many-to-many join table | HIGH |
| androidx.room:room-ktx | 2.8.4 | Kotlin coroutine extensions | `Flow<List<Card>>` queries for reactive UI updates | HIGH |
| androidx.room:room-compiler (KSP) | 2.8.4 | Code generation | Use KSP (not KAPT); Room 2.7+ defaults to KSP + Kotlin code generation | HIGH |

**Schema design note:** Tags require a many-to-many relationship (Card ↔ Tag via CardTagCrossRef). Room handles this natively with `@Junction`.

### Camera

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| androidx.camera:camera-camera2 | 1.5.3 | Camera hardware access | Includes camera-core; CameraX is the modern AndroidX camera API — handles lifecycle, permissions, device compatibility | HIGH |
| androidx.camera:camera-lifecycle | 1.5.3 | Lifecycle binding | Binds camera to LifecycleOwner to prevent leaks | HIGH |
| androidx.camera:camera-view | 1.5.3 | PreviewView composable integration | `PreviewView` in a `AndroidView` wrapper for Compose; handles surface orientation | HIGH |

**Why CameraX over Camera2 directly:** CameraX wraps Camera2 and handles device quirks, rotation, and lifecycle management automatically. Direct Camera2 requires hundreds of lines of boilerplate.

**Why no camera-mlkit-vision:** The `camera-mlkit-vision` artifact integrates ML Kit directly into the CameraX pipeline for real-time analysis. For business card scanning, real-time analysis is not required — capture once, analyze on demand. Using ImageCapture + ML Kit TextRecognizer directly is simpler and avoids streaming overhead.

### OCR / Machine Learning

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| com.google.mlkit:text-recognition | 16.0.1 | Latin text OCR (on-device) | Project constraint: on-device, no API key, free. v2 API with block/line/element hierarchy | MEDIUM — verify at google.github.io/mlkit; training data shows 16.0.1 as latest Latin model |
| com.google.mlkit:text-recognition-common | (transitive) | Base OCR types | Included transitively by text-recognition | HIGH |

**Bundled vs. Play Services model:**
- `com.google.mlkit:text-recognition` — **bundled model** in the APK. Use this.
- `com.google.android.gms:play-services-mlkit-text-recognition` — downloads model via Play Services. Avoid: requires internet on first use, adds latency, complicates offline guarantee.

**Why text-recognition (Latin only) not text-recognition-chinese/korean/devanagari:** Business cards in most target locales use Latin script. If Korean/Japanese card support is needed later, add the script-specific artifact separately. Starting with Latin keeps APK size down (~4MB vs. 20MB+ for all scripts).

### Image Loading

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| io.coil-kt.coil3:coil-compose | 3.1.0 | Display saved card photos in Compose | Native Compose `AsyncImage` API; handles file:// URI loading from internal storage; disk cache built-in | MEDIUM — Coil 3.x is the current major; verify exact patch at github.com/coil-kt/coil/releases |

**Why Coil over Glide:** Glide predates Compose and requires a custom Compose integration adapter. Coil 3 was built Compose-first, uses Kotlin Coroutines natively, and has a simpler API for `AsyncImage`. For a Compose-only app, Coil is the correct choice.

**Why Coil over Picasso:** Picasso is Java-based, maintained in maintenance mode, has no native Compose support.

### Dependency Injection

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| com.google.dagger:hilt-android | 2.57.1 | DI container | Project constraint direction; standard Android DI; integrates with ViewModel, WorkManager, Navigation | HIGH |
| com.google.dagger:hilt-android-compiler (KSP) | 2.57.1 | Code generation | Use KSP path (`hilt-android-compiler` with KSP plugin, not KAPT) | HIGH |
| androidx.hilt:hilt-navigation-compose | 1.3.0 | `hiltViewModel()` in Compose | Required to inject ViewModels in Compose nav destinations | HIGH |

**Why Hilt over Koin:** For a greenfield Android app with Compose + Room + ViewModel, Hilt provides compile-time verification (missing bindings fail at build time, not runtime). Koin is runtime DI. Hilt is also the Google-recommended approach and has first-party AndroidX integrations.

### Asynchronous Programming

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.10.x | Structured concurrency | Room Flow, CameraX ImageCapture, ML Kit all have coroutine-first APIs | MEDIUM — 1.10.x series; verify at github.com/Kotlin/kotlinx.coroutines/releases |

Coroutines are a Kotlin standard, not a third-party choice. No alternative needed.

### Contacts Export

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Android ContactsContract API | (platform) | Export parsed card data to system contacts | Platform API, no dependency needed. Use `Intent(ContactsContract.Intents.Insert.ACTION)` with extras — launches system contacts app | HIGH |

No library needed. The platform Intent approach requires no permissions and delegates the UX to the system contacts app.

---

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| UI | Jetpack Compose | Android Views (XML) | Project constraint; Views require more boilerplate, no type-safe binding without ViewBinding; Compose is current standard |
| DI | Hilt | Koin | Koin is runtime DI (runtime crashes on missing bindings); Hilt is compile-time verified |
| DI | Hilt | Manual DI / no DI | Fine for tiny apps, but ViewModel + Room + ML Kit dependency graph benefits from injection |
| Image Loading | Coil 3 | Glide | Glide requires `GlideApp` Compose adapter; not Compose-native; more complex for file:// URIs |
| Image Loading | Coil 3 | Picasso | Java-based, no Compose support, maintenance mode |
| Database | Room | SQLDelight | SQLDelight is excellent for KMP, but this is Android-only; Room has tighter ViewModel/Flow integration |
| Database | Room | Realm / ObjectBox | Third-party; overkill for a simple card+tag schema; adds APK size |
| OCR | ML Kit on-device | Google Cloud Vision API | Requires internet, API key, cost per call — contradicts project constraints |
| OCR | ML Kit on-device | Tesseract (tess-four) | Inferior accuracy vs. ML Kit on Latin text; poor Kotlin integration; larger binary |
| Camera | CameraX | Camera2 direct | Hundreds of lines of boilerplate for device compatibility; CameraX wraps this correctly |
| Code Gen | KSP | KAPT | KAPT is deprecated in favor of KSP; KAPT requires Java stub generation, slower builds |
| Navigation | Navigation Compose 2.9 (type-safe) | Navigation Compose (string routes) | String routes cause runtime crashes from typos; type-safe routes are compile-time verified |

---

## Build Configuration Summary

```kotlin
// build.gradle.kts (project level)
plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("com.google.devtools.ksp") version "2.3.20-1.0.31" apply false  // verify version
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
}

// build.gradle.kts (app level)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        targetSdk = 36
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:2.0.0")  // verify version

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Room
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // CameraX
    val cameraVersion = "1.5.3"
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")

    // ML Kit OCR (bundled, on-device Latin)
    implementation("com.google.mlkit:text-recognition:16.0.1")  // verify version

    // Image loading
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")  // verify version

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-android-compiler:2.57.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")  // verify version
}
```

---

## Version Verification Checklist

Before starting implementation, verify these versions that could not be confirmed via official docs in this research session:

| Library | Reported Version | Verify At | Confidence |
|---------|-----------------|-----------|------------|
| KSP | 2.3.20-1.0.31 | github.com/google/ksp/releases | MEDIUM |
| ML Kit text-recognition | 16.0.1 | developers.google.com/ml-kit/android/text-recognition or goo.gle/mlkit-text-recognition | MEDIUM |
| Coil 3 coil-compose | 3.1.0 | github.com/coil-kt/coil/releases | MEDIUM |
| kotlinx-coroutines-android | 1.10.1 | github.com/Kotlin/kotlinx.coroutines/releases | MEDIUM |
| kotlinx-serialization-json | 2.0.0 | github.com/Kotlin/kotlinx.serialization/releases | MEDIUM |

All Jetpack libraries (Compose BOM, Room, CameraX, Navigation, Hilt AndroidX, Lifecycle) are HIGH confidence — verified via official developer.android.com release pages.

---

## What NOT to Use

| Library | Reason to Avoid |
|---------|----------------|
| `com.google.android.gms:play-services-mlkit-text-recognition` | Downloads model via Play Services; requires internet on first use; violates offline guarantee |
| KAPT | Deprecated; replaced by KSP; slower build times |
| `androidx.navigation:navigation-safe-args-gradle-plugin` | Legacy string-based safe args; replaced by Navigation 2.8+ type-safe Serializable routes |
| Firebase ML | Requires Firebase project setup, google-services.json; overkill for on-device-only OCR |
| Cloud Vision API | Internet required, per-call billing — contradicts project constraints |
| Any cloud sync library (Firebase Firestore, Supabase, etc.) | Out of scope per PROJECT.md |
| `io.coil-kt:coil-compose` (Coil 2.x) | Superseded by Coil 3.x (`io.coil-kt.coil3` group ID); Coil 2 not actively developed |

---

## Sources

- Android Jetpack Stable Releases: https://developer.android.com/jetpack/androidx/versions/stable-channel (accessed 2026-03-24) — HIGH confidence
- Compose BOM mapping: https://developer.android.com/jetpack/compose/bom/bom-mapping (accessed 2026-03-24) — HIGH confidence
- Room release notes: https://developer.android.com/jetpack/androidx/releases/room (accessed 2026-03-24) — HIGH confidence
- CameraX release notes: https://developer.android.com/jetpack/androidx/releases/camera (accessed 2026-03-24) — HIGH confidence
- Navigation release notes: https://developer.android.com/jetpack/androidx/releases/navigation (accessed 2026-03-24) — HIGH confidence
- Lifecycle release notes: https://developer.android.com/jetpack/androidx/releases/lifecycle (accessed 2026-03-24) — HIGH confidence
- Hilt setup guide: https://developer.android.com/training/dependency-injection/hilt-android (accessed 2026-03-24) — HIGH confidence
- Kotlin release notes: https://kotlinlang.org/docs/releases.html (accessed 2026-03-24) — HIGH confidence
- AGP release notes: https://developer.android.com/build/releases/gradle-plugin (accessed 2026-03-24) — HIGH confidence
- ML Kit text-recognition: Training data (version 16.0.1 for Latin bundled model) — MEDIUM confidence, needs verification
- Coil 3.x: Training data (version 3.1.0) — MEDIUM confidence, needs verification
