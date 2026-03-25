---
phase: 03-crud-tags-search
plan: "03-05"
subsystem: tags-search-memo
tags: [search, debounce, memo, tag-crud, tag-filter, filter-chips, tag-selector]
dependency_graph:
  requires: ["03-01", "03-02"]
  provides: ["real-time-search", "per-card-memo", "tag-management", "tag-filtering"]
  affects: ["CardListScreen", "CardListViewModel", "CardDetailScreen", "CardDetailViewModel", "TagManagerScreen", "TagManagerViewModel"]
tech_stack:
  added: ["CardEditData", "debounce(300)", "distinctUntilChanged", "flatMapLatest", "filter chips"]
  patterns: ["search-query-debounce", "tag-chip-selector", "filter-all-chip", "memo-field-in-edit"]
key_files:
  created:
    - app/src/main/kotlin/com/cardkeeper/ui/tags/TagManagerViewModel.kt
  modified:
    - app/src/main/kotlin/com/cardkeeper/ui/cardlist/CardListScreen.kt (search bar + tag filter chips)
    - app/src/main/kotlin/com/cardkeeper/ui/cardlist/CardListViewModel.kt (debounced search + tag filter)
    - app/src/main/kotlin/com/cardkeeper/ui/carddetail/CardDetailScreen.kt (memo view/edit + tag selector)
    - app/src/main/kotlin/com/cardkeeper/ui/carddetail/CardDetailViewModel.kt (tag injection + save with tags)
    - app/src/main/kotlin/com/cardkeeper/ui/tags/TagManagerScreen.kt (tag CRUD UI)
decisions:
  - "debounce(300) + distinctUntilChanged on search query — prevents excessive Room queries while typing"
  - "LIKE-based search (not FTS) — Korean text compatibility for v1, already in CardDao"
  - "TagRepository.getAllTags() loaded in CardDetailViewModel — available for tag selector in edit mode"
  - "CardEditData data class wraps 7 edit fields — avoids 7-parameter lambda type inference issues"
  - "Tag filter chips in CardListScreen — 'All' chip resets filter, selected tags show checkmark"
  - "Tag selector in CardDetailScreen edit mode — toggle chips to add/remove tags per card"
  - "Memo displayed only when non-blank in view mode — avoids empty section clutter"
metrics:
  duration: "15min"
  completed_date: "2026-03-25"
  tasks_completed: 3
  tasks_total: 3
  files_changed: 6
---

# Phase 03 Plans 03-05 Summary: Search + Memo + Tags

Combined implementation of real-time search, per-card memo, and tag management/filtering — completing all Phase 3 requirements.

## What Was Built

### 03-03: Real-time Search
- `OutlinedTextField` search bar with Korean+English placeholder
- `debounce(300)` + `distinctUntilChanged` + `flatMapLatest` in ViewModel
- Switches between `getAllCards()` and `searchCards()` based on query
- "No results" empty state for non-matching queries

### 03-04: Per-card Memo
- Memo field added to CardDetailScreen edit mode (minLines = 3)
- Memo saved via `CardEntity.copy(memo = memo)` in ViewModel
- Memo displayed in view mode only when non-blank

### 03-05: Tags
- **TagManagerScreen**: Full CRUD — add tag dialog, delete confirmation, list of all tags
- **TagManagerViewModel**: Hilt-injected, subscribes to `TagRepository.getAllTags()`
- **CardDetailScreen edit mode**: Tag chip selector — toggle tags on/off with checkmark
- **CardListScreen**: Tag filter chips above card list — "All" + one per tag, checkmark on selected
- **CardDetailViewModel**: Injects `TagRepository`, saves tags via `setTagsForCard()` on card save

## Acceptance Criteria Verification

| Criterion | Status |
|-----------|--------|
| Real-time search filters name, company, jobTitle | PASS |
| LIKE-based query for Korean compatibility | PASS |
| Debounced input (300ms) | PASS |
| Memo field in edit mode | PASS |
| Memo displayed in view mode when non-blank | PASS |
| TagManagerScreen with add/delete | PASS |
| Tag selector in card edit mode | PASS |
| Filter chips in card list | PASS |
| `./gradlew :app:assembleDebug` succeeds | PASS |

## Build Verification

`./gradlew :app:assembleDebug` — **BUILD SUCCESSFUL** (11s)

## Phase 3 Completion

All 5 plans complete:
- 03-01: CardListScreen + CardListViewModel
- 03-02: CardDetailScreen (view/edit/delete)
- 03-03: Real-time search
- 03-04: Per-card memo
- 03-05: Tags (CRUD + filter + selector)

All requirements satisfied: BROWSE-01, BROWSE-02, BROWSE-03, CARD-02, CARD-03, CARD-04, TAG-01, TAG-02, TAG-03.

## Next: Phase 4 (Export + Polish)
