@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
    // Pull everything; we'll hide archived here
    val events by vm.allEvents.collectAsState()

    var query by remember { mutableStateOf("") }
    var startFilter: LocalDate? by remember { mutableStateOf(null) }
    var endFilter: LocalDate? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    val zone = ZoneId.systemDefault()

    // Sort toggle (persist across recompositions & config changes)
    var oldestFirst by rememberSaveable { mutableStateOf(true) }

    fun withinRange(e: Event): Boolean {
        val d = Instant.ofEpochMilli(e.startTime).atZone(zone).toLocalDate()
        val afterStart = startFilter?.let { d >= it } ?: true
        val beforeEnd = endFilter?.let { d <= it } ?: true
        return afterStart && beforeEnd
    }

    // Hide archived first, then apply query/date filters
    val base = events
        .filter { !it.archived }
        .filter {
            val q = query.trim().lowercase()
            val textMatch = q.isBlank() || listOf(it.title, it.location, it.description)
                .any { s -> s.lowercase().contains(q) }
            textMatch && withinRange(it)
        }

    // Apply sort order
    val filtered = if (oldestFirst) {
        base.sortedBy { it.startTime }
    } else {
        base.sortedByDescending { it.startTime }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("All Events") },
            actions = {
                // Archive All
                TextButton(
                    onClick = {
                        scope.launch {
                            val idsToArchive = events.filter { !it.archived }.map { it.id }
                            if (idsToArchive.isEmpty()) {
                                snackbarHostState.showSnackbar("Nothing to archive")
                                return@launch
                            }
                            idsToArchive.forEach { vm.archive(it) }
                            val res = snackbarHostState.showSnackbar(
                                message = "Archived ${idsToArchive.size} event(s)",
                                actionLabel = "Undo",
                                withDismissAction = true,
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                idsToArchive.forEach { vm.unarchive(it) }
                            }
                        }
                    }
                ) { Text("Archive All") }
                // Sort toggle
                TextButton(onClick = { oldestFirst = !oldestFirst }) {
                    Text(if (oldestFirst) "Oldest First" else "Newest First")
                }
            }
        )

        FilterBar(
            query = query,
            onQueryChange = { query = it },
            start = startFilter,
            end = endFilter,
            onSetStart = { startFilter = it },
            onSetEnd = { endFilter = it },
            onClearDates = { startFilter = null; endFilter = null }
        )

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching events.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { e ->
                    EventRow(
                        e = e,
                        onDelete = { event: Event ->
                            vm.delete(event.id)
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
                                    message = "Event deleted",
                                    actionLabel = "Undo",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    vm.add(event.copy(id = 0L))
                                }
                            }
                        },
                        onEdit = { onEdit(e.id) },
                        onArchive = { event: Event ->
                            vm.archive(event.id)
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
                                    message = "Archived “${event.title.ifBlank { "Untitled" }}”",
                                    actionLabel = "Undo",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    vm.unarchive(event.id)
                                }
                            }
                        },
                        isToday = isToday(e.startTime, zone)
                    )
                }
            }
        }
    }
}

private fun isToday(millis: Long, zone: ZoneId): Boolean {
    val d = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    return d == LocalDate.now(zone)
}

/* ---------- Shared filter UI (public so other screens can use) ---------- */

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
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField(label = "Start date", date = start, onPick = onSetStart, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField(label = "End date", date = end, onPick = onSetEnd, modifier = Modifier.weight(1f))
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

/* ---------- Row ---------- */

@Composable
private fun EventRow(
    e: Event,
    onDelete: (Event) -> Unit,
    onEdit: () -> Unit,
    onArchive: (Event) -> Unit,
    isToday: Boolean
) {
    ElevatedCard {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onEdit() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        e.title.ifBlank { "(no title)" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isToday) AssistChip(onClick = {}, label = { Text("Today") })
                }
                if (e.location.isNotBlank()) {
                    Text(e.location, style = MaterialTheme.typography.labelMedium)
                }
                val tf = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")
                val zone = ZoneId.systemDefault()
                val start = Instant.ofEpochMilli(e.startTime).atZone(zone)
                val end = e.endTime?.let { Instant.ofEpochMilli(it).atZone(zone) }
                Text(
                    if (end != null) "${tf.format(start)} — ${tf.format(end)}" else tf.format(start),
                    style = MaterialTheme.typography.bodySmall
                )
                if (e.description.isNotBlank()) {
                    Text(
                        e.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            var open by remember { mutableStateOf(false) }
            Box {
                FilledTonalButton(onClick = { open = true }) { Text("Actions") }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { open = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { open = false; onDelete(e) }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        onClick = { open = false; onArchive(e) }
                    )
                }
            }
        }
    }
}
