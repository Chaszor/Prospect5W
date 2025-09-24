@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
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
import com.yourname.prospect5w.export.eventsToCsv
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun ArchivesScreen(
    vm: EventViewModel,
    snackbarHostState: SnackbarHostState,
    onOpen: (Long) -> Unit = {}
) {
    val events by vm.events.collectAsState(initial = emptyList())
    val archived = remember(events) { events.filter { e -> e.archived } }

    var query by remember { mutableStateOf("") }
    var startFilter: LocalDate? by remember { mutableStateOf(null) }
    var endFilter: LocalDate? by remember { mutableStateOf(null) }
    var oldestFirst by rememberSaveable { mutableStateOf(true) } // <-- sort toggle

    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val zone = ZoneId.systemDefault()

    // Export ALL archived (ignores filters)
    val exportAllLauncher = rememberLauncherForActivityResult(CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val csv = eventsToCsv(archived)
            runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            }
        }
    }

    // Import into archives (CSV must match ExportCsv.kt header & date format)
    val importLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    val text = input.bufferedReader().readText()
                    val imported = csvToEvents(text).map { it.copy(archived = true, id = 0L) }
                    imported.forEach { ev -> vm.add(ev) }
                }
            }.onSuccess {
                scope.launch { snackbarHostState.showSnackbar("Imported archived events") }
            }.onFailure { err ->
                scope.launch { snackbarHostState.showSnackbar("Import failed: ${err.message}") }
            }
        }
    }

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
                DateField(
                    label = "Start date",
                    date = start,
                    onPick = onSetStart,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    label = "End date",
                    date = end,
                    onPick = onSetEnd,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClearDates) { Text("Clear") }
            }
        }
    }

    fun withinRange(e: Event): Boolean {
        val d = Instant.ofEpochMilli(e.startTime).atZone(zone).toLocalDate()
        val afterStart = startFilter?.let { d >= it } ?: true
        val beforeEnd = endFilter?.let { d <= it } ?: true
        return afterStart && beforeEnd
    }

    val filteredBase = archived.filter { e ->
        val q = query.trim().lowercase()
        val textMatch = q.isBlank() || listOf(e.title, e.location, e.description).any { s -> s.lowercase().contains(q) }
        textMatch && withinRange(e)
    }

    // Apply sort order
    val filtered = if (oldestFirst) {
        filteredBase.sortedBy { e -> e.startTime }
    } else {
        filteredBase.sortedByDescending { e -> e.startTime }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Archived") },
            actions = {
                TextButton(onClick = { importLauncher.launch(arrayOf("text/*", "application/*", "*/*")) }) {
                    Text("Import")
                }
                TextButton(onClick = { exportAllLauncher.launch("events-archived.csv") }) {
                    Text("Export All")
                }
                // sort toggle
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
                Text("No archived events.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { e -> e.id }) { e ->
                    ArchiveRow(
                        e = e,
                        onUnarchive = {
                            vm.unarchive(e.id)
                            scope.launch {
                                snackbarHostState.showSnackbar("Unarchived “${e.title.ifBlank { "Untitled" }}”")
                            }
                        },
                        onDelete = { ev ->
                            vm.deleteById(ev.id)
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
                                    message = "Archived event deleted",
                                    actionLabel = "Undo",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    // simple undo: re-add with new id
                                    vm.add(ev.copy(id = 0L))
                                }
                            }
                        },
                        onOpen = { onOpen(e.id) }
                    )
                }
            }
        }
    }
}

/* ---------------- CSV import helpers ----------------
   Matches ExportCsv.kt format:
   Header: id,title,description,location,start,end,archived
   Date format: "yyyy-MM-dd HH:mm"
*/
private fun csvToEvents(csv: String): List<Event> {
    val lines = csv.trim().lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()

    // Validate/skip header
    val header = lines.first().trim().lowercase()
    val expects = listOf("id","title","description","location","start","end","archived").joinToString(",")
    val dataLines = if (header.replace(" ", "") == expects) lines.drop(1) else lines

    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val zone = ZoneId.systemDefault()

    return buildList {
        for (line in dataLines) {
            val cols = parseCsvLine(line)
            if (cols.size < 7) continue

            val title        = cols[1]
            val description  = cols[2]
            val location     = cols[3]
            val startStr     = cols[4].trim()
            val endStr       = cols[5].trim()
            // cols[6] archived is ignored — we always set archived=true on import

            val startMillis = runCatching {
                LocalDateTime.parse(startStr, fmt).atZone(zone).toInstant().toEpochMilli()
            }.getOrNull() ?: continue

            val endMillis = runCatching {
                if (endStr.isBlank()) null
                else LocalDateTime.parse(endStr, fmt).atZone(zone).toInstant().toEpochMilli()
            }.getOrNull()

            add(
                Event(
                    id = 0L, // let DB assign
                    title = title,
                    description = description,
                    location = location,
                    startTime = startMillis,
                    endTime = endMillis,
                    archived = true
                )
            )
        }
    }
}

/** CSV line parser that handles quotes and commas inside fields. */
private fun parseCsvLine(line: String): List<String> {
    val out = ArrayList<String>(8)
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when (c) {
            '"' -> {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    // Escaped quote ("")
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            }
            ',' -> {
                if (inQuotes) sb.append(c) else {
                    out.add(sb.toString())
                    sb.setLength(0)
                }
            }
            else -> sb.append(c)
        }
        i++
    }
    out.add(sb.toString())
    return out
}

/* ---------------- Row UI ---------------- */

@Composable
private fun ArchiveRow(
    e: Event,
    onUnarchive: () -> Unit,
    onDelete: (Event) -> Unit,
    onOpen: () -> Unit
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
                    .clickable { onOpen() }
            ) {
                val tf = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")
                val zone = ZoneId.systemDefault()
                val start = Instant.ofEpochMilli(e.startTime).atZone(zone)
                val end = e.endTime?.let { Instant.ofEpochMilli(it).atZone(zone) }

                Text(
                    e.title.ifBlank { "(no title)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (e.location.isNotBlank()) {
                    Text(e.location, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    if (end != null) "${tf.format(start)} — ${tf.format(end)}" else tf.format(start),
                    style = MaterialTheme.typography.bodySmall
                )
                if (e.description.isNotBlank()) {
                    Text(e.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                AssistChip(onClick = {}, label = { Text("Archived") })
            }
            Spacer(Modifier.width(8.dp))
            var open by remember { mutableStateOf(false) }
            Box {
                FilledTonalButton(onClick = { open = true }) { Text("Actions") }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(text = { Text("Unarchive") }, onClick = { open = false; onUnarchive() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { open = false; onDelete(e) })
                }
            }
        }
    }
}
