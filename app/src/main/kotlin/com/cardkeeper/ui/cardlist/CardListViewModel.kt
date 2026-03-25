package com.cardkeeper.ui.cardlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardkeeper.data.db.CardWithTags
import com.cardkeeper.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardListUiState(
    val cards: List<CardWithTags> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CardListViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardListUiState())
    val uiState: StateFlow<CardListUiState> = _uiState.asStateFlow()

    init {
        loadCards()
    }

    private fun loadCards() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                cardRepository.getAllCards()
                    .collect { cards ->
                        _uiState.value = _uiState.value.copy(
                            cards = cards,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load cards"
                )
            }
        }
    }

    fun refresh() {
        loadCards()
    }
}
