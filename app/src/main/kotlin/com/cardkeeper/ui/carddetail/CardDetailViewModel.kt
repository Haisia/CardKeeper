package com.cardkeeper.ui.carddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardkeeper.data.db.CardEntity
import com.cardkeeper.data.db.CardWithTags
import com.cardkeeper.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardDetailUiState(
    val card: CardWithTags? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    private var loadedCardId: Long = -1

    fun loadCard(cardId: Long) {
        if (cardId == loadedCardId) return
        loadedCardId = cardId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                cardRepository.getCardById(cardId)
                    .collect { card ->
                        _uiState.value = _uiState.value.copy(
                            card = card,
                            isLoading = false,
                            error = if (card == null) "Card not found" else null
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load card"
                )
            }
        }
    }

    fun setEditing(editing: Boolean) {
        _uiState.value = _uiState.value.copy(isEditing = editing)
    }

    fun setShowDeleteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = show)
    }

    fun saveCard(
        name: String, company: String, jobTitle: String,
        phone: String, email: String, address: String
    ) {
        val card = _uiState.value.card ?: return
        if (_uiState.value.isSaving) return

        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val updated = card.card.copy(
                    name = name, company = company, jobTitle = jobTitle,
                    phone = phone, email = email, address = address,
                    updatedAt = System.currentTimeMillis()
                )
                cardRepository.updateCard(updated)
                _uiState.value = _uiState.value.copy(isSaving = false, isEditing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save"
                )
            }
        }
    }

    fun deleteCard() {
        val card = _uiState.value.card ?: return
        viewModelScope.launch {
            try {
                cardRepository.deleteCard(card.card)
                _uiState.value = _uiState.value.copy(showDeleteDialog = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showDeleteDialog = false,
                    error = e.message ?: "Failed to delete"
                )
            }
        }
    }
}
