package com.cardkeeper.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class CardWithTags(
    @Embedded val card: CardEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            CardTagCrossRef::class,
            parentColumn = "cardId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
