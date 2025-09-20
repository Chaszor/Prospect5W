package com.yourname.prospect5w.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProspectDao {
    @Insert suspend fun insertContact(contact: Contact): Long
    @Update suspend fun updateContact(contact: Contact)
    @Query("SELECT * FROM Contact ORDER BY lastName, firstName")
    fun contacts(): Flow<List<Contact>>

    @Insert suspend fun insertCompany(company: Company): Long
    @Query("SELECT * FROM Company ORDER BY name")
    fun companies(): Flow<List<Company>>

    @Insert suspend fun insertInteraction(i: Interaction): Long
    @Query("SELECT * FROM Interaction WHERE contactId = :contactId ORDER BY whenAt DESC")
    fun interactionsForContact(contactId: Long): Flow<List<Interaction>>

    @Query("SELECT * FROM Interaction WHERE nextFollowUpAt IS NOT NULL AND nextFollowUpAt <= :byTime ORDER BY nextFollowUpAt ASC")
    fun dueFollowUps(byTime: Long): Flow<List<Interaction>>

    @Query("""
        SELECT i.* FROM Interaction i
        LEFT JOIN Contact c ON c.id = i.contactId
        LEFT JOIN Company co ON co.id = i.companyId
        WHERE (c.firstName || ' ' || c.lastName || ' ' || IFNULL(co.name,'') || ' ' || i.whatNotes || ' ' || IFNULL(i.whySummary,''))
              LIKE '%' || :q || '%'
        ORDER BY whenAt DESC
    """)
    fun search(q: String): Flow<List<Interaction>>
}
