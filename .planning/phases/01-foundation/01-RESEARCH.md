# Phase 1: Foundation - Research

**Researched:** 2026-03-24
**Domain:** Android project scaffold — Gradle/KSP, Room DB with many-to-many, Hilt DI, Navigation Compose type-safe routes
**Confidence:** HIGH (all critical findings verified against live official sources or Maven Central)

---

## Summary

Phase 1 lays the non-negotiable scaffold that every subsequent phase writes directly into. All four
plans are pure setup work: no user-visible features, but the schema decisions made here cannot be
changed later without Room migrations. Getting three things exactly right at this phase prevents
significant pain in Phases 2–4: (1) the Room schema must have the three-table many-to-many
structure from day one, (2) schema export must be enabled and committed before any data is ever
written, and (3) `fallbackToDestructiveMigration` must be gated on `BuildConfig.DEBUG` so it is
never reachable in a shipped build.

The standard Jetpack stack is a mature, well-documented fit for this project — no controversial
choices remain. One note: several version numbers in `.planning/research/STACK.md` were stale
(KSP changed versioning format, Coil shipped two major patches, coroutines shipped a patch, and
serialization versioning assumption was wrong). Verified versions are documented below and must
be used in `build.gradle.kts` in 01-01. The Room Gradle Plugin (`id("androidx.room")`) is now
the recommended approach for schema export and replaces the old `ksp { arg("room.schemaLocation",
...) }` pattern — it works with AGP 8.4+ which this project already requires (AGP 9.0.1).

Navigation Compose 2.9.7 type-safe routes are now the stable, preferred API. For no-argument
screens, use `@Serializable object ScreenName`. For screens with arguments, use `@Serializable
data class ScreenName(val id: Long)`. The `composable<Route> {}` syntax and `backStackEntry.toRoute<Route>()` are fully stable since Navigation 2.8.0.

**Primary recommendation:** Write all four plans in dependency order — Gradle setup (01-01) before
Room schema (01-02) since KSP must be configured before Room annotation processing runs. The
critical correctness invariants are: three-table schema, schema export via Room Gradle Plugin,
DEBUG-only destructive migration guard, and relative image paths in `CardEntity.imagePath` from
the very first migration (version 1).

---

## Project Constraints

No CONTEXT.md exists for this phase — no user-locked decisions to carry forward. All decisions are
from the project-level research files and ROADMAP.md success criteria.

**Binding decisions from ROADMAP.md Phase 1 success criteria (treat as locked):**

1. Room database with exactly three tables: `cards`, `tags`, `card_tag_cross_ref` — schema export
   JSON committed to source control.
2. Hilt modules wired end-to-end: a ViewModel can receive an injected repository without runtime
   errors.
3. Navigation graph contains stubs for all five screens: `CardList`, `CardDetail`, `Scan`,
   `OcrReview`, `TagManager`.
4. `fallbackToDestructiveMigration` gated on `BuildConfig.DEBUG` only.
5. Relative image paths established in `CardEntity` from day one (no absolute paths in DB ever).

---

## Standard Stack

### Core (verified versions — use these, not STACK.md versions)

