package hr.jkacan.setmaker.models

import java.io.Serializable

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val provider: SongProvider,
    val isPinned: Boolean = false
) : Serializable
