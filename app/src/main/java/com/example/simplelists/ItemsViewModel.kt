package com.example.simplelists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.simplelists.data.AppDatabase
import com.example.simplelists.data.ItemEntity
import com.example.simplelists.data.ItemRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiItem(
    val id: Long,
    val title: String,
    val done: Boolean,
    val parentId: Long?,
    val isFolder: Boolean
)
data class UiList(val id: Long, val name: String)

class ItemsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ItemRepository(AppDatabase.get(app).itemDao())

    // aktywna lista + aktywny rodzic (dla widoku podlisty)
    private val _selectedListId = MutableStateFlow(-1L)
    private val _selectedParentId = MutableStateFlow<Long?>(null)

    // "stos" okruszków: (id, title) kolejnych otwieranych folderów
    private val _path = MutableStateFlow<List<Pair<Long, String>>>(emptyList())

    val selectedListId: StateFlow<Long> = _selectedListId.asStateFlow()
    val selectedParentId: StateFlow<Long?> = _selectedParentId.asStateFlow()
    val path: StateFlow<List<Pair<Long, String>>> = _path.asStateFlow()

    // listy
    val lists: StateFlow<List<UiList>> =
        repo.observeLists()
            .onEach { ls ->
                if (ls.isNotEmpty() && (_selectedListId.value == -1L || ls.none { it.id == _selectedListId.value })) {
                    _selectedListId.value = ls.first().id
                    clearPath()
                }
            }
            .map { it.map { l -> UiList(l.id, l.name) } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // elementy (na aktualnym poziomie: lista + ewentualny rodzic)
    val items: StateFlow<List<UiItem>> =
        combine(_selectedListId, _selectedParentId) { listId, parentId -> listId to parentId }
            .flatMapLatest { (listId, parentId) -> repo.observeItems(listId, parentId) }
            .map { it.map { e -> UiItem(e.id, e.title, e.done, e.parentId, e.isFolder) } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // zapewnij jedną listę startową
        viewModelScope.launch {
            val current = repo.observeLists().first()
            if (current.isEmpty()) {
                val id = repo.addList("My list")
                _selectedListId.value = id
            } else if (_selectedListId.value == -1L) {
                _selectedListId.value = current.first().id
            }
        }
    }

    // --- Listy ---
    fun selectList(id: Long) {
        _selectedListId.value = id
        clearPath()
    }

    fun addList(name: String) = viewModelScope.launch {
        val n = name.trim()
        if (n.isNotEmpty()) {
            val id = repo.addList(n)
            _selectedListId.value = id
            clearPath()
        }
    }

    fun deleteCurrentList() = viewModelScope.launch {
        repo.deleteList(_selectedListId.value)
        clearPath()
    }

    // w klasie ItemsViewModel:
    fun updateList(id: Long, newName: String) = viewModelScope.launch {
        val n = newName.trim()
        if (n.isNotEmpty()) {
            repo.updateList(id, n)
        }
    }

    // --- Foldery / podlisty ---
    fun openSublist(id: Long, title: String) {
        _selectedParentId.value = id
        _path.value = _path.value + (id to title)
    }

    fun goUp() {
        val p = _path.value
        if (p.isNotEmpty()) {
            val np = p.dropLast(1)
            _path.value = np
            _selectedParentId.value = np.lastOrNull()?.first
        } else {
            _selectedParentId.value = null
        }
    }

    // Strumień wszystkich folderów w aktywnej liście (niezależnie od poziomu)
    val allFoldersInSelectedList: StateFlow<List<UiItem>> =
        _selectedListId
            .flatMapLatest { listId -> repo.observeAllFoldersInList(listId) }
            .map { it.map { e -> UiItem(e.id, e.title, e.done, e.parentId, e.isFolder) } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun moveItemToFolder(itemId: Long, targetFolderId: Long?) = viewModelScope.launch {
        // targetFolderId == null -> przeniesienie do "roota" listy
        repo.moveItemToParent(itemId, targetFolderId)
    }


    // przejdź do poziomu z breadcrumbs (index 0 = root listy)
    fun goToBreadcrumb(index: Int) {
        if (index <= 0) {
            clearPath(); return
        }
        val newPath = _path.value.take(index)
        _path.value = newPath
        _selectedParentId.value = newPath.lastOrNull()?.first
    }

    private fun clearPath() {
        _path.value = emptyList()
        _selectedParentId.value = null
    }

    // --- Dodawanie / modyfikacja ---
    fun addItem(title: String) = viewModelScope.launch {
        val t = title.trim()
        if (t.isNotEmpty()) {
            repo.add(t, _selectedListId.value, _selectedParentId.value, isFolder = false)
        }
    }

    fun addFolder(title: String) = viewModelScope.launch {
        val t = title.trim()
        if (t.isNotEmpty()) {
            repo.add(t, _selectedListId.value, _selectedParentId.value, isFolder = true)
        }
    }

    fun updateItem(id: Long, title: String, done: Boolean, parentId: Long?, isFolder: Boolean) =
        viewModelScope.launch {
            val t = title.trim()
            if (t.isNotEmpty()) {
                repo.update(ItemEntity(id, t, done, _selectedListId.value, parentId, isFolder))
            }
        }

    fun deleteItem(id: Long, title: String, done: Boolean, parentId: Long?, isFolder: Boolean) =
        viewModelScope.launch {
            repo.delete(ItemEntity(id, title, done, _selectedListId.value, parentId, isFolder))
        }

    fun toggleDone(id: Long, current: Boolean) = viewModelScope.launch {
        repo.toggleDone(id, !current)
    }
}
