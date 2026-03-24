---
phase: 01-foundation
plan: 04
subsystem: ui/navigation
tags: [compose, navigation, theme, material3, screen-stubs, bottom-nav]
dependency_graph:
  requires: ["01-01"]
  provides: ["CardKeeperTheme", "AppNavHost", "type-safe routes", "five screen stubs", "MainActivity with bottom nav"]
  affects: ["all UI phases (02, 03, 04) — write screen content into stubs"]
tech_stack:
  added:
    - "Navigation Compose 2.9.7 type-safe routes (@Serializable objects/data class)"
    - "Material3 CardKeeperTheme with light/dark color schemes (seed #1A73E8)"
    - "Material3 Typography (4-role scale: titleLarge/titleMedium/bodyLarge/labelMedium)"
    - "NavigationBar + NavigationBarItem (3-item bottom nav shell)"
  patterns:
    - "@Serializable object for no-arg routes, @Serializable data class for routes with args"
    - "composable<Route> {} type-safe composable registration"
    - "backStackEntry.toRoute<CardDetailRoute>() for argument extraction"
    - "isSystemInDarkTheme() drives light/dark color scheme selection automatically"
    - "popUpTo(CardListRoute) on bottom nav taps prevents back-stack accumulation"
key_files:
  created:
    - app/src/main/kotlin/com/cardkeeper/ui/theme/Color.kt
    - app/src/main/kotlin/com/cardkeeper/ui/theme/Type.kt
    - app/src/main/kotlin/com/cardkeeper/ui/theme/Theme.kt
    - app/src/main/kotlin/com/cardkeeper/ui/navigation/Screen.kt
    - app/src/main/kotlin/com/cardkeeper/ui/navigation/AppNavHost.kt
    - app/src/main/kotlin/com/cardkeeper/ui/cardlist/CardListScreen.kt
    - app/src/main/kotlin/com/cardkeeper/ui/carddetail/CardDetailScreen.kt
    - app/src/main/kotlin/com/cardkeeper/ui/scan/ScanScreen.kt
    - app/src/main/kotlin/com/cardkeeper/ui/scan/OcrReviewScreen.kt
    - app/src/main/kotlin/com/cardkeeper/ui/tags/TagManagerScreen.kt
    - app/src/main/kotlin/com/cardkeeper/MainActivity.kt
    - app/src/main/res/values/themes.xml
  modified:
    - .planning/config.json
decisions:
  - "hasRoute<T>() used for bottom nav selected-state detection (Navigation 2.9.7 type-safe API) rather than comparing route string to qualifiedName"
  - "themes.xml added with Theme.Material3.DayNight.NoActionBar to satisfy AndroidManifest reference"
metrics:
  duration: "~25 minutes"
  completed: "2026-03-24"
  tasks_completed: 2
  files_created: 12
---

# Phase 01 Plan 04: Navigation Scaffold + Theme Summary

**One-liner:** Material3 CardKeeperTheme with light/dark schemes, type-safe @Serializable navigation routes, five screen stubs with TopAppBar/Scaffold, AppNavHost wiring all five destinations, and MainActivity with 3-item bottom NavigationBar — app compiles and launches (BUILD SUCCESSFUL in 23s).

## What Was Built

Complete UI shell for CardKeeper:

