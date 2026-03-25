---
phase: 03-crud-tags-search
plan: "02"
subsystem: card-detail
tags: [card-detail, edit-mode, delete-dialog, viewmode, detail-row]
dependency_graph:
  requires: ["03-01"]
  provides: ["card-detail-screen", "card-detail-viewmodel", "edit-and-delete"]
  affects: ["CardDetailScreen", "CardDetailViewModel", "AppNavHost"]
tech_stack:
  added: ["CardDetailUiState", "EditMode", "ViewMode", "AlertDialog"]
  patterns: ["view/edit toggle", "delete confirmation dialog", "Flow-based card loading"]
key_files:
  created:
    - app/src/main/kotlin/com/cardkeeper/ui/carddetail/CardDetailViewModel.kt
  modified:
    - app/src/main/kotlin/com/cardkeeper/ui/carddetail/CardDetailScreen.kt
    - app/src/main/kotlin/com/cardkeeper/ui/navigation/AppNavHost.kt
    - app/src/main/kotlin/com/cardkeeper/ui/cardlist/CardListScreen.kt (fixed package)
    - app/src/main/kotlin/com/cardkeeper/ui/cardlist/CardListViewModel.kt (moved to correct package)
decisions:
  - "ViewModel injected internally via hiltViewModel() — matches CardListScreen pattern"
  - "View/Edit toggle via isEditing state — single screen handles both modes"
  - "Delete confirmation dialog before cardRepository.deleteCard() — prevents accidental loss"
  - "card == null && !isLoading triggers onDeleted() popBackStack — clean navigation after delete"
  - "Fixed 03-01 package mismatch: moved CardListScreen from ui.card to ui.cardlist, deleted duplicates"
  - "CardListScreen keeps original callback signatures (onCardClick: Long, onScanClick, onTagManagerClick)"
metrics:
  duration: "12min"
  completed_date: "2026-03-25"
  tasks_completed: 3
  tasks_total: 3
  files_changed: 5
---

# Phase 03 Plan 02: CardDetailScreen + CardDetailViewModel Summary

Full card detail view with photo, all parsed fields, tags, edit mode with 6-field form, and delete confirmation dialog.

## What Was Built

### Fix: 03-01 Package Mismatch
CardListScreen was created in `ui.card` but AppNavHost imports from `ui.cardlist`. Fixed by replacing the `ui.cardlist` stub with the full implementation and deleting `ui.card` duplicates. CardListViewModel also moved to `ui.cardlist`.

### Task 1: CardDetailViewModel
- `CardDetailUiState` with card, isLoading, isEditing, isSaving, showDeleteDialog, error
- `loadCard(cardId)` subscribes to `cardRepository.getCardById()` Flow
- `saveCard()` updates CardEntity with edited fields + updatedAt timestamp
- `deleteCard()` calls cardRepository.deleteCard() after confirmation dialog

### Task 2: CardDetailScreen
- **ViewMode**: AsyncImage card photo (200dp, rounded), 6 DetailRow fields (한국어+English labels), tag chips
- **EditMode**: 6 OutlinedTextField fields with remember + mutableStateOf, Save/Cancel buttons
- **Delete**: AlertDialog confirmation dialog before deletion
- **Navigation**: Edit icon and Delete icon in TopAppBar actions; onDeleted() pops back stack

### Task 3: AppNavHost Wiring
- CardDetailRoute composable now uses hiltViewModel for CardDetailViewModel
- LaunchedEffect observes uiState.card — if null after load, pops back stack

## Acceptance Criteria Verification

| Criterion | Status |
|-----------|--------|
| CardDetailViewModel loads card by id via Flow | PASS |
| CardDetailScreen shows card photo + all fields in ViewMode | PASS |
| Edit icon in TopAppBar toggles to EditMode | PASS |
| EditMode has 6 editable fields + Save/Cancel | PASS |
| Save calls cardRepository.updateCard() with new values | PASS |
| Delete icon shows confirmation dialog | PASS |
| Delete calls cardRepository.deleteCard() then navigates back | PASS |
| Tags displayed as FilledTonalButton chips | PASS |
| `./gradlew :app:assembleDebug` succeeds | PASS |

## Build Verification

`./gradlew :app:assembleDebug` — **BUILD SUCCESSFUL** (4s)
