package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recycle_bin")
data class RecycleBinEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val itemType: String, // e.g., "transaction", "animal", "person", "feed", "note"
    val originalId: Int, // The ID from its original table
    val itemJson: String, // The serialized JSON representation of the entity
    val deletedAt: Long // Epoch millis
)
