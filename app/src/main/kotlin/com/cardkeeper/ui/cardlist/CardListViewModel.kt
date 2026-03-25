package com.cardkeeper.ui.cardlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardkeeper.data.db.CardWithTags
import com.cardkeeper.data.db.TagEntity
import com.cardkeeper.domain.repository.CardRepository
import com.cardkeeper.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardListUiState(
    val cards: List<CardWithTags> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val tags: List<TagEntity> = emptyList(),
    val filterTagId: Long? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CardListViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterTagId = MutableStateFlow<Long?>(null)
    val filterTagId: StateFlow<Long?> = _filterTagId.asStateFlow()

    private val _uiState = MutableStateFlow(CardListUiState())
    val uiState: StateFlow<CardListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tagRepository.getAllTags().collect { tags ->
                _uiState.value = _uiState.value.copy(tags = tags)
            }
        }
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    cardRepository.getAllCards()
                }
                .onEach { _uiState.value = _uiState.value.copy(isLoading = false, error = null) }
                .collect { cards ->
                    _uiState.value = _uiState.value.copy(cards = cards)
                }
        }
        _uiState.value = _uiState.value.copy(isLoading = true)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onFilterTagChanged(tagId: Long?) {
        _filterTagId.value = tagId
        val currentCards = _uiState.value.cards
        _uiState.value = _uiState.value.copy(filterTagId = tagId)
        val allCards = currentCards
        if (tagId == null) {
            viewModelScope.launch {
                cardRepository.getAllCards().collect { cards ->
                    _uiState.value = _uiState.value.copy(cards = cards)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                cards = allCards.filter { card -> card.tags.any { it.id == tagId } }
            )
        }
    }
}
