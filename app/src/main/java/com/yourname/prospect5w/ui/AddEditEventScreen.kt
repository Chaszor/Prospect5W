@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.EventViewModel
import com.yourname.prospect5w.data.Event
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun AddEditEventScreen(
    vm: EventViewModel,
    eventId: Long? = null,
    onSaved: () -> Unit = {},
    onBack: () -> Unit = {}          // NEW: back handler
) {
    val ctx = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var existing by remember { mutableStateOf<Event?>(null) }

    LaunchedEffect(eventId) {
        if (eventId != null) existing = vm.get(eventId)
        loading = false
    }

    var title by remember(existing) { mutableStateOf(TextFieldValue(existing?.title ?: "")) }
    var location by remember(existing) { mutableStateOf(TextFieldValue(existing?.location ?: "")) }
    var description by remember(existing) { mutableStateOf(TextFieldValue(existing?.description ?: "")) }

    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val zone = ZoneId.systemDefault()

    fun millisToStr(m: Long?) = m?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime().format(fmt) } ?: ""
    fun strToMillis(s: String): Long? = runCatching {
        LocalDateTime.parse(s.trim(), fmt).atZone(zone).toInstant().toEpochMilli()
    }.getOrNull()

    var startStr by remember(existing) { mutableStateOf(TextFieldValue(millisToStr(existing?.startTime ?: System.currentTimeMillis()))) }
    var endStr by remember(existing) { mutableStateOf(TextFieldValue(millisToStr(existing?.endTime))) }

    var error by remember { mutableStateOf<String?>(null) }

    fun showDateTimePick(initial: LocalDateTime?, onPicked: (LocalDateTime) -> Unit) {
        val cal = Calendar.getInstance().apply {
            val base = initial ?: LocalDateTime.now()
            set(Calendar.YEAR, base.year)
            set(Calendar.MONTH, base.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, base.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, base.hour)
            set(Calendar.MINUTE, base.minute)
        }
        DatePickerDialog(ctx, { _, y, m, d ->
            TimePickerDialog(ctx, { _, h, min ->
                onPicked(LocalDateTime.of(y, m + 1, d, h, min))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (eventId == null) "Add Event" else "Edit Event") },
            navigationIcon = {
                // Simple text “Back” so you don’t need the icons dependency
                TextButton(onClick = onBack) { Text("Back") }
            }
        )
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(startStr, { startStr = it }, label = { Text("Start* (yyyy-MM-dd HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                    FilledTonalButton(onClick = {
                        val base = strToMillis(startStr.text)?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
                        showDateTimePick(base) { dt -> startStr = TextFieldValue(dt.format(fmt)) }
                    }) { Text("Pick") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(endStr, { endStr = it }, label = { Text("End (yyyy-MM-dd HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                    FilledTonalButton(onClick = {
                        val base = strToMillis(endStr.text)?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
                        showDateTimePick(base) { dt -> endStr = TextFieldValue(dt.format(fmt)) }
                    }) { Text("Pick") }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = {
                        val now = LocalDateTime.now().withSecond(0).withNano(0)
                        startStr = TextFieldValue(now.format(fmt))
                        endStr = TextFieldValue(now.plusHours(1).format(fmt))
                    }) { Text("Now +1h") }

                    val canSave = title.text.isNotBlank() && strToMillis(startStr.text) != null
                    Button(
                        enabled = canSave,
                        onClick = {
                            val start = strToMillis(startStr.text)
                            val end = endStr.text.trim().takeIf { it.isNotEmpty() }?.let { strToMillis(it) }

                            error = when {
                                title.text.isBlank() -> "Title is required."
                                start == null -> "Start time format invalid."
                                end != null && end < start -> "End must be after start."
                                else -> null
                            }
                            if (error != null) return@Button

                            val event = Event(
                                id = existing?.id ?: 0L,
                                title = title.text.trim(),
                                description = description.text.trim(),
                                location = location.text.trim(),
                                startTime = start!!,
                                endTime = end
                            )
                            if (existing == null) vm.add(event) else vm.update(event)
                            onSaved()
                        }
                    ) { Text(if (existing == null) "Save" else "Update") }
                }
            }
        }
    }
}
