@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
// (rest of the file unchanged)
package com.yourname.prospect5w.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.EventViewModel
import com.yourname.prospect5w.data.Event
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EventsScreen(vm: EventViewModel) {
    val events by vm.allEvents.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("All Events") })
        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No events yet. Add one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { e ->
                    EventRow(e, onDelete = { vm.delete(e.id) })
                }
            }
        }
    }
}

@Composable
private fun EventRow(e: Event, onDelete: () -> Unit) {
    ElevatedCard {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (e.location.isNotBlank()) {
                    Text(e.location, style = MaterialTheme.typography.labelMedium)
                }
                val tf = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")
                val start = Instant.ofEpochMilli(e.startTime).atZone(ZoneId.systemDefault())
                val end = e.endTime?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
                Text(
                    if (end != null) "${tf.format(start)} — ${tf.format(end)}" else tf.format(start),
                    style = MaterialTheme.typography.bodySmall
                )
                if (e.description.isNotBlank()) {
                    Text(e.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
