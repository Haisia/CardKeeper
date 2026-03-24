# Architecture Patterns

**Project:** CardKeeper (명함 관리 앱)
**Domain:** Android business card management app
**Researched:** 2026-03-24
**Confidence:** MEDIUM (training data, August 2025 cutoff — no live web verification available)

---

## Recommended Architecture

**MVVM + Clean Architecture**, layered as UI → Domain → Data. This is the officially recommended Android architecture pattern per Google's Android Architecture Guide and is the standard for Jetpack Compose + Room + ViewModel projects.

```
┌──────────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)                          │
│  Screens / Composables / Navigation                  │
│  ↕ state / events                                    │
├──────────────────────────────────────────────────────┤
│  ViewModel Layer (Jetpack ViewModel + StateFlow)     │
│  CardListViewModel / CardDetailViewModel /           │
│  ScanViewModel / SearchViewModel                     │
│  ↕ UseCases                                          │
├──────────────────────────────────────────────────────┤
│  Domain Layer (Pure Kotlin — no Android deps)        │
│  Use Cases: ScanCardUseCase, SaveCardUseCase,        │
│  SearchCardsUseCase, ExportToContactsUseCase,        │
│  ParseOcrResultUseCase                               │
│  Domain Models: Card, Tag, OcrResult                 │
│  Repository Interfaces                               │
│  ↕ Repository implementations                       │
├──────────────────────────────────────────────────────┤
│  Data Layer                                          │
│  Repositories (CardRepository, TagRepository)        │
│  DataSources:                                        │
│    - Room DB (CardDao, TagDao)                       │
│    - ImageStorage (internal storage)                 │
│    - OcrDataSource (ML Kit)                         │
│    - ContactsDataSource (Android ContactsContract)   │
└──────────────────────────────────────────────────────┘
```

---

## Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `CardListScreen` | Show paginated card list, search bar, filter chips | `CardListViewModel` |
| `CardDetailScreen` | Full card view, edit fields, tag management, memo | `CardDetailViewModel` |
| `ScanScreen` | Camera preview via CameraX, capture trigger | `ScanViewModel` |
| `OcrReviewScreen` | Show parsed fields, allow correction before save | `ScanViewModel` |
| `CardListViewModel` | Holds list state, search query, filter state | `SearchCardsUseCase`, `GetCardsUseCase` |
| `CardDetailViewModel` | Holds card state, edit mode, save/delete actions | `SaveCardUseCase`, `DeleteCardUseCase`, `ExportToContactsUseCase` |
| `ScanViewModel` | Manages camera state, OCR result, image save | `ScanCardUseCase`, `ParseOcrResultUseCase`, `SaveCardUseCase` |
| `CardRepository` | Coordinates Room + ImageStorage for card CRUD | `CardDao`, `ImageStorageDataSource` |
| `TagRepository` | Tag CRUD and card-tag association | `TagDao` |
| `OcrDataSource` | Wraps ML Kit Text Recognition, returns raw text blocks | ML Kit |
| `ImageStorageDataSource` | Save/delete/retrieve images from internal storage | Android `filesDir` |
| `ContactsDataSource` | Write parsed card fields to Android ContactsContract | Android ContactsProvider |

---

## Data Model (Room DB Schema)

### Entities

```kotlin
// Card entity — one row per business card
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String?,          // relative path inside filesDir/cards/
    val name: String,
    val company: String,
    val jobTitle: String,
    val phone: String,
    val email: String,
    val address: String,
    val memo: String,
    val createdAt: Long,             // epoch millis
    val updatedAt: Long
)

// Tag entity — tag vocabulary
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String                 // unique constraint recommended
)

// Junction table — many-to-many card ↔ tag
@Entity(
    tableName = "card_tag_cross_ref",
    primaryKeys = ["cardId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = CASCADE),
        ForeignKey(entity = TagEntity::class,  parentColumns = ["id"], childColumns = ["tagId"],  onDelete = CASCADE)
    ]
)
data class CardTagCrossRef(
    val cardId: Long,
    val tagId: Long
)

// Relationship POJO (not stored, used for queries)
data class CardWithTags(
    @Embedded val card: CardEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(CardTagCrossRef::class, parentColumn = "cardId", entityColumn = "tagId")
    )
    val tags: List<TagEntity>
)
```

