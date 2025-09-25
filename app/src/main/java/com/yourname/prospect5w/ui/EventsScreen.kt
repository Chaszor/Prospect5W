@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.yourname.prospect5w.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyItemScope.animateItem
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.EventViewModel
import com.yourname.prospect5w.data.Event
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EventsScreen(
    vm: EventViewModel,
    snackbarHostState: SnackbarHostState,
    onEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val scope = rememberCoroutineScope()

    val events by vm.events.collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    var startFilter: LocalDate? by remember { mutableStateOf(null) }
    var endFilter: LocalDate? by remember { mutableStateOf(null) }
    var oldestFirst by remember { mutableStateOf(true) }

    val selected = remember { mutableStateListOf<Long>() }
    val selectionMode = selected.isNotEmpty()

    fun withinRange(e: Event): Boolean {
        val d = Instant.ofEpochMilli(e.startTime).atZone(zone).toLocalDate()
        val sOk = startFilter?.let { d >= it } ?: true
        val eOk = endFilter?.let { d <= it } ?: true
        return sOk && eOk
    }

    val active = events
        .filter { !it.archived }
        .filter { e ->
            val q = query.trim().lowercase()
            (q.isBlank() || listOf(e.title, e.location, e.description).any { s -> s.lowercase().contains(q) }) &&
                    withinRange(e)
        }

    val items = if (oldestFirst) active.sortedBy { it.startTime } else active.sortedByDescending { it.startTime }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            selectionMode = selectionMode,
            selectedCount = selected.size,
            onClearSelection = { selected.clear() },
            onSelectAll = { selected.clear(); selected.addAll(items.map { it.id }) },
            onBulkArchive = {
                val ids = selected.toList()
                selected.clear()
                scope.launch {
                    ids.forEach { vm.archive(it) }
                    val res = snackbarHostState.showSnackbar("Archived ${ids.size} item(s)", "Undo", true)
                    if (res == SnackbarResult.ActionPerformed) ids.forEach { vm.unarchive(it) }
                }
            },
            onBulkDelete = {
                val toDelete = items.filter { it.id in selected }.map { it.copy() }
                val ids = toDelete.map { it.id }
                selected.clear()
                scope.launch {
                    ids.forEach { vm.deleteById(it) }
                    val res = snackbarHostState.showSnackbar("Deleted ${ids.size} item(s)", "Undo", true)
                    if (res == SnackbarResult.ActionPerformed) toDelete.forEach { vm.add(it.copy(id = 0L)) }
                }
            },
            query = query,
            onQueryChange = { query = it },
            oldestFirst = oldestFirst,
            onToggleSort = { oldestFirst = !oldestFirst },
            start = startFilter,
            end = endFilter,
            onSetStart = { startFilter = it },
            onSetEnd = { endFilter = it },
            onClearDates = { startFilter = null; endFilter = null }
        )

        if (items.isEmpty()) {
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) { Text("No matching events.") }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { e ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.StartToEnd -> { // swipe right → archive
                                    scope.launch {
                                        vm.archive(e.id)
                                        val res = snackbarHostState.showSnackbar("Archived", "Undo", true)
                                        if (res == SnackbarResult.ActionPerformed) vm.unarchive(e.id)
                                    }
                                    true
                                }
                                SwipeToDismissBoxValue.EndToStart -> { // swipe left → delete
                                    scope.launch {
                                        val snapshot = e.copy()
                                        vm.deleteById(e.id)
                                        val res = snackbarHostState.showSnackbar("Deleted", "Undo", true)
                                        if (res == SnackbarResult.ActionPerformed) vm.add(snapshot.copy(id = 0L))
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { DismissBackgroundEvents(dismissState.currentValue) }
                    ) {
                        EventRow(
                            e = e,
                            selected = e.id in selected,
                            selectionMode = selectionMode,
                            onToggleSelect = {
                                if (e.id in selected) selected.remove(e.id) else selected.add(e.id)
                            },
                            onEdit = { onEdit(e.id) },
                            onDelete = {
                                scope.launch {
                                    val snapshot = e.copy()
                                    vm.deleteById(e.id)
                                    val res = snackbarHostState.showSnackbar("Deleted", "Undo", true)
                                    if (res == SnackbarResult.ActionPerformed) vm.add(snapshot.copy(id = 0L))
                                }
                            },
                            onArchive = {
                                scope.launch {
                                    vm.archive(e.id)
                                    val res = snackbarHostState.showSnackbar("Archived", "Undo", true)
                                    if (res == SnackbarResult.ActionPerformed) vm.unarchive(e.id)
                                }
                            },
                            onOpenMap = { openMapForAddress(context, e.location) },
                            onNavigate = { startNavigation(context, e.location) },
                            isToday = isToday(e.startTime, zone),
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = spring<IntOffset>(
                                        stiffness = Spring.StiffnessMediumLow,
                                        visibilityThreshold = IntOffset.VisibilityThreshold
                                    )
                            )
                                .semantics(mergeDescendants = true) {}
                                .combinedClickable(
                                    onClick = {
                                        if (selectionMode) {
                                            if (e.id in selected) selected.remove(e.id) else selected.add(e.id)
                                        } else onEdit(e.id)
                                    },
                                    onLongClick = {
                                        if (e.id in selected) selected.remove(e.id) else selected.add(e.id)
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

/* ---------- Top bar with bulk actions + filters ---------- */

@Composable
private fun TopBar(
    selectionMode: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onBulkArchive: () -> Unit,
    onBulkDelete: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    oldestFirst: Boolean,
    onToggleSort: () -> Unit,
    start: LocalDate?,
    end: LocalDate?,
    onSetStart: (LocalDate?) -> Unit,
    onSetEnd: (LocalDate?) -> Unit,
    onClearDates: () -> Unit
) {
    Column {
        TopAppBar(
            title = { if (selectionMode) Text("$selectedCount selected") else Text("All Events") },
            actions = {
                if (selectionMode) {
                    IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, "Select all") }
                    IconButton(onClick = onBulkArchive) { Icon(Icons.Default.Archive, "Archive") }
                    IconButton(onClick = onBulkDelete) { Icon(Icons.Default.Delete, "Delete") }
                    TextButton(onClick = onClearSelection) { Text("Clear") }
                } else {
                    //IconButton(onClick = { /* filters placeholder */ }) { Icon(Icons.Default.FilterList, "Filters") }
                    TextButton(onClick = onToggleSort) { Text(if (oldestFirst) "Oldest First" else "Newest First") }
                }
            }
        )
        FilterBar(
            query = query,
            onQueryChange = onQueryChange,
            start = start,
            end = end,
            onSetStart = onSetStart,
            onSetEnd = onSetEnd,
            onClearDates = onClearDates
        )
    }
}

/* ---------- Dismiss background ---------- */

@Composable
private fun DismissBackgroundEvents(value: SwipeToDismissBoxValue) {
    val color = when (value) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer // archive
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer     // delete
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val label = when (value) {
        SwipeToDismissBoxValue.StartToEnd -> "Archive"
        SwipeToDismissBoxValue.EndToStart -> "Delete"
        else -> ""
    }
    Surface(color = color) {
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.CenterStart) {
            Text(label, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

/* ---------- Row ---------- */

@Composable
private fun EventRow(
    e: Event,
    selected: Boolean,
    selectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onOpenMap: () -> Unit,
    onNavigate: () -> Unit,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    e.title.ifBlank { "(no title)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isToday) {
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = {}, label = { Text("Today") })
                }
            }
            Spacer(Modifier.height(6.dp))
            val tf = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")
            val zone = ZoneId.systemDefault()
            val start = Instant.ofEpochMilli(e.startTime).atZone(zone)
            val end = e.endTime?.let { Instant.ofEpochMilli(it).atZone(zone) }
            Text(
                if (end != null) "${tf.format(start)} — ${tf.format(end)}" else tf.format(start),
                style = MaterialTheme.typography.bodySmall
            )
            // Address + nav icons row
            if (e.location.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        e.location,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2, // show up to 2 lines of address
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onOpenMap) { Icon(Icons.Default.Map, contentDescription = "Map") }
                    IconButton(onClick = onNavigate) { Icon(Icons.Default.Directions, contentDescription = "Navigate") }
                }
            }



            if (e.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    e.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Actions dropdown aligned to the right
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box {
                    FilledTonalButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Actions")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = { showMenu = false; onArchive() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

/* ---------- Shared filter UI ---------- */

@Composable
fun FilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    start: LocalDate?,
    end: LocalDate?,
    onSetStart: (LocalDate?) -> Unit,
    onSetEnd: (LocalDate?) -> Unit,
    onClearDates: () -> Unit
) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DateField("Start date", start, onSetStart, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DateField("End date", end, onSetEnd, Modifier.weight(1f))
            TextButton(onClick = onClearDates) { Text("Clear") }
        }
    }
}

@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onPick: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val cal = java.util.Calendar.getInstance()
    val year = date?.year ?: cal.get(java.util.Calendar.YEAR)
    val month = (date?.monthValue ?: (cal.get(java.util.Calendar.MONTH) + 1)) - 1
    val day = date?.dayOfMonth ?: cal.get(java.util.Calendar.DAY_OF_MONTH)

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = date?.toString() ?: "",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(onClick = {
            android.app.DatePickerDialog(ctx, { _, y, m, d ->
                onPick(LocalDate.of(y, m + 1, d))
            }, year, month, day).show()
        }) { Text("Pick") }
    }
}

/* ---------- Helpers ---------- */

private fun isToday(millis: Long, zone: ZoneId): Boolean {
    val d = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    return d == LocalDate.now(zone)
}
private fun openMapForAddress(context: android.content.Context, address: String) {
    if (address.isBlank()) return
    val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
    context.startActivity(intent)
}
private fun startNavigation(context: android.content.Context, address: String) {
    if (address.isBlank()) return
    val uri = Uri.parse("google.navigation:q=${Uri.encode(address)}&mode=d")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
    context.startActivity(intent)
}
