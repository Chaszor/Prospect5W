@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.yourname.prospect5w.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyItemScope.animateItem
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.EventViewModel
import com.yourname.prospect5w.data.Event
import com.yourname.prospect5w.export.eventsToCsv
import com.yourname.prospect5w.export.readCsvFromUri
import com.yourname.prospect5w.export.writeCsvToUri
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ArchivesScreen(
    vm: EventViewModel,
    snackbarHostState: SnackbarHostState,
    onOpen: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val scope = rememberCoroutineScope()

    val events by vm.events.collectAsState(initial = emptyList())
    val archived = events.filter { it.archived }

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

    val filteredBase = archived.filter { e ->
        val q = query.trim().lowercase()
        (q.isBlank() || listOf(e.title, e.location, e.description).any { s -> s.lowercase().contains(q) }) &&
                withinRange(e)
    }
    val items = if (oldestFirst) filteredBase.sortedBy { it.startTime } else filteredBase.sortedByDescending { it.startTime }

    // --- Import / Export launchers (SAF) ---
    var pendingExport by remember { mutableStateOf<List<Event>?>(null) }
    val exportFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val data = pendingExport
        pendingExport = null
        if (uri != null && data != null) {
            val count = writeCsvToUri(context, uri, data)
            scope.launch { snackbarHostState.showSnackbar("Exported $count item(s)") }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val imported = readCsvFromUri(context, uri)
            scope.launch {
                imported.forEach { vm.add(it.copy(id = 0L, archived = true)) }
                snackbarHostState.showSnackbar("Imported ${imported.size} item(s)")
            }
        }
    }

    var ioMenu by remember { mutableStateOf(false) } // Import/Export dropdown menu state

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { if (selectionMode) Text("${selected.size} selected") else Text("Archived") },
            actions = {
                if (selectionMode) {
                    IconButton(onClick = { selected.clear(); selected.addAll(items.map { it.id }) }) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                    }
                    IconButton(onClick = {
                        val ids = selected.toList()
                        selected.clear()
                        scope.launch {
                            ids.forEach { vm.unarchive(it) }
                            snackbarHostState.showSnackbar("Unarchived ${ids.size} item(s)")
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Unarchive") }

                    // Share (selected only)
                    IconButton(onClick = {
                        val toShare = items.filter { it.id in selected }
                        if (toShare.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar("No items selected") }
                        } else {
                            shareEventsCsv(context, toShare)
                        }
                    }) { Icon(Icons.Default.Share, contentDescription = "Share selected") }

                    // Import/Export menu (selected for export)
                    Box {
                        IconButton(onClick = { ioMenu = true }) {
                            Icon(Icons.Default.ImportExport, contentDescription = "Import / Export")
                        }
                        DropdownMenu(expanded = ioMenu, onDismissRequest = { ioMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Export to file") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, null) },
                                onClick = {
                                    ioMenu = false
                                    val toExport = items.filter { it.id in selected }
                                    if (toExport.isEmpty()) {
                                        scope.launch { snackbarHostState.showSnackbar("No items selected") }
                                    } else {
                                        pendingExport = toExport
                                        exportFileLauncher.launch(defaultExportFilename())
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import from file") },
                                leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                                onClick = {
                                    ioMenu = false
                                    importFileLauncher.launch(arrayOf("text/*", "text/csv", "application/csv"))
                                }
                            )
                        }
                    }

                    IconButton(onClick = {
                        val toDelete = items.filter { it.id in selected }.map { it.copy() }
                        val ids = toDelete.map { it.id }
                        selected.clear()
                        scope.launch {
                            ids.forEach { vm.deleteById(it) }
                            val res = snackbarHostState.showSnackbar("Deleted ${ids.size} item(s)", "Undo", true)
                            if (res == SnackbarResult.ActionPerformed) toDelete.forEach { vm.add(it.copy(id = 0L)) }
                        }
                    }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    TextButton(onClick = { selected.clear() }) { Text("Clear") }
                } else {
                    // Share (all filtered)
                    IconButton(onClick = {
                        if (items.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar("No archived items to share") }
                        } else {
                            shareEventsCsv(context, items)
                        }
                    }) { Icon(Icons.Default.Share, contentDescription = "Share") }

                    // Import/Export menu (filtered for export)
                    Box {
                        IconButton(onClick = { ioMenu = true }) {
                            Icon(Icons.Default.ImportExport, contentDescription = "Import / Export")
                        }
                        DropdownMenu(expanded = ioMenu, onDismissRequest = { ioMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Export to file") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, null) },
                                onClick = {
                                    ioMenu = false
                                    if (items.isEmpty()) {
                                        scope.launch { snackbarHostState.showSnackbar("No archived items to export") }
                                    } else {
                                        pendingExport = items
                                        exportFileLauncher.launch(defaultExportFilename())
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import from file") },
                                leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                                onClick = {
                                    ioMenu = false
                                    importFileLauncher.launch(arrayOf("text/*", "text/csv", "application/csv"))
                                }
                            )
                        }
                    }

                    TextButton(onClick = { oldestFirst = !oldestFirst }) {
                        Text(if (oldestFirst) "Oldest First" else "Newest First")
                    }
                }
            }
        )

        // Shared filter bar (from EventsScreen)
        FilterBar(
            query = query,
            onQueryChange = { query = it },
            start = startFilter,
            end = endFilter,
            onSetStart = { startFilter = it },
            onSetEnd = { endFilter = it },
            onClearDates = { startFilter = null; endFilter = null }
        )

        if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No archived events.")
            }
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
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    scope.launch {
                                        vm.unarchive(e.id)
                                        snackbarHostState.showSnackbar("Unarchived")
                                    }
                                    true
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
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
                        backgroundContent = { DismissBackgroundArchives(dismissState.currentValue) }
                    ) {
                        ArchiveRow(
                            e = e,
                            selected = e.id in selected,
                            selectionMode = selectionMode,
                            onToggleSelect = {
                                if (e.id in selected) selected.remove(e.id) else selected.add(e.id)
                            },
                            onUnarchive = {
                                scope.launch {
                                    vm.unarchive(e.id)
                                    snackbarHostState.showSnackbar("Unarchived")
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    val snapshot = e.copy()
                                    vm.deleteById(e.id)
                                    val res = snackbarHostState.showSnackbar("Deleted", "Undo", true)
                                    if (res == SnackbarResult.ActionPerformed) vm.add(snapshot.copy(id = 0L))
                                }
                            },
                            onOpen = { onOpen(e.id) },
                            onOpenMap = { openMapForAddress(context, e.location) },
                            onNavigate = { startNavigation(context, e.location) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = spring<IntOffset>(
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                )
                            ).combinedClickable(
                                onClick = {
                                    if (selectionMode) {
                                        if (e.id in selected) selected.remove(e.id) else selected.add(e.id)
                                    } else onOpen(e.id)
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

@Composable
private fun ArchiveRow(
    e: Event,
    selected: Boolean,
    selectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onOpenMap: () -> Unit,
    onNavigate: () -> Unit,
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
                AssistChip(onClick = onOpen, label = { Text("Open") })
            }

            if (e.location.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        e.location,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onOpenMap) { Icon(Icons.Default.Map, contentDescription = "Map") }
                    IconButton(onClick = onNavigate) { Icon(Icons.Default.Directions, contentDescription = "Navigate") }
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

            if (e.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    e.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box {
                    FilledTonalButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Actions")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Open") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                            onClick = { showMenu = false; onOpen() }
                        )
                        DropdownMenuItem(
                            text = { Text("Unarchive") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null) },
                            onClick = { showMenu = false; onUnarchive() }
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

/* ---------- Dismiss background ---------- */

@Composable
private fun DismissBackgroundArchives(value: SwipeToDismissBoxValue) {
    val color = when (value) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val label = when (value) {
        SwipeToDismissBoxValue.StartToEnd -> "Unarchive"
        SwipeToDismissBoxValue.EndToStart -> "Delete"
        else -> ""
    }
    Surface(color = color) {
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.CenterStart) {
            Text(label, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

/* ---------- Share / Import / Export helpers ---------- */

private fun defaultExportFilename(): String {
    val ts = java.time.LocalDateTime.now().toString().replace(':', '-')
    return "archived_events_$ts.csv"
}

private fun shareEventsCsv(context: android.content.Context, events: List<Event>) {
    val csv = eventsToCsv(events)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Archived events (${events.size})")
        putExtra(Intent.EXTRA_TEXT, csv)
    }
    context.startActivity(Intent.createChooser(share, "Share events"))
}

/* ---------- Shared helpers ---------- */

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
