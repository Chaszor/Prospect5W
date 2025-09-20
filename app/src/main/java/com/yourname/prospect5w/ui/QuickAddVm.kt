package com.yourname.prospect5w.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.prospect5w.data.Interaction
import com.yourname.prospect5w.domain.ProspectRepo
import com.yourname.prospect5w.notify.ReminderScheduler
import kotlinx.coroutines.launch

data class QuickAddState(
    var firstName: String = "",
    var lastName: String = "",
    var companyName: String = "",
    var whatType: String = "Call",
    var whatNotes: String = "",
    var whenAt: Long = System.currentTimeMillis(),
    var whereText: String? = null,
    var why: String = "",
    var nextFollowUpAt: Long? = null,
    var followUpNote: String? = null
)

class QuickAddVm(
    private val repo: ProspectRepo,
    private val scheduler: ReminderScheduler
) : ViewModel() {
    fun save(s: QuickAddState, onDone: () -> Unit) = viewModelScope.launch {
        val companyId = repo.addCompanyIfNeeded(s.companyName)
        val contactId = repo.addOrGetContact(s.firstName.trim(), s.lastName.trim(), companyId)
        val id = repo.addInteraction(
            Interaction(
                contactId = contactId,
                companyId = companyId,
                whatType = s.whatType,
                whatNotes = s.whatNotes,
                whenAt = s.whenAt,
                whereText = s.whereText,
                whySummary = s.why,
                nextFollowUpAt = s.nextFollowUpAt,
                followUpNote = s.followUpNote
            )
        )
        s.nextFollowUpAt?.let { scheduler.schedule(id, it) }
        onDone()
    }
}
