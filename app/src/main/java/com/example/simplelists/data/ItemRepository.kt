package com.example.simplelists.data

import kotlinx.coroutines.flow.Flow

class ItemRepository(private val dao: ItemDao) {
    // Listy
    fun observeLists(): Flow<List<ListEntity>> = dao.observeLists()
    suspend fun addList(name: String): Long = dao.insertList(ListEntity(name = name))
    suspend fun renameList(id: Long, name: String) = dao.updateList(ListEntity(id = id, name = name))
    suspend fun deleteList(id: Long) = dao.deleteList(ListEntity(id = id, name = ""))

    // Pozycje
    fun observeItems(listId: Long, parentId: Long? = null): Flow<List<ItemEntity>> =
        dao.observeItems(listId, parentId)

    suspend fun add(title: String, listId: Long, parentId: Long?, isFolder: Boolean = false) =
        dao.insert(ItemEntity(title = title, listId = listId, parentId = parentId, isFolder = isFolder))

    suspend fun update(item: ItemEntity) = dao.update(item)
    suspend fun delete(item: ItemEntity) = dao.delete(item)
    suspend fun toggleDone(id: Long, done: Boolean) = dao.toggleDone(id, done)
}
