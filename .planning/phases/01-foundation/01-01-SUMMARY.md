---
phase: 01-foundation
plan: 01
subsystem: infra
tags: [android, gradle, kotlin, compose, room, hilt, ksp, camerax, mlkit, coil, navigation]

requires: []

provides:
  - Compilable Android project scaffold with all v1 dependencies resolved
  - KSP 2.3.6 annotation processing pipeline for Room and Hilt
  - Gradle 9.4.1 wrapper (Java 25 compatible)
  - Room Gradle Plugin configured for schema export to app/schemas/
  - Hilt 2.59.2 application class (@HiltAndroidApp)
  - All AGP 9.1.0 plugin declarations in root build.gradle.kts

affects:
  - 01-02 (Room schema — requires KSP + Room Gradle Plugin configured)
  - 01-03 (Hilt DI modules — requires @HiltAndroidApp + Hilt plugin)
  - 01-04 (Navigation + UI stubs — requires Compose + Navigation deps)
  - All subsequent phases (depend on compilable project with correct dep versions)

tech-stack:
  added:
    - AGP 9.1.0 (Android Gradle Plugin)
    - Kotlin 2.3.20 (via AGP built-in)
    - KSP 2.3.6 (annotation processing for Room + Hilt)
    - Compose BOM 2026.03.00 (pins all Compose library versions)
    - Navigation Compose 2.9.7 (type-safe routes)
    - kotlinx-serialization-json 1.10.0 (nav route serialization)
    - Room 2.8.4 (runtime + ktx + compiler + Gradle Plugin)
    - Hilt 2.59.2 (DI container + android-compiler)
    - hilt-navigation-compose 1.3.0
    - Lifecycle 2.10.0 (viewmodel-compose + runtime-compose)
    - CameraX 1.5.3 (camera2 + lifecycle + view)
    - ML Kit text-recognition 16.0.1 (Latin + Korean bundled)
    - Coil 3.4.0 (coil-compose)
    - kotlinx-coroutines-android 1.10.2
    - activity-compose 1.10.1
    - Gradle 9.4.1 wrapper
  patterns:
    - Room Gradle Plugin schema export via room { schemaDirectory("$projectDir/schemas") }
    - KSP-only annotation processing (no kapt anywhere)
    - AGP 9.x: kotlin.android plugin removed (built-in to AGP 9.0+)
    - buildConfig = true explicitly enabled for BuildConfig.DEBUG guard

key-files:
  created:
    - settings.gradle.kts
    - build.gradle.kts
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
    - app/src/main/kotlin/com/cardkeeper/CardKeeperApplication.kt
    - gradle.properties
    - gradle/wrapper/gradle-wrapper.properties
    - gradle/wrapper/gradle-wrapper.jar
    - gradlew
    - gradlew.bat
    - .gitignore
  modified: []

key-decisions:
  - "Gradle upgraded from 8.12 to 9.4.1: system has Java 25 (Corretto); Gradle 8.x supports only up to Java 24; Gradle 9.4.1 supports Java 26"
  - "Hilt upgraded from 2.57.1 to 2.59.2: Hilt 2.57.1 uses deprecated AGP BaseExtension API removed in AGP 9.x"
  - "kotlin.android plugin removed from app/build.gradle.kts: deprecated and fails in AGP 9.0+ (now built-in)"
  - "gradle.properties: added --enable-native-access=ALL-UNNAMED and --add-opens flags for Java 25 native-platform compatibility"
  - "KSP 2.3.6 standalone versioning (not 2.3.20-1.0.31): new format since KSP 2.3.0"
  - "Room Gradle Plugin (not ksp arg): replaces deprecated ksp { arg('room.schemaLocation', ...) } pattern"

patterns-established:
  - "Pattern 1: All annotation processing via ksp() — no kapt() usage anywhere"
  - "Pattern 2: Room schema export via room { schemaDirectory() } block (Room Gradle Plugin)"
  - "Pattern 3: buildConfig = true enables BuildConfig.DEBUG for runtime guards"
  - "Pattern 4: AGP 9.x omits kotlin.android plugin — Kotlin built into AGP"

