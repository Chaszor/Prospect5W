@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.yourname.prospect5w.EventViewModel
import com.yourname.prospect5w.data.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun AddEditEventScreen(
    vm: EventViewModel,
    eventId: Long? = null,
    onSaved: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val zone = ZoneId.systemDefault()
    val fmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.getDefault()) }

    var loading by remember { mutableStateOf(true) }
    var existing by remember { mutableStateOf<Event?>(null) }
    LaunchedEffect(eventId) {
        if (eventId != null) existing = vm.get(eventId)
        loading = false
    }

    var title by remember(existing) { mutableStateOf(TextFieldValue(existing?.title ?: "")) }
    var location by remember(existing) { mutableStateOf(TextFieldValue(existing?.location ?: "")) }
    var description by remember(existing) { mutableStateOf(TextFieldValue(existing?.description ?: "")) }

    var start by remember(existing) { mutableStateOf(existing?.startTime ?: System.currentTimeMillis()) }
    var end by remember(existing) { mutableStateOf(existing?.endTime) }

    val startStr by derivedStateOf { Instant.ofEpochMilli(start).atZone(zone).toLocalDateTime().format(fmt) }
    val endStr by derivedStateOf { end?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime().format(fmt) } ?: "" }

    val formError = remember(start, end, title) {
        when {
            title.text.isBlank() -> "Title is required."
            end != null && end!! < start -> "End must be after start."
            else -> null
        }
    }

    val fused: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(ctx) }
    val askLocationPerms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) scope.launch { fetchAddress(ctx, fused) { line -> location = TextFieldValue(line) } }
    }

    fun onUseCurrent() {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasPerm = fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
        if (hasPerm) scope.launch { fetchAddress(ctx, fused) { line -> location = TextFieldValue(line) } }
        else askLocationPerms.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    fun pickDate(existingMillis: Long, onSelected: (Long) -> Unit) {
        val ldt = Instant.ofEpochMilli(existingMillis).atZone(zone).toLocalDateTime()
        DatePickerDialog(ctx, { _, y, m, d ->
            val newDate = LocalDate.of(y, m + 1, d)
            val newMillis = LocalDateTime.of(newDate, ldt.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
            onSelected(newMillis)
        }, ldt.year, ldt.monthValue - 1, ldt.dayOfMonth).show()
    }

    fun pickTime(existingMillis: Long, onSelected: (Long) -> Unit) {
        val ldt = Instant.ofEpochMilli(existingMillis).atZone(zone).toLocalDateTime()
        TimePickerDialog(ctx, { _, h, min ->
            val newMillis = LocalDateTime.of(ldt.toLocalDate(), LocalTime.of(h, min)).atZone(zone).toInstant().toEpochMilli()
            onSelected(newMillis)
        }, ldt.hour, ldt.minute, false).show()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (eventId == null) "Add Event" else "Edit Event") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title*") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = location, onValueChange = { location = it },
                    label = { Text("Location") }, singleLine = true, modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                )
                FilledTonalButton(onClick = { onUseCurrent() }) { Text("Use current") }
            }

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            DateTimeRow(
                label = "Start*",
                value = startStr,
                onPickDate = { pickDate(start) { start = it } },
                onPickTime = { pickTime(start) { start = it } }
            )
            DateTimeRow(
                label = "End",
                value = endStr,
                onPickDate = { pickDate(end ?: start) { end = it } },
                onPickTime = { pickTime(end ?: start) { end = it } }
            )

            formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = formError == null,
                    onClick = {
                        val event = Event(
                            id = existing?.id ?: 0L,
                            title = title.text.trim(),
                            description = description.text.trim(),
                            location = location.text.trim(),
                            startTime = start,
                            endTime = end,
                            archived = existing?.archived ?: false
                        )
                        if (existing == null) vm.add(event) else vm.update(event)
                        onSaved(); onBack()
                    }
                ) { Text(if (existing == null) "Save" else "Update") }

                FilledTonalButton(onClick = {
                    val now = LocalDateTime.now().withSecond(0).withNano(0)
                    start = now.atZone(zone).toInstant().toEpochMilli()
                    end = now.plusMinutes(30).atZone(zone).toInstant().toEpochMilli()
                }) { Text("Now +30m") }
            }
        }
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    value: String,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true,
            label = { Text(label) }, singleLine = true, modifier = Modifier.weight(1f)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(onClick = onPickDate) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(6.dp)); Text("Date") }
        FilledTonalButton(onClick = onPickTime) { Icon(Icons.Default.AccessTime, null); Spacer(Modifier.width(6.dp)); Text("Time") }
    }
}

private suspend fun fetchAddress(
    ctx: android.content.Context,
    fused: FusedLocationProviderClient,
    onPick: (String) -> Unit
) {
    val last = runCatching { fused.lastLocation.await() }.getOrNull()
    val loc = last ?: runCatching {
        val cts = CancellationTokenSource()
        fused.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
    }.getOrNull() ?: return

    val line = withContext(Dispatchers.IO) {
        try {
            val g = Geocoder(ctx, java.util.Locale.getDefault())
            @Suppress("DEPRECATION")
            val res = g.getFromLocation(loc.latitude, loc.longitude, 1) ?: return@withContext null
            val addr = res.firstOrNull() ?: return@withContext null

            val street = buildString {
                if (!addr.subThoroughfare.isNullOrBlank()) append(addr.subThoroughfare + " ")
                if (!addr.thoroughfare.isNullOrBlank()) append(addr.thoroughfare)
            }
            val city = addr.locality ?: addr.subAdminArea
            val state = addr.adminArea
            val postal = addr.postalCode
            val tail = listOfNotNull(city, state, postal).joinToString(" ")

            when {
                street.isNotBlank() && tail.isNotBlank() -> "$street, $tail"
                street.isNotBlank() -> street
                !addr.getAddressLine(0).isNullOrBlank() -> addr.getAddressLine(0)
                else -> null
            }
        } catch (_: Exception) { null }
    }
    if (!line.isNullOrBlank()) onPick(line)
}
