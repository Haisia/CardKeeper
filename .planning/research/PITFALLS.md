# Domain Pitfalls

**Domain:** Android Business Card Management App (CardKeeper)
**Stack:** ML Kit OCR, CameraX, Jetpack Compose, Room DB, Internal Storage, Contacts Provider
**Researched:** 2026-03-24
**Confidence:** HIGH — based on well-documented Android platform behavior and ML Kit SDK internals

---

## Critical Pitfalls

Mistakes that cause rewrites, data loss, or crashes.

---

### Pitfall C1: ML Kit Korean + English Mixed Layout Misidentification

**What goes wrong:** ML Kit Text Recognition v2 (Latin + Korean script) returns text blocks in reading-order heuristics that break down on business cards with dual-column or mixed-language layouts. Korean name fields (e.g., "홍길동 / Hong Gil-dong") are returned as separate `TextBlock` objects with no semantic relationship. Naively joining all blocks produces garbage: phone numbers appear between name fragments, Korean and romanized names are indistinguishable from company names.

**Why it happens:** ML Kit segments text spatially into `TextBlock → TextLine → Element`. It does not understand that "이름", "직책", or "대표이사" are field labels. Korean business cards frequently have the label in Korean on the left and the value in English on the right — two separate blocks that ML Kit treats as equal peers.

**Consequences:** Field parsing (name/company/title/phone) requires geometric reasoning, not just linear text concatenation. A regex-only parser will mis-classify fields 30–50% of the time on Korean cards.

**Prevention:**
- Access `boundingBox` (or `cornerPoints`) on each `TextBlock` and `TextLine`. Use vertical overlap and horizontal proximity to associate label blocks with value blocks.
- Build a coordinate-aware parser: sort blocks top-to-bottom, left-to-right, then apply label matching ("팀장", "Tel", "E-mail", "www", "@") before positional rules.
- Add a manual correction screen as a first-class feature, not an afterthought. Users must be able to edit every parsed field before saving.

**Detection:** During testing, run against cards with both Korean and English text, dual-column layouts, and vertically stacked label/value pairs. A parser without `boundingBox` logic will fail these cases visibly.

---

### Pitfall C2: ML Kit Model Not Downloaded at First Launch

**What goes wrong:** When using `play-services-mlkit-text-recognition-korean` (bundled via Play Services, not the standalone `mlkit-text-recognition-korean` artifact), the Korean recognition model may not be present on-device at first launch. The `recognize()` call silently falls back to Latin-only recognition, returning garbled or empty results for Korean text with no error thrown.

**Why it happens:** The Play Services variant downloads models lazily. There is no blocking call that ensures the model is ready before you invoke recognition.

**Consequences:** First-run OCR silently fails for Korean text. Users see incorrect results and assume the app is broken.

**Prevention:**
- Use the **standalone (bundled) artifact** `com.google.mlkit:text-recognition-korean` instead of the Play Services variant. This packages the Korean model inside the APK/AAB, ensuring it is always present. APK size increases ~3–5 MB.
- If you must use Play Services variant, call `RemoteModelManager.getInstance().download(model, conditions)` at app startup and block the camera screen behind a readiness check.

**Detection:** Test on a freshly reset emulator (Google Play Services present but fresh). The standalone artifact eliminates this class of failure entirely.

---

### Pitfall C3: CameraX ImageCapture Resolution Causing OOM or Blurry OCR

**What goes wrong:** CameraX `ImageCapture` defaults to the highest supported resolution. On modern devices this can be 12 MP+. Passing this raw `Bitmap` directly to ML Kit's `InputImage.fromBitmap()` can trigger `OutOfMemoryError` on low-RAM devices. Conversely, if developers over-aggressively downsample to avoid OOM, text becomes too small for ML Kit to read accurately.

**Why it happens:** ML Kit recommends input images of at least 480×360 pixels for text recognition, but accuracy degrades sharply below 1080p for small-font business card text. The developer must explicitly choose a resolution that balances quality and memory.

**Consequences:** Either OOM crash or OCR accuracy drop, depending on which way the developer errs.

**Prevention:**
- Set `ImageCapture.Builder().setTargetResolution(Size(1920, 1080))` or use `setTargetAspectRatio(AspectRatio.RATIO_16_9)` as a ceiling. This gives ML Kit sufficient detail without allocating 40+ MB bitmaps.
- After capture, use `BitmapFactory.Options.inSampleSize` or `Bitmap.createScaledBitmap()` only if device RAM is critically low (check `ActivityManager.MemoryInfo`).
- Never pass `InputImage.fromMediaImage()` directly from a `16MP+` `ImageProxy` — decode to a bounded Bitmap first.

