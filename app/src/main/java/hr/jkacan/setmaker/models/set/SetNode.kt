package hr.jkacan.setmaker.models.set

data class SetNode(
    val id: Int,
    val setId: Int,
    val songId: Int,
    val note: String?
)