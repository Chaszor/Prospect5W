package com.yourname.prospect5w.domain

import com.yourname.prospect5w.data.Interaction
import com.yourname.prospect5w.data.ProspectDao
import kotlinx.coroutines.flow.Flow

class ProspectRepo(private val dao: ProspectDao) {
    // expose streams & helpers used by UI
    fun observeAll(): Flow<List<Interaction>> = dao.observeAll()
    suspend fun getById(id: Long): Interaction? = dao.getById(id)
    suspend fun deleteById(id: Long) = dao.deleteById(id)

    // keep your existing save/insert APIs here, delegating to dao
}
