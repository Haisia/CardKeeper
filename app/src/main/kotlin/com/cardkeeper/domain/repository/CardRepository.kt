package com.cardkeeper.domain.repository

import com.cardkeeper.data.db.CardEntity
import com.cardkeeper.data.db.CardWithTags
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun getAllCards(): Flow<List<CardWithTags>>
    fun getCardById(cardId: Long): Flow<CardWithTags?>
    fun searchCards(query: String): Flow<List<CardWithTags>>
    suspend fun insertCard(card: CardEntity): Long
    suspend fun updateCard(card: CardEntity)
    suspend fun deleteCard(card: CardEntity)
}
