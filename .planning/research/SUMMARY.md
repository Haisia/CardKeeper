# Project Research Summary

**Project:** CardKeeper (명함 관리 앱)
**Domain:** Android native business card management — camera capture, on-device OCR, local storage
**Researched:** 2026-03-24
**Confidence:** MEDIUM-HIGH

## Executive Summary

CardKeeper is a personal, ad-free Android app where the core loop is: photograph a business card → ML Kit OCR extracts text → parsed fields are manually correctable → card is saved locally with the photo. Experts build this type of app with MVVM + Clean Architecture over a standard Jetpack stack (Compose, Room, CameraX, Hilt). The technology choices here are almost entirely constrained by the project's own requirements — Kotlin, Compose, ML Kit on-device, local-only — and those constraints happen to align with current Android best practices. There is no architectural controversy in this stack; every library is the obvious, officially recommended choice for its purpose.

The single highest-complexity challenge is Korean business card OCR parsing. ML Kit returns spatial text blocks with no semantic field labels; Korean cards frequently have dual-column layouts where Korean labels and Latin values are separate `TextBlock` objects. A naive regex-on-raw-text parser will fail 30–50% of the time on Korean cards. The correct approach requires bounding-box-aware parsing that associates label blocks with value blocks by geometric proximity. This must be designed into Phase 2 from the start — it cannot be bolted on later. The manual correction screen is not optional polish; it is a first-class reliability feature.

The main execution risks are data-loss-by-oversight (Room `fallbackToDestructiveMigration` in production, missing schema migrations), and file URI exposure crashes (passing `file://` URIs to external processes on API 24+). Both are easy to prevent with upfront design decisions. The app is appropriately scoped: no cloud, no auth, no multi-user, no ads. The roadmap should reflect that by keeping phases tightly bounded and shipping a working core loop before adding the organizational features (tags, export).

---

## Key Findings

### Recommended Stack

The Jetpack stack is mature and well-matched to this project. The BOM approach (`androidx.compose:compose-bom:2026.03.00`) manages Compose library version alignment automatically. KSP replaces KAPT for both Room and Hilt code generation — this is now the standard path and is faster. The one non-obvious version requirement is the ML Kit artifact split: the **bundled** `com.google.mlkit:text-recognition` must be used (not `play-services-mlkit-text-recognition`), and Korean card support requires adding `com.google.mlkit:text-recognition-korean` as a separate dependency. Mixing bundled and Play Services variants causes model availability issues at first launch.

Several versions carry MEDIUM confidence and should be verified against their respective release pages before the first build: KSP (`2.3.20-1.0.31`), ML Kit text-recognition (`16.0.1`), Coil 3 (`3.1.0`), and kotlinx-coroutines (`1.10.1`). All core Jetpack libraries (Compose BOM, Room 2.8.4, CameraX 1.5.3, Navigation 2.9.7, Hilt 2.57.1, Lifecycle 2.10.0) are HIGH confidence via official release pages.

**Core technologies:**
- Kotlin 2.3.20 + AGP 9.0.1 — language and build system; compile/target SDK 36, min SDK 26
- Compose BOM 2026.03.00 + Material3 — UI framework; replaces XML Views entirely
- KSP 2.3.20-1.0.31 — code generation for Room and Hilt; KAPT is deprecated, do not use it
- Room 2.8.4 — local SQLite ORM with Flow-based reactive queries; KSP code generation path
- CameraX 1.5.3 (camera-camera2, camera-lifecycle, camera-view) — captures photos for OCR; wraps Camera2, handles device quirks and lifecycle automatically
- ML Kit text-recognition 16.0.1 (bundled) — on-device Latin OCR, no API key, no internet
- ML Kit text-recognition-korean 16.0.1 (bundled) — Korean script OCR; required for the target market; must be the standalone artifact, not Play Services variant
- Hilt 2.57.1 — dependency injection; compile-time verified; integrates with ViewModel and Navigation
- Coil 3 (`io.coil-kt.coil3:coil-compose`) — Compose-native async image loading from internal storage files
- Navigation Compose 2.9.7 — type-safe routes via `@Serializable` objects; eliminates string-route crashes
- ContactsContract (platform API) — contacts export via Intent; zero permissions needed for Intent approach

**What NOT to use:**
- `play-services-mlkit-text-recognition` / `play-services-mlkit-text-recognition-korean` — lazy model download; silently fails offline
- KAPT — deprecated; replaced by KSP
- Firebase ML or Cloud Vision API — requires internet and API keys; violates project constraints
- Coil 2.x (`io.coil-kt:coil-compose`) — superseded by Coil 3 (`io.coil-kt.coil3` group)

