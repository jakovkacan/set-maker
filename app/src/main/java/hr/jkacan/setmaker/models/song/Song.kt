package hr.jkacan.setmaker.models.song

import java.io.Serializable

data class Song(
    val id: Int?,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val provider: SongProvider,
    val previewUrl: String?,
    val songUrl: String?
) : Serializable
