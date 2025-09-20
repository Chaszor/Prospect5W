package com.yourname.prospect5w.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.data.Interaction
import com.yourname.prospect5w.domain.ProspectRepo
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TodayScreen(repo: ProspectRepo) {
    val now = System.currentTimeMillis()
    var due by remember { mutableStateOf<List<Interaction>>(emptyList()) }

    LaunchedEffect(Unit) {
        repo.dueFollowUps(now).collectLatest { due = it }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Due Follow-ups", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (due.isEmpty()) {
            Text("No follow-ups due. Good job.")
        } else {
            LazyColumn {
                items(due) { i ->
                    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(i.whySummary, style = MaterialTheme.typography.titleMedium)
                            Text("What: ${'$'}{i.whatType} — ${'$'}{i.whatNotes}")
                            i.whereText?.let { Text("Where: ${'$'}it") }
                            i.followUpNote?.let { Text("Next: ${'$'}it") }
                        }
                    }
                }
            }
        }
    }
}