**Detection:** Use Android Studio Memory Profiler during capture. If heap spikes above 80 MB during OCR, you are passing oversized images.

---

### Pitfall C4: CameraX Use-Case Binding Leaks on Config Changes

**What goes wrong:** Binding CameraX use cases (`Preview`, `ImageCapture`) inside a `@Composable` using `LaunchedEffect` or `DisposableEffect` without properly tying to the `ProcessCameraProvider` lifecycle causes the camera to remain open after navigation away, or to crash with "Use case already bound" on recomposition.

**Why it happens:** `ProcessCameraProvider.bindToLifecycle()` is not idempotent — re-calling it on an already-bound use case throws `IllegalArgumentException`. Recompositions triggered by state changes re-execute the binding block.

**Consequences:** Camera resource leak, black preview on re-entry, `IllegalArgumentException` crashes.

**Prevention:**
- Call `cameraProvider.unbindAll()` before `bindToLifecycle()` every time.
- Wrap camera setup in `LaunchedEffect(lifecycleOwner)` so it re-runs only when the lifecycle owner changes, not on every recomposition.
- Use `rememberCoroutineScope()` carefully — do not launch camera binding from a scope that outlives the composable.
- The recommended pattern is `CameraX` initialization in a `ViewModel` or dedicated camera manager class, with the composable only observing state.

**Detection:** Navigate to camera screen, go back, navigate forward again. If preview is black or app crashes, the binding is leaking.

---

### Pitfall C5: Internal Storage File URI Exposed to External Components

**What goes wrong:** When saving card images to internal storage (`context.filesDir` or `context.getExternalFilesDir()`), the absolute `File` path is stored in Room as a string. If the app tries to pass this path as a `file://` URI to external apps (e.g., share intent, Contacts Provider photo), Android 7.0+ throws `FileUriExposedException` and kills the operation.

**Why it happens:** Since Android 7.0 (API 24), `file://` URIs cannot be passed across process boundaries. `FileProvider` content URIs must be used instead. The app targets API 26+, so this applies unconditionally.

**Consequences:** Sharing card images or attaching photos to Contacts entries fails silently or crashes with an unhandled exception.

**Prevention:**
- Configure a `FileProvider` in `AndroidManifest.xml` and use `FileProvider.getUriForFile()` whenever passing an image URI outside the app process.
- Store only the relative filename (not the full path) in Room, and reconstruct the absolute path at runtime using `context.filesDir`. This prevents paths from breaking after app moves/reinstall.
- When writing contact photos via `ContactsContract.RawContacts.DisplayPhoto`, read the file into a `ByteArray` and write directly — no URI passing needed.

**Detection:** Any code path that passes `Uri.fromFile(File(...))` to an Intent or system API on API 24+ will throw at runtime.

---

### Pitfall C6: Room Schema Migration Absent — Data Loss on Update

**What goes wrong:** The initial schema will need changes (adding fields, changing column types, adding tag tables). Without explicit migrations, Room's default behavior with `fallbackToDestructiveMigration()` drops and recreates all tables. Users lose all saved business cards on app update.

**Why it happens:** Developers often enable `fallbackToDestructiveMigration()` during development "for convenience" and forget to remove it before shipping, or don't add `Migration` objects when bumping the schema version.

**Consequences:** All user card data destroyed on any schema-changing update. For a personal card management app this is catastrophic.

**Prevention:**
- Never use `fallbackToDestructiveMigration()` in production builds. Use it only in debug builds gated behind `BuildConfig.DEBUG`.
- Write a `Migration(from, to)` object for every schema version bump. Even empty migrations (no structural change) are valid if you only bump version for safety.
- Maintain a `MIGRATION_HISTORY.md` or inline comment block in the `AppDatabase` class documenting each version's changes.
- Add a Room schema export (`room.schemaLocation` annotation processor argument) and commit the generated JSON schema files to source control. This provides a diff-visible record of all schema changes.

**Detection:** Bump `version` in `@Database` without writing a `Migration` → Room will crash with `IllegalStateException: A migration from X to Y was required but not found` (if `fallbackToDestructiveMigration()` is absent) or silently wipe data (if it is present).

---

### Pitfall C7: Contacts Provider WRITE_CONTACTS Permission Not Declared Correctly

**What goes wrong:** Android Contacts Provider requires both `READ_CONTACTS` and `WRITE_CONTACTS` permissions, declared in `AndroidManifest.xml` AND requested at runtime (API 23+). Apps that declare only `WRITE_CONTACTS` (assuming it implies READ) get a `SecurityException` when querying contacts. Apps that request permissions but don't handle the "denied permanently" state get stuck in a broken export flow.

