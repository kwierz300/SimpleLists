package com.example.simplelists.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    // --- LISTY ---
    @Query("SELECT * FROM lists ORDER BY id ASC")
    fun observeLists(): Flow<List<ListEntity>>

    @Insert
    suspend fun insertList(list: ListEntity): Long

    // ZAMIANA: zamiast @Update(ListEntity) używamy bezpośredniego UPDATE po id
    @Query("UPDATE lists SET name = :name WHERE id = :id")
    suspend fun updateListName(id: Long, name: String)

    @Delete
    suspend fun deleteList(list: ListEntity)

    // --- POZYCJE / PODLISTY ---
    @Query(
        """
        SELECT * FROM items
        WHERE listId = :listId AND 
              CASE WHEN :parentId IS NULL THEN parentId IS NULL ELSE parentId = :parentId END
        ORDER BY id DESC
        """
    )
    fun observeItems(listId: Long, parentId: Long? = null): Flow<List<ItemEntity>>

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Delete
    suspend fun delete(item: ItemEntity)

    @Query("UPDATE items SET done = :done WHERE id = :id")
    suspend fun toggleDone(id: Long, done: Boolean)

    // ItemDao.kt
    @Query("UPDATE items SET parentId = :newParentId WHERE id = :id")
    suspend fun moveItemToParent(id: Long, newParentId: Long?)

    @Query("SELECT * FROM items WHERE listId = :listId AND isFolder = 1 ORDER BY title ASC")
    fun observeAllFoldersInList(listId: Long): Flow<List<ItemEntity>>

}
