---
phase: 02-camera-ocr
plan: 05
status: completed
completed_at: "2026-03-24"
---

# 02-05 Summary: OcrReviewScreen + Save

## What was built

- **ImageStorageDataSource.kt**: Full implementation with `saveImage()` (scales to 1024px max, JPEG 85%), `deleteImage()`, `getImageFile()`. Returns relative path `cards/{uuid}.jpg` stored in Room.
- **ScanViewModel.kt**: Expanded with `OcrDataSource`, `ParseOcrResultUseCase`, `ImageStorageDataSource`, `CardRepository` injections. Added `ReviewFormState`, `ScanState.LoadingOcr/Saving/Saved`, `loadOcrResult()`, `saveCard()`, 6 `update*()` methods, `formState: StateFlow<ReviewFormState>`.
- **OcrReviewScreen.kt**: Full correction form with 6 `OutlinedTextField` fields (Name/Company/Job Title/Phone/Email/Address), `TopAppBar`, loading overlay on OCR, Save button with progress indicator, `SnackbarHost` for errors. Navigates to CardListScreen on save.

## Scan-to-save loop completed

```
ScanScreen → capture/gallery → setTempImagePath() → OcrReviewRoute
OcrReviewScreen → LaunchedEffect → loadOcrResult() → OcrDataSource → ParseOcrResultUseCase → formState
Save button → saveCard() → ImageStorageDataSource.saveImage() → CardRepository.insertCard() → ScanState.Saved → onSaved() → CardListScreen
```

## Requirements satisfied

- SCAN-05: Full correction form with pre-filled OCR fields
- CARD-01: Card persisted to Room with compressed image at relative path