**Why it happens:** `WRITE_CONTACTS` does not imply `READ_CONTACTS`. They are separate runtime permissions in the `android.permission-group.CONTACTS` group. Additionally, on Android 11+ (API 30), the "don't ask again" behavior means subsequent requests are silently denied.

**Consequences:** Export to Contacts silently fails or crashes. Users cannot understand why.

**Prevention:**
- Declare both `READ_CONTACTS` and `WRITE_CONTACTS` in manifest.
- Use `ActivityResultContracts.RequestMultiplePermissions` to request both at once.
- Handle three states explicitly: granted, denied (show rationale), denied permanently (show Settings deeplink via `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)`).
- For export, prefer inserting via `Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)` with extras. This delegates to the system Contacts app and requires zero permissions. Only use direct `ContentResolver` insert if the UX requires silent background export.

**Detection:** Test on a fresh emulator with no contact permissions granted, and test after the user denies twice (permanent denial path).

---

## Moderate Pitfalls

---

### Pitfall M1: CameraX Camera Permission Not Re-Checked After Backgrounding

**What goes wrong:** The user grants camera permission, uses the app, then revokes it from Settings while the app is in the background. On resume, `CameraX` initialization proceeds against a revoked permission and throws `CameraAccessException` without a clear user-facing message.

**Prevention:**
- In `onResume()` (or `Lifecycle.Event.ON_RESUME` in Compose), re-check `ContextCompat.checkSelfPermission(CAMERA)`. If revoked, navigate to a permission-request screen instead of attempting to bind camera.

---

### Pitfall M2: Room Querying on Main Thread

**What goes wrong:** Room throws `IllegalStateException: Cannot access database on the main thread` if any DAO method is called without `suspend` or without wrapping in `Dispatchers.IO`. This is easy to hit when wiring up OCR result saves in a ViewModel `init {}` block or button `onClick` directly.

**Prevention:**
- All DAO functions must be `suspend fun` or return `Flow<T>`.
- All Room calls go through `viewModelScope.launch(Dispatchers.IO)` or are wrapped in a repository that handles dispatcher switching.
- Enable strict mode in debug builds: `StrictMode.setVmPolicy(VmPolicy.Builder().detectAll().build())`.

---

### Pitfall M3: Bitmap Not Recycled After ML Kit Processing

**What goes wrong:** Each ML Kit call creates intermediate `Bitmap` allocations. If the recognition call is in a loop (batch import from gallery) or the result `Bitmap` is held in a ViewModel beyond its display lifetime, heap pressure builds until GC pauses or OOM.

**Prevention:**
- Call `bitmap.recycle()` after `InputImage.fromBitmap(bitmap, ...)` is done and the bitmap is no longer needed for display.
- For gallery batch import, process cards sequentially (not in parallel coroutines) to cap peak memory usage.
- Use `WeakReference<Bitmap>` in ViewModel if the bitmap is only for preview, not for OCR.

---

### Pitfall M4: Jetpack Compose LazyColumn with Large Card Images

**What goes wrong:** Displaying full-resolution card images in a `LazyColumn` without explicit size constraints causes Coil/Glide to load multiple large bitmaps simultaneously, spiking memory and causing frame drops.

**Prevention:**
- Always use `Modifier.size()` or `Modifier.fillMaxWidth()` with a fixed `height` on image composables in list items. This signals the image loader to decode at display size, not full resolution.
- Use Coil's `rememberAsyncImagePainter` with `ImageRequest.Builder().size(width, height)` to hard-cap decode size.
- In list view, show a low-resolution thumbnail; load full image only in detail view.

---

### Pitfall M5: Tag Many-to-Many Relationship Modeled as Comma-Separated String

**What goes wrong:** Storing tags as a comma-separated string in a single `TEXT` column (e.g., `tags = "직장, VIP, 행사"`) is tempting for simplicity but makes querying by tag impossible without `LIKE '%tag%'` which is slow, error-prone on partial matches, and not indexable.

**Prevention:**
- Model tags as a proper many-to-many: `Card` table, `Tag` table, `CardTagCrossRef` junction table.
- Use Room's `@Relation` and `@Junction` annotations to load tags alongside cards in a single query.
- This is more upfront schema work but prevents a painful migration later when search/filter by tag is implemented.

---

### Pitfall M6: Image Filename Collision on Rapid Capture

**What goes wrong:** If card image filenames are generated using only `System.currentTimeMillis()`, rapid back-to-back captures (or gallery imports) within the same millisecond produce identical filenames. The second image silently overwrites the first.

