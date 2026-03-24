package com.cardkeeper.domain.repository

import com.cardkeeper.data.db.TagEntity
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<TagEntity>>
    suspend fun insertTag(tag: TagEntity): Long
    suspend fun deleteTag(tag: TagEntity)
    suspend fun setTagsForCard(cardId: Long, tagIds: List<Long>)
}
