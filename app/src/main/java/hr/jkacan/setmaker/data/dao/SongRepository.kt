package hr.jkacan.setmaker.data.dao

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.core.net.toUri
import hr.jkacan.setmaker.data.provider.SongsContentProvider
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.utils.formatDate
import hr.jkacan.setmaker.utils.parseStringAsDate
import java.util.Date

class SongRepository(private val context: Context) : Repository<Song> {

    private val contentResolver = context.contentResolver

    override fun insert(item: Song): Long {
        val values = ContentValues().apply {
            put(DatabaseContract.SongEntry.COLUMN_PLATFORM_ID, item.platformId)
            put(DatabaseContract.SongEntry.COLUMN_TITLE, item.title)
            put(DatabaseContract.SongEntry.COLUMN_ARTIST, item.artist)
            put(DatabaseContract.SongEntry.COLUMN_COVER_URL, item.coverUrl)
            put(DatabaseContract.SongEntry.COLUMN_PROVIDER, item.provider.name)
            put(DatabaseContract.SongEntry.COLUMN_PREVIEW_URL, item.previewUrl)
            put(DatabaseContract.SongEntry.COLUMN_SONG_URL, item.songUrl)
            put(DatabaseContract.SongEntry.COLUMN_DATE_ADDED, formatDate(Date()))
        }
        val uri = contentResolver.insert(SongsContentProvider.CONTENT_URI, values)
        return uri?.let { ContentUris.parseId(it) } ?: -1
    }

    override fun update(item: Song): Int {
        val values = ContentValues().apply {
            put(DatabaseContract.SongEntry.COLUMN_PLATFORM_ID, item.platformId)
            put(DatabaseContract.SongEntry.COLUMN_TITLE, item.title)
            put(DatabaseContract.SongEntry.COLUMN_ARTIST, item.artist)
            put(DatabaseContract.SongEntry.COLUMN_COVER_URL, item.coverUrl)
            put(DatabaseContract.SongEntry.COLUMN_PROVIDER, item.provider.name)
            put(DatabaseContract.SongEntry.COLUMN_PREVIEW_URL, item.previewUrl)
            put(DatabaseContract.SongEntry.COLUMN_SONG_URL, item.songUrl)
            put(DatabaseContract.SongEntry.COLUMN_DATE_ADDED, formatDate(item.dateAdded))
        }
        val uri = ContentUris.withAppendedId(SongsContentProvider.CONTENT_URI, item.id!!.toLong())
        return contentResolver.update(uri, values, null, null)
    }

    override fun delete(id: Int): Int {
        val uri = ContentUris.withAppendedId(SongsContentProvider.CONTENT_URI, id.toLong())
        return contentResolver.delete(uri, null, null)
    }

    override fun getById(id: Int): Song? {
        val uri = ContentUris.withAppendedId(SongsContentProvider.CONTENT_URI, id.toLong())
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) cursorToSong(it) else null
        }
    }

    fun getByPlatformId(platformId: String): Song? {
        val uri = "content://${SongsContentProvider.AUTHORITY}/songs/platform/$platformId".toUri()
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) cursorToSong(it) else null
        }
    }

    override fun getAll(): List<Song> {
        val songs = mutableListOf<Song>()
        val cursor = contentResolver.query(
            SongsContentProvider.CONTENT_URI,
            null,
            null,
            null,
            "${DatabaseContract.SongEntry.COLUMN_DATE_ADDED} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                songs.add(cursorToSong(it))
            }
        }

        return songs
    }

    fun getSongsByProvider(provider: SongProvider): List<Song> {
        val songs = mutableListOf<Song>()
        val selection = "${DatabaseContract.SongEntry.COLUMN_PROVIDER} = ?"
        val selectionArgs = arrayOf(provider.name)

        val cursor = contentResolver.query(
            SongsContentProvider.CONTENT_URI,
            null,
            selection,
            selectionArgs,
            "${DatabaseContract.SongEntry.COLUMN_DATE_ADDED} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                songs.add(cursorToSong(it))
            }
        }

        return songs
    }

    fun getSavedSongPlatformIds(): List<String> {
        val platformIds = mutableListOf<String>()
        val projection = arrayOf(DatabaseContract.SongEntry.COLUMN_PLATFORM_ID)

        val cursor = contentResolver.query(
            SongsContentProvider.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                platformIds.add(
                    it.getString(it.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_PLATFORM_ID))
                )
            }
        }

        return platformIds
    }

    fun toggleSavedSong(song: Song): Boolean {
        val savedSongs = getSavedSongPlatformIds()
        if (savedSongs.contains(song.platformId)) {
            val song = getByPlatformId(song.platformId!!)
            delete(song?.id!!)
            return false
        } else {
            insert(song)
            return true
        }
    }

    /**
     * Deletes all songs that are not part of any set.
     * Returns the number of songs deleted.
     */
    fun pruneUnusedSongs(context: Context): Int {
        val setGraphRepository = SetGraphRepository(context)

        try {
            // Get all song IDs
            val allSongIds = mutableSetOf<Int>()
            val allSongs = getAll()
            allSongs.forEach { song ->
                song.id?.let { allSongIds.add(it) }
            }

            // Get all song IDs that are used in sets
            val usedSongIds = mutableSetOf<Int>()
            val setRepository = SetRepository(context)
            val allSets = setRepository.getAll()

            allSets.forEach { set ->
                val nodesWithSongs = setGraphRepository.getNodesWithSongsBySet(set.id!!)
                nodesWithSongs.forEach { nodeWithSong ->
                    usedSongIds.add(nodeWithSong.song.id!!)
                }
            }

            // Find songs that are not used in any set
            val unusedSongIds = allSongIds - usedSongIds

            // Delete unused songs
            var deletedCount = 0
            unusedSongIds.forEach { songId ->
                val deleted = delete(songId)
                if (deleted > 0) {
                    deletedCount++
                }
            }

            setGraphRepository.close()
            setRepository.close()

            return deletedCount
        } catch (e: Exception) {
            setGraphRepository.close()
            throw e
        }
    }

    private fun cursorToSong(cursor: Cursor): Song {
        return Song(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_ID)),
            platformId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_PLATFORM_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_TITLE)),
            artist = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_ARTIST)),
            coverUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_COVER_URL)),
            provider = SongProvider.valueOf(
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_PROVIDER))
            ),
            previewUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_PREVIEW_URL)),
            songUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_SONG_URL)),
            dateAdded = parseStringAsDate(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        DatabaseContract.SongEntry.COLUMN_DATE_ADDED
                    )
                )
            )
        )
    }

    fun close() {
        // No longer need to close dbHelper as we're using ContentResolver
        // ContentResolver is managed by the system
    }
}