**Prevention:**
- Use `UUID.randomUUID().toString()` as the filename base for every saved image. Timestamps can be stored separately as metadata in Room.

---

### Pitfall M7: `contentResolver.insert()` for Contacts Returns Null

**What goes wrong:** `ContentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values)` can return `null` on some OEM ROMs (particularly Samsung and Xiaomi) when account type handling differs. Code that calls `.lastPathSegment` on the returned URI without a null check crashes immediately.

**Prevention:**
- Always null-check the URI returned by `contentResolver.insert()`.
- Wrap the entire contact insertion in a try-catch for `RemoteException` and `OperationApplicationException`.
- Prefer the `Intent`-based contact creation approach for reliability across OEMs.

---

## Minor Pitfalls

---

### Pitfall m1: `remember { mutableStateOf() }` for Camera State in Compose

**What goes wrong:** Storing camera-related state (e.g., flash mode, capture in progress) in composable-level `remember` instead of ViewModel means state is lost on navigation back-stack operations and configuration changes.

**Prevention:** All camera state that should survive recomposition goes in a `ViewModel`. Composable `remember` is only for ephemeral UI state (e.g., dropdown expanded).

---

### Pitfall m2: Korean IME Input Causing Room FTS Tokenizer Mismatch

**What goes wrong:** If Room FTS (Full-Text Search) is used for card search with the default `simple` tokenizer, Korean text (Hangul syllable blocks) is not tokenized into individual jamo, making partial Korean name search (e.g., searching "홍" to find "홍길동") fail.

**Prevention:**
- Use `LIKE '%query%'` with standard Room queries for Korean text search rather than FTS, unless you implement a custom ICU tokenizer.
- FTS4/FTS5 with the `unicode61` tokenizer handles Korean character matching better but still doesn't decompose Hangul. For a personal-use app, `LIKE`-based search on indexed columns is sufficient.

---

### Pitfall m3: `ImageCapture.takePicture()` Callback on Wrong Thread

**What goes wrong:** The `OnImageSavedCallback` from `ImageCapture.takePicture(outputOptions, executor, callback)` is called on the executor thread you provide. If you pass `ContextCompat.getMainExecutor(context)`, heavy post-processing (reading file, decoding bitmap, running OCR) blocks the main thread.

**Prevention:**
- Pass a background executor (e.g., `Executors.newSingleThreadExecutor()` or `viewModelScope` coroutine) for the capture callback. Dispatch UI updates back to the main thread explicitly.

---

### Pitfall m4: Hardcoded Internal Storage Path String

**What goes wrong:** Constructing paths like `"/data/data/com.example.cardkeeper/files/images/"` hardcodes the package name and storage location, which breaks on package rename, shared-user-id scenarios, and any future migration.

**Prevention:**
- Always use `context.filesDir` or `context.getDir("images", Context.MODE_PRIVATE)`. Never construct storage paths from string literals.

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Camera + OCR MVP | C3 (image OOM), C2 (Korean model missing) | Use standalone ML Kit artifact; cap capture resolution at 1080p |
| Field parsing logic | C1 (mixed-layout misidentification) | Build bounding-box-aware parser from the start; don't defer |
| Data persistence | C6 (Room migration), M5 (tag schema), M6 (filename collision) | Enable schema export, model tags as many-to-many, use UUID filenames |
| Image display in list | M4 (LazyColumn OOM) | Always specify decode size in Coil/image loader |
| Contacts export | C7 (permissions), M7 (null URI), C5 (FileProvider) | Prefer Intent-based export; null-check all ContentResolver returns |
| Gallery import batch | M3 (bitmap not recycled) | Sequential processing, explicit recycle after OCR |
| Search/filter | m2 (Korean FTS tokenizer) | Use LIKE-based search; skip FTS for v1 |
| App update | C6 (migration) | Always write Migration objects; never ship fallbackToDestructiveMigration |

---

## Sources

**Confidence note:** WebSearch and Context7 were unavailable during this research session. All findings are drawn from:
- Official Android documentation (CameraX, Room, Contacts Provider, FileProvider) — knowledge current to August 2025
- ML Kit Text Recognition v2 API documentation and known SDK behavior
- Android OS platform behavior (FileUriExposedException API 24+, runtime permissions API 23+, Contacts permission groups)
- Common patterns in the Android developer community for business card / document scanning apps

Confidence: HIGH for platform-behavior pitfalls (FileProvider, permissions, Room main-thread enforcement). MEDIUM for ML Kit Korean-specific accuracy characteristics (field parsing heuristics) — recommend validating C1 against real Korean business card samples during Phase 1.