### DAO Design

```kotlin
@Dao
interface CardDao {
    @Transaction
    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    fun getAllCardsWithTags(): Flow<List<CardWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT c.* FROM cards c
        LEFT JOIN card_tag_cross_ref ct ON c.id = ct.cardId
        LEFT JOIN tags t ON ct.tagId = t.id
        WHERE c.name LIKE :query OR c.company LIKE :query
           OR c.jobTitle LIKE :query OR t.name LIKE :query
        ORDER BY c.updatedAt DESC
    """)
    fun searchCards(query: String): Flow<List<CardWithTags>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: CardTagCrossRef)

    @Query("DELETE FROM card_tag_cross_ref WHERE cardId = :cardId")
    suspend fun deleteTagsForCard(cardId: Long)
}
```

---

## Navigation Structure

Use Jetpack Compose Navigation (`androidx.navigation:navigation-compose`). Single-activity, single `NavHost`.

```
NavHost(startDestination = "card_list") {
    composable("card_list")              → CardListScreen
    composable("card_detail/{cardId}")   → CardDetailScreen(cardId)
    composable("card_new")               → OcrReviewScreen (new card, no id)
    composable("scan")                   → ScanScreen
    composable("tag_manager")            → TagManagerScreen
}
```

**Navigation flow:**

```
CardListScreen
  ├── FAB tap               → ScanScreen
  ├── Gallery pick          → OcrReviewScreen (imagePicker result)
  ├── Card tap              → CardDetailScreen(cardId)
  └── Tag filter bar tap    → (inline sheet, no nav)

ScanScreen
  └── Photo captured        → OcrReviewScreen (image URI passed as nav arg or SavedStateHandle)

OcrReviewScreen
  └── Save confirmed        → CardListScreen (popBackStack to root)

CardDetailScreen
  ├── Edit mode (inline)    → (same screen, toggle edit)
  └── Export to Contacts    → (triggers ContactsDataSource, no nav)
```

**Image URI passing between screens:** Do NOT pass image URIs directly as nav args (size limit). Write image to internal storage first in `ScanViewModel`, pass the resulting file path as a nav arg string.

---

## Key Integration Points

### CameraX Integration Flow

**Confidence: MEDIUM** — CameraX API surface stable as of 1.3.x; verify current version.

```
ScanScreen (Composable)
  └── AndroidView { PreviewView }          ← CameraX Preview use case
      + ImageCapture use case (bound together)
  └── "Capture" button onClick
      → ScanViewModel.captureImage()
          → imageCapture.takePicture(outputOptions, executor, callback)
              → on success: Uri → ImageStorageDataSource.saveImage(uri) → filePath
              → on filePath: OcrDataSource.recognize(filePath) → OcrResult
              → navigate to OcrReviewScreen(filePath)
```

**CameraX binding:**

```kotlin
// In ScanScreen, tied to lifecycle
val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
cameraProviderFuture.addListener({
    val cameraProvider = cameraProviderFuture.get()
    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()
    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
}, ContextCompat.getMainExecutor(context))
```

Permission required: `CAMERA` — request at runtime before opening ScanScreen.

---

### ML Kit OCR Pipeline

**Confidence: HIGH** — ML Kit Text Recognition v2 is on-device, stable, well-documented.

```
OcrDataSource
  Input:  InputImage (from file path or Bitmap)
  Process: TextRecognizer.process(inputImage)
  Output: Raw vkText blocks → list of TextBlock/Line/Element

ParseOcrResultUseCase (Domain Layer — pure Kotlin)
  Input:  List<String> (text lines from OCR)
  Output: OcrResult(name, company, jobTitle, phone, email, address)
  Logic:  Regex-based field extraction
           - phone: \+?[\d\s\-\(\)]{7,}
           - email: \S+@\S+\.\S+
           - name: heuristic (first short line, CJK or Latin)
           - company/title: remaining lines scored by position
```

