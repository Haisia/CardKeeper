---
phase: 01-foundation
plan: 02
subsystem: data/db
tags: [room, database, entities, dao, schema]
dependency_graph:
  requires: ["01-01"]
  provides: ["Room schema", "CardDao", "TagDao", "AppDatabase", "schema-export-json"]
  affects: ["01-03", "01-04", "all data access in subsequent phases"]
tech_stack:
  added: ["Room 2.8.4 entities", "Room DAOs", "Room schema export JSON"]
  patterns: ["three-table many-to-many", "@Junction for relation loading", "DEBUG-only destructive migration guard", "tagId index on junction table"]
key_files:
  created:
    - app/src/main/kotlin/com/cardkeeper/data/db/CardEntity.kt
    - app/src/main/kotlin/com/cardkeeper/data/db/TagEntity.kt
    - app/src/main/kotlin/com/cardkeeper/data/db/CardTagCrossRef.kt
    - app/src/main/kotlin/com/cardkeeper/data/db/CardWithTags.kt
    - app/src/main/kotlin/com/cardkeeper/data/db/CardDao.kt
    - app/src/main/kotlin/com/cardkeeper/data/db/TagDao.kt
    - app/src/main/kotlin/com/cardkeeper/data/db/AppDatabase.kt
    - app/schemas/com.cardkeeper.data.db.AppDatabase/1.json
  modified: []
decisions:
  - "Index added on tagId in CardTagCrossRef to prevent full table scans on FK parent modification (Room KSP warning)"
metrics:
  duration: "5 minutes"
  completed: "2026-03-24"
  tasks_completed: 2
  files_created: 8
---

# Phase 01 Plan 02: Room Database Schema Summary

**One-liner:** Three-table Room schema (cards/tags/junction) with CASCADE deletes, @Junction relation loading, DEBUG-only destructive migration guard, and schema export JSON committed.

## What Was Built

Complete Room database layer for CardKeeper:

1. **CardEntity** — `cards` table with 10 fields: auto-PK, nullable imagePath (relative paths only), name/company/jobTitle/phone/email/address, memo (default ""), createdAt/updatedAt (epoch millis)
2. **TagEntity** — `tags` table with auto-PK and name
3. **CardTagCrossRef** — Junction table with composite PK `[cardId, tagId]`, CASCADE deletes on both foreign keys, and index on `tagId` for query performance
4. **CardWithTags** — Query-result POJO (NOT an @Entity) using @Embedded + @Relation/@Junction for many-to-many loading
5. **CardDao** — 6 methods: 3 @Transaction query methods returning CardWithTags/List<CardWithTags>, plus insert/update/delete
6. **TagDao** — 5 methods: getAllTags, insertTag, deleteTag, insertCrossRef, deleteTagsForCard
7. **AppDatabase** — version 1, exportSchema=true, all 3 entities registered, DEBUG-only fallbackToDestructiveMigration guard
8. **Schema JSON** — `app/schemas/com.cardkeeper.data.db.AppDatabase/1.json` generated and committed (5061 bytes)

## Verification

- `./gradlew :app:kspDebugKotlin` — BUILD SUCCESSFUL, no errors or warnings
- Room generated: `AppDatabase_Impl.kt`, `CardDao_Impl.kt`, `TagDao_Impl.kt`
- Schema JSON exported with all 3 tables, correct CASCADE foreign keys, tagId index
- 3 @Transaction annotations in CardDao (confirmed via grep)
- BuildConfig.DEBUG guard confirmed in AppDatabase

Note: `./gradlew :app:assembleDebug` fails at `mergeDebugResources` due to pre-existing AAPT resource errors from other parallel agents (missing `Theme.Material3.DayNight.NoActionBar` in AndroidManifest). This is out of scope for plan 01-02 — our Room layer compiles cleanly.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical Functionality] Added index on tagId in CardTagCrossRef**
- **Found during:** Task 2 (KSP compilation)
- **Issue:** Room KSP warned that `tagId` column in the junction entity is used to resolve a @Relation but not covered by any index, causing full table scans on parent table modification
- **Fix:** Added `indices = [Index("tagId")]` to CardTagCrossRef @Entity annotation
- **Files modified:** `app/src/main/kotlin/com/cardkeeper/data/db/CardTagCrossRef.kt`
- **Commit:** 587472d

## Commits

| Commit | Message | Files |
|--------|---------|-------|
| f531c2e | feat(01-02): create Room entities and CardWithTags POJO | CardEntity.kt, TagEntity.kt, CardTagCrossRef.kt, CardWithTags.kt |
| 587472d | feat(01-02): create DAOs, AppDatabase, and schema export | CardDao.kt, TagDao.kt, AppDatabase.kt, CardTagCrossRef.kt (index fix), 1.json |

## Known Stubs

None — all seven files implement their full intended functionality. Schema JSON is committed to source control.

## Self-Check: PASSED

- app/src/main/kotlin/com/cardkeeper/data/db/CardEntity.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/data/db/TagEntity.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/data/db/CardTagCrossRef.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/data/db/CardWithTags.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/data/db/CardDao.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/data/db/TagDao.kt — FOUND
- app/src/main/kotlin/com/cardkeeper/data/db/AppDatabase.kt — FOUND
- app/schemas/com.cardkeeper.data.db.AppDatabase/1.json — FOUND
- Commit f531c2e — FOUND
- Commit 587472d — FOUND