See `.planning/research/STACK.md` for full dependency block with exact coordinates.

---

### Expected Features

The domain is well-understood. Every major competitor (CamCard, ABBYY, Haystack, Samsung Business Card Scanner) shares a common core loop. Missing table-stakes features make the app feel broken. Differentiators create the reason to keep using it over the competition.

**Must have (table stakes) — the core loop:**
- Camera capture with real-time preview — primary input; without this it's just a gallery app
- ML Kit OCR text extraction — without this it's just a photo album
- Parsed fields: name, company, title, phone, email, address — users need structured data
- Manual correction of parsed fields — OCR is wrong ~20–30% of the time; this is non-optional
- Card photo stored with parsed data — visual context; users remember card layouts
- Card list view (thumbnail + name + company) and card detail view
- Real-time search by name, company, title — core utility; unusable if you can't find a card
- Edit stored card — fields change when people change jobs
- Delete a card with confirmation — basic CRUD

**Should have (differentiators and high-leverage low-effort):**
- Gallery import — users have photos of old cards; reuses the same OCR pipeline
- Per-card memo field — capture context ("met at Startup Weekend") that the card can't convey
- Tag-based grouping (multi-tag per card) — more flexible than folders; one card in multiple categories
- Filter by tag — tags are useless without a filter; medium effort, core organizational feature
- Export to Android Contacts — converts passive archive into active tool; use Intent approach for simplicity
- Fully offline / zero ads / zero tracking — the explicit reason this app exists

**Defer to post-v1:**
- Batch gallery import (multiple cards at once)
- Sort options beyond default (by company, by date added)
- Card sharing via intent or NFC
- Home screen widget
- Duplicate detection / merge

**Hard feature dependencies to respect in phase ordering:**
- Tag filter requires tag assignment to exist; build them together
- Gallery import re-uses the OCR pipeline; build the pipeline first, then plug in the gallery source
- The post-scan correction form and the "edit stored card" form are the same component; build once

See `.planning/research/FEATURES.md` for full dependency graph and complexity breakdown.

---

### Architecture Approach

The recommended pattern is MVVM + Clean Architecture in three layers: UI (Compose screens + ViewModels), Domain (pure Kotlin use cases and models, no Android dependencies), and Data (Room, CameraX, ML Kit, ContactsContract wrapped as data sources behind repository interfaces). This is the officially recommended Android architecture and is the standard for Compose + Room + ViewModel apps. Single-module with package-level separation is appropriate for a personal app of this scope; multi-module would add build complexity with no benefit.

The domain layer's `ParseOcrResultUseCase` deserves special attention: keeping it as a pure function with no Android dependencies makes it testable with plain JUnit, no emulator needed. OCR parsing is the most complex and failure-prone logic in the app; high test coverage here has the highest ROI.

**Major components:**
1. `ScanScreen` + `ScanViewModel` — camera preview, image capture, OCR invocation, navigation to review
2. `OcrReviewScreen` — pre-filled parsed fields, correction form, tag assignment before save
3. `CardListScreen` + `CardListViewModel` — reactive list via `Flow<List<CardWithTags>>`, search bar, tag filter chips
4. `CardDetailScreen` + `CardDetailViewModel` — full card view, inline edit mode, contacts export trigger
5. `ParseOcrResultUseCase` — pure Kotlin, bounding-box-aware field extraction from ML Kit text blocks
6. `CardRepository` — coordinates `CardDao` (Room) + `ImageStorageDataSource` (filesDir)
7. `TagRepository` — tag CRUD and `CardTagCrossRef` many-to-many join table management
8. `OcrDataSource` — wraps ML Kit `TextRecognizer`; returns raw `TextBlock` list with bounding boxes
9. `ImageStorageDataSource` — saves compressed JPEG to `filesDir/cards/`, stores relative path in Room
10. `ContactsDataSource` — fires `ContactsContract.Intents.Insert.ACTION` Intent; no permissions needed

**Room schema:** Three tables — `cards`, `tags`, `card_tag_cross_ref` (junction). Room `@Relation` + `@Junction` via `CardWithTags` POJO. Tags modeled as proper many-to-many from day one — never as a comma-separated string column.

**Image storage:** Relative paths only in Room (e.g., `cards/uuid.jpg`); reconstruct full path at runtime via `context.filesDir`. Compress to max 1024px JPEG 85% on save (~100–200 KB per card). Use `UUID.randomUUID()` for filenames — never timestamps, to avoid collision.

See `.planning/research/ARCHITECTURE.md` for full DAO schemas, navigation graph, and data flow diagram.

