@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui
// (rest unchanged)


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.EventViewModel
import com.yourname.prospect5w.data.Event
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(vm: EventViewModel) {
    val events by vm.todayEvents.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Today") })
        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing scheduled today.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { e ->
                    TodayRow(e)
                }
            }
        }
    }
}

@Composable
private fun TodayRow(e: Event) {
    val tf = DateTimeFormatter.ofPattern("h:mm a")
    val start = Instant.ofEpochMilli(e.startTime).atZone(ZoneId.systemDefault())
    val end = e.endTime?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
    val line = if (end != null) "${tf.format(start)} — ${tf.format(end)}" else tf.format(start)

    androidx.compose.material3.ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Text(e.title, style = MaterialTheme.typography.titleMedium)
            Text(line, style = MaterialTheme.typography.bodySmall)
            if (e.location.isNotBlank()) Text(e.location, style = MaterialTheme.typography.labelMedium)
            if (e.description.isNotBlank()) Text(e.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
