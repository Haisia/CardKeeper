package com.cardkeeper.data.repository

import com.cardkeeper.data.db.CardTagCrossRef
import com.cardkeeper.data.db.TagDao
import com.cardkeeper.data.db.TagEntity
import com.cardkeeper.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {
    override fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()
    override suspend fun insertTag(tag: TagEntity): Long = tagDao.insertTag(tag)
    override suspend fun deleteTag(tag: TagEntity) = tagDao.deleteTag(tag)
    override suspend fun setTagsForCard(cardId: Long, tagIds: List<Long>) {
        tagDao.deleteTagsForCard(cardId)
        tagIds.forEach { tagId ->
            tagDao.insertCrossRef(CardTagCrossRef(cardId = cardId, tagId = tagId))
        }
    }
}
