---
phase: 03-crud-tags-search
plan: "01"
subsystem: card-list
tags: [card-list, lazy-column, coil, viewmodel, empty-state]
dependency_graph:
  requires: ["02-01", "02-02", "02-03", "02-04", "02-05"]
  provides: ["card-list-screen", "card-list-viewmodel", "reactive-card-flow"]
  affects: ["CardListScreen", "CardListViewModel"]
tech_stack:
  added: ["CardListUiState", "LazyColumn", "coil3.compose.AsyncImage"]
  patterns: ["StateFlow-collectAsState", "LazyColumn-items-with-key", "empty-state"]
key_files:
  created:
    - app/src/main/kotlin/com/cardkeeper/ui/card/CardListViewModel.kt
    - app/src/main/kotlin/com/cardkeeper/ui/card/CardListScreen.kt
  modified: []
decisions:
  - "Row-based list item instead of ListItem — avoids Material3 ListItem API instability (headlineContent vs title)"
  - "coil3.compose.AsyncImage with direct path string — simple and sufficient for internal storage files"
  - "CardListUiState holds cards, isLoading, error in single StateFlow — single source of truth"
  - "Empty state shown when cards.isEmpty() — meaningful prompt instead of blank screen"
  - "Icons.AutoMirrored.Filled.ArrowBack — correct RTL-aware icon import"
  - "key = { it.card.id } in LazyColumn items — stable composable identity during recomposition"
metrics:
  duration: "8min"
  completed_date: "2026-03-25"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 2
---

# Phase 03 Plan 01: CardListScreen + CardListViewModel Summary

Scrollable card list showing thumbnails, names, and company names with FAB to scan new cards. Reactive Flow from Room ensures new cards appear immediately.

## What Was Built

### Task 1: CardListViewModel

- `data class CardListUiState(cards, isLoading, error)` — single UI state holder
- `loadCards()` subscribes to `cardRepository.getAllCards()` Flow — Room reactive updates
- `refresh()` restarts Flow subscription for pull-to-refresh
- Error captured in state for snackbar display

### Task 2: CardListScreen

- `Scaffold` with TopAppBar ("My Cards") + back arrow + FAB (Add icon)
- `LazyColumn` with `items(uiState.cards, key = { it.card.id })` — efficient scrolling
- `CardListItem` composable: Row layout with AsyncImage thumbnail (64dp), name, company, first tag
- Empty state: "No cards yet" + "Tap the + button to scan your first card"
- Loading state: centered CircularProgressIndicator
- Error state: snackbar via `LaunchedEffect(uiState.error)`

## Acceptance Criteria Verification

| Criterion | Status |
|-----------|--------|
| CardListViewModel.kt contains `data class CardListUiState(cards, isLoading, error)` | PASS |
| CardListViewModel.kt contains `fun loadCards()` subscribing to `cardRepository.getAllCards()` | PASS |
| CardListViewModel.kt contains `fun refresh()` calling `loadCards()` | PASS |
| CardListScreen.kt contains `LazyColumn` with `items(uiState.cards)` | PASS |
| CardListScreen.kt contains `AsyncImage` from coil3.compose | PASS |
| CardListScreen.kt contains `CardListItem` composable | PASS |
| CardListScreen.kt contains `FloatingActionButton` for scan | PASS |
| CardListScreen.kt contains empty state text + prompt | PASS |
| CardListScreen.kt contains loading indicator | PASS |
| `./gradlew :app:assembleDebug` succeeds | PASS |

## Build Verification

`./gradlew :app:assembleDebug` — **BUILD SUCCESSFUL** (8s)

## Self-Check: PASSED

Files verified present and compiling:
- `app/src/main/kotlin/com/cardkeeper/ui/card/CardListViewModel.kt`
- `app/src/main/kotlin/com/cardkeeper/ui/card/CardListScreen.kt`
