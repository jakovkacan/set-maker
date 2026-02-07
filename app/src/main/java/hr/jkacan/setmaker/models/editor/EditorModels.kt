package hr.jkacan.setmaker.models.editor

import hr.jkacan.setmaker.models.song.Song

data class UiNode(
    val id: Long,
    val col: Int,   // lane (x)
    val row: Int,    // layer (y)
    val song: Song
)

data class UiEdge(val fromId: Long, val toId: Long)
