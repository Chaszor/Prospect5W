@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    val events by vm.events.collectAsState(initial = emptyList())
    val e = events.firstOrNull { it.id == eventId }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Event Details") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )
        if (e == null) {
            EmptyState("Event not found.")
        } else {
            EventDetailBody(
                e = e,
                onDelete = { vm.deleteById(e.id); onBack() }
            )
        }
    }
}

@Composable
private fun EventDetailBody(e: Event, onDelete: () -> Unit) {
    val context = LocalContext.current
    val clip = LocalClipboardManager.current

    val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a")
    val tz = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(e.startTime).atZone(tz)
    val end = e.endTime?.let { Instant.ofEpochMilli(it).atZone(tz) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(e.title.ifBlank { "(no title)" }, style = MaterialTheme.typography.titleLarge)
        Text(
            if (end != null) "${dateFmt.format(start)} — ${dateFmt.format(end)}" else dateFmt.format(start),
            style = MaterialTheme.typography.bodyMedium
        )
        if (e.location.isNotBlank()) {
            Text(e.location, style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { openMapForAddress(context, e.location) }) {
                    Icon(Icons.Default.Map, null); Spacer(Modifier.width(6.dp)); Text("Open in Maps")
                }
                FilledTonalButton(onClick = { startNavigation(context, e.location) }) {
                    Icon(Icons.Default.Directions, null); Spacer(Modifier.width(6.dp)); Text("ETA / Navigate")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    clip.setText(AnnotatedString(e.location))
                }) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy address") }
                OutlinedButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${e.title}\n${e.location}")
                    }
                    context.startActivity(Intent.createChooser(share, "Share"))
                }) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Share") }
            }
        }
        if (e.description.isNotBlank()) {
            Text(e.description, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text("Delete")
        }
    }
}

@Composable
private fun EmptyState(msg: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) { Text(msg, style = MaterialTheme.typography.bodyLarge) }
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
