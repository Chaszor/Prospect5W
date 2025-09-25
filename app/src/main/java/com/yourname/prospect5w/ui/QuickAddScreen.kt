@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.EventViewModel
import com.yourname.prospect5w.data.Event
import java.time.*

@Composable
fun QuickAddScreen(vm: EventViewModel, onSaved: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val context = LocalContext.current

    var title by remember { mutableStateOf(TextFieldValue("")) }
    var location by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }

    var start by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var end by rememberSaveable { mutableStateOf<Long?>(System.currentTimeMillis() + 60L * 60 * 1000) }

    val startStr by derivedStateOf {
        Instant.ofEpochMilli(start).atZone(zone).toLocalDateTime().toString().replace('T', ' ')
    }
    val endStr by derivedStateOf {
        end?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime().toString().replace('T', ' ') } ?: ""
    }

    val formError = remember(start, end, title) {
        when {
            title.text.isBlank() -> "Title is required."
            end != null && end!! < start -> "End must be after start."
            else -> null
        }
    }

    fun pickDate(existingMillis: Long, onSelected: (Long) -> Unit) {
        val ldt = Instant.ofEpochMilli(existingMillis).atZone(zone).toLocalDateTime()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val newMillis = LocalDateTime.of(LocalDate.of(y, m + 1, d), ldt.toLocalTime())
                    .atZone(zone).toInstant().toEpochMilli()
                onSelected(newMillis)
            },
            ldt.year, ldt.monthValue - 1, ldt.dayOfMonth
        ).show()
    }

    fun pickTime(existingMillis: Long, onSelected: (Long) -> Unit) {
        val ldt = Instant.ofEpochMilli(existingMillis).atZone(zone).toLocalDateTime()
        TimePickerDialog(
            context,
            { _, h, min ->
                val newMillis = LocalDateTime.of(ldt.toLocalDate(), LocalTime.of(h, min))
                    .atZone(zone).toInstant().toEpochMilli()
                onSelected(newMillis)
            },
            ldt.hour, ldt.minute, false
        ).show()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Add Event") })

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title*") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

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
                FilledTonalButton(onClick = {
                    val now = LocalDateTime.now().withSecond(0).withNano(0)
                    start = now.atZone(zone).toInstant().toEpochMilli()
                    end = now.plusHours(1).atZone(zone).toInstant().toEpochMilli()
                }) { Text("Now +1h") }

                Button(
                    enabled = formError == null,
                    onClick = {
                        val e = Event(
                            id = 0L,
                            title = title.text.trim(),
                            description = description.text.trim(),
                            location = location.text.trim(),
                            startTime = start,
                            endTime = end,
                            archived = false
                        )
                        vm.add(e)
                        onSaved()
                    }
                ) { Text("Save") }
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
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(onClick = onPickDate) {
            Icon(Icons.Default.CalendarMonth, null)
            Spacer(Modifier.width(6.dp))
            Text("Date")
        }
        FilledTonalButton(onClick = onPickTime) {
            Icon(Icons.Default.AccessTime, null)
            Spacer(Modifier.width(6.dp))
            Text("Time")
        }
    }
}
