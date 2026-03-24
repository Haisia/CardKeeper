package com.cardkeeper.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String?,       // RELATIVE path only: "cards/uuid.jpg"
    val name: String,
    val company: String,
    val jobTitle: String,
    val phone: String,
    val email: String,
    val address: String,
    val memo: String = "",
    val createdAt: Long,          // System.currentTimeMillis()
    val updatedAt: Long           // System.currentTimeMillis()
)
