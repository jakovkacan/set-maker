package hr.jkacan.setmaker.models.set

import java.util.Date

data class SetItem(
    val id: Int?,
    val name: String,
    val coverUrl: String?,
    val dateAdded: Date?,
    val dateUpdated: Date?
)