**Key design decision:** Keep `ParseOcrResultUseCase` in the domain layer with no Android dependencies. This makes it unit-testable with plain JUnit — no instrumented test needed. OCR parsing logic is the most complex and bug-prone part; testability here is high value.

**ML Kit setup:**

```kotlin
// build.gradle.kts
implementation("com.google.mlkit:text-recognition:16.0.1")
// Korean cards may also need:
implementation("com.google.mlkit:text-recognition-korean:16.0.1")
```

InputImage from file:

```kotlin
val inputImage = InputImage.fromFilePath(context, Uri.fromFile(File(filePath)))
val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
recognizer.process(inputImage)
    .addOnSuccessListener { visionText -> /* parse visionText.text */ }
```

---

### Image Storage Strategy (Internal Storage)

**Confidence: HIGH** — Standard Android pattern; no permissions needed for `filesDir`.

```
filesDir/
  cards/
    {cardId}-{timestamp}.jpg    ← compressed JPEG, max ~800px on longest edge
```

**ImageStorageDataSource responsibilities:**

```kotlin
class ImageStorageDataSource(private val context: Context) {

    // Called from ScanViewModel after capture
    fun saveImage(sourceUri: Uri): String {
        val file = File(context.filesDir, "cards/${UUID.randomUUID()}.jpg")
        // Copy + compress: decode Bitmap, scale to max 1024px, re-encode as JPEG 85%
        return file.absolutePath
    }

    // Called from CardRepository.deleteCard
    fun deleteImage(filePath: String) {
        File(filePath).delete()
    }

    // Returns File for Coil/Glide to load
    fun getImageFile(filePath: String): File = File(filePath)
}
```

**Why compress on save:** Original camera images are 3-10 MB. For a card list with thumbnails, uncompressed originals are unnecessary. 800-1024px at JPEG 85% yields ~100-200 KB per card — acceptable for hundreds of cards.

**Displaying images:** Use Coil (`io.coil-kt:coil-compose`) with `AsyncImage(model = File(imagePath))`. Coil handles caching automatically.

---

### Android Contacts Provider Integration

**Confidence: MEDIUM** — ContactsContract API is stable and unchanged since API 5; integration pattern is well-established.

```
ExportToContactsUseCase
  Input:  Card (domain model)
  Output: Unit (triggers system intent OR direct insert)
```

**Two approaches — use Intent (recommended for simplicity):**

```kotlin
// Approach A: System intent — no permission needed, user confirms in Contacts app
val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
    type = ContactsContract.RawContacts.CONTENT_TYPE
    putExtra(ContactsContract.Intents.Insert.NAME, card.name)
    putExtra(ContactsContract.Intents.Insert.COMPANY, card.company)
    putExtra(ContactsContract.Intents.Insert.JOB_TITLE, card.jobTitle)
    putExtra(ContactsContract.Intents.Insert.PHONE, card.phone)
    putExtra(ContactsContract.Intents.Insert.EMAIL, card.email)
}
context.startActivity(intent)
```

**Approach B: Direct ContentProvider insert** — requires `WRITE_CONTACTS` permission, more code, but seamless (no hand-off to Contacts app). Overkill for v1 personal use.

**Recommendation:** Use Approach A (Intent) for v1. No permission needed, leverages user's existing Contacts app, lower implementation risk.

---

## Data Flow: End-to-End (Scan → Save → Display)

```
1. User opens ScanScreen
   → REQUEST_PERMISSION(CAMERA) if not granted

2. CameraX preview renders in PreviewView

3. User taps Capture
   → ScanViewModel.captureImage()
   → ImageCapture.takePicture() → temp file URI

4. ScanViewModel
   → ImageStorageDataSource.saveImage(tempUri) → permanentPath
   → OcrDataSource.recognize(permanentPath) → rawText
   → ParseOcrResultUseCase.parse(rawText) → OcrResult
   → navController.navigate("card_new?imagePath=$permanentPath&ocrResult=...")
      (OcrResult serialized via SavedStateHandle or JSON nav arg)

5. OcrReviewScreen shows pre-filled fields
   User corrects fields, assigns tags

6. User taps Save
   → ScanViewModel.saveCard(editedFields, imagePath, tags)
   → SaveCardUseCase
       → CardRepository.insertCard(card)
           → CardDao.insertCard(entity) → cardId
           → TagRepository.insertTagsForCard(cardId, tags)
   → navController.popBackStack("card_list", inclusive = false)

7. CardListScreen observes Flow<List<CardWithTags>> from Room
   → list updates reactively (no manual refresh needed)
```