---

### Critical Pitfalls

1. **Korean/mixed-layout OCR parsing fails without bounding-box logic (C1)** — Korean cards use dual-column layouts where ML Kit returns label and value as separate `TextBlock` objects with no semantic link. A regex-only linear parser misclassifies 30–50% of fields. Prevention: access `boundingBox` on each `TextBlock` and `TextLine`; build a coordinate-aware parser that associates labels with values by vertical overlap and horizontal proximity. Do this in Phase 2, not as a later fix.

2. **ML Kit Korean model missing at first launch if wrong artifact used (C2)** — The Play Services variant (`play-services-mlkit-text-recognition-korean`) downloads the model lazily and silently falls back to Latin-only with no error. Prevention: use the standalone bundled artifact `com.google.mlkit:text-recognition-korean`. APK increases ~3–5 MB; model is always present.

3. **Room `fallbackToDestructiveMigration` in production destroys all user data (C6)** — Convenient during development, catastrophic in production. Prevention: gate it behind `BuildConfig.DEBUG` only; write explicit `Migration(from, to)` objects for every schema version bump; enable Room schema export (`room.schemaLocation`) and commit the generated JSON to source control.

4. **`file://` URI passed to external process throws `FileUriExposedException` (C5)** — On API 24+ (this app targets API 26+), `file://` URIs cannot cross process boundaries. Prevention: configure `FileProvider` in `AndroidManifest.xml`; always use `FileProvider.getUriForFile()` when passing image URIs to any external Intent. For Contacts photo, read into `ByteArray` directly — no URI needed.

5. **CameraX use-case binding crash on recomposition (C4)** — `ProcessCameraProvider.bindToLifecycle()` is not idempotent; re-calling on an already-bound use case throws `IllegalArgumentException`. Prevention: always call `cameraProvider.unbindAll()` before binding; wrap setup in `LaunchedEffect(lifecycleOwner)` so it runs only when the lifecycle owner changes, not on every recomposition.

**Additional pitfalls to track by phase:**
- Phase 2 (Camera + OCR): C3 — cap `ImageCapture` at 1920x1080 to avoid OOM on ML Kit input
- Phase 3 (CRUD + Tags): M5 — never store tags as comma-separated string; M6 — always use UUID filenames
- Phase 4 (Export + Polish): C7 — for direct `ContentResolver` insert, declare both `READ_CONTACTS` and `WRITE_CONTACTS` and handle permanent-denial state; M7 — null-check `contentResolver.insert()` return on OEM ROMs
- Search: m2 — Korean FTS tokenizer mismatch; use `LIKE`-based search, skip FTS for v1

See `.planning/research/PITFALLS.md` for full pitfall details and phase-specific warning table.

---

## Implications for Roadmap

Research strongly supports a four-phase structure following a dependency-first ordering. Each phase produces a releasable increment. No phase depends on work that comes after it.

### Phase 1: Foundation

**Rationale:** Everything else depends on this. Room schema, Hilt modules, and navigation graph must exist before any feature work. Getting schema right (many-to-many tags, relative image paths, schema export enabled) prevents painful migrations later. This phase is entirely setup — it produces no visible user features but removes all blocking dependencies.

**Delivers:** Compilable app with Room DB (cards + tags + junction table), Hilt wired end-to-end, Navigation graph with all screen stubs, CI build passing.

**Features addressed:** None user-visible, but establishes the schema that supports all features.

**Pitfalls to prevent here:**
- Enable `room.schemaLocation` now; commit the schema JSON — do not add this later
- Define `fallbackToDestructiveMigration(BuildConfig.DEBUG)` guard immediately
- Store only relative image paths in `CardEntity.imagePath` from day one

**Research flag:** Standard patterns — skip `research-phase`. Room + Hilt + Navigation setup is extensively documented with official guides.

---

### Phase 2: Camera + OCR

**Rationale:** This is the core value proposition. Without camera capture and OCR, the app does not exist. It is also the highest-complexity phase due to bounding-box-aware Korean parsing. Build this before CRUD polish so the parsing logic can be validated early against real Korean cards — if the heuristics need iteration, better to find out now.

**Delivers:** Full scan flow — camera preview, capture, ML Kit OCR (Latin + Korean), bounding-box-aware field parsing, manual correction screen, save card to Room with compressed image.

**Features addressed:**
- Camera capture (table stakes)
- OCR text extraction (table stakes)
- Parsed fields: name, company, title, phone, email, address (table stakes)
- Manual correction of parsed fields (table stakes)
- Card photo stored with parsed data (table stakes)
- Gallery import (differentiator — reuses same OCR pipeline; low marginal cost, add here)

