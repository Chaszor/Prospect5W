@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
fun EventDetailScreen(
    vm: EventViewModel,
    eventId: Long,
    onBack: () -> Unit = {}
) {
    val events by vm.allEvents.collectAsState()
    val event = events.firstOrNull { it.id == eventId }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Event Details") })
        if (event == null) {
            EmptyState("Event not found.")
        } else {
            EventDetailBody(event, onDelete = {
                vm.delete(event.id)
                onBack()
            })
        }
    }
}

@Composable
private fun EventDetailBody(e: Event, onDelete: () -> Unit) {
    val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")
    val tz = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(e.startTime).atZone(tz)
    val end = e.endTime?.let { Instant.ofEpochMilli(it).atZone(tz) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(e.title, style = MaterialTheme.typography.titleLarge)
        if (e.location.isNotBlank()) Text(e.location, style = MaterialTheme.typography.labelLarge)

        Text(
            if (end != null) "${dateFmt.format(start)} — ${dateFmt.format(end)}"
            else dateFmt.format(start),
            style = MaterialTheme.typography.bodyMedium
        )

        if (e.description.isNotBlank()) {
            Text(e.description, style = MaterialTheme.typography.bodyMedium)
        }

        Button(onClick = onDelete) { Text("Delete") }
    }
}

@Composable
private fun EmptyState(msg: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(PaddingValues(24.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(msg, style = MaterialTheme.typography.bodyLarge)
    }
}
