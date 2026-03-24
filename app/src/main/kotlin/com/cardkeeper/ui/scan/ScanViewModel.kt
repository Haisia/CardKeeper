package com.cardkeeper.ui.scan

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed class ScanState {
    object Idle : ScanState()
    object Capturing : ScanState()
    data class Error(val message: String) : ScanState()
}

@HiltViewModel
class ScanViewModel @Inject constructor() : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // Temp image path set after CameraX capture; read by OcrReviewScreen in 02-05
    var tempImagePath: String? = null
        private set

    fun setTempImagePath(path: String) {
        tempImagePath = path
        _scanState.value = ScanState.Idle
    }

    fun setCapturing() {
        _scanState.value = ScanState.Capturing
    }

    fun setError(message: String) {
        _scanState.value = ScanState.Error(message)
    }

    fun resetState() {
        _scanState.value = ScanState.Idle
        tempImagePath = null
    }

    /**
     * Copy a gallery image URI to a temp file in cacheDir.
     *
     * On success, calls [onReady] with the absolute path of the temp file.
     * On error, calls [onError] with a description message.
     *
     * The content is read via ContentResolver — works for both content:// and file:// URIs
     * returned by ActivityResultContracts.GetContent.
     */
    fun processGalleryImage(
        context: android.content.Context,
        uri: android.net.Uri,
        onReady: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        _scanState.value = ScanState.Capturing
        try {
            val tempFile = java.io.File(
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
}
