package com.yourname.prospect5w.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.data.Interaction
import com.yourname.prospect5w.domain.ProspectRepo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    repo: ProspectRepo,
    modifier: Modifier = Modifier,
    onOpen: (Long) -> Unit = {}
) {
    val source by repo.observeAll().collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    var onlyWithFollowUps by remember { mutableStateOf(false) }
    var sortDesc by remember { mutableStateOf(true) }

    val fmt = remember { SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault()) }

    val list = remember(source, query, onlyWithFollowUps, sortDesc) {
        source
            .asSequence()
            .filter { it.matchesQuery(query) }
            .filter { if (onlyWithFollowUps) it.nextFollowUpAt != null else true }
            .sortedBy { it.whenAt }
            .let { if (sortDesc) it.toList().asReversed() else it.toList() }
    }

    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize()) {
        // Search / filter row
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search (name, company, notes)") },
                modifier = Modifier.weight(1f)
            )
            FilterChipGroup(
                onlyWithFollowUps = onlyWithFollowUps,
                onToggleFollowUps = { onlyWithFollowUps = !onlyWithFollowUps },
                sortDesc = sortDesc,
                onToggleSort = { sortDesc = !sortDesc },
            )
        }

        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Text("No matching events.")
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(list, key = { it.id }) { i ->
                val dismissState = rememberDismissState(
                    confirmValueChange = { value ->
                        if (value == DismissValue.DismissedToStart || value == DismissValue.DismissedToEnd) {
                            scope.launch { repo.deleteById(i.id) }
                            true
                        } else false
                    }
                )
                SwipeToDismiss(
                    state = dismissState,
                    background = {},
                    directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
                    dismissContent = {
                        EventRow(i, fmt = fmt, onClick = { onOpen(i.id) })
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterChipGroup(
    onlyWithFollowUps: Boolean,
    onToggleFollowUps: () -> Unit,
    sortDesc: Boolean,
    onToggleSort: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = onToggleFollowUps,
            label = { Text(if (onlyWithFollowUps) "Follow-ups: On" else "Follow-ups: Off") }
        )
        AssistChip(
            onClick = onToggleSort,
            label = { Text(if (sortDesc) "Newest first" else "Oldest first") }
        )
    }
}

@Composable
private fun EventRow(i: Interaction, fmt: SimpleDateFormat, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = buildTitle(i),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "When: ${fmt.format(Date(i.whenAt))}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!i.whatNotes.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("Notes: ${i.whatNotes}", style = MaterialTheme.typography.bodyMedium)
            }
            if (!i.whereText.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("Where: ${i.whereText}", style = MaterialTheme.typography.bodyMedium)
            }
            if (!i.whySummary.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("Why: ${i.whySummary}", style = MaterialTheme.typography.bodyMedium)
            }
            if (i.nextFollowUpAt != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Next follow-up: ${fmt.format(Date(i.nextFollowUpAt))}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun Interaction.matchesQuery(q: String): Boolean {
    if (q.isBlank()) return true
    val needle = q.trim().lowercase()
    fun s(s: String?) = s?.lowercase()?.contains(needle) == true
    val name = listOfNotNull(firstName, lastName).joinToString(" ")
    return s(name) || s(companyName) || s(whatNotes) || s(whereText) || s(whySummary) || s(whatType)
}

private fun buildTitle(i: Interaction): String {
    val name = listOfNotNull(i.firstName, i.lastName).joinToString(" ").ifBlank { "Unknown" }
    val company = i.companyName?.takeIf { it.isNotBlank() }
    val what = i.whatType?.takeIf { it.isNotBlank() }
    return listOfNotNull(name, company, what).joinToString(" • ")
}