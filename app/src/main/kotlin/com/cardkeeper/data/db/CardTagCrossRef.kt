package com.cardkeeper.data.db

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "card_tag_cross_ref",
    primaryKeys = ["cardId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CardTagCrossRef(
    val cardId: Long,
    val tagId: Long
)
