package hr.jkacan.setmaker.data.dao

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "setmaker.db"
        private const val DATABASE_VERSION = 4

        private const val SQL_CREATE_SONGS = """
            CREATE TABLE ${DatabaseContract.SongEntry.TABLE_NAME} (
                ${DatabaseContract.SongEntry.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DatabaseContract.SongEntry.COLUMN_PLATFORM_ID} TEXT,
                ${DatabaseContract.SongEntry.COLUMN_TITLE} TEXT NOT NULL,
                ${DatabaseContract.SongEntry.COLUMN_ARTIST} TEXT NOT NULL,
                ${DatabaseContract.SongEntry.COLUMN_COVER_URL} TEXT,
                ${DatabaseContract.SongEntry.COLUMN_PROVIDER} TEXT NOT NULL,
                ${DatabaseContract.SongEntry.COLUMN_PREVIEW_URL} TEXT,
                ${DatabaseContract.SongEntry.COLUMN_SONG_URL} TEXT,
                ${DatabaseContract.SongEntry.COLUMN_DATE_ADDED} DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """

        private const val SQL_CREATE_SETS = """
            CREATE TABLE ${DatabaseContract.SetEntry.TABLE_NAME} (
                ${DatabaseContract.SetEntry.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DatabaseContract.SetEntry.COLUMN_NAME} TEXT NOT NULL,
                ${DatabaseContract.SetEntry.COLUMN_COVER_URL} TEXT,
                ${DatabaseContract.SetEntry.COLUMN_DATE_ADDED} DATETIME DEFAULT CURRENT_TIMESTAMP,
                ${DatabaseContract.SetEntry.COLUMN_DATE_UPDATED} DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """

        private const val SQL_CREATE_SET_NODES = """
            CREATE TABLE ${DatabaseContract.SetNodeEntry.TABLE_NAME} (
                ${DatabaseContract.SetNodeEntry.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DatabaseContract.SetNodeEntry.COLUMN_SET_ID} INTEGER NOT NULL,
                ${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID} INTEGER NOT NULL,
                ${DatabaseContract.SetNodeEntry.COLUMN_NOTE} TEXT,
                FOREIGN KEY (${DatabaseContract.SetNodeEntry.COLUMN_SET_ID}) 
                    REFERENCES ${DatabaseContract.SetEntry.TABLE_NAME}(${DatabaseContract.SetEntry.COLUMN_ID}) 
                    ON DELETE CASCADE,
                FOREIGN KEY (${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID}) 
                    REFERENCES ${DatabaseContract.SongEntry.TABLE_NAME}(${DatabaseContract.SongEntry.COLUMN_ID}) 
                    ON DELETE CASCADE
            )
        """

        private const val SQL_CREATE_SET_EDGES = """
            CREATE TABLE ${DatabaseContract.SetEdgeEntry.TABLE_NAME} (
                ${DatabaseContract.SetEdgeEntry.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} INTEGER NOT NULL,
                ${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} INTEGER NOT NULL,
                ${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID} INTEGER NOT NULL,
                ${DatabaseContract.SetEdgeEntry.COLUMN_ORD} INTEGER NOT NULL DEFAULT 0,
                ${DatabaseContract.SetEdgeEntry.COLUMN_KIND} TEXT,
                FOREIGN KEY (${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID}) 
                    REFERENCES ${DatabaseContract.SetEntry.TABLE_NAME}(${DatabaseContract.SetEntry.COLUMN_ID}) 
                    ON DELETE CASCADE,
                FOREIGN KEY (${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID}) 
                    REFERENCES ${DatabaseContract.SetNodeEntry.TABLE_NAME}(${DatabaseContract.SetNodeEntry.COLUMN_ID}) 
                    ON DELETE CASCADE,
                FOREIGN KEY (${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID}) 
                    REFERENCES ${DatabaseContract.SetNodeEntry.TABLE_NAME}(${DatabaseContract.SetNodeEntry.COLUMN_ID}) 
                    ON DELETE CASCADE,
                UNIQUE (${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID}, 
                       ${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID}, 
                       ${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID})
            )
        """

        private const val SQL_CREATE_INDEX_EDGE_FROM = """
            CREATE INDEX idx_edge_from ON ${DatabaseContract.SetEdgeEntry.TABLE_NAME}(
                ${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID}, 
                ${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID}, 
                ${DatabaseContract.SetEdgeEntry.COLUMN_ORD}
            )
        """

        private const val SQL_CREATE_INDEX_EDGE_TO = """
            CREATE INDEX idx_edge_to ON ${DatabaseContract.SetEdgeEntry.TABLE_NAME}(
                ${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID}, 
                ${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID}
            )
        """

        private const val SQL_UPDATE_SONGS_WITH_SONG_URL = """
            ALTER TABLE ${DatabaseContract.SongEntry.TABLE_NAME}
            ADD COLUMN ${DatabaseContract.SongEntry.COLUMN_SONG_URL} TEXT
        """

        private const val SQL_UPDATE_SONGS_WITH_PLATFORMID_DATEADDED = """
            ALTER TABLE ${DatabaseContract.SongEntry.TABLE_NAME}
            ADD COLUMN ${DatabaseContract.SongEntry.COLUMN_PLATFORM_ID} TEXT,
            ADD COLUMN ${DatabaseContract.SongEntry.COLUMN_DATE_ADDED} DATETIME DEFAULT CURRENT_TIMESTAMP
        """

        private const val SQL_UPDATE_SETS_WITH_DATEADDED_DATEUPDATED = """
            ALTER TABLE ${DatabaseContract.SetEntry.TABLE_NAME}
            ADD COLUMN ${DatabaseContract.SetEntry.COLUMN_DATE_ADDED} DATETIME DEFAULT CURRENT_TIMESTAMP,
            ADD COLUMN ${DatabaseContract.SetEntry.COLUMN_DATE_UPDATED} DATETIME DEFAULT CURRENT_TIMESTAMP
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_SONGS)
        db.execSQL(SQL_CREATE_SETS)
        db.execSQL(SQL_CREATE_SET_NODES)
        db.execSQL(SQL_CREATE_SET_EDGES)
        db.execSQL(SQL_CREATE_INDEX_EDGE_FROM)
        db.execSQL(SQL_CREATE_INDEX_EDGE_TO)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when (oldVersion) {
            4 -> {
                db.execSQL(SQL_UPDATE_SONGS_WITH_PLATFORMID_DATEADDED)
                db.execSQL(SQL_UPDATE_SETS_WITH_DATEADDED_DATEUPDATED)
            }
            3 -> {
                db.execSQL(SQL_UPDATE_SONGS_WITH_SONG_URL)
            }
            2 -> {}
            1 -> {
                db.execSQL("DROP INDEX IF EXISTS idx_edge_from")
                db.execSQL("DROP INDEX IF EXISTS idx_edge_to")
                db.execSQL("DROP TABLE IF EXISTS ${DatabaseContract.SetEdgeEntry.TABLE_NAME}")
                db.execSQL("DROP TABLE IF EXISTS ${DatabaseContract.SetNodeEntry.TABLE_NAME}")
                db.execSQL("DROP TABLE IF EXISTS song_sets")

                db.execSQL(SQL_CREATE_SET_NODES)
                db.execSQL(SQL_CREATE_SET_EDGES)
                db.execSQL(SQL_CREATE_INDEX_EDGE_FROM)
                db.execSQL(SQL_CREATE_INDEX_EDGE_TO)
            }

            else -> {
                db.execSQL("DROP TABLE IF EXISTS ${DatabaseContract.SetEdgeEntry.TABLE_NAME}")
                db.execSQL("DROP TABLE IF EXISTS ${DatabaseContract.SetNodeEntry.TABLE_NAME}")
                db.execSQL("DROP TABLE IF EXISTS ${DatabaseContract.SetEntry.TABLE_NAME}")
                db.execSQL("DROP TABLE IF EXISTS ${DatabaseContract.SongEntry.TABLE_NAME}")
                onCreate(db)
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }
}