---

## Patterns to Follow

### Pattern 1: Single Source of Truth via Room + Flow

**What:** All UI state derives from Room `Flow<>` queries. Never hold a separate in-memory copy of the card list.

**When:** Always, for card list and detail state.

**Benefit:** When a card is saved or updated, `CardListScreen` reflects the change automatically without explicit refresh calls.

### Pattern 2: ViewModel as State Holder, Not Business Logic

**What:** ViewModels hold `StateFlow<UiState>` and call use cases. They do not contain business logic (parsing, validation, OCR).

**When:** Always. Keep ViewModels thin — they translate domain results to UI state.

### Pattern 3: ParseOcrResultUseCase as Pure Function

**What:** OCR parsing is a pure function: `(List<String>) -> OcrResult`. No side effects, no Android dependencies.

**When:** ML Kit OCR pipeline.

**Benefit:** Fast, deterministic unit tests without emulator or real device.

### Pattern 4: Coroutines + Flow throughout

**What:** All DAO operations are `suspend` or return `Flow`. Repository layer wraps callbacks (ML Kit, CameraX) in `suspendCoroutine` or `callbackFlow`.

**Example:**

```kotlin
// Wrapping ML Kit callback in coroutine
suspend fun recognize(imagePath: String): String = suspendCoroutine { cont ->
    val image = InputImage.fromFilePath(context, Uri.fromFile(File(imagePath)))
    recognizer.process(image)
        .addOnSuccessListener { cont.resume(it.text) }
        .addOnFailureListener { cont.resumeWithException(it) }
}
```

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Passing Large Objects as Navigation Arguments

**What:** Passing full `OcrResult` or `Card` objects as nav args by serializing to URL query params.

**Why bad:** Nav arg strings have size limits. Crashes or data truncation on large OCR results.

**Instead:** Write result to `SavedStateHandle` in `ScanViewModel`. `OcrReviewScreen`'s ViewModel shares the same `ScanViewModel` instance (same backstack entry) or reads from a shared `StateFlow`.

### Anti-Pattern 2: Context Leaks in ViewModel

**What:** Holding an `Activity` or `View` reference in ViewModel.

**Why bad:** ViewModel outlives Activity — memory leak and crash on rotation.

**Instead:** Use `Application` context injected via `AndroidViewModel` or Hilt, or pass context only inside `suspend` function calls (not stored as field).

### Anti-Pattern 3: Blocking Main Thread with Image Operations

**What:** Running `Bitmap` decode/compress on the main thread inside a `Composable`.

**Why bad:** Drops frames, jank, ANR risk on older devices.

**Instead:** All image operations run in `Dispatchers.IO` inside `viewModelScope.launch(Dispatchers.IO)`.

### Anti-Pattern 4: Storing Absolute Paths in Room

**What:** Storing the full absolute path like `/data/user/0/com.example.cardkeeper/files/cards/image.jpg`.

**Why bad:** `filesDir` path includes the package name. During development/testing, if the app is renamed or reinstalled, absolute paths break.

**Instead:** Store relative paths (`cards/image.jpg`) and reconstruct full path as `context.filesDir.absolutePath + "/" + relativePath` at read time.

### Anti-Pattern 5: One ViewModel for Everything

**What:** A single `MainViewModel` holding all card list, detail, scan, and search state.

**Why bad:** Recomposition scope is too broad. State from scan triggers recomposition of card list unnecessarily.

**Instead:** Scope ViewModels to their NavBackStackEntry. Each screen gets its own ViewModel.

---

## Dependency Injection

