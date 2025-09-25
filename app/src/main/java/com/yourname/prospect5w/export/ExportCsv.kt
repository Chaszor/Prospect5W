package com.yourname.prospect5w.export

import android.content.Context
import android.net.Uri
import com.yourname.prospect5w.data.Event
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CSV import/export for Event.
 *
 * Export columns (in order): id,title,description,location,start,end
 *   - Date format used: "yyyy-MM-dd HH:mm"
 *
 * Import accepts:
 *   - the new 6-column export format above (no "archived" column)
 *   - the legacy 7-column format with trailing "archived"
 *   - ISO_LOCAL_DATE_TIME (e.g., 2025-09-24T20:41:00)
 *   - epoch millis as a number
 *
 * Note: For imports that omit "archived", the field defaults to false.
 *       Screens like Archives can still force archived=true after import if desired.
 */
private const val DATE_PATTERN: String = "yyyy-MM-dd HH:mm"
private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)

// Current export header (no "archived")
private val CURRENT_HEADER = listOf("id","title","description","location","start","end")
// Legacy header kept for backward-compatible import
private val LEGACY_HEADER = listOf("id","title","description","location","start","end","archived")

/** Export a list of events to CSV text (no "archived" column). */
fun eventsToCsv(
    events: List<Event>,
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val header = CURRENT_HEADER.joinToString(",")
    val rows = events.asSequence().map { e ->
        val start = Instant.ofEpochMilli(e.startTime).atZone(zone).toLocalDateTime().format(DATE_FMT)
        val end = e.endTime?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime().format(DATE_FMT) }.orEmpty()
        listOf(
            e.id.toString(),
            e.title.toCsvField(),
            e.description.toCsvField(),
            e.location.toCsvField(),
            start.toCsvField(),
            end.toCsvField()
        ).joinToString(",")
    }
    return buildString {
        appendLine(header)
        rows.forEach { appendLine(it) }
    }
}

/** Import events from CSV text (accepts current 6-col or legacy 7-col with "archived"). */
fun csvToEvents(
    csv: String,
    zone: ZoneId = ZoneId.systemDefault()
): List<Event> {
    if (csv.isBlank()) return emptyList()
    val lines = csv.lineSequence().toList()
    if (lines.isEmpty()) return emptyList()

    val out = mutableListOf<Event>()
    val firstRow = parseCsvLine(lines.first())
    val headerVariant = detectHeaderVariant(firstRow)
    val startIndex = if (headerVariant != HeaderVariant.NONE) 1 else 0
    val hasArchived = (headerVariant == HeaderVariant.LEGACY)

    for (i in startIndex until lines.size) {
        val line = lines[i]
        if (line.isBlank()) continue
        val cols = parseCsvLine(line)
        if (cols.size < CURRENT_HEADER.size) continue

        val id = cols[0].toLongOrNull() ?: 0L
        val title = cols[1]
        val description = cols[2]
        val location = cols[3]
        val startMillis = parseDateOrMillis(cols[4], zone) ?: continue
        val endMillis = cols.getOrNull(5)?.takeIf { it.isNotBlank() }?.let { parseDateOrMillis(it, zone) }
        val archived = if (hasArchived) {
            cols.getOrNull(6)?.equals("true", ignoreCase = true) == true
        } else {
            false
        }

        out.add(
            Event(
                id = id,
                title = title,
                description = description,
                location = location,
                startTime = startMillis,
                endTime = endMillis,
                archived = archived
            )
        )
    }
    return out
}

/** Convenience: write CSV to a URI (Storage Access Framework). Returns number of events written. */
fun writeCsvToUri(
    context: Context,
    uri: Uri,
    events: List<Event>,
    zone: ZoneId = ZoneId.systemDefault()
): Int {
    context.contentResolver.openOutputStream(uri)?.use { out ->
        val csv = eventsToCsv(events, zone)
        out.write(csv.toByteArray(StandardCharsets.UTF_8))
    }
    return events.size
}

/** Convenience: read CSV from a URI (Storage Access Framework). */
fun readCsvFromUri(
    context: Context,
    uri: Uri,
    zone: ZoneId = ZoneId.systemDefault()
): List<Event> {
    context.contentResolver.openInputStream(uri)?.use { input ->
        BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { br ->
            val text = br.readText()
            return csvToEvents(text, zone)
        }
    }
    return emptyList()
}

/* -------------------- internals -------------------- */

private enum class HeaderVariant { NONE, CURRENT, LEGACY }

private fun detectHeaderVariant(firstRow: List<String>): HeaderVariant {
    val lc = firstRow.map { it.trim().lowercase() }
    return when {
        lc.size >= LEGACY_HEADER.size && lc.subList(0, LEGACY_HEADER.size) == LEGACY_HEADER -> HeaderVariant.LEGACY
        lc.size >= CURRENT_HEADER.size && lc.subList(0, CURRENT_HEADER.size) == CURRENT_HEADER -> HeaderVariant.CURRENT
        else -> HeaderVariant.NONE
    }
}

private fun String.toCsvField(): String {
    val needsQuotes = contains('"') || contains(',') || contains('\n') || contains('\r')
    val escaped = replace("\"", "\"\"")
    return if (needsQuotes) "\"$escaped\"" else escaped
}

/** Lightweight CSV parser supporting quotes and double-quoted escapes. */
private fun parseCsvLine(line: String): List<String> {
    val out = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' -> {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"'); i++ // escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            }
            c == ',' && !inQuotes -> {
                out.add(sb.toString()); sb.clear()
            }
            else -> sb.append(c)
        }
        i++
    }
    out.add(sb.toString())
    return out
}

private fun parseDateOrMillis(s: String, zone: ZoneId): Long? {
    val t = s.trim()
    if (t.isEmpty()) return null

    // epoch millis
    t.toLongOrNull()?.let { return it }

    // yyyy-MM-dd HH:mm
    try {
        return LocalDateTime.parse(t, DATE_FMT).atZone(zone).toInstant().toEpochMilli()
    } catch (_: Exception) {}

    // ISO_LOCAL_DATE_TIME (e.g., 2025-09-24T20:41:00)
    try {
        return LocalDateTime.parse(t).atZone(zone).toInstant().toEpochMilli()
    } catch (_: Exception) {}

    // ISO_INSTANT (e.g., 2025-09-24T20:41:00Z)
    return try {
        Instant.parse(t).toEpochMilli()
    } catch (_: Exception) {
        null
    }
}
