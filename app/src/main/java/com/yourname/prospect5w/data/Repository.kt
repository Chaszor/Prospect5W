package com.yourname.prospect5w.data

import kotlinx.coroutines.flow.Flow

class Repository(private val dao: ProspectDao) {
    fun observeAll(): Flow<List<Event>> = dao.observeAllEvents()
    fun observeForDay(startOfDay: Long, endOfDay: Long): Flow<List<Event>> =
        dao.observeEventsForDay(startOfDay, endOfDay)

    suspend fun add(event: Event): Long = dao.insertEvent(event)
    suspend fun update(event: Event) = dao.updateEvent(event)
    suspend fun deleteById(id: Long) = dao.deleteEventById(id)
    suspend fun get(id: Long) = dao.getEventById(id)
}
