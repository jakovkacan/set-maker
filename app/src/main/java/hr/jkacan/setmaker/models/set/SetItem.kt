package hr.jkacan.setmaker.models.set

import java.io.Serializable
import java.util.Date

data class SetItem(
    val id: Int?,
    val name: String,
    val coverPath: String?,
    val dateAdded: Date?,
    val dateUpdated: Date?
) : Serializable
