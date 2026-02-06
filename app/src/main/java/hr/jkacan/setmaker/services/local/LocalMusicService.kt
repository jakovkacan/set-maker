package hr.jkacan.setmaker.services.local

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import hr.jkacan.setmaker.R

class LocalMusicService(private val context: Context) {

    private var cachedMusicFiles: List<LocalMusicFile>? = null

    suspend fun getAllMusicFiles(): List<LocalMusicFile> = withContext(Dispatchers.IO) {
        if (cachedMusicFiles != null) return@withContext cachedMusicFiles!!

        val musicFiles = mutableListOf<LocalMusicFile>()
        val contentResolver: ContentResolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: context.getString(R.string.unknown)
                var artist =
                    it.getString(artistColumn) ?: context.getString(R.string.unknown_artist)
                var album = it.getString(albumColumn)
                val duration = it.getLong(durationColumn)
                val data = it.getString(dataColumn)
                val albumId = it.getLong(albumIdColumn)

                val albumArtUri = "content://media/external/audio/albumart".toUri()
                val artUri = Uri.withAppendedPath(albumArtUri, albumId.toString()).toString()

                val actualArtUri = try {
                    contentResolver.openInputStream(artUri.toUri())
                        ?.use { null }
                    artUri
                } catch (e: Exception) {
                    null // Album art doesn't exist
                }

                if (artist == "<unknown>")
                    artist = context.getString(R.string.unknown_artist)

                if (album == "<unknown>")
                    album = context.getString(R.string.unknown)


                musicFiles.add(
                    LocalMusicFile(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        data = data,
                        albumArtUri = actualArtUri
                    )
                )
            }
        }

        cachedMusicFiles = musicFiles
        musicFiles
    }

    suspend fun query(queryString: String): List<Song> = withContext(Dispatchers.IO) {
        val allFiles = getAllMusicFiles()
        val filtered = if (queryString.isBlank()) {
            allFiles
        } else {
            allFiles.filter {
                it.title.contains(queryString, ignoreCase = true) ||
                        it.artist.contains(queryString, ignoreCase = true)
            }
        }

        filtered.mapIndexed { index, file ->
            Song(
                id = index + 1,
                platformId = file.id.toString(),
                title = file.title,
                artist = file.artist,
                coverUrl = file.albumArtUri,
                provider = SongProvider.LOCAL,
                previewUrl = file.data,
                songUrl = file.data,
                dateAdded = null
            )
        }
    }

    fun clearCache() {
        cachedMusicFiles = null
    }
}
