package com.yourname.prospect5w.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {
    @Query("SELECT * FROM interactions ORDER BY whenAt DESC")
    fun observeAll(): Flow<List<Interaction>>

    @Query("SELECT * FROM interactions WHERE id = :id")
    suspend fun getById(id: Long): Interaction?

    @Query("DELETE FROM interactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}