package com.yourname.prospect5w.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProspectDao {

    // --- Inserts / Updates / Deletes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    // --- Queries ---
    @Query("SELECT * FROM events ORDER BY startTime ASC")
    fun observeAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): Event?

    // "Today" by epoch range boundaries (inclusive start, exclusive end)
    @Query("""
        SELECT * FROM events
        WHERE startTime >= :startOfDay AND startTime < :endOfDay
        ORDER BY startTime ASC
    """)
    fun observeEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<Event>>
}
