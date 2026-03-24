---
phase: 01-foundation
plan: 03
subsystem: database
tags: [hilt, dagger, di, room, kotlin, repository-pattern, clean-architecture]

# Dependency graph
requires:
  - phase: 01-02
    provides: Room DAOs (CardDao, TagDao), AppDatabase, entities (CardEntity, TagEntity, CardTagCrossRef)
provides:
  - CardRepository and TagRepository interfaces in domain layer
  - CardRepositoryImpl and TagRepositoryImpl with @Inject constructors in data layer
  - ImageStorageDataSource stub with @ApplicationContext in data.datasource
  - DatabaseModule providing AppDatabase, CardDao, TagDao as singletons
  - RepositoryModule binding interfaces to implementations via @Binds
  - StorageModule and OcrModule as Phase 2 stubs
affects: [02-viewmodels, 03-ui-features, all-phases-using-injection]

# Tech tracking
tech-stack:
  added: [Hilt DI modules (DatabaseModule, RepositoryModule, StorageModule, OcrModule)]
  patterns: [Repository pattern with domain/data layer separation, @Binds for @Inject-constructible impls, @Provides for framework objects (Room/ML Kit), abstract class for @Binds modules]

key-files:
  created:
    - app/src/main/kotlin/com/cardkeeper/domain/repository/CardRepository.kt
    - app/src/main/kotlin/com/cardkeeper/domain/repository/TagRepository.kt
    - app/src/main/kotlin/com/cardkeeper/data/repository/CardRepositoryImpl.kt
    - app/src/main/kotlin/com/cardkeeper/data/repository/TagRepositoryImpl.kt
    - app/src/main/kotlin/com/cardkeeper/data/datasource/ImageStorageDataSource.kt
    - app/src/main/kotlin/com/cardkeeper/di/DatabaseModule.kt
    - app/src/main/kotlin/com/cardkeeper/di/RepositoryModule.kt
    - app/src/main/kotlin/com/cardkeeper/di/StorageModule.kt
    - app/src/main/kotlin/com/cardkeeper/di/OcrModule.kt
  modified: []

key-decisions:
  - "@Binds used in abstract class RepositoryModule for interface-to-impl bindings (requires abstract class, not object)"
  - "@Provides used in object modules for Room and ML Kit (framework objects without @Inject constructors)"
  - "searchCards wraps query with % wildcards in impl layer, not at DAO call sites"
  - "TagRepositoryImpl.setTagsForCard uses clear-then-insert pattern (deleteTagsForCard + insertCrossRef per tag)"

patterns-established:
  - "Pattern 1: Repository interfaces in domain/repository, implementations in data/repository"
  - "Pattern 2: @Binds in abstract class for @Inject-constructible implementations"
  - "Pattern 3: @Provides in object for framework/external objects"
  - "Pattern 4: @Inject constructor on all data layer classes for automatic Hilt wiring"

requirements-completed: []

# Metrics
duration: 20min
completed: 2026-03-24
---

# Phase 1 Plan 3: Hilt DI Wiring Summary

**Hilt DI wired end-to-end: DatabaseModule->AppDatabase/DAOs, RepositoryModule->@Binds CardRepositoryImpl/TagRepositoryImpl, StorageModule/OcrModule as Phase 2 stubs — assembleDebug BUILD SUCCESSFUL**

## Performance

- **Duration:** 20 min
- **Started:** 2026-03-24T13:18:36Z
- **Completed:** 2026-03-24T13:38:36Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments
- Two repository interfaces (CardRepository, TagRepository) in domain layer with Flow-based reactive queries
- Two @Inject constructor implementations (CardRepositoryImpl, TagRepositoryImpl) in data layer
- ImageStorageDataSource stub with @ApplicationContext for Phase 2 image handling
- Four Hilt modules compile and Hilt code generation completes without errors (BUILD SUCCESSFUL)
- Dependency chain established: DatabaseModule -> AppDatabase/DAOs -> RepositoryModule -> CardRepositoryImpl/TagRepositoryImpl

