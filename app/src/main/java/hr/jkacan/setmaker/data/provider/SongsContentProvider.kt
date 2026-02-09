package hr.jkacan.setmaker.data.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import hr.jkacan.setmaker.data.dao.DatabaseContract
import hr.jkacan.setmaker.data.dao.DatabaseHelper

class SongsContentProvider : ContentProvider() {

    private lateinit var dbHelper: DatabaseHelper

    companion object {
        const val AUTHORITY = "hr.jkacan.setmaker.provider"
        val CONTENT_URI: Uri = "content://$AUTHORITY/songs".toUri()

        private const val SONGS = 1
        private const val SONG_ID = 2
        private const val SONG_BY_PLATFORM_ID = 3

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "songs", SONGS)
            addURI(AUTHORITY, "songs/#", SONG_ID)
            addURI(AUTHORITY, "songs/platform/*", SONG_BY_PLATFORM_ID)
        }
    }

    override fun onCreate(): Boolean {
        dbHelper = DatabaseHelper(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = when (uriMatcher.match(uri)) {
            SONGS -> {
                db.query(
                    DatabaseContract.SongEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder ?: "${DatabaseContract.SongEntry.COLUMN_DATE_ADDED} DESC"
                )
            }

            SONG_ID -> {
                val id = ContentUris.parseId(uri)
                db.query(
                    DatabaseContract.SongEntry.TABLE_NAME,
                    projection,
                    "${DatabaseContract.SongEntry.COLUMN_ID} = ?",
                    arrayOf(id.toString()),
                    null,
                    null,
                    sortOrder
                )
            }

            SONG_BY_PLATFORM_ID -> {
                val platformId = uri.lastPathSegment
                db.query(
                    DatabaseContract.SongEntry.TABLE_NAME,
                    projection,
                    "${DatabaseContract.SongEntry.COLUMN_PLATFORM_ID} = ?",
                    arrayOf(platformId),
                    null,
                    null,
                    sortOrder
                )
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            SONGS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.songs"
            SONG_ID, SONG_BY_PLATFORM_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.songs"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        val id = when (uriMatcher.match(uri)) {
            SONGS -> db.insert(DatabaseContract.SongEntry.TABLE_NAME, null, values)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        if (id > 0) {
            val resultUri = ContentUris.withAppendedId(CONTENT_URI, id)
            context?.contentResolver?.notifyChange(resultUri, null)
            return resultUri
        }
        return null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val db = dbHelper.writableDatabase
        val count = when (uriMatcher.match(uri)) {
            SONGS -> db.update(
                DatabaseContract.SongEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )

            SONG_ID -> {
                val id = ContentUris.parseId(uri)
                db.update(
                    DatabaseContract.SongEntry.TABLE_NAME,
                    values,
                    "${DatabaseContract.SongEntry.COLUMN_ID} = ?",
                    arrayOf(id.toString())
                )
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        if (count > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return count
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        val count = when (uriMatcher.match(uri)) {
            SONGS -> db.delete(DatabaseContract.SongEntry.TABLE_NAME, selection, selectionArgs)
            SONG_ID -> {
                val id = ContentUris.parseId(uri)
                db.delete(
                    DatabaseContract.SongEntry.TABLE_NAME,
                    "${DatabaseContract.SongEntry.COLUMN_ID} = ?",
                    arrayOf(id.toString())
                )
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        if (count > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return count
    }
}


