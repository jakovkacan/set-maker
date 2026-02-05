package hr.jkacan.setmaker.models.song

import java.io.Serializable
import java.util.Date

data class Song(
    val id: Int?,
    val platformId: String?,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val provider: SongProvider,
    val previewUrl: String?,
    val songUrl: String?,
    val dateAdded: Date?
) : Serializable
