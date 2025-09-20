package com.yourname.prospect5w.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.domain.ProspectRepo
import com.yourname.prospect5w.notify.ReminderScheduler

@Composable
fun QuickAddScreen(repo: ProspectRepo, scheduler: ReminderScheduler) {
    var s by remember { mutableStateOf(QuickAddState()) }
    val vm = remember { QuickAddVm(repo, scheduler) }
    val save = {
        vm.save(s) {
            s = QuickAddState()
        }
    }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Quick Add", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = s.firstName, onValueChange = { s = s.copy(firstName = it) },
                label = { Text("First name") }, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = s.lastName, onValueChange = { s = s.copy(lastName = it) },
                label = { Text("Last name") }, modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = s.companyName, onValueChange = { s = s.copy(companyName = it) },
            label = { Text("Company") }, modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = s.whatNotes, onValueChange = { s = s.copy(whatNotes = it) },
            label = { Text("What (notes)") }, modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = s.whereText ?: "", onValueChange = { s = s.copy(whereText = it) },
            label = { Text("Where (address or note)") }, modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = s.why, onValueChange = { s = s.copy(why = it) },
            label = { Text("Why (need / angle)") }, modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = s.followUpNote ?: "", onValueChange = { s = s.copy(followUpNote = it) },
            label = { Text("Next step note (optional)") }, modifier = Modifier.fillMaxWidth()
        )
        Row {
            Button(onClick = { s = s.copy(nextFollowUpAt = System.currentTimeMillis() + 24*60*60*1000) }) {
                Text("Follow-up: +1 day")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { s = s.copy(nextFollowUpAt = null) }) { Text("Clear follow-up") }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = save, modifier = Modifier.fillMaxWidth()) { Text("Save") }
    }
}