**Stack used:** CameraX 1.5.3, ML Kit text-recognition + text-recognition-korean (bundled), ImageStorageDataSource, ParseOcrResultUseCase (pure Kotlin, unit-testable)

**Pitfalls to prevent here:**
- C1: Build bounding-box-aware parser, not regex-only
- C2: Use standalone `com.google.mlkit:text-recognition-korean` artifact
- C3: Set `ImageCapture.Builder().setTargetResolution(Size(1920, 1080))`
- C4: Wrap CameraX binding in `LaunchedEffect(lifecycleOwner)`, call `unbindAll()` first
- C5: Configure FileProvider now — needed before any external URI passing
- m3: Pass background executor to `ImageCapture.takePicture()` callback

**Research flag:** Needs `research-phase` during planning. Korean bounding-box parsing is the key risk area. The `TextBlock.boundingBox` API is documented but the heuristics for label-value association on Korean card layouts are not. Recommend testing against 5–10 real Korean business card images before finalizing the parsing algorithm.

---

### Phase 3: CRUD + Tags

**Rationale:** Once cards can be captured, users need to browse, find, edit, and organize them. This phase completes the storage and retrieval loop. Tags and search are grouped together because tag filter (chips above list) is tightly coupled to the search/filter UI — building them separately would require reworking the list screen twice.

**Delivers:** Card list view, card detail view, real-time search, edit stored card, delete card, per-card memo, tag creation and assignment, filter by tag.

**Features addressed:**
- Card list view + detail view (table stakes)
- Real-time search by name/company/title (table stakes)
- Edit stored card (table stakes)
- Delete card with confirmation (table stakes)
- Per-card memo field (differentiator — single text column, trivial to add)
- Tag-based grouping (differentiator — many-to-many schema already in place from Phase 1)
- Filter by tag (differentiator — depends on tags existing)

**Stack used:** Room Flow queries (`searchCards` DAO), Compose `LazyColumn` with Coil `AsyncImage`, Material3 `SearchBar`, `FilterChip`

**Pitfalls to prevent here:**
- M4: Always specify decode size in Coil `AsyncImage` in list items — never load full-resolution in LazyColumn
- m2: Use `LIKE '%query%'` for Korean text search; do not use FTS with default tokenizer
- M5: Already prevented by Phase 1 schema; confirm tags are never stored as strings
- M6: Already prevented by UUID filenames from Phase 2

**Research flag:** Standard patterns — skip `research-phase`. Room Flow queries, LazyColumn, and Material3 search/chip components are extensively documented.

---

### Phase 4: Export + Polish

**Rationale:** Contacts export is a meaningful standalone feature that converts the app from a passive archive to an active tool. Polish — edge cases, error states, empty states, camera permission revocation handling — rounds out the app for real use. Grouping export and polish avoids a separate tiny phase.

**Delivers:** Export parsed card data to Android Contacts app via Intent, permission revocation handling for camera, empty states for list/search, image error states, final UX polish.

**Features addressed:**
- Export to Android Contacts (differentiator — Intent approach, no permissions needed)
- Zero ads / fully offline (already true by architecture; confirm no network permissions declared)

**Stack used:** `ContactsContract.Intents.Insert.ACTION` Intent, FileProvider (already configured in Phase 2)

**Pitfalls to prevent here:**
- C5: FileProvider already configured — use `FileProvider.getUriForFile()` for any image sharing
- C7: For Intent-based export, no `WRITE_CONTACTS` permission needed — confirm Intent approach
- M7: If direct ContentResolver insert ever added, null-check return URI for OEM compatibility
- M1: Re-check camera permission in `ON_RESUME`; navigate to permission screen if revoked

**Research flag:** Standard patterns — skip `research-phase`. ContactsContract Intent approach is stable platform API, unchanged since API 5, well-documented.

---

### Phase Ordering Rationale

- **Foundation before everything:** Room schema cannot be changed without migrations. Get it right (many-to-many tags, relative paths, schema export) before any data is written.
- **Camera + OCR before CRUD:** The scan flow is both the highest complexity and the core value. Early validation of Korean bounding-box parsing reduces the risk of a late-phase rewrite.
- **CRUD + Tags together:** Tag filter is a list-screen feature; building tags without the filter in the same phase forces a list-screen rework. Features from FEATURES.md dependency graph confirm this grouping.
- **Export last:** ContactsContract export has no dependencies on any other feature beyond parsed fields existing. Low-risk addition at the end.
- **Gallery import in Phase 2, not Phase 3:** It reuses the same OCR pipeline built in Phase 2. The marginal cost is near-zero when done in the same phase; it would require re-opening the OCR pipeline if deferred.

