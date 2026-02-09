package hr.jkacan.setmaker.data.dao

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import hr.jkacan.setmaker.data.provider.SetsContentProvider
import hr.jkacan.setmaker.models.set.SetItem
import hr.jkacan.setmaker.utils.formatDate
import hr.jkacan.setmaker.utils.parseStringAsDate
import java.util.Date

class SetRepository(private val context: Context) : Repository<SetItem> {

    private val contentResolver = context.contentResolver

    override fun insert(item: SetItem): Long {
        val values = ContentValues().apply {
            put(DatabaseContract.SetEntry.COLUMN_NAME, item.name)
            put(DatabaseContract.SetEntry.COLUMN_COVER_URL, item.coverPath)
            put(DatabaseContract.SetEntry.COLUMN_DATE_ADDED, formatDate(Date()))
            put(DatabaseContract.SetEntry.COLUMN_DATE_UPDATED, formatDate(Date()))
        }
        val uri = contentResolver.insert(SetsContentProvider.CONTENT_URI, values)
        return uri?.let { ContentUris.parseId(it) } ?: -1
    }

    override fun update(item: SetItem): Int {
        val values = ContentValues().apply {
            put(DatabaseContract.SetEntry.COLUMN_NAME, item.name)
            put(DatabaseContract.SetEntry.COLUMN_COVER_URL, item.coverPath)
            put(DatabaseContract.SetEntry.COLUMN_DATE_ADDED, formatDate(item.dateAdded))
            put(DatabaseContract.SetEntry.COLUMN_DATE_UPDATED, formatDate(Date()))
        }
        val uri = ContentUris.withAppendedId(SetsContentProvider.CONTENT_URI, item.id!!.toLong())
        return contentResolver.update(uri, values, null, null)
    }

    override fun delete(id: Int): Int {
        val uri = ContentUris.withAppendedId(SetsContentProvider.CONTENT_URI, id.toLong())
        return contentResolver.delete(uri, null, null)
    }

    override fun getById(id: Int): SetItem? {
        val uri = ContentUris.withAppendedId(SetsContentProvider.CONTENT_URI, id.toLong())
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) cursorToSet(it) else null
        }
    }

    override fun getAll(): List<SetItem> {
        val sets = mutableListOf<SetItem>()
        val cursor = contentResolver.query(
            SetsContentProvider.CONTENT_URI,
            null,
            null,
            null,
            "${DatabaseContract.SetEntry.COLUMN_NAME} ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                sets.add(cursorToSet(it))
            }
        }

        return sets
    }

    private fun cursorToSet(cursor: Cursor): SetItem {
        return SetItem(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetEntry.COLUMN_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SetEntry.COLUMN_NAME)),
            coverPath = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SetEntry.COLUMN_COVER_URL)),
            dateAdded = parseStringAsDate(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        DatabaseContract.SetEntry.COLUMN_DATE_ADDED
                    )
                )
            ),
            dateUpdated = parseStringAsDate(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        DatabaseContract.SetEntry.COLUMN_DATE_UPDATED
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
