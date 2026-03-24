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
}
