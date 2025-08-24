package com.example.simplelists.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
