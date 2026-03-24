# Roadmap: CardKeeper (명함 관리 앱)

## Overview

CardKeeper is built in four phases following a strict dependency-first order. Phase 1 lays the
database schema and dependency injection skeleton that every subsequent phase writes into. Phase 2
delivers the app's entire reason to exist — camera capture, ML Kit OCR (Latin + Korean, bundled),
bounding-box-aware field parsing, manual correction, and save. Phase 3 completes the storage and
retrieval loop: list, detail, search, edit, delete, memo, tags, and tag filtering. Phase 4 closes
the loop for the user's contacts workflow by exporting parsed data via Intent, then polishes edge
cases and empty states. Every v1 requirement maps to exactly one phase and the core value — "camera
once, find instantly" — is fully realized by the end of Phase 3, with Phase 4 adding the active-tool
layer on top.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Foundation** - Android project scaffold with Room DB, Hilt DI, and Navigation stub wired end-to-end
- [ ] **Phase 2: Camera + OCR** - Full scan flow — camera preview, ML Kit OCR, field parsing, manual correction, save card with image
- [ ] **Phase 3: CRUD + Tags + Search** - Card list/detail, real-time search, edit/delete, per-card memo, tag creation/assignment/filter
- [ ] **Phase 4: Export + Polish** - Contacts export via Intent, permission revocation handling, empty states, final UX polish

## Phase Details

### Phase 1: Foundation
**Goal**: A compilable, runnable Android project where Room DB (cards + tags + junction table), Hilt DI, and Jetpack Compose Navigation are wired end-to-end with screen stubs — enabling all subsequent phases to write features directly without revisiting the scaffold
**Depends on**: Nothing (first phase)
**Requirements**: None (no user-visible requirements — unblocks all other phases)
**Success Criteria** (what must be TRUE):
  1. The app compiles and launches to a card list stub screen with no crashes
  2. Room database exists with all three tables (cards, tags, card_tag_cross_ref) and schema export JSON committed to source control
  3. Hilt modules are wired end-to-end: a ViewModel can receive an injected repository without runtime errors
  4. Navigation graph contains stubs for all five screens (CardList, CardDetail, Scan, OcrReview, TagManager) and routes between them without crashes
  5. fallbackToDestructiveMigration is gated on BuildConfig.DEBUG only; relative image paths are the established pattern in CardEntity from day one
**Plans**: 4 plans

Plans:
- [x] 01-01-PLAN.md — Gradle setup: Compose BOM, KSP, Room, Hilt, CameraX, ML Kit, Navigation, Coil dependencies configured in build.gradle.kts
- [x] 01-02-PLAN.md — Room schema: CardEntity, TagEntity, CardTagCrossRef, CardWithTags POJO, CardDao, TagDao, AppDatabase with schema export enabled
- [x] 01-03-PLAN.md — Hilt modules: DatabaseModule, StorageModule, OcrModule, RepositoryModule; repository interfaces and implementations; all bindings wired
- [x] 01-04-PLAN.md — Navigation scaffold: theme, AppNavHost with all five screen stubs, type-safe routes via @Serializable objects, MainActivity with bottom nav
**UI hint**: yes

### Phase 2: Camera + OCR
**Goal**: Users can photograph or import a business card, see ML Kit OCR extract and parse fields into a correction screen, and save the card (photo + parsed data) to Room — the complete scan-to-save core loop
**Depends on**: Phase 1
**Requirements**: SCAN-01, SCAN-02, SCAN-03, SCAN-04, SCAN-05, CARD-01
**Success Criteria** (what must be TRUE):
  1. User can open a camera preview, capture a business card photo, and have OCR run automatically without manual intervention
  2. User can select a photo from the device gallery and have it run through the same OCR pipeline as camera capture
  3. ML Kit extracts text from both Latin and Korean business cards using the bundled (offline) model — no internet connection required
  4. The OCR correction screen pre-fills name, company, job title, phone, email, and address fields from parsed OCR output, and the user can edit any field before saving
  5. After saving, a card record exists in Room with the compressed image stored in internal storage (relative path only in DB) and all parsed fields populated
**Plans**: TBD

