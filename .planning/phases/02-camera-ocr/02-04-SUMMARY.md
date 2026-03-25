---
phase: 02-camera-ocr
plan: "04"
subsystem: gallery-import
tags: [gallery, activity-result, uri, temp-file, navigation]
dependency_graph:
  requires: ["02-01", "02-02"]
  provides: ["gallery-uri-to-temp-path", "onGalleryImageReady-callback"]
  affects: ["ScanScreen", "ScanViewModel", "AppNavHost"]
tech_stack:
  added: ["ActivityResultContracts.GetContent", "ContentResolver.openInputStream"]
  patterns: ["rememberLauncherForActivityResult", "URI-to-cacheDir temp copy"]
key_files:
  created: []
  modified:
    - app/src/main/kotlin/com/cardkeeper/ui/scan/ScanViewModel.kt
    - app/src/main/kotlin/com/cardkeeper/ui/scan/ScanScreen.kt
    - app/src/main/kotlin/com/cardkeeper/ui/navigation/AppNavHost.kt
decisions:
  - "processGalleryImage() runs synchronously on calling thread — ContentResolver.openInputStream is fast for business card images (<5 MB); defer IO dispatch to Phase 3+ if needed"
  - "Gallery and camera paths produce identical output (absolute cacheDir JPEG path) so OCR pipeline needs no branching"
  - "onGalleryClick parameter removed from ScanScreen; galleryLauncher is internal to the composable — cleaner API"
metrics:
  duration: "pre-implemented (committed in ecbef07 + da09d02)"
  completed_date: "2026-03-24"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 3
---

# Phase 02 Plan 04: Gallery Import Summary

Gallery import via `ActivityResultContracts.GetContent`, copying selected URI to a cacheDir JPEG and routing through the same `setTempImagePath → navigate(OcrReviewRoute)` pipeline as camera capture.

## What Was Built

Both tasks were already fully implemented in prior commits when this summary was generated:

- `da09d02` — `feat(02-04): add processGalleryImage() to ScanViewModel`
- `ecbef07` — `feat(wave-2): gallery import, field parser, unit tests`

### Task 1: processGalleryImage() in ScanViewModel

`ScanViewModel.processGalleryImage(context, uri, onReady, onError)` copies a content:// or file:// URI to a temp file in `context.cacheDir`. Sets `ScanState.Capturing` at start, `ScanState.Idle` on success. Calls `onReady(absolutePath)` on success, `onError(message)` on failure. Sets `tempImagePath` on success — same field the camera path uses.

### Task 2: Gallery launcher wired in ScanScreen + AppNavHost

ScanScreen now declares `onGalleryImageReady: (String) -> Unit` as a parameter (replacing the old `onGalleryClick: () -> Unit = {}` stub). A `rememberLauncherForActivityResult(ActivityResultContracts.GetContent())` launcher is created inside the composable and passed `"image/*"` on gallery icon tap. The launcher callback calls `viewModel.processGalleryImage(context, uri, onReady = onGalleryImageReady)`.

AppNavHost passes `onGalleryImageReady = { imagePath -> viewModel.setTempImagePath(imagePath); navController.navigate(OcrReviewRoute) }` — identical to the `onPhotoReady` lambda.

## Acceptance Criteria Verification

| Criterion | Status |
|-----------|--------|
| ScanViewModel.kt contains `fun processGalleryImage(` | PASS |
| ScanViewModel.kt contains `context.contentResolver.openInputStream(uri)` | PASS |
| ScanViewModel.kt contains `cacheDir` for temp file | PASS |
| ScanViewModel.kt contains `_scanState.value = ScanState.Capturing` | PASS |
| ScanViewModel.kt contains `onReady(tempFile.absolutePath)` | PASS |
| ScanScreen.kt contains `ActivityResultContracts.GetContent` | PASS |
| ScanScreen.kt contains `galleryLauncher.launch("image/*")` | PASS |
| ScanScreen.kt contains `onGalleryImageReady: (String) -> Unit` | PASS |
| ScanScreen.kt contains `viewModel.processGalleryImage(` | PASS |
| AppNavHost.kt passes `onGalleryImageReady` with navigate to OcrReviewRoute | PASS |

## Build Verification

`./gradlew :app:assembleDebug` failed — Google Maven repository not accessible in this environment (plugin resolution for `com.android.application:9.0.1` timed out). This is a network/environment limitation, not a code issue. All acceptance criteria verified via grep.

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — all data flows are wired.

## Self-Check: PASSED

Files verified present:
- `/home/user/CardKeeper/app/src/main/kotlin/com/cardkeeper/ui/scan/ScanViewModel.kt` — contains `processGalleryImage`
- `/home/user/CardKeeper/app/src/main/kotlin/com/cardkeeper/ui/scan/ScanScreen.kt` — contains `galleryLauncher`, `onGalleryImageReady`
- `/home/user/CardKeeper/app/src/main/kotlin/com/cardkeeper/ui/navigation/AppNavHost.kt` — contains `onGalleryImageReady`

Commits verified: `da09d02` and `ecbef07` present in git log.