## Task Commits

Each task was committed atomically:

1. **Task 1: Create repository interfaces and implementations** - `25e7c06` (feat)
2. **Task 2: Create Hilt DI modules** - `0c9643e` (feat)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified
- `app/src/main/kotlin/com/cardkeeper/domain/repository/CardRepository.kt` - Interface with Flow-based getAllCards, getCardById, searchCards, and suspend insertCard/updateCard/deleteCard
- `app/src/main/kotlin/com/cardkeeper/domain/repository/TagRepository.kt` - Interface with getAllTags, insertTag, deleteTag, setTagsForCard
- `app/src/main/kotlin/com/cardkeeper/data/repository/CardRepositoryImpl.kt` - @Inject constructor impl wrapping CardDao, searchCards adds % wildcards
- `app/src/main/kotlin/com/cardkeeper/data/repository/TagRepositoryImpl.kt` - @Inject constructor impl wrapping TagDao, setTagsForCard uses clear-then-insert
- `app/src/main/kotlin/com/cardkeeper/data/datasource/ImageStorageDataSource.kt` - @Inject constructor stub with @ApplicationContext, Phase 2 placeholder
- `app/src/main/kotlin/com/cardkeeper/di/DatabaseModule.kt` - @Provides AppDatabase singleton, CardDao, TagDao
- `app/src/main/kotlin/com/cardkeeper/di/RepositoryModule.kt` - abstract class with @Binds for CardRepository and TagRepository
- `app/src/main/kotlin/com/cardkeeper/di/StorageModule.kt` - @Provides ImageStorageDataSource for Phase 2
- `app/src/main/kotlin/com/cardkeeper/di/OcrModule.kt` - @Provides ML Kit TextRecognizer for Phase 2

## Decisions Made
- RepositoryModule is an `abstract class` (not `object`) — required by Dagger/Hilt for `@Binds` abstract methods
- `@Binds` used for repo bindings (impls have `@Inject constructor`, so Hilt can construct them automatically)
- `@Provides` used for DatabaseModule (Room's `AppDatabase.getInstance()` cannot have @Inject constructor) and OcrModule (ML Kit client factory)
- `searchCards` wraps the query with `%` wildcards in `CardRepositoryImpl` — keeps DAO signature clean and callers pass plain strings

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The build succeeded on the first attempt. A Kotlin compiler informational warning about `@ApplicationContext` annotation target in `ImageStorageDataSource.kt` was observed but is not an error — it relates to a future Kotlin behavior change (KT-73255) and does not affect compilation or runtime behavior.

## Known Stubs

- `ImageStorageDataSource.kt` — body is empty (comment-only). This is intentional; the plan explicitly designates it as a Phase 2 stub. All methods (saveImage, deleteImage, getImageFile) will be implemented in Phase 2 when camera/gallery capture is implemented.
- `StorageModule.kt` — provides the stub `ImageStorageDataSource` to satisfy Phase 2 injection sites. No data flows through it until Phase 2.
- `OcrModule.kt` — provides `TextRecognizer` singleton. The recognizer is initialized here but not called until Phase 2 OCR implementation.

These stubs do not prevent this plan's goal (Hilt DI compiles end-to-end). They are intentional scaffolding for Phase 2.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Hilt DI is fully wired. Any future `@HiltViewModel` with `@Inject constructor(private val cardRepository: CardRepository)` will resolve at compile time
- Phase 2 ViewModels can inject CardRepository or TagRepository directly with no additional module changes
- ImageStorageDataSource and TextRecognizer are pre-wired — Phase 2 only needs to implement the method bodies
- No blockers for Phase 2

---
*Phase: 01-foundation*
*Completed: 2026-03-24*

## Self-Check: PASSED

All 10 files verified present on disk. Both task commits (25e7c06, 0c9643e) confirmed in git log.