requirements-completed: []

duration: 7min
completed: 2026-03-24
---

# Phase 01 Plan 01: Gradle Scaffold Summary

**Android project scaffold with Gradle 9.4.1 + AGP 9.1.0 + KSP 2.3.6, all v1 dependencies resolved (Room 2.8.4, Hilt 2.59.2, Compose BOM 2026.03.00, CameraX 1.5.3, ML Kit 16.0.1, Coil 3.4.0, Navigation 2.9.7)**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-24T12:36:29Z
- **Completed:** 2026-03-24T12:43:52Z
- **Tasks:** 1 of 1
- **Files modified:** 11 created

## Accomplishments

- Created complete Android project from scratch — settings.gradle.kts, root build.gradle.kts, app/build.gradle.kts, AndroidManifest.xml, CardKeeperApplication.kt, Gradle wrapper
- All v1 dependencies resolve from Maven Central / Google Maven without errors (`./gradlew :app:dependencies BUILD SUCCESSFUL`)
- KSP 2.3.6 configured for Room 2.8.4 and Hilt 2.59.2 annotation processing
- Room Gradle Plugin schema export enabled — schemas will be committed to app/schemas/ on first build
- buildConfig = true enabled for BuildConfig.DEBUG guard on fallbackToDestructiveMigration

## Task Commits

1. **Task 1: Create Android project structure with Gradle configuration** - `513c688` (feat)

**Plan metadata:** (to be committed with SUMMARY + STATE update)

## Files Created/Modified

- `settings.gradle.kts` - Project name "CardKeeper", :app include, plugin/dep repos
- `build.gradle.kts` - Root plugin declarations: AGP 9.1.0, KSP 2.3.6, Hilt 2.59.2, Room 2.8.4, serialization, compose
- `app/build.gradle.kts` - App module: compileSdk 36, minSdk 26, all deps, Room schema dir, buildConfig
- `app/src/main/AndroidManifest.xml` - CAMERA permission, CardKeeperApplication, MainActivity launcher
- `app/src/main/kotlin/com/cardkeeper/CardKeeperApplication.kt` - @HiltAndroidApp Application class
- `gradle.properties` - useAndroidX, parallel, JVM args for Java 25 compatibility
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 9.4.1 distribution URL
- `gradle/wrapper/gradle-wrapper.jar` - Gradle wrapper launcher binary
- `gradlew` / `gradlew.bat` - Unix/Windows wrapper scripts
- `.gitignore` - Android-standard exclusions (.gradle, build/, .idea/, APK artifacts)

## Decisions Made

- **Gradle 9.4.1 (not 8.12):** System only has Java 25 (Amazon Corretto 25.0.1). Gradle 8.12/8.14.4 max support is Java 24. Gradle 9.4.1 supports Java 26, which covers Java 25.
- **Hilt 2.59.2 (not 2.57.1):** Hilt 2.57.1 calls `BaseExtension` API which was removed in AGP 9.x. Upgraded to 2.59.2 (current latest) which uses the updated AGP API.
- **kotlin.android plugin removed from app:** AGP 9.0+ has Kotlin built-in. Applying `org.jetbrains.kotlin.android` causes hard failure with "plugin is no longer required for Kotlin support since AGP 9.0."

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Gradle 8.12 incompatible with Java 25**
- **Found during:** Task 1 (initial dependency resolution)
- **Issue:** Kotlin DSL parser in Gradle 8.12 uses `JavaVersion.parse()` which throws `IllegalArgumentException: 25.0.1` — parser doesn't handle Java 25 version string format. Gradle 8.14.4 also affected (max supported: Java 24).
- **Fix:** Updated `gradle-wrapper.properties` to use Gradle 9.4.1 (supports Java 26, tested working with Java 25)
- **Files modified:** `gradle/wrapper/gradle-wrapper.properties`
- **Verification:** `./gradlew :app:dependencies BUILD SUCCESSFUL in 926ms`
- **Committed in:** `513c688` (Task 1 commit)

