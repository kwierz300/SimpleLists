package com.example.simplelists.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    val listId: Long = 1,
    val parentId: Long? = null,
    val isFolder: Boolean = false          // ← NOWE: odróżniamy „podlistę” od zwykłego itemu
)
