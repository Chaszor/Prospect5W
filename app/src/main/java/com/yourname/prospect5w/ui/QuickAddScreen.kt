@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yourname.prospect5w.ui
// (rest unchanged)


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.EventViewModel
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
            OutlinedTextField(title, { title = it }, label = { Text("Title*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(location, { location = it }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3
            )
            OutlinedTextField(startStr, { startStr = it }, label = { Text("Start (yyyy-MM-dd HH:mm)*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(endStr, { endStr = it }, label = { Text("End (yyyy-MM-dd HH:mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            val parse: (TextFieldValue) -> Long? = { v ->
                runCatching { LocalDateTime.parse(v.text.trim(), fmt).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
            }

            val canSave = title.text.isNotBlank() && parse(startStr) != null

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = {
                    // Quick set to now/+1h
                    startStr = TextFieldValue(LocalDateTime.now().withSecond(0).withNano(0).format(fmt))
                    endStr = TextFieldValue(LocalDateTime.now().withSecond(0).withNano(0).plusHours(1).format(fmt))
                }) { Text("Now +1h") }

                Button(
                    enabled = canSave,
                    onClick = {
                        val start = parse(startStr)!!
                        val end = parse(endStr)
                        vm.addQuick(
                            title = title.text.trim(),
                            description = description.text.trim(),
                            location = location.text.trim(),
                            startMillis = start,
                            endMillis = end
                        )
                        onSaved()
                    }
                ) { Text("Save") }
            }
        }
    }
}
