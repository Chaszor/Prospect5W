package com.yourname.prospect5w.domain

import com.yourname.prospect5w.data.*
import kotlinx.coroutines.flow.Flow

class ProspectRepo(private val dao: ProspectDao) {
    suspend fun addCompanyIfNeeded(name: String?): Long? {
        if (name.isNullOrBlank()) return null
        return dao.insertCompany(Company(name = name))
    }

    suspend fun addOrGetContact(first: String, last: String, companyId: Long?): Long {
        return dao.insertContact(Contact(firstName = first, lastName = last, companyId = companyId))
    }

    suspend fun addInteraction(i: Interaction): Long = dao.insertInteraction(i)
    fun dueFollowUps(now: Long): Flow<List<Interaction>> = dao.dueFollowUps(now)
    fun search(q: String): Flow<List<Interaction>> = dao.search(q)
}