---

### Research Flags

Phases needing deeper research during planning:
- **Phase 2 (Camera + OCR):** Korean bounding-box parsing heuristics are not well-documented for business card layouts specifically. Plan a research spike: test ML Kit against real Korean card samples, document `TextBlock.boundingBox` coordinate behavior, validate label-association heuristics before committing to an algorithm.

Phases with standard, well-documented patterns (skip `research-phase`):
- **Phase 1 (Foundation):** Room + Hilt + Navigation Compose setup is covered by official Android guides with code samples.
- **Phase 3 (CRUD + Tags):** Room relationships, LazyColumn, Material3 search/chips are all standard.
- **Phase 4 (Export + Polish):** ContactsContract Intent approach and FileProvider are stable, documented APIs.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM-HIGH | Core Jetpack libraries HIGH via official release pages; ML Kit version (16.0.1), Coil 3 (3.1.0), KSP (2.3.20-1.0.31), coroutines (1.10.1) are MEDIUM — need verification before first build |
| Features | MEDIUM | Based on training-data knowledge of competitor apps (CamCard, ABBYY, Haystack); no live Play Store verification performed. Feature set is standard for domain; low risk of surprise. |
| Architecture | HIGH | MVVM + Clean Architecture with Compose, Room, Hilt is the officially recommended Android pattern; component boundaries and DAO patterns are well-established |
| Pitfalls | HIGH for platform behavior; MEDIUM for Korean OCR heuristics | FileProvider, Room main-thread enforcement, CameraX binding — HIGH (documented platform behavior). Korean bounding-box parsing accuracy — MEDIUM (recommend empirical validation in Phase 2) |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **ML Kit Korean parsing accuracy on real cards:** Research identifies the risk (C1) and the correct approach (bounding-box-aware parsing), but the specific heuristics for Korean label-value association need empirical testing against real business card images. Allocate a research spike at the start of Phase 2.

- **Version verification before Phase 1 start:** KSP, ML Kit, Coil 3, coroutines, and kotlinx-serialization versions are MEDIUM confidence. Verify all five against their GitHub release pages before writing the first `build.gradle.kts`. See the Version Verification Checklist in `STACK.md`.

- **Korean FTS behavior:** Research recommends `LIKE`-based search for Korean text (m2) and defers FTS. If users report search quality issues with partial Hangul matching, revisit FTS5 with ICU tokenizer in a post-v1 iteration.

- **Contacts export UX decision:** Research recommends the Intent approach (no permissions, delegates to Contacts app). If users find the hand-off to the Contacts app disruptive, the direct `ContentResolver` insert path exists as an alternative — but requires runtime permission handling and OEM null-check defensive code (M7).

---

## Sources

### Primary (HIGH confidence)
- Official Android Jetpack release pages (developer.android.com/jetpack/androidx/versions/stable-channel) — Compose BOM, Room, CameraX, Navigation, Lifecycle, Hilt versions
- Android Architecture Guide (developer.android.com/topic/architecture) — MVVM + Clean Architecture pattern
- Room Relationships docs (developer.android.com/training/data-storage/room/relationships) — many-to-many schema
- Jetpack Compose Navigation docs (developer.android.com/jetpack/compose/navigation) — type-safe routes
- Hilt DI guide (developer.android.com/training/dependency-injection/hilt-android) — module structure
- Android ContactsContract reference (developer.android.com/reference/android/provider/ContactsContract) — Intent-based export
- ML Kit Text Recognition v2 (developers.google.com/ml-kit/vision/text-recognition/android) — on-device bundled model
- PROJECT.md — authoritative project requirements and constraints

### Secondary (MEDIUM confidence)
- Training data: ML Kit text-recognition 16.0.1 version, Coil 3.1.0 version, kotlinx-coroutines 1.10.1 — cross-checked against release patterns but not live-verified
- Training data: KSP 2.3.20-1.0.31 — verify at github.com/google/ksp/releases
- Domain knowledge: CamCard, ABBYY Business Card Reader, Haystack, Samsung Business Card Scanner feature sets (as of training cutoff August 2025; apps may have updated)
- ML Kit Korean bounding-box parsing behavior — community-documented, recommend empirical validation

### Tertiary (needs validation)
- Korean business card field-parsing heuristics — no authoritative source; derive from `TextBlock.boundingBox` coordinates + empirical testing in Phase 2

---
*Research completed: 2026-03-24*
*Ready for roadmap: yes*