| Library | Version | Purpose | Confidence |
|---------|---------|---------|------------|
| Kotlin | 2.3.20 | Primary language | HIGH |
| AGP | 9.0.1 | Build system | HIGH |
| KSP | 2.3.6 | Code generation (Room + Hilt) — new versioning, NOT `2.3.20-1.0.31` | HIGH — verified Maven Central |
| Compose BOM | 2026.03.00 | Pins all Compose library versions | HIGH |
| Material3 | (via BOM) | UI components | HIGH |
| Navigation Compose | 2.9.7 | Type-safe screen routing | HIGH |
| kotlinx-serialization-json | 1.10.0 | Nav route argument serialization — NOT `2.0.0` | HIGH — verified Maven Central |
| Room runtime | 2.8.4 | SQLite ORM | HIGH |
| Room KTX | 2.8.4 | Flow-based queries | HIGH |
| Room compiler (KSP) | 2.8.4 | Code generation | HIGH |
| Room Gradle Plugin | 2.8.4 | Schema export config | HIGH |
| Hilt Android | 2.57.1 | DI container | HIGH |
| Hilt Android Compiler (KSP) | 2.57.1 | DI code gen | HIGH |
| Hilt Navigation Compose | 1.3.0 | `hiltViewModel()` in Compose | HIGH |
| Lifecycle ViewModel Compose | 2.10.0 | `viewModel()` in Compose | HIGH |
| Lifecycle Runtime Compose | 2.10.0 | `collectAsStateWithLifecycle()` | HIGH |
| CameraX camera-camera2 | 1.5.3 | Camera (declared now, used Phase 2) | HIGH |
| CameraX camera-lifecycle | 1.5.3 | Camera lifecycle binding | HIGH |
| CameraX camera-view | 1.5.3 | PreviewView integration | HIGH |
| ML Kit text-recognition | 16.0.1 | Latin OCR bundled — verify at developers.google.com/ml-kit | MEDIUM |
| ML Kit text-recognition-korean | 16.0.1 | Korean OCR bundled — separate artifact required | MEDIUM |
| Coil 3 coil-compose | 3.4.0 | Image loading — NOT `3.1.0`; 3.4.0 released 2026-02-24 | HIGH — verified Maven Central |
| kotlinx-coroutines-android | 1.10.2 | Structured concurrency — NOT `1.10.1` | HIGH — verified search |

### Version Verification Status

| Library | STACK.md Said | Verified Version | Action |
|---------|---------------|-----------------|--------|
| KSP | 2.3.20-1.0.31 | 2.3.6 (new versioning since KSP 2.3.0) | **Use 2.3.6** |
| Coil 3 | 3.1.0 | 3.4.0 (released 2026-02-24) | **Use 3.4.0** |
| kotlinx-coroutines | 1.10.1 | 1.10.2 | **Use 1.10.2** |
| kotlinx-serialization-json | 2.0.0 (wrong series) | 1.10.0 | **Use 1.10.0** |
| ML Kit text-recognition | 16.0.1 | Not live-verified | Verify before build |

### Installation — build.gradle.kts (project level)

```kotlin
plugins {
    id("com.android.application") version "9.0.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    id("androidx.room") version "2.8.4" apply false
}
```

### Installation — build.gradle.kts (app level)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.room")
}

android {
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        targetSdk = 36
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true   // needed for BuildConfig.DEBUG guard in AppDatabase
    }
}

