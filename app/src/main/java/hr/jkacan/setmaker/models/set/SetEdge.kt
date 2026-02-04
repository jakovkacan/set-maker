package hr.jkacan.setmaker.models.set

data class SetEdge(
    val id: Int,
    val setId: Int,
    val fromNodeId: Int,
    val toNodeId: Int,
    val ord: Int = 0,
    val kind: String? = null
)