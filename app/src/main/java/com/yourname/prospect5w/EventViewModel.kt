package com.yourname.prospect5w

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.prospect5w.data.Event
import com.yourname.prospect5w.data.Repository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventViewModel(app: Application) : AndroidViewModel(app) {

    // Pull the Repository from Application (App exposes it)
    private val repo: Repository by lazy {
        (getApplication<Application>() as App).repo
    }

    // Persisted stream from Room
    val events: StateFlow<List<Event>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun add(event: Event) = viewModelScope.launch { repo.add(event) }
    fun update(event: Event) = viewModelScope.launch { repo.update(event) }
    fun deleteById(id: Long) = viewModelScope.launch { repo.deleteById(id) }
    suspend fun get(id: Long) = repo.get(id)

    fun archive(id: Long) = viewModelScope.launch {
        repo.get(id)?.let { repo.update(it.copy(archived = true)) }
    }

    fun unarchive(id: Long) = viewModelScope.launch {
        repo.get(id)?.let { repo.update(it.copy(archived = false)) }
    }

    // Optional helper if QuickAdd used to exist
    fun addQuick(title: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val e = Event(
            title = title,
            startTime = now,
            archived = false
            // add any other required defaults here
        )
        repo.add(e)
    }
}