// Room Gradle Plugin schema export (recommended for Room 2.6.0+, requires AGP 8.4+)
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation + Serialization for type-safe routes
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Room
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // CameraX (declared now; used Phase 2)
    val cameraVersion = "1.5.3"
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")

    // ML Kit OCR — bundled (on-device, no Play Services)
    implementation("com.google.mlkit:text-recognition:16.0.1")       // verify version
    implementation("com.google.mlkit:text-recognition-korean:16.0.1") // separate artifact — required for Korean

    // Coil 3 image loading
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-android-compiler:2.57.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
```

---

## Architecture Patterns

### Recommended Project Structure

```
app/src/main/kotlin/com/example/cardkeeper/
├── ui/
│   ├── cardlist/           # CardListScreen, CardListViewModel
│   ├── carddetail/         # CardDetailScreen, CardDetailViewModel
│   ├── scan/               # ScanScreen, OcrReviewScreen, ScanViewModel
│   ├── tags/               # TagManagerScreen, TagManagerViewModel
│   ├── navigation/         # AppNavHost.kt, Screen.kt (route objects)
│   ├── components/         # Shared composables (CardItem, TagChip, etc.)
│   └── theme/              # Theme.kt, Color.kt, Type.kt
├── domain/
│   ├── model/              # Card, Tag, OcrResult (pure Kotlin, no Android deps)
│   ├── usecase/            # All use cases
│   └── repository/         # Repository interfaces
├── data/
│   ├── db/                 # AppDatabase, CardDao, TagDao, entities, CardWithTags
│   ├── repository/         # CardRepositoryImpl, TagRepositoryImpl
│   └── datasource/         # ImageStorageDataSource, OcrDataSource, ContactsDataSource
└── di/                     # Hilt modules: DatabaseModule, StorageModule, OcrModule, RepositoryModule
```

### Pattern 1: Room Schema — Three Tables with Many-to-Many

**What:** Three Room entities. `CardEntity` and `TagEntity` joined through `CardTagCrossRef`
junction table. `CardWithTags` is a query-result POJO (not stored).

**Critical invariants:**
- `CardEntity.imagePath` stores relative paths only (e.g., `"cards/uuid.jpg"`) — never absolute
- `CardTagCrossRef` has composite primary key `[cardId, tagId]` with `CASCADE` deletes
- `@Database` version starts at 1 and increments only with a `Migration` object

```kotlin
// Source: https://developer.android.com/training/data-storage/room/relationships
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String?,          // RELATIVE path: "cards/uuid.jpg" — never absolute
    val name: String,
    val company: String,
    val jobTitle: String,
    val phone: String,
    val email: String,
    val address: String,
    val memo: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "card_tag_cross_ref",
    primaryKeys = ["cardId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CardTagCrossRef(
    val cardId: Long,
    val tagId: Long
)

// Query-result POJO — NOT a @Entity, never stored directly
data class CardWithTags(
    @Embedded val card: CardEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            CardTagCrossRef::class,
            parentColumn = "cardId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
```

### Pattern 2: AppDatabase with DEBUG-Only Destructive Migration Guard

**What:** Room `@Database` with schema version, Room Gradle Plugin handling schema export (no
`room.schemaLocation` ksp arg needed), and `fallbackToDestructiveMigration()` wrapped in
`if (BuildConfig.DEBUG)`.

**Why the guard matters:** `fallbackToDestructiveMigration()` destroys all user data when called
without an explicit migration. It must never execute in a production build. The conditional
pattern — no boolean overload exists — is the correct implementation.

```kotlin
// Source: https://developer.android.com/training/data-storage/room/migrating-db-versions
@Database(
    entities = [CardEntity::class, TagEntity::class, CardTagCrossRef::class],
    version = 1,
    exportSchema = true  // true by default; explicit for clarity; Room Gradle Plugin handles output path
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cardkeeper.db"
                )
                if (BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }
                // Production: add Migration objects here before bumping version
                builder.build().also { INSTANCE = it }
            }
        }
    }
}
```

### Pattern 3: Hilt Module Structure

**What:** Four modules, all installed in `SingletonComponent`. `DatabaseModule` uses `@Provides`
(Room is not constructor-injectable). `RepositoryModule` uses `@Binds` (repository implementations
have `@Inject constructor`).

```kotlin
// Source: https://developer.android.com/training/dependency-injection/hilt-android

// Application class
@HiltAndroidApp
class CardKeeperApplication : Application()

// MainActivity
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CardKeeperApp() }
    }
}

// DatabaseModule — @Provides because Room builds are not constructor-injectable
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    fun provideCardDao(db: AppDatabase): CardDao = db.cardDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
}

// RepositoryModule — @Binds because impls have @Inject constructors
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Singleton
    @Binds
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
}

