package com.example.simplelists.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    // --- LISTY ---
    @Query("SELECT * FROM lists ORDER BY id ASC")
    fun observeLists(): Flow<List<ListEntity>>

    @Insert suspend fun insertList(list: ListEntity): Long
    @Update suspend fun updateList(list: ListEntity)
    @Delete suspend fun deleteList(list: ListEntity)

    // --- POZYCJE / PODLISTY ---
    @Query("""
        SELECT * FROM items
        WHERE listId = :listId AND 
              CASE WHEN :parentId IS NULL THEN parentId IS NULL ELSE parentId = :parentId END
        ORDER BY id DESC
    """)
    fun observeItems(listId: Long, parentId: Long? = null): Flow<List<ItemEntity>>

    @Insert suspend fun insert(item: ItemEntity): Long
    @Update suspend fun update(item: ItemEntity)
    @Delete suspend fun delete(item: ItemEntity)

    @Query("UPDATE items SET done = :done WHERE id = :id")
    suspend fun toggleDone(id: Long, done: Boolean)
}
