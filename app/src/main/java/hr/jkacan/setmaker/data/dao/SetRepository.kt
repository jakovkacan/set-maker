package hr.jkacan.setmaker.data.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import hr.jkacan.setmaker.models.set.SetItem
import hr.jkacan.setmaker.utils.formatDate
import hr.jkacan.setmaker.utils.parseStringAsDate
import java.util.Date

class SetRepository(context: Context) : Repository<SetItem> {
    private val dbHelper = DatabaseHelper(context)

    override fun insert(item: SetItem): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.SetEntry.COLUMN_NAME, item.name)
            put(DatabaseContract.SetEntry.COLUMN_COVER_URL, item.coverUrl)
            put(DatabaseContract.SetEntry.COLUMN_DATE_ADDED, formatDate(Date()))
            put(DatabaseContract.SetEntry.COLUMN_DATE_UPDATED, formatDate(Date()))
        }
        return db.insert(DatabaseContract.SetEntry.TABLE_NAME, null, values)
    }

    override fun update(item: SetItem): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.SetEntry.COLUMN_NAME, item.name)
            put(DatabaseContract.SetEntry.COLUMN_COVER_URL, item.coverUrl)
            put(DatabaseContract.SetEntry.COLUMN_DATE_ADDED, formatDate(item.dateAdded))
            put(DatabaseContract.SetEntry.COLUMN_DATE_UPDATED, formatDate(Date()))
        }
        val selection = "${DatabaseContract.SetEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(item.id.toString())
        return db.update(
            DatabaseContract.SetEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
    }

    override fun delete(id: Int): Int {
        val db = dbHelper.writableDatabase
        val selection = "${DatabaseContract.SetEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return db.delete(
            DatabaseContract.SetEntry.TABLE_NAME,
            selection,
            selectionArgs
        )
    }

    override fun getById(id: Int): SetItem? {
        val db = dbHelper.readableDatabase
        val projection = arrayOf(
            DatabaseContract.SetEntry.COLUMN_ID,
            DatabaseContract.SetEntry.COLUMN_NAME,
            DatabaseContract.SetEntry.COLUMN_COVER_URL,
            DatabaseContract.SetEntry.COLUMN_DATE_ADDED,
            DatabaseContract.SetEntry.COLUMN_DATE_UPDATED
        )

        val selection = "${DatabaseContract.SetEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        val cursor = db.query(
            DatabaseContract.SetEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                cursorToSet(it)
            } else {
                null
            }
        }
    }

    override fun getAll(): List<SetItem> {
        val sets = mutableListOf<SetItem>()
        val db = dbHelper.readableDatabase
        val projection = arrayOf(
            DatabaseContract.SetEntry.COLUMN_ID,
            DatabaseContract.SetEntry.COLUMN_NAME,
            DatabaseContract.SetEntry.COLUMN_COVER_URL,
            DatabaseContract.SetEntry.COLUMN_DATE_ADDED,
            DatabaseContract.SetEntry.COLUMN_DATE_UPDATED
        )

        val cursor = db.query(
            DatabaseContract.SetEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            DatabaseContract.SetEntry.COLUMN_NAME + " ASC"
        )

        cursor.use {
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
            coverUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SetEntry.COLUMN_COVER_URL)),
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
        dbHelper.close()
    }
}
