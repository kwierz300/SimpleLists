package com.example.simplelists.data

import kotlinx.coroutines.flow.Flow

class ItemRepository(private val dao: ItemDao) {

    // --- Listy ---
    fun observeLists(): Flow<List<ListEntity>> = dao.observeLists()

    suspend fun addList(name: String): Long =
        dao.insertList(ListEntity(name = name))

    suspend fun updateList(id: Long, name: String) {
        dao.updateListName(id, name) // @Query("UPDATE lists SET name = :name WHERE id = :id")
    }

    suspend fun deleteList(id: Long) {
        // Room @Delete wymaga encji; nazwa nieistotna przy delete po PK
        dao.deleteList(ListEntity(id = id, name = ""))
    }

    // ItemRepository.kt
    fun observeAllFoldersInList(listId: Long): Flow<List<ItemEntity>> =
        dao.observeAllFoldersInList(listId)

    suspend fun moveItemToParent(id: Long, newParentId: Long?) =
        dao.moveItemToParent(id, newParentId)


    // --- Pozycje ---
    fun observeItems(listId: Long, parentId: Long? = null): Flow<List<ItemEntity>> =
        dao.observeItems(listId, parentId)

    suspend fun add(
        title: String,
        listId: Long,
        parentId: Long?,
        isFolder: Boolean = false
    ) = dao.insert(
        ItemEntity(
            title = title,
            listId = listId,
            parentId = parentId,
            isFolder = isFolder
        )
    )

    suspend fun update(item: ItemEntity) = dao.update(item)

    suspend fun delete(item: ItemEntity) = dao.delete(item)

    suspend fun toggleDone(id: Long, done: Boolean) = dao.toggleDone(id, done)
}
