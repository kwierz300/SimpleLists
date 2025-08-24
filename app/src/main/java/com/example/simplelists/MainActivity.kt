package com.example.simplelists

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.simplelists.ui.theme.SimpleListsTheme



class MainActivity : ComponentActivity() {
    private val vm: ItemsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            SimpleListsTheme(darkTheme = darkMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    App(vm, onToggleTheme = { darkMode = !darkMode })
                }
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun App(vm: ItemsViewModel, onToggleTheme: () -> Unit) {
    val lists by vm.lists.collectAsState()
    val items by vm.items.collectAsState()
    val selectedListId by vm.selectedListId.collectAsState()
    val selectedParentId by vm.selectedParentId.collectAsState()
    val path by vm.path.collectAsState()

    var editDialog by remember { mutableStateOf<UiItem?>(null) }
    var editText by remember { mutableStateOf(TextFieldValue("")) }
    var editNote by remember { mutableStateOf(TextFieldValue("")) }

    var editListDialog by remember { mutableStateOf<UiList?>(null) }
    var editListText by remember { mutableStateOf(TextFieldValue("")) }

    var newList by remember { mutableStateOf(TextFieldValue("")) }
    var newItem by remember { mutableStateOf(TextFieldValue("")) }
    var newFolder by remember { mutableStateOf(TextFieldValue("")) }
    var confirmDeleteList by remember { mutableStateOf(false) }

    val bigShape = MaterialTheme.shapes.extraLarge

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (selectedParentId != null) {
                        IconButton(onClick = { vm.goUp() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Up")
                        }
                    }
                },
                title = { Text("Simple Lists") },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(Icons.Filled.DarkMode, contentDescription = "Toggle theme")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- Dodawanie LISTY ---
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newList,
                    onValueChange = { newList = it },
                    label = { Text("New list") },
                    singleLine = true,
                    shape = bigShape,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = {
                    vm.addList(newList.text)
                    newList = TextFieldValue("")
                }) { Text("Add") }
            }

            // --- Zakładki LIST ---
            if (lists.isNotEmpty() && selectedListId != -1L) {
                val rawIndex = lists.indexOfFirst { it.id == selectedListId }
                val safeIndex = (if (rawIndex == -1) 0 else rawIndex).coerceIn(0, lists.lastIndex)

                key(lists.size) {
                    ScrollableTabRow(selectedTabIndex = safeIndex, edgePadding = 12.dp, divider = {}) {
                        lists.forEachIndexed { idx, l ->
                            val selected = idx == safeIndex
                            val bg by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant, label = ""
                            )
                            val fg by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant, label = ""
                            )
                            Tab(
                                selected = selected,
                                onClick = { vm.selectList(l.id) },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            l.name,
                                            color = fg,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(bg)
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                        if (selected) {
                                            IconButton(onClick = {
                                                editListDialog = l
                                                editListText = TextFieldValue(l.name)
                                            }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit list")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // --- Breadcrumbs jako chipsy ---
            val crumbTexts = buildList {
                add(lists.firstOrNull { it.id == selectedListId }?.name ?: "-")
                addAll(path.map { it.second })
            }
            if (crumbTexts.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    crumbTexts.forEachIndexed { i, text ->
                        val isLast = i == crumbTexts.lastIndex
                        AssistChip(
                            onClick = { if (!isLast) vm.goToBreadcrumb(i) },
                            label = { Text(text) },
                            enabled = !isLast,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isLast) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // --- Dodawanie ITEMU ---
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    label = { Text("New item") },
                    singleLine = true,
                    shape = bigShape,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = {
                    vm.addItem(newItem.text)
                    newItem = TextFieldValue("")
                }) { Text("Add item") }
            }

            // --- Dodawanie FOLDERU ---
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newFolder,
                    onValueChange = { newFolder = it },
                    label = { Text("New sublist (folder)") },
                    singleLine = true,
                    shape = bigShape,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = {
                    vm.addFolder(newFolder.text)
                    newFolder = TextFieldValue("")
                }) { Text("Add folder") }
            }

            Spacer(Modifier.height(8.dp))

            // --- Lista elementów z animacją ---
            AnimatedContent(
                targetState = items,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn(tween(200))) with
                            (slideOutHorizontally { -it } + fadeOut(tween(200)))
                }, label = ""
            ) { listContent ->
                if (listContent.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No items yet.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(listContent.sortedWith(compareBy<UiItem> { it.done }.thenBy { it.title })) { item ->
                            val dismissState = rememberDismissState(
                                confirmStateChange = { value ->
                                    if (value == DismissValue.DismissedToEnd || value == DismissValue.DismissedToStart) {
                                        vm.deleteItem(item.id, item.title, item.done, item.parentId, item.isFolder)
                                        true
                                    } else false
                                }
                            )

                            val baseColor = when {
                                item.isFolder -> MaterialTheme.colorScheme.primaryContainer
                                item.done     -> Color(0xFF81C784) // zielony pastel (Material Green 300)
                                else          -> MaterialTheme.colorScheme.surface
                            }

                            val cardColor by animateColorAsState(baseColor, label = "")

                            SwipeToDismiss(
                                state = dismissState,
                                background = {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                    )
                                },
                                dismissContent = {
                                    ElevatedCard(
                                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                        shape = MaterialTheme.shapes.extraLarge,
                                        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = item.isFolder) {
                                                if (item.isFolder) vm.openSublist(item.id, item.title)
                                            }
                                    ) {
                                        Row(
                                            Modifier
                                                .padding(14.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (item.isFolder) {
                                                Icon(Icons.Filled.Folder, contentDescription = null)
                                                Spacer(Modifier.width(10.dp))
                                                Text(item.title, modifier = Modifier.weight(1f))
                                            } else {
                                                Checkbox(
                                                    checked = item.done,
                                                    onCheckedChange = { vm.toggleDone(item.id, item.done) }
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(item.title)
                                                    if (item.done) Text("Done", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                            IconButton(onClick = {
                                                editDialog = item
                                                editText = TextFieldValue(item.title)
                                            }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                            }
                                        }
                                    }
                                },
                                directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart)
                            )
                        }
                    }
                }
            }
        }

        // --- Potwierdzenie usunięcia listy ---
        if (confirmDeleteList) {
            AlertDialog(
                onDismissRequest = { confirmDeleteList = false },
                confirmButton = {
                    TextButton(onClick = { confirmDeleteList = false; vm.deleteCurrentList() }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteList = false }) { Text("Cancel") }
                },
                title = { Text("Delete list?") },
                text = { Text("This will remove the current list with all its items.") }
            )
        }

        // --- Dialog edycji itemu/folderu ---
        if (editDialog != null) {
            val cur = editDialog!!
            AlertDialog(
                onDismissRequest = { editDialog = null },
                confirmButton = {
                    TextButton(onClick = {
                        vm.updateItem(
                            id = cur.id,
                            title = editText.text,
                            done = cur.done,
                            parentId = cur.parentId,
                            isFolder = cur.isFolder
                        )
                        editDialog = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editDialog = null }) { Text("Cancel") }
                },
                title = { Text(if (cur.isFolder) "Rename folder" else "Edit item") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            label = { Text("Title") }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            shape = MaterialTheme.shapes.large,
                            label = { Text("Note (optional)") }
                        )
                    }
                }
            )
        }

        // --- Dialog edycji nazwy listy ---
        if (editListDialog != null) {
            val cur = editListDialog!!
            AlertDialog(
                onDismissRequest = { editListDialog = null },
                confirmButton = {
                    TextButton(onClick = {
                        // tu wystarczy update w repo jak w items (dodaj w ViewModel)
                        // vm.updateList(cur.id, editListText.text)
                        editListDialog = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editListDialog = null }) { Text("Cancel") }
                },
                title = { Text("Rename list") },
                text = {
                    OutlinedTextField(
                        value = editListText,
                        onValueChange = { editListText = it },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        label = { Text("Name") }
                    )
                }
            )
        }
    }
}
