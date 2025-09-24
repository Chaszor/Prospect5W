@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.EventViewModel
import com.yourname.prospect5w.data.Event
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun QuickAddScreen(vm: EventViewModel, onSaved: () -> Unit) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var location by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }

    // Simple text-based date/time. Format: yyyy-MM-dd HH:mm
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val now = remember { LocalDateTime.now().withSecond(0).withNano(0) }
    var startStr by remember { mutableStateOf(TextFieldValue(now.format(fmt))) }
    var endStr by remember { mutableStateOf(TextFieldValue(now.plusHours(1).format(fmt))) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Add Event") })
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3
            )
            OutlinedTextField(value = startStr, onValueChange = { startStr = it }, label = { Text("Start (yyyy-MM-dd HH:mm)*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = endStr, onValueChange = { endStr = it }, label = { Text("End (yyyy-MM-dd HH:mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            val parse: (TextFieldValue) -> Long? = { v ->
                runCatching { LocalDateTime.parse(v.text.trim(), fmt).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
            }

            val canSave = title.text.isNotBlank() && parse(startStr) != null

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = {
                    val n = LocalDateTime.now().withSecond(0).withNano(0)
                    startStr = TextFieldValue(n.format(fmt))
                    endStr = TextFieldValue(n.plusHours(1).format(fmt))
                }) { Text("Now +1h") }

                Button(
                    enabled = canSave,
                    onClick = {
                        val start = parse(startStr)!!
                        val end = parse(endStr)
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
