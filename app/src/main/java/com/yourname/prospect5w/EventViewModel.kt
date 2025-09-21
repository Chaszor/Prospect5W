package com.yourname.prospect5w

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.prospect5w.data.AppDatabase
import com.yourname.prospect5w.data.Event
import com.yourname.prospect5w.data.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class EventViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(AppDatabase.get(app).dao())

    // All events
    val allEvents: StateFlow<List<Event>> =
        repo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Today's events (based on device local timezone)
    private val todayDate = MutableStateFlow(LocalDate.now())
    private val dayBounds = todayDate.flatMapLatest { date ->
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        repo.observeForDay(start, end)
    }
    val todayEvents: StateFlow<List<Event>> =
        dayBounds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshToday() {
        todayDate.value = LocalDate.now()
    }

    fun addQuick(
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long?
    ) = viewModelScope.launch {
        repo.add(Event(title = title, description = description, location = location, startTime = startMillis, endTime = endMillis))
    }

    fun delete(id: Long) = viewModelScope.launch {
        repo.deleteById(id)
    }
}
