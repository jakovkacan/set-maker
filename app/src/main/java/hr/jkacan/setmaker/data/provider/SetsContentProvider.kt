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

class SetsContentProvider : ContentProvider() {

    private lateinit var dbHelper: DatabaseHelper

    companion object {
        const val AUTHORITY = "hr.jkacan.setmaker.provider.sets"
        val CONTENT_URI: Uri = "content://$AUTHORITY/sets".toUri()

        private const val SETS = 1
        private const val SET_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "sets", SETS)
            addURI(AUTHORITY, "sets/#", SET_ID)
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
            SETS -> {
                db.query(
                    DatabaseContract.SetEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder ?: "${DatabaseContract.SetEntry.COLUMN_NAME} ASC"
                )
            }
            SET_ID -> {
                val id = ContentUris.parseId(uri)
                db.query(
                    DatabaseContract.SetEntry.TABLE_NAME,
                    projection,
                    "${DatabaseContract.SetEntry.COLUMN_ID} = ?",
                    arrayOf(id.toString()),
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
            SETS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.sets"
            SET_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.sets"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        val id = when (uriMatcher.match(uri)) {
            SETS -> db.insert(DatabaseContract.SetEntry.TABLE_NAME, null, values)
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
            SETS -> db.update(
                DatabaseContract.SetEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )
            SET_ID -> {
                val id = ContentUris.parseId(uri)
                db.update(
                    DatabaseContract.SetEntry.TABLE_NAME,
                    values,
                    "${DatabaseContract.SetEntry.COLUMN_ID} = ?",
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
            SETS -> db.delete(DatabaseContract.SetEntry.TABLE_NAME, selection, selectionArgs)
            SET_ID -> {
                val id = ContentUris.parseId(uri)
                db.delete(
                    DatabaseContract.SetEntry.TABLE_NAME,
                    "${DatabaseContract.SetEntry.COLUMN_ID} = ?",
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

