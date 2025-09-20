package com.yourname.prospect5w.data

import androidx.room.*

@Entity(indices = [Index("companyId")])
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val email: String? = null,
    val title: String? = null,
    val companyId: Long? = null,
    val tags: String = ""
)

@Entity
data class Company(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val city: String? = null,
    val state: String? = null,
    val notes: String? = null
)

@Entity(
    foreignKeys = [
        ForeignKey(entity = Contact::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Company::class, parentColumns = ["id"], childColumns = ["companyId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("contactId"), Index("companyId"), Index("nextFollowUpAt")]
)
data class Interaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val companyId: Long? = null,
    val whatType: String,
    val whatNotes: String,
    val whenAt: Long,
    val whereLat: Double? = null,
    val whereLng: Double? = null,
    val whereText: String? = null,
    val whySummary: String,
    val nextFollowUpAt: Long? = null,
    val followUpNote: String? = null
)
