package hr.jkacan.setmaker.models.set

import hr.jkacan.setmaker.models.song.Song

data class SetNodeWithSong(
    val node: SetNode,
    val song: Song
)