Plans:
- [ ] 02-01: CameraX integration — ScanScreen with PreviewView, ImageCapture bound in LaunchedEffect, CAMERA permission request flow, 1080p capture cap
- [ ] 02-02: ML Kit OCR pipeline — OcrDataSource wrapping TextRecognizer (bundled Latin + Korean), suspendCoroutine adapter, InputImage from file
- [ ] 02-03: Bounding-box-aware field parser — ParseOcrResultUseCase (pure Kotlin, no Android deps), coordinate-aware label-value association for Korean dual-column layouts, unit tests against sample cards
- [ ] 02-04: Gallery import — ActivityResultContracts.GetContent picker plugged into same OCR pipeline as camera path
- [ ] 02-05: OcrReviewScreen + save — pre-filled correction form, ScanViewModel.saveCard(), ImageStorageDataSource (UUID filename, compress to 1024px JPEG 85%), CARD-01 Room insert
**UI hint**: yes

### Phase 3: CRUD + Tags + Search
**Goal**: Users can browse all saved cards in a searchable list, view full card details, edit or delete any card, add per-card memos, and organize cards with multi-tag assignment and tag-based filtering
**Depends on**: Phase 2
**Requirements**: CARD-02, CARD-03, CARD-04, BROWSE-01, BROWSE-02, BROWSE-03, TAG-01, TAG-02, TAG-03
**Success Criteria** (what must be TRUE):
  1. User can see all saved cards in a scrollable list showing thumbnail, name, and company name; tapping a card opens a full detail view with photo, all parsed fields, tags, and memo
  2. User can type in a search bar and the card list filters in real time across name, company, and job title (Korean and Latin text both work)
  3. User can edit any field on a saved card (reusing the OcrReview form) and save changes, and can delete a card after confirming a dialog
  4. User can add or edit a plain-text memo on any card and see it in the detail view
  5. User can create tags, attach multiple tags to a card, and filter the card list to show only cards matching a selected tag (OR logic via filter chips above the list)
**Plans**: TBD

Plans:
- [ ] 03-01: CardListScreen + CardListViewModel — LazyColumn with Coil AsyncImage (decode size specified), reactive Flow<List<CardWithTags>> from Room, FAB to scan
- [ ] 03-02: CardDetailScreen + CardDetailViewModel — full card view with inline edit mode (reusing OcrReview form), delete with confirmation dialog
- [ ] 03-03: Real-time search — LIKE-based DAO query (not FTS) for Korean text compatibility, debounced StateFlow search input, Material3 SearchBar
- [ ] 03-04: Per-card memo — memo field in edit form, CARD-04 persistence in CardEntity, display in detail view
- [ ] 03-05: Tags — TagManagerScreen, tag CRUD via TagRepository, multi-tag chip selector on card edit screen, TAG-03 filter chips above CardListScreen
**UI hint**: yes

### Phase 4: Export + Polish
**Goal**: Users can export any card's parsed data to the Android Contacts app in one tap, and the app handles edge cases gracefully — revoked camera permission, empty card list, empty search results, and missing card images
**Depends on**: Phase 3
**Requirements**: EXPORT-01
**Success Criteria** (what must be TRUE):
  1. User can tap an export button on any card detail screen and the Android Contacts app opens pre-filled with the card's name, company, job title, phone, and email — no special permissions required from the user
  2. If camera permission has been revoked after first grant, the app detects this on resume and navigates to a permission explanation screen rather than crashing
  3. An empty card list shows a meaningful empty state (illustration + prompt to scan first card) instead of a blank screen
  4. A search with no matching results shows a "no results" message instead of a blank list
**Plans**: TBD

Plans:
- [ ] 04-01: Contacts export — ContactsDataSource firing ContactsContract.Intents.Insert.ACTION Intent, export button wired in CardDetailScreen, EXPORT-01
- [ ] 04-02: Permission revocation handling — ON_RESUME camera permission re-check in ScanScreen, navigation to permission rationale screen if revoked
- [ ] 04-03: Empty states and polish — empty list state, empty search state, Coil image error placeholder, final UX review and edge case fixes
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 3/4 | In Progress|  |
| 2. Camera + OCR | 0/5 | Not started | - |
| 3. CRUD + Tags + Search | 0/5 | Not started | - |
| 4. Export + Polish | 0/3 | Not started | - |
