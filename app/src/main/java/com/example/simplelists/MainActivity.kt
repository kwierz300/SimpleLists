package com.example.simplelists

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.simplelists.ui.theme.SimpleListsTheme
import androidx.compose.foundation.ExperimentalFoundationApi


class MainActivity : ComponentActivity() {
    private val vm: ItemsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            SimpleListsTheme(darkTheme = darkMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    App(
                        vm = vm,
                        isDarkTheme = darkMode,
                        onToggleTheme = { darkMode = !darkMode }
                    )
                }
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun App(
    vm: ItemsViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val lists by vm.lists.collectAsState()
    val items by vm.items.collectAsState()
    val selectedListId by vm.selectedListId.collectAsState()
    val selectedParentId by vm.selectedParentId.collectAsState()
    val path by vm.path.collectAsState()

    // foldery w aktywnej liście (do dialogu przenoszenia)
    val allFolders by vm.allFoldersInSelectedList.collectAsState()

    var editDialog by remember { mutableStateOf<UiItem?>(null) }
    var editText by remember { mutableStateOf(TextFieldValue("")) }
    var editNote by remember { mutableStateOf(TextFieldValue("")) }

    var editListDialog by remember { mutableStateOf<UiList?>(null) }
    var editListText by remember { mutableStateOf(TextFieldValue("")) }

    var newList by remember { mutableStateOf(TextFieldValue("")) }
    var newItem by remember { mutableStateOf(TextFieldValue("")) }
    var newFolder by remember { mutableStateOf(TextFieldValue("")) }
    var confirmDeleteList by remember { mutableStateOf(false) }

    // FAB (lewy dół) do panelu add item/folder
    var showAddSection by remember { mutableStateOf(false) }
    // Mały przycisk „Add list” w appbarze
    var showAddList by remember { mutableStateOf(false) }
    // dialog przenoszenia
    var moveDialogFor by remember { mutableStateOf<UiItem?>(null) }

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
                    // Mały przycisk, który pokazuje/ukrywa pole „New list”
                    IconButton(onClick = { showAddList = !showAddList }) {
                        Icon(
                            imageVector = if (showAddList) Icons.Filled.Close else Icons.Filled.Add,
                            contentDescription = if (showAddList) "Close add list" else "Add list"
                        )
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(Icons.Filled.DarkMode, contentDescription = "Toggle theme")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                Modifier.fillMaxSize()
            ) {
                // --- Dodawanie LISTY (ukryte pod Add list w appbarze) ---
                AnimatedVisibility(
                    visible = showAddList,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
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
                            showAddList = false // schowaj po dodaniu
                        }) { Text("Add") }
                    }
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

                // --- Panel dodawania ITEM/FOLDER (ukryty FABem w lewym dolnym rogu) ---
                AnimatedVisibility(
                    visible = showAddSection,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
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
                    }
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

                                // Tło karty
                                val baseColor = when {
                                    item.isFolder -> MaterialTheme.colorScheme.primaryContainer
                                    item.done     -> Color(0xFFA5D6A7) // jasny zielony dla "done"
                                    else          -> MaterialTheme.colorScheme.surface
                                }
                                val cardColor by animateColorAsState(baseColor, label = "")

                                // Czytelność treści (kontrast)
                                val preferredOn = when {
                                    item.isFolder -> MaterialTheme.colorScheme.onPrimaryContainer
                                    item.done && isDarkTheme -> Color.Black
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                val autoOn = if (cardColor.luminance() > 0.5f) Color.Black else Color.White
                                val contentColor = if (item.done) {
                                    if (isDarkTheme) Color.Black else autoOn
                                } else {
                                    preferredOn
                                }

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
                                            colors = CardDefaults.elevatedCardColors(
                                                containerColor = cardColor,
                                                contentColor = contentColor
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                // foldery otwieramy kliknięciem
                                                .clickable(enabled = item.isFolder) {
                                                    if (item.isFolder) vm.openSublist(item.id, item.title)
                                                }
                                        ) {
                                            // dodaj warunkowy modifier: gest long‑press tylko dla NIE‑folderów
                                            val rowGestures =
                                                if (!item.isFolder) {
                                                    Modifier.combinedClickable(
                                                        onLongClick = { moveDialogFor = item },
                                                        onClick = { /* nic - klik obsługują inne elementy */ }
                                                    )
                                                } else {
                                                    Modifier // dla folderów: brak gestu na Row, żeby klik karty działał na całej powierzchni
                                                }

                                            Row(
                                                Modifier
                                                    .padding(14.dp)
                                                    .fillMaxWidth()
                                                    .then(rowGestures),          // <— kluczowa zmiana
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (item.isFolder) {
                                                    Icon(
                                                        Icons.Filled.Folder,
                                                        contentDescription = null,
                                                        tint = contentColor
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        item.title,
                                                        color = contentColor,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                } else {
                                                    Checkbox(
                                                        checked = item.done,
                                                        onCheckedChange = { vm.toggleDone(item.id, item.done) },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor   = if (item.done && isDarkTheme) Color.Black else MaterialTheme.colorScheme.primary,
                                                            checkmarkColor = Color.White,
                                                            uncheckedColor = MaterialTheme.colorScheme.outline
                                                        )
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Column(Modifier.weight(1f)) {
                                                        Text(item.title, color = contentColor)
                                                        if (item.done) {
                                                            Text(
                                                                "Done",
                                                                color = contentColor.copy(alpha = 0.8f),
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                    }
                                                }
                                                IconButton(onClick = {
                                                    editDialog = item
                                                    editText = TextFieldValue(item.title)
                                                }) {
                                                    Icon(
                                                        Icons.Filled.Edit,
                                                        contentDescription = "Edit",
                                                        tint = contentColor
                                                    )
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

            // --- FAB: pokaz/ukryj panel dodawania item/folder ---
            FloatingActionButton(
                onClick = { showAddSection = !showAddSection },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (showAddSection) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = if (showAddSection) "Close add panel" else "Open add panel"
                )
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
                        vm.updateList(cur.id, editListText.text)  // zapis nazwy
                        editListDialog = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editListDialog = null }) { Text("Cancel") }
                },
                title = { Text("Rename list") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editListText,
                            onValueChange = { editListText = it },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            label = { Text("Name") }
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    editListDialog = null
                                    confirmDeleteList = true
                                },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete list")
                            }
                        }
                    }
                }
            )
        }

        // --- Dialog „Move to…” dla itemu ---
        if (moveDialogFor != null) {
            val src = moveDialogFor!!
            AlertDialog(
                onDismissRequest = { moveDialogFor = null },
                confirmButton = {}, // niepotrzebny – wybór poniżej
                dismissButton = {
                    TextButton(onClick = { moveDialogFor = null }) { Text("Cancel") }
                },
                title = { Text("Move \"${src.title}\" to…") },
                text = {
                    Column {
                        TextButton(onClick = {
                            vm.moveItemToFolder(src.id, null) // do roota listy
                            moveDialogFor = null
                        }) { Text("— Top level (no folder) —") }

                        Spacer(Modifier.height(8.dp))

                        if (allFolders.isEmpty()) {
                            Text("No folders in this list yet.")
                        } else {
                            LazyColumn {
                                items(allFolders) { folder ->
                                    TextButton(onClick = {
                                        vm.moveItemToFolder(src.id, folder.id)
                                        moveDialogFor = null
                                    }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Folder, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(folder.title)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
