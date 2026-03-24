package com.cardkeeper.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Transaction
    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    fun getAllCardsWithTags(): Flow<List<CardWithTags>>

    @Transaction
    @Query("SELECT * FROM cards WHERE id = :cardId")
    fun getCardWithTagsById(cardId: Long): Flow<CardWithTags?>

    @Transaction
    @Query("""
        SELECT DISTINCT c.* FROM cards c
        LEFT JOIN card_tag_cross_ref ct ON c.id = ct.cardId
        LEFT JOIN tags t ON ct.tagId = t.id
        WHERE c.name LIKE :query
           OR c.company LIKE :query
           OR c.jobTitle LIKE :query
        ORDER BY c.updatedAt DESC
    """)
    fun searchCards(query: String): Flow<List<CardWithTags>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)
}
