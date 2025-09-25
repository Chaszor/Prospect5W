@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.EventViewModel
import com.yourname.prospect5w.data.Event
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(vm: EventViewModel) {
    val context = LocalContext.current
    val allEvents by vm.events.collectAsState(initial = emptyList())
    var oldestFirst by rememberSaveable { mutableStateOf(true) }

    val zone = ZoneId.systemDefault()
    val startOfDay = remember { LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli() }
    val endOfDay = remember { startOfDay + 24L * 60 * 60 * 1000L }

    val todays = remember(allEvents, oldestFirst) {
        val base = allEvents
            .filter { e -> !e.archived }
            .filter { e -> e.startTime in startOfDay until endOfDay }
        if (oldestFirst) base.sortedBy { e -> e.startTime } else base.sortedByDescending { e -> e.startTime }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Today") },
            actions = {
                TextButton(onClick = { oldestFirst = !oldestFirst }) {
                    Text(if (oldestFirst) "Oldest First" else "Newest First")
                }
            }
        )
        if (todays.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing scheduled today.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todays, key = { e -> e.id }) { e ->
                    TodayRow(
                        e = e,
                        zone = zone,
                        onOpenMap = { if (e.location.isNotBlank()) openMapForAddress(context, e.location) },
                        onNavigate = { if (e.location.isNotBlank()) startNavigation(context, e.location) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayRow(
    e: Event,
    zone: ZoneId,
    onOpenMap: () -> Unit,
    onNavigate: () -> Unit
) {
    val tf = DateTimeFormatter.ofPattern("h:mm a")
    val start = Instant.ofEpochMilli(e.startTime).atZone(zone)
    val end = e.endTime?.let { Instant.ofEpochMilli(it).atZone(zone) }
    val line = if (end != null) "${tf.format(start)} — ${tf.format(end)}" else tf.format(start)

    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Text(e.title.ifBlank { "(no title)" }, style = MaterialTheme.typography.titleMedium)
            Text(line, style = MaterialTheme.typography.bodySmall)
            if (e.location.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = onOpenMap, label = { Text("Map") }, leadingIcon = { Icon(Icons.Default.Map, null) })
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = onNavigate, label = { Text("ETA / Navigate") }, leadingIcon = { Icon(Icons.Default.Directions, null) })
                }
            }
            if (e.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(e.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun openMapForAddress(context: android.content.Context, address: String) {
    val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
    context.startActivity(intent)
}
private fun startNavigation(context: android.content.Context, address: String) {
    val uri = Uri.parse("google.navigation:q=${Uri.encode(address)}&mode=d")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
    context.startActivity(intent)
}