Use **Hilt** (`com.google.dagger:hilt-android`). It is the officially recommended DI framework for Android and integrates directly with Jetpack ViewModel and Navigation.

```
@HiltAndroidApp  → Application class
@AndroidEntryPoint → MainActivity (single activity)
@HiltViewModel   → All ViewModels

Modules:
  DatabaseModule   → provides RoomDatabase, CardDao, TagDao
  StorageModule    → provides ImageStorageDataSource
  OcrModule        → provides TextRecognizer
  RepositoryModule → binds CardRepositoryImpl to CardRepository interface
```

---

## Testability

| Layer | Test Type | Tool | What to Test |
|-------|-----------|------|--------------|
| Domain (ParseOcrResultUseCase) | Unit | JUnit 5 + Truth | Field extraction from sample text, Korean/English card formats |
| Domain (other use cases) | Unit | JUnit 5 + Mockito/MockK | Business logic with mocked repositories |
| Repository | Integration | Room in-memory DB | DAO queries, cascade deletes, search correctness |
| ViewModel | Unit | JUnit 5 + Turbine (Flow testing) | State transitions, error states |
| UI (key flows) | Instrumented | Compose UI Test | Scan → review → save, search, export |

**Priority:** `ParseOcrResultUseCase` unit tests are the highest ROI — OCR parsing will fail on edge cases. Write these first, before instrumented tests.

---

## Scalability Considerations

| Concern | At ~100 cards | At ~1000 cards | At ~10000 cards |
|---------|--------------|----------------|-----------------|
| DB query speed | Not a concern | Add index on `name`, `company` | Add FTS (Room FTS4/FTS5) |
| Image storage | ~20 MB | ~200 MB | ~2 GB — add image size warning |
| Search | Simple LIKE query | LIKE still fast | Migrate to FTS5 virtual table |
| List rendering | `LazyColumn` default | `LazyColumn` + `key=` param | Paging 3 library |

For a personal app (realistically 100-500 cards), none of these are concerns for v1. Add FTS and Paging only if performance complaints arise.

---

## Module Structure (Single-Module Recommended for v1)

For a personal app of this scope, a **single-module** setup with package-level separation is sufficient and reduces build complexity.

```
com.example.cardkeeper/
  ui/
    cardlist/       CardListScreen, CardListViewModel
    carddetail/     CardDetailScreen, CardDetailViewModel
    scan/           ScanScreen, OcrReviewScreen, ScanViewModel
    tags/           TagManagerScreen, TagManagerViewModel
    navigation/     AppNavHost, Screen.kt (sealed nav destinations)
    components/     Shared Composables (CardItem, TagChip, etc.)
    theme/          Theme.kt, Color.kt, Type.kt
  domain/
    model/          Card, Tag, OcrResult (data classes, no Android deps)
    usecase/        All use cases
    repository/     Repository interfaces
  data/
    db/             AppDatabase, CardDao, TagDao, entities, CardWithTags
    repository/     CardRepositoryImpl, TagRepositoryImpl
    datasource/     ImageStorageDataSource, OcrDataSource, ContactsDataSource
  di/               Hilt modules
```

---

## Sources

- Android Architecture Guide (Google): https://developer.android.com/topic/architecture — HIGH confidence
- Room Relationships (official docs): https://developer.android.com/training/data-storage/room/relationships — HIGH confidence
- CameraX Overview: https://developer.android.com/training/camerax — MEDIUM confidence (API verified against training data)
- ML Kit Text Recognition: https://developers.google.com/ml-kit/vision/text-recognition/android — HIGH confidence
- Android ContactsContract: https://developer.android.com/reference/android/provider/ContactsContract — HIGH confidence
- Jetpack Navigation Compose: https://developer.android.com/jetpack/compose/navigation — HIGH confidence
- Hilt dependency injection: https://developer.android.com/training/dependency-injection/hilt-android — HIGH confidence

**Note:** No live web verification was performed (external search/fetch tools unavailable in this environment). All findings are based on training data with knowledge cutoff August 2025. Verify library versions against current Maven Central before implementation.
