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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val selectedTagIds: Set<Long> = emptySet(),
    val showAll: Boolean = true
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CardListViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    private val _uiState = MutableStateFlow(CardListUiState())
    val uiState: StateFlow<CardListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tagRepository.getAllTags().collect { tags ->
                _uiState.value = _uiState.value.copy(tags = tags)
            }
        }
        viewModelScope.launch {
            combine(_searchQuery.debounce(300), _selectedTagIds) { query, tagIds ->
                query to tagIds
            }
                .distinctUntilChanged()
                .flatMapLatest { (query, tagIds) ->
                    val filteredCards: Flow<List<CardWithTags>> = when {
                        tagIds.isNotEmpty() && query.isNotBlank() -> cardRepository.searchCardsByTags(tagIds, query)
                        tagIds.isNotEmpty() -> cardRepository.getCardsByTags(tagIds)
                        query.isNotBlank() -> cardRepository.searchCards(query)
                        else -> cardRepository.getAllCards()
                    }
                    filteredCards
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

    fun onToggleTag(tagId: Long) {
        val current = _selectedTagIds.value.toMutableSet()
        if (current.contains(tagId)) current.remove(tagId) else current.add(tagId)
        _selectedTagIds.value = current
        _uiState.value = _uiState.value.copy(selectedTagIds = current, showAll = false)
    }

    fun onSelectAllTags() {
        _selectedTagIds.value = emptySet()
        _uiState.value = _uiState.value.copy(selectedTagIds = emptySet(), showAll = true)
    }
}
