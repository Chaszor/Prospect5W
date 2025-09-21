package com.yourname.prospect5w.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Simple Event entity for prospecting/appointments/etc.
 * Times are epoch millis (UTC). We'll compute "today" by local boundaries in DAO.
 */
@Entity(
    tableName = "events",
    indices = [Index("startTime")]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val location: String = "",
    val startTime: Long,            // epoch millis
    val endTime: Long? = null,      // epoch millis
    val createdAt: Long = System.currentTimeMillis()
)
