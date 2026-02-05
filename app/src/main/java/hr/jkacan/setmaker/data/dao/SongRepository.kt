package hr.jkacan.setmaker.data.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.utils.formatDate
import hr.jkacan.setmaker.utils.parseStringAsDate
import java.util.Date

class SongRepository(context: Context) : Repository<Song> {
    private val dbHelper = DatabaseHelper(context)

    override fun insert(item: Song): Long {
        val db = dbHelper.writableDatabase
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
        return db.insert(DatabaseContract.SongEntry.TABLE_NAME, null, values)
    }

    override fun update(item: Song): Int {
        val db = dbHelper.writableDatabase
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
        val selection = "${DatabaseContract.SongEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(item.id.toString())
        return db.update(
            DatabaseContract.SongEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
    }

    override fun delete(id: Int): Int {
        val db = dbHelper.writableDatabase
        val selection = "${DatabaseContract.SongEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return db.delete(
            DatabaseContract.SongEntry.TABLE_NAME,
            selection,
            selectionArgs
        )
    }

    override fun getById(id: Int): Song? {
        val db = dbHelper.readableDatabase
        val projection = arrayOf(
            DatabaseContract.SongEntry.COLUMN_ID,
            DatabaseContract.SongEntry.COLUMN_PLATFORM_ID,
            DatabaseContract.SongEntry.COLUMN_TITLE,
            DatabaseContract.SongEntry.COLUMN_ARTIST,
            DatabaseContract.SongEntry.COLUMN_COVER_URL,
            DatabaseContract.SongEntry.COLUMN_PROVIDER,
            DatabaseContract.SongEntry.COLUMN_PREVIEW_URL,
            DatabaseContract.SongEntry.COLUMN_SONG_URL,
            DatabaseContract.SongEntry.COLUMN_DATE_ADDED
        )

        val selection = "${DatabaseContract.SongEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        val cursor = db.query(
            DatabaseContract.SongEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                cursorToSong(it)
            } else {
                null
            }
        }
    }

    override fun getAll(): List<Song> {
        val songs = mutableListOf<Song>()
        val db = dbHelper.readableDatabase
        val projection = arrayOf(
            DatabaseContract.SongEntry.COLUMN_ID,
            DatabaseContract.SongEntry.COLUMN_PLATFORM_ID,
            DatabaseContract.SongEntry.COLUMN_TITLE,
            DatabaseContract.SongEntry.COLUMN_ARTIST,
            DatabaseContract.SongEntry.COLUMN_COVER_URL,
            DatabaseContract.SongEntry.COLUMN_PROVIDER,
            DatabaseContract.SongEntry.COLUMN_PREVIEW_URL,
            DatabaseContract.SongEntry.COLUMN_SONG_URL,
            DatabaseContract.SongEntry.COLUMN_DATE_ADDED
        )

        val cursor = db.query(
            DatabaseContract.SongEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            DatabaseContract.SongEntry.COLUMN_TITLE + " ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                songs.add(cursorToSong(it))
            }
        }

        return songs
    }

    fun getSongsByProvider(provider: SongProvider): List<Song> {
        val songs = mutableListOf<Song>()
        val db = dbHelper.readableDatabase

        val selection = "${DatabaseContract.SongEntry.COLUMN_PROVIDER} = ?"
        val selectionArgs = arrayOf(provider.name)

        val cursor = db.query(
            DatabaseContract.SongEntry.TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            DatabaseContract.SongEntry.COLUMN_TITLE + " ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                songs.add(cursorToSong(it))
            }
        }

        return songs
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
        dbHelper.close()
    }
}
