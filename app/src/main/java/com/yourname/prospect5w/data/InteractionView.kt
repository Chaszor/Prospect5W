package com.yourname.prospect5w.data

/**
 * A view model representing an [Interaction] along with the associated contact
 * and company names.  Room will populate this data class from the results of
 * the join queries defined in [ProspectDao].  Fields correspond exactly to
 * the columns returned by those queries.
 */
data class InteractionView(
    val id: Long,
    val contactId: Long,
    val companyId: Long?,
    val whatType: String,
    val whatNotes: String,
    val whenAt: Long,
    val whereLat: Double?,
    val whereLng: Double?,
    val whereText: String?,
    val whySummary: String,
    val nextFollowUpAt: Long?,
    val followUpNote: String?,
    val firstName: String?,
    val lastName: String?,
    val companyName: String?
)
