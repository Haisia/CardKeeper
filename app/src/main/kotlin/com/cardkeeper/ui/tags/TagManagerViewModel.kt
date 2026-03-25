package com.cardkeeper.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardkeeper.data.db.TagEntity
import com.cardkeeper.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagManagerViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _tags = MutableStateFlow<List<TagEntity>>(emptyList())
    val tags: StateFlow<List<TagEntity>> = _tags.asStateFlow()

    init {
        viewModelScope.launch {
            tagRepository.getAllTags().collect { _tags.value = it }
        }
    }

    fun addTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            tagRepository.insertTag(TagEntity(name = name))
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
        }
    }
}