// @HiltViewModel usage
@HiltViewModel
class CardListViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {
    val cards: StateFlow<List<CardWithTags>> = cardRepository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

### Pattern 4: Navigation Compose Type-Safe Routes

**What:** Navigation 2.8+ type-safe API with `@Serializable` objects and data classes.
`@Serializable object` for screens with no arguments. `@Serializable data class` for screens
that receive a typed argument. `composable<Route> {}` replaces `composable("route_string") {}`.

```kotlin
// Source: https://developer.android.com/guide/navigation/design/type-safety

// Route definitions — in navigation/Screen.kt
@Serializable object CardListRoute
@Serializable data class CardDetailRoute(val cardId: Long)
@Serializable object ScanRoute
@Serializable object OcrReviewRoute
@Serializable object TagManagerRoute

// AppNavHost
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = CardListRoute) {

        composable<CardListRoute> {
            CardListScreen(
                onCardClick = { cardId -> navController.navigate(CardDetailRoute(cardId)) },
                onScanClick = { navController.navigate(ScanRoute) },
                onTagManagerClick = { navController.navigate(TagManagerRoute) }
            )
        }

        composable<CardDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CardDetailRoute>()
            CardDetailScreen(cardId = route.cardId, onBack = { navController.popBackStack() })
        }

        composable<ScanRoute> {
            ScanScreen(
                onPhotoReady = { navController.navigate(OcrReviewRoute) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<OcrReviewRoute> {
            OcrReviewScreen(
                onSaved = { navController.popBackStack(CardListRoute, inclusive = false) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<TagManagerRoute> {
            TagManagerScreen(onBack = { navController.popBackStack() })
        }
    }
}
```

**Retrieving route args in ViewModel:**
```kotlin
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardRepository: CardRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CardDetailRoute>()
    val card = cardRepository.getCardById(route.cardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
```

### Pattern 5: Room Schema Export via Room Gradle Plugin

**What:** The Room Gradle Plugin (`id("androidx.room")`) introduced in Room 2.6.0 is the
recommended approach for Room 2.8.4. It replaces the older `ksp { arg("room.schemaLocation", ...) }`
pattern and provides reproducible, cacheable builds. Requires AGP 8.4+ (this project uses 9.0.1).

```kotlin
// In app/build.gradle.kts — replaces ksp { arg("room.schemaLocation", ...) }
room {
    schemaDirectory("$projectDir/schemas")
}
```

The `schemas/` directory must be committed to source control. Each schema version creates a file
like `schemas/com.example.cardkeeper.data.db.AppDatabase/1.json`. This file is used by Room's
`MigrationTestHelper` and provides a diff-visible record of all schema changes.

### Anti-Patterns to Avoid

- **`ksp { arg("room.schemaLocation", ...) }`**: Works but deprecated in favor of the Room Gradle
  Plugin for Room 2.6.0+. Do not use for new projects.
- **`kapt` for any processor**: KAPT is deprecated; all annotation processing goes through `ksp()`.
- **`@Database(exportSchema = false)`**: Never suppress schema export — it prevents migrations.
- **String-based nav routes**: `composable("card_list") {}` with string arguments — replaced by
  type-safe `composable<CardListRoute> {}`.
- **Absolute paths in `CardEntity.imagePath`**: Store `"cards/uuid.jpg"`, reconstruct absolute
  path at runtime with `context.filesDir.absolutePath + "/" + imagePath`.
- **`fallbackToDestructiveMigration()` without DEBUG guard**: Must be wrapped in
  `if (BuildConfig.DEBUG)` — there is no boolean overload.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Many-to-many query loading | Custom JOIN query + manual mapping | Room `@Relation` + `@Junction` POJO | Room handles the join query and result mapping automatically with `@Transaction` |
| Reactive list updates | Manual observer/callback chains | Room `Flow<List<CardWithTags>>` | Room emits a new list on every DB write — zero polling needed |
| ViewModel DI | Manual ViewModel factory | `@HiltViewModel` + `hiltViewModel()` | Hilt generates SavedStateHandle-aware factories automatically |
| Nav argument encoding | URL-encoding strings manually | `@Serializable` data class routes | Navigation handles serialization/deserialization; compile-time type checking |
| Schema version tracking | Manual SQL CREATE TABLE scripts | Room Gradle Plugin + `exportSchema = true` | Room generates JSON snapshots; diffs are git-visible |
| Image loading from files | Custom Bitmap decode + RecyclerView adapter | Coil `AsyncImage(model = File(path))` | Coil handles disk caching, decode sizing, memory limits, error states |

---

## Common Pitfalls

### Pitfall 1: KSP Version Format Changed

**What goes wrong:** Using the old KSP version format `2.3.20-1.0.31` (Kotlin-version-prefixed)
fails with a plugin resolution error because KSP decoupled from Kotlin versioning as of KSP 2.3.0.

**Why it happens:** KSP versions before 2.0.0 were always `{kotlinVersion}-{kspMinor}`. Starting
with KSP 2.3.0 (released 2025), the version is just `2.3.x`.

**How to avoid:** Use `id("com.google.devtools.ksp") version "2.3.6"` — the standalone version
number, no Kotlin prefix.

**Warning signs:** `Plugin [id: 'com.google.devtools.ksp', version: '2.3.20-1.0.31'] was not found`
in sync output.

### Pitfall 2: Room Destructive Migration in Production

**What goes wrong:** `fallbackToDestructiveMigration()` called unconditionally wipes all user card
data on any schema version bump.

**Why it happens:** Developers add it during initial development for convenience and ship it.

**How to avoid:** Always use the conditional pattern:
```kotlin
if (BuildConfig.DEBUG) { builder.fallbackToDestructiveMigration() }
```
Also: enable `buildConfig = true` in `buildFeatures {}` so `BuildConfig` is generated.

**Warning signs:** App compiles and runs fine but users report all data disappearing after update.

### Pitfall 3: `@Transaction` Missing on `CardWithTags` Queries

**What goes wrong:** Room loads `CardWithTags` in two separate queries (one for cards, one for
their tags via junction). Without `@Transaction`, a concurrent write between the two queries
produces inconsistent results — cards returned with wrong or missing tags.

**Why it happens:** Room `@Relation` always triggers multiple queries. Without `@Transaction` the
queries are not atomic.

**How to avoid:** Always annotate any DAO method that returns a `@Relation`-containing POJO with
`@Transaction`.
```kotlin
@Transaction
@Query("SELECT * FROM cards ORDER BY updatedAt DESC")
fun getAllCardsWithTags(): Flow<List<CardWithTags>>
```

**Warning signs:** `Lint warning: The return type includes a Pojo with a @Relation. It is usually
desired to annotate this method with @Transaction to avoid inconsistent results.`

### Pitfall 4: `buildConfig` Not Enabled

**What goes wrong:** `BuildConfig.DEBUG` reference in `AppDatabase.getInstance()` causes compile
error because `buildConfig` generation is disabled by default in newer AGP versions.

**Why it happens:** AGP 8.0+ disabled `BuildConfig` generation by default to reduce build times.

**How to avoid:** Explicitly add to `android { buildFeatures { buildConfig = true } }` in
`app/build.gradle.kts`.

### Pitfall 5: Schema Export JSON Not Committed to Git

**What goes wrong:** Room schema JSON files are generated into `app/schemas/` but not committed.
Future schema version bumps cannot be validated against the baseline, and `MigrationTestHelper`
fails with "Schema export not found".

**How to avoid:** After first build, commit `schemas/com.example.cardkeeper.data.db.AppDatabase/1.json`.
Add `schemas/` to the repo (not .gitignore). The file is ~5 KB of JSON.

### Pitfall 6: `hilt-compiler` vs `hilt-android-compiler`

**What goes wrong:** Using `ksp("com.google.dagger:hilt-compiler:...")` (the non-Android variant)
works for pure JVM modules but does not generate Android-specific entry points for
`@AndroidEntryPoint`, `@HiltAndroidApp`, etc.

**How to avoid:** Always use `ksp("com.google.dagger:hilt-android-compiler:2.57.1")` for Android
app and feature modules.

### Pitfall 7: `kotlinx-serialization-json` Wrong Version Series

**What goes wrong:** Adding `kotlinx-serialization-json:2.0.0` fails with "Unresolved reference"
because there is no 2.x series — the library is in the 1.x series (current: 1.10.0).

**How to avoid:** Use `implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")`.

---

## Code Examples

### CardDao with correct @Transaction annotations

```kotlin
// Source: https://developer.android.com/training/data-storage/room/relationships
@Dao
interface CardDao {
    @Transaction
    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    fun getAllCardsWithTags(): Flow<List<CardWithTags>>

    @Transaction
    @Query("SELECT * FROM cards WHERE id = :cardId")
    fun getCardWithTagsById(cardId: Long): Flow<CardWithTags?>

    @Transaction
    @Query("""
        SELECT DISTINCT c.* FROM cards c
        LEFT JOIN card_tag_cross_ref ct ON c.id = ct.cardId
        LEFT JOIN tags t ON ct.tagId = t.id
        WHERE c.name LIKE :query
           OR c.company LIKE :query
           OR c.jobTitle LIKE :query
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
```

### TagDao

```kotlin
@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: CardTagCrossRef)

    @Query("DELETE FROM card_tag_cross_ref WHERE cardId = :cardId")
    suspend fun deleteTagsForCard(cardId: Long)
}
```

### Hilt module for OcrModule and StorageModule (declared now, implementation Phase 2)

```kotlin
// StorageModule — Context is available via @ApplicationContext
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Singleton
    @Provides
    fun provideImageStorageDataSource(
        @ApplicationContext context: Context
    ): ImageStorageDataSource = ImageStorageDataSource(context)
}

// OcrModule — TextRecognizer is not constructor-injectable
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {
    @Singleton
    @Provides
    fun provideTextRecognizer(): TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
}
```

---

## Environment Availability

Step 2.6: SKIPPED — Phase 1 is purely code/config changes with no external service dependencies
beyond the standard Android build toolchain (Android Studio, Gradle, Java 17). No external
databases, running services, or CLI tools beyond the build system are required.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (standard Android) + Robolectric or Instrumented tests for Room |
| Config file | None yet — Wave 0 creates test configuration |
| Quick run (local unit) | `./gradlew :app:testDebugUnitTest` |
| Full suite (instrumented) | `./gradlew :app:connectedDebugAndroidTest` |

### Phase Requirements to Test Map

Phase 1 has no user-visible requirements. The five success criteria are structural/compile-time
verifications, not behavioral tests requiring test code. However, one architectural invariant
should be tested:

| Success Criterion | Test Type | Command | Notes |
|-------------------|-----------|---------|-------|
| Room schema correct (3 tables, junction) | Instrumented (Room in-memory) | `./gradlew :app:connectedDebugAndroidTest` | `RoomDatabase.Builder().inMemory()` test |
| Hilt wiring end-to-end | Compile verification | `./gradlew :app:assembleDebug` | Missing binding = build error |
| App launches without crash | Smoke | `./gradlew :app:connectedDebugAndroidTest` | Espresso launch test |

**Compile itself is the primary validation gate for Phase 1.** If `./gradlew :app:assembleDebug`
succeeds and the app launches to CardListScreen stub without crashing, Phase 1 success criteria
1, 3, and 4 are verified. Room schema export JSON existence verifies criterion 2. Code review
verifies criterion 5 (relative paths).

### Wave 0 Gaps

- [ ] No test infrastructure exists yet; the project scaffold itself is the output of this phase.
  Wave 0 for Phase 1 = creating the project. No existing test files to configure.
- [ ] After 01-02 (Room schema), add `app/src/androidTest/kotlin/.../db/AppDatabaseTest.kt` to
  verify the three-table schema and `CardWithTags` relationship query.

---

## State of the Art

| Old Approach | Current Approach | Changed | Impact |
|--------------|-----------------|---------|--------|
| KAPT annotation processing | KSP annotation processing | ~2021-2023, now standard | Faster builds; KAPT deprecated |
| String-based nav routes `"card_list/{id}"` | `@Serializable` type-safe routes | Navigation 2.8.0 (2024) | Compile-time safety; no string typos |
| `ksp { arg("room.schemaLocation", ...) }` | `room { schemaDirectory(...) }` via Room Gradle Plugin | Room 2.6.0 (2023) | Reproducible/cached builds |
| KSP version format `2.x.y-1.0.z` | KSP standalone version `2.3.x` | KSP 2.3.0 (2025) | No more Kotlin version prefix |
| Coil 2.x `io.coil-kt:coil-compose` | Coil 3.x `io.coil-kt.coil3:coil-compose` | Coil 3.0.0 (2024) | New group ID; native Compose-first API |

**Deprecated/outdated:**
- `kapt("...")`: Replaced by `ksp("...")` universally — do not use.
- `androidx.navigation:navigation-safe-args-gradle-plugin`: String-based safe args — replaced by
  Navigation 2.8+ type-safe Serializable routes.
- `@Database(exportSchema = false)`: Never suppress — migration pain guarantee.

---

## Open Questions

1. **ML Kit text-recognition exact current version**
   - What we know: STACK.md reports 16.0.1 (MEDIUM confidence, training data)
   - What's unclear: Live Maven Central version not verified in this session
   - Recommendation: Run `curl https://dl.google.com/android/maven2/com/google/mlkit/text-recognition/maven-metadata.xml` or check `developers.google.com/ml-kit/android/text-recognition` before writing 01-01. If version has changed, use the latest stable.

2. **`buildConfig = true` already set in new project template**
   - What we know: AGP 8.0+ disables it by default
   - What's unclear: Android Studio new project wizard may or may not include it
   - Recommendation: Explicitly add `buildConfig = true` in 01-01 regardless — safe to include twice if template already has it.

---

## Sources

### Primary (HIGH confidence)
- Android Jetpack releases (developer.android.com/jetpack/androidx/releases/room) — Room 2.8.4 confirmed latest stable, Room Gradle Plugin introduced 2.6.0
- Navigation type-safety guide (developer.android.com/guide/navigation/design/type-safety) — `@Serializable` route syntax, `composable<Route>` pattern, `toRoute<Route>()` confirmed stable
- Hilt DI guide (developer.android.com/training/dependency-injection/hilt-android) — `@HiltAndroidApp`, `@AndroidEntryPoint`, `DatabaseModule` pattern, `@Binds` vs `@Provides`
- Room migration docs (developer.android.com/training/data-storage/room/migrating-db-versions) — `fallbackToDestructiveMigration()` has no boolean overload; conditional pattern confirmed
- Maven Central (central.sonatype.com/artifact/io.coil-kt.coil3/coil-compose) — Coil 3.4.0 confirmed latest stable (released ~27 days before 2026-03-24)
- Maven Central (central.sonatype.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json) — 1.10.0 confirmed as latest stable (not 2.0.0)
- WebSearch (2026-03-24) — kotlinx-coroutines-android 1.10.2 confirmed latest stable
- WebSearch (2026-03-24) — KSP 2.3.6 confirmed latest; versioning decoupled from Kotlin since KSP 2.3.0

### Secondary (MEDIUM confidence)
- Room @Relation + @Junction syntax — cross-referenced against official docs and community sources; HIGH for the annotation pattern, MEDIUM for edge cases
- ML Kit text-recognition 16.0.1 — training data, not live-verified. Verify before 01-01.

### Tertiary (needs validation)
- ML Kit text-recognition-korean 16.0.1 — same as Latin version; not independently live-verified.

---

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH — all critical versions verified live against Maven Central or official search
- Architecture: HIGH — official Android docs and Hilt guide confirmed patterns
- Room schema: HIGH — annotation syntax from official Room relationships docs
- Navigation: HIGH — confirmed from official type-safety guide (navigation-compose 2.9.7)
- Pitfalls: HIGH for platform behavior (BuildConfig, @Transaction, schema export); MEDIUM for KSP
  version format gotcha (based on release notes interpretation)

**Research date:** 2026-03-24
**Valid until:** 2026-06-24 (stable libraries; ML Kit version should be re-checked before build)
