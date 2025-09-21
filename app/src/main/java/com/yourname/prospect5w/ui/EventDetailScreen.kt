package com.yourname.prospect5w.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.prospect5w.data.Interaction
import com.yourname.prospect5w.domain.ProspectRepo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventDetailScreen(
    id: Long,
    repo: ProspectRepo,
    onDeleted: () -> Unit = {}
) {
    var item by remember { mutableStateOf<Interaction?>(null) }
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault()) }

    LaunchedEffect(id) {
        item = repo.getById(id)
    }

    val i = item
    if (i == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("Loading…") }
        return
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Event detail") }) }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(buildTitle(i), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("When: ${fmt.format(Date(i.whenAt))}")
            if (!i.whatNotes.isNullOrBlank()) Text("Notes: ${i.whatNotes}")
            if (!i.whereText.isNullOrBlank()) Text("Where: ${i.whereText}")
            if (!i.whySummary.isNullOrBlank()) Text("Why: ${i.whySummary}")
            if (i.nextFollowUpAt != null) Text("Next follow-up: ${fmt.format(Date(i.nextFollowUpAt))}")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        scope.launch {
                            repo.deleteById(i.id)
                            onDeleted()
                        }
                    }
                ) { Text("Delete") }
            }
        }
    }
}

private fun buildTitle(i: Interaction): String {
    val name = listOfNotNull(i.firstName, i.lastName).joinToString(" ").ifBlank { "Unknown" }
    val company = i.companyName?.takeIf { it.isNotBlank() }
    val what = i.whatType?.takeIf { it.isNotBlank() }
    return listOfNotNull(name, company, what).joinToString(" • ")
}