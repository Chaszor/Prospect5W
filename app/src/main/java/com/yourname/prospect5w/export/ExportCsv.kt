package com.yourname.prospect5w.export

import com.yourname.prospect5w.data.Event
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

private fun csvEscape(s: String): String {
    if (s.isEmpty()) return ""
    val needQuotes = s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    val body = s.replace("\"", "\"\"")
    return if (needQuotes) "\"$body\"" else body
}

/** Convert events to a simple CSV (header included). */
fun eventsToCsv(events: List<Event>): String {
    val header = listOf(
        "id","title","description","location","start","end","archived"
    ).joinToString(",")
    val rows = events.map { e ->
        val start = FMT.format(Instant.ofEpochMilli(e.startTime))
        val end = e.endTime?.let { FMT.format(Instant.ofEpochMilli(it)) } ?: ""
        listOf(
            e.id.toString(),
            csvEscape(e.title),
            csvEscape(e.description),
            csvEscape(e.location),
            csvEscape(start),
            csvEscape(end),
            e.archived.toString()
        ).joinToString(",")
    }
    return (sequenceOf(header) + rows.asSequence()).joinToString("\n")
}
