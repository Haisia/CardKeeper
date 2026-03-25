package com.cardkeeper.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardkeeper.data.datasource.ImageStorageDataSource
import com.cardkeeper.data.datasource.OcrDataSource
import com.cardkeeper.data.db.CardEntity
import com.cardkeeper.domain.repository.CardRepository
import com.cardkeeper.domain.usecase.ParseOcrResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ----------------------------------------------------------------
// UI state models
// ----------------------------------------------------------------

sealed class ScanState {
    object Idle : ScanState()
    object LoadingOcr : ScanState()
    object Saving : ScanState()
    object Saved : ScanState()
    data class Error(val message: String) : ScanState()
}

data class ReviewFormState(
    val name: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = ""
)

// ----------------------------------------------------------------
// ViewModel
// ----------------------------------------------------------------

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val ocrDataSource: OcrDataSource,
    private val parseOcrResultUseCase: ParseOcrResultUseCase,
    private val imageStorageDataSource: ImageStorageDataSource,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _formState = MutableStateFlow(ReviewFormState())
    val formState: StateFlow<ReviewFormState> = _formState.asStateFlow()

    /** Temp image path set after CameraX capture or gallery import. */
    var tempImagePath: String? = null
        private set

    // ----------------------------------------------------------------
    // Called from AppNavHost / ScanScreen
    // ----------------------------------------------------------------

    fun setTempImagePath(path: String) {
        tempImagePath = path
        _scanState.value = ScanState.Idle
    }

    fun setError(message: String) {
        _scanState.value = ScanState.Error(message)
    }

    fun resetState() {
        _scanState.value = ScanState.Idle
        tempImagePath = null
        _formState.value = ReviewFormState()
    }

    // ----------------------------------------------------------------
    // Gallery import
    // ----------------------------------------------------------------

    fun processGalleryImage(
        context: android.content.Context,
        uri: android.net.Uri,
        onReady: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        _scanState.value = ScanState.Idle
        try {
            val tempFile = File(
                context.cacheDir,
                "temp_gallery_${System.currentTimeMillis()}.jpg"
            )
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                _scanState.value = ScanState.Error("Could not open gallery image")
                onError("Could not open gallery image")
                return
            }
            tempImagePath = tempFile.absolutePath
            _scanState.value = ScanState.Idle
            onReady(tempFile.absolutePath)
        } catch (e: Exception) {
            _scanState.value = ScanState.Error(e.message ?: "Gallery import failed")
            onError(e.message ?: "Gallery import failed")
        }
    }

    // ----------------------------------------------------------------
    // OcrReviewScreen: load OCR result into form
    // ----------------------------------------------------------------

    /**
     * Run OCR on [tempImagePath] and populate [formState] with parsed fields.
     *
     * Idempotent — if formState is already populated (all fields non-empty)
     * this is a no-op so rotating the screen does not re-run OCR.
     */
    fun loadOcrResult() {
        val imagePath = tempImagePath ?: return
        // Idempotency guard: skip if already loaded
        val current = _formState.value
        if (current.name.isNotEmpty() || current.email.isNotEmpty() || current.phone.isNotEmpty()) return
        if (_scanState.value == ScanState.LoadingOcr) return

        _scanState.value = ScanState.LoadingOcr
        viewModelScope.launch {
            try {
                val imageFile = File(imagePath)
                val blocks = ocrDataSource.recognizeText(imageFile)
                val parsed = parseOcrResultUseCase.invoke(blocks)
                _formState.value = ReviewFormState(
                    name = parsed.name,
                    company = parsed.company,
                    jobTitle = parsed.jobTitle,
                    phone = parsed.phone,
                    email = parsed.email,
                    address = parsed.address
                )
                _scanState.value = ScanState.Idle
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "OCR failed")
            }
        }
    }

    // ----------------------------------------------------------------
    // OcrReviewScreen: form field updates
    // ----------------------------------------------------------------

    fun updateName(value: String)     { _formState.value = _formState.value.copy(name = value) }
    fun updateCompany(value: String)  { _formState.value = _formState.value.copy(company = value) }
    fun updateJobTitle(value: String) { _formState.value = _formState.value.copy(jobTitle = value) }
    fun updatePhone(value: String)    { _formState.value = _formState.value.copy(phone = value) }
    fun updateEmail(value: String)    { _formState.value = _formState.value.copy(email = value) }
    fun updateAddress(value: String)  { _formState.value = _formState.value.copy(address = value) }

    // ----------------------------------------------------------------
    // OcrReviewScreen: save
    // ----------------------------------------------------------------

    /**
     * Compress the captured image to permanent storage and insert a card into Room.
     *
     * On success: [_scanState] transitions to [ScanState.Saved] — OcrReviewScreen
     * observes this and calls [onSaved] to navigate back to CardListScreen.
     * On error: [_scanState] transitions to [ScanState.Error].
     */
    fun saveCard() {
        val imagePath = tempImagePath ?: run {
            _scanState.value = ScanState.Error("No image to save")
            return
        }
        if (_scanState.value == ScanState.Saving) return  // prevent double-tap

        _scanState.value = ScanState.Saving
        viewModelScope.launch {
            try {
                val form = _formState.value
                val tempFile = File(imagePath)

                // Compress and move to permanent storage
                val relativeImagePath = imageStorageDataSource.saveImage(tempFile)

                // Delete temp file from cacheDir
                tempFile.delete()

                val now = System.currentTimeMillis()
                val entity = CardEntity(
                    imagePath = relativeImagePath,
                    name = form.name,
                    company = form.company,
                    jobTitle = form.jobTitle,
                    phone = form.phone.filter { it.isDigit() },
                    email = form.email,
                    address = form.address,
                    memo = "",
                    createdAt = now,
                    updatedAt = now
                )

                cardRepository.insertCard(entity)
                _scanState.value = ScanState.Saved
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Save failed")
            }
        }
    }
}