1. **Color.kt** — Color constants derived from seed #1A73E8: Blue40/Blue80 (primary light/dark), Error40/Error80, SurfaceLight/SurfaceDark, SurfaceVariantLight/SurfaceVariantDark, OnSurface/OnPrimary pairs
2. **Type.kt** — `AppTypography` with exactly 4 roles per UI-SPEC: titleLarge (28sp), titleMedium (20sp), bodyLarge (16sp), labelMedium (12sp)
3. **Theme.kt** — `CardKeeperTheme` composable: isSystemInDarkTheme() default, lightColorScheme/darkColorScheme, MaterialTheme with AppTypography, SideEffect for status bar color sync
4. **Screen.kt** — 5 type-safe route declarations: `CardListRoute`, `ScanRoute`, `OcrReviewRoute`, `TagManagerRoute` (all @Serializable objects), `CardDetailRoute(cardId: Long)` (@Serializable data class)
5. **AppNavHost.kt** — NavHost with startDestination = CardListRoute, all five composable<Route> {} registrations, CardDetailRoute argument extraction via toRoute<>(), popBackStack/popBackStack(inclusive=false) navigation actions
6. **CardListScreen.kt** — Scaffold stub with TopAppBar ("CardKeeper"), centered Text("Cards"), FloatingActionButton with CameraAlt icon calling onScanClick
7. **CardDetailScreen.kt** — Scaffold stub with TopAppBar ("Card Detail") + back arrow, centered Text("Card Detail"), cardId: Long + onBack params
8. **ScanScreen.kt** — Scaffold stub with TopAppBar ("Scan"), centered Text("Scan"), onPhotoReady + onBack params
9. **OcrReviewScreen.kt** — Scaffold stub with TopAppBar ("Review") + back arrow, centered Text("Review"), onSaved + onBack params
10. **TagManagerScreen.kt** — Scaffold stub with TopAppBar ("Tags") + back arrow, centered Text("Tags"), onBack param
11. **MainActivity.kt** — @AndroidEntryPoint ComponentActivity, setContent with CardKeeperTheme, rememberNavController, Scaffold with NavigationBar (Cards/Scan/Tags, Icons.Rounded.Style/CameraAlt/Label), Box(padding) wrapping AppNavHost
12. **themes.xml** — `Theme.Material3.DayNight.NoActionBar` XML theme referenced by AndroidManifest

## Verification

- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL in 23s, 13 executed / 29 up-to-date
- 42 actionable tasks completed with zero compile errors
- Deprecation-only warnings (cosmetic): AutoMirrored icon variants, fallbackToDestructiveMigration overload, statusBarColor Java API

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] hasRoute<T>() for bottom nav selected state (not qualifiedName)**
- **Found during:** Task 2 (compile/runtime verification)
- **Issue:** Plan noted "NOTE on route matching: may need hasRoute<T>()" — Navigation 2.9.7 type-safe routes use a serialized route format that does not match `Route::class.qualifiedName` strings; using qualifiedName would cause no bottom nav item to ever appear selected
- **Fix:** Used `navBackStackEntry?.destination?.hasRoute<CardListRoute>()` (and same for Scan/TagManager) for selected-state detection per Navigation 2.9.7 type-safe API
- **Files modified:** `app/src/main/kotlin/com/cardkeeper/MainActivity.kt`

**2. [Rule 2 - Missing Critical] Added themes.xml resource**
- **Found during:** Task 2 (assembleDebug — mergeDebugResources step)
- **Issue:** AndroidManifest.xml references `@style/Theme.Material3.DayNight.NoActionBar` which must be declared in res/values/themes.xml or the resource merge fails
- **Fix:** Created `app/src/main/res/values/themes.xml` declaring the theme extending `Theme.MaterialComponents.DayNight.NoActionBar`
- **Files modified:** `app/src/main/res/values/themes.xml` (created)

## Commits

| Commit | Message | Files |
|--------|---------|-------|
| f1eefff | feat(01-04): create theme files (Color, Type, Theme) | Color.kt, Type.kt, Theme.kt |
| 1a0e9ba | feat(01-04): create navigation scaffold, screen stubs, and MainActivity | Screen.kt, AppNavHost.kt, CardListScreen.kt, CardDetailScreen.kt, ScanScreen.kt, OcrReviewScreen.kt, TagManagerScreen.kt, MainActivity.kt, themes.xml, config.json |

## Known Stubs

All five screen files are intentional stubs — Scaffold + TopAppBar + centered title Text only. They will be replaced with real content in Phases 2 and 3:
- CardListScreen.kt → Phase 3 (03-01)
- CardDetailScreen.kt → Phase 3 (03-02)
- ScanScreen.kt → Phase 2 (02-01)
- OcrReviewScreen.kt → Phase 2 (02-05)
- TagManagerScreen.kt → Phase 3 (03-05)

## Self-Check: PASSED

- app/src/main/kotlin/com/cardkeeper/ui/theme/Color.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/ui/theme/Type.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/ui/theme/Theme.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/ui/navigation/Screen.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/ui/navigation/AppNavHost.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/ui/cardlist/CardListScreen.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/ui/carddetail/CardDetailScreen.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/ui/scan/ScanScreen.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/ui/scan/OcrReviewScreen.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/ui/tags/TagManagerScreen.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/MainActivity.kt — FOUND
- app/src/main/res/values/themes.xml — FOUND
- Commit f1eefff — FOUND
- Commit 1a0e9ba — FOUND
- ./gradlew :app:assembleDebug BUILD SUCCESSFUL — VERIFIED
