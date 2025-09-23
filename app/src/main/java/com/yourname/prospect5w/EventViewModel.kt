package com.yourname.prospect5w

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.prospect5w.data.Event
import com.yourname.prospect5w.export.eventsToCsv
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.time.LocalDate
import java.time.ZoneId

class EventViewModel(app: Application) : AndroidViewModel(app) {

    // In-memory source of truth (swap for your own persistence anytime)
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    // Keep "allEvents" for compatibility with your UI
    val allEvents: StateFlow<List<Event>> get() = events

    private val todayDate = MutableStateFlow(LocalDate.now())
    val todayEvents: StateFlow<List<Event>> =
        combine(_events, todayDate) { list, date ->
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            list.filter { it.startTime in start until end }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshToday() { todayDate.value = LocalDate.now() }

    // CRUD
    fun add(event: Event) = viewModelScope.launch {
        val nextId = (events.value.maxOfOrNull { it.id } ?: 0L) + 1
        _events.value = events.value + event.copy(id = nextId)
    }

    fun update(event: Event) = viewModelScope.launch {
        _events.value = events.value.map { if (it.id == event.id) event else it }
    }

    fun delete(id: Long) = viewModelScope.launch {
        _events.value = events.value.filterNot { it.id == id }
    }

    fun addQuick(
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long?
    ) = viewModelScope.launch {
        val nextId = (events.value.maxOfOrNull { it.id } ?: 0L) + 1
        _events.value = events.value + Event(
            id = nextId,
            title = title,
            description = description,
            location = location,
            startTime = startMillis,
            endTime = endMillis
        )
    }

    suspend fun get(id: Long): Event? = events.value.firstOrNull { it.id == id }

    fun archive(id: Long) = viewModelScope.launch {
        _events.value = events.value.map { if (it.id == id) it.copy(archived = true) else it }
    }

    fun unarchive(id: Long) = viewModelScope.launch {
        _events.value = events.value.map { if (it.id == id) it.copy(archived = false) else it }
    }

    // Optional: Export helpers (not required by your screen, but handy if you call from VM)
    fun exportAllToDownloads(context: Context, fileName: String = "events.csv") = viewModelScope.launch {
        exportToDownloads(context, fileName, events.value)
    }

    private fun exportToDownloads(context: Context, fileName: String, list: List<Event>): Boolean {
        if (list.isEmpty()) return false
        val csv = eventsToCsv(list)
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
            }
        }
        return try {
            val uri = resolver.insert(collection, values) ?: return false
            resolver.openOutputStream(uri).use { out: OutputStream? ->
                out?.write(csv.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (_: Exception) { false }
    }

    fun shareAll(context: Context) {
        val csv = eventsToCsv(events.value)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Events export")
            putExtra(Intent.EXTRA_TEXT, csv)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share CSV"))
    }
}
