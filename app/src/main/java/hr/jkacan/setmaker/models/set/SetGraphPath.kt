package hr.jkacan.setmaker.models.set

data class SetGraphPath(
    val nodes: List<SetNodeWithSong>,
    val edges: List<SetEdge>
)