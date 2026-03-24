package com.cardkeeper.data.repository

import com.cardkeeper.data.db.CardDao
import com.cardkeeper.data.db.CardEntity
import com.cardkeeper.data.db.CardWithTags
import com.cardkeeper.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao
) : CardRepository {
    override fun getAllCards(): Flow<List<CardWithTags>> = cardDao.getAllCardsWithTags()
    override fun getCardById(cardId: Long): Flow<CardWithTags?> = cardDao.getCardWithTagsById(cardId)
    override fun searchCards(query: String): Flow<List<CardWithTags>> = cardDao.searchCards("%$query%")
    override suspend fun insertCard(card: CardEntity): Long = cardDao.insertCard(card)
    override suspend fun updateCard(card: CardEntity) = cardDao.updateCard(card)
    override suspend fun deleteCard(card: CardEntity) = cardDao.deleteCard(card)
}