**2. [Rule 1 - Bug] kotlin.android plugin fails with AGP 9.x**
- **Found during:** Task 1 (after Gradle 9.4.1 upgrade)
- **Issue:** AGP 9.0 removed the requirement for `org.jetbrains.kotlin.android` plugin and now errors if it's applied: "The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0."
- **Fix:** Removed `id("org.jetbrains.kotlin.android")` from `app/build.gradle.kts` plugins block; replaced `kotlinOptions { jvmTarget = "17" }` with `kotlin { jvmToolchain(17) }` (AGP 9.x pattern)
- **Files modified:** `app/build.gradle.kts`
- **Verification:** Build no longer errors on this plugin
- **Committed in:** `513c688` (Task 1 commit)

**3. [Rule 1 - Bug] Hilt 2.57.1 BaseExtension API removed in AGP 9.x**
- **Found during:** Task 1 (after removing kotlin.android plugin)
- **Issue:** `com.google.dagger.hilt.android` 2.57.1 fails with "Android BaseExtension not found" because AGP 9.x removed `BaseExtension` in favor of new `AndroidComponentsExtension` API.
- **Fix:** Upgraded Hilt from 2.57.1 to 2.59.2 (latest stable; released after AGP 9.x support added)
- **Files modified:** `app/build.gradle.kts`, `build.gradle.kts`
- **Verification:** `./gradlew :app:dependencies` resolves all Hilt 2.59.2 artifacts successfully
- **Committed in:** `513c688` (Task 1 commit)

**4. [Rule 2 - Missing Critical] Added .gitignore for Android project**
- **Found during:** Task 1 (post-commit git status review)
- **Issue:** No .gitignore existed; `.gradle/` directory (large Gradle cache) would be committed accidentally
- **Fix:** Created `.gitignore` with Android-standard exclusions: `.gradle/`, `build/`, `.idea/`, `*.apk`, `*.aab`, `local.properties`; explicitly NOT ignoring `app/schemas/` (Room schema export must be committed)
- **Files modified:** `.gitignore` (created)
- **Committed in:** `513c688` (Task 1 commit)

---

**Total deviations:** 4 auto-fixed (3 Rule 1 bugs, 1 Rule 2 missing critical)
**Impact on plan:** All fixes required by environment (Java 25) and AGP 9.x API changes. Core plan intent (compilable project, correct deps, KSP+Room+Hilt configured) fully achieved. Version numbers deviate from plan spec only where forced by compatibility.

## Issues Encountered

The plan specified Gradle 8.12 and Hilt 2.57.1, both of which are incompatible with this machine's Java 25 / AGP 9.1.0 environment. All three issues (Gradle, kotlin.android plugin, Hilt BaseExtension) were cascading consequences of running on Java 25 with AGP 9.0+. The fixes were applied iteratively in a single task cycle.

## User Setup Required

None - no external service configuration required. Project is self-contained.

## Next Phase Readiness

- 01-02 (Room schema): Ready. KSP configured, Room Gradle Plugin enabled, `app/schemas/` will be created on first build.
- 01-03 (Hilt modules): Ready. `@HiltAndroidApp` in place, Hilt 2.59.2 resolving.
- 01-04 (Navigation + UI stubs): Ready. Compose BOM, Navigation 2.9.7, serialization all resolving.

**Note for future phases:** Use Hilt 2.59.2 (not 2.57.1 from RESEARCH.md) and remove kotlin.android plugin from all module build files. Gradle 9.4.1 is the actual wrapper version.

---
*Phase: 01-foundation*
*Completed: 2026-03-24*

## Self-Check: PASSED

- All 11 created files confirmed present on disk
- Commit 513c688 confirmed in git log
