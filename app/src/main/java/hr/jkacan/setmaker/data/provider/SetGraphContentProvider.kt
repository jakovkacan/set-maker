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

class SetGraphContentProvider : ContentProvider() {

    private lateinit var dbHelper: DatabaseHelper

    companion object {
        const val AUTHORITY = "hr.jkacan.setmaker.provider.setgraph"
        val NODES_URI: Uri = "content://$AUTHORITY/nodes".toUri()
        val EDGES_URI: Uri = "content://$AUTHORITY/edges".toUri()

        private const val NODES = 1
        private const val NODE_ID = 2
        private const val NODES_BY_SET = 3
        private const val EDGES = 4
        private const val EDGE_ID = 5
        private const val EDGES_BY_SET = 6
        private const val OUTGOING_EDGES = 7
        private const val INCOMING_EDGES = 8
        private const val START_NODES = 9
        private const val END_NODES = 10
        private const val NODES_WITH_SONGS = 11
        private const val NEXT_NODES = 12
        private const val PREVIOUS_NODES = 13

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "nodes", NODES)
            addURI(AUTHORITY, "nodes/#", NODE_ID)
            addURI(AUTHORITY, "nodes/set/#", NODES_BY_SET)
            addURI(AUTHORITY, "nodes/set/#/with_songs", NODES_WITH_SONGS)
            addURI(AUTHORITY, "nodes/set/#/start", START_NODES)
            addURI(AUTHORITY, "nodes/set/#/end", END_NODES)
            addURI(AUTHORITY, "nodes/set/#/next/#", NEXT_NODES)
            addURI(AUTHORITY, "nodes/set/#/previous/#", PREVIOUS_NODES)
            addURI(AUTHORITY, "edges", EDGES)
            addURI(AUTHORITY, "edges/#", EDGE_ID)
            addURI(AUTHORITY, "edges/set/#", EDGES_BY_SET)
            addURI(AUTHORITY, "edges/set/#/outgoing/#", OUTGOING_EDGES)
            addURI(AUTHORITY, "edges/set/#/incoming/#", INCOMING_EDGES)
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
            NODES -> {
                db.query(
                    DatabaseContract.SetNodeEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder ?: "${DatabaseContract.SetNodeEntry.COLUMN_ID} ASC"
                )
            }
            NODE_ID -> {
                val id = ContentUris.parseId(uri)
                db.query(
                    DatabaseContract.SetNodeEntry.TABLE_NAME,
                    projection,
                    "${DatabaseContract.SetNodeEntry.COLUMN_ID} = ?",
                    arrayOf(id.toString()),
                    null,
                    null,
                    sortOrder
                )
            }
            NODES_BY_SET -> {
                val setId = uri.pathSegments[2]
                db.query(
                    DatabaseContract.SetNodeEntry.TABLE_NAME,
                    projection,
                    "${DatabaseContract.SetNodeEntry.COLUMN_SET_ID} = ?",
                    arrayOf(setId),
                    null,
                    null,
                    sortOrder ?: "${DatabaseContract.SetNodeEntry.COLUMN_ID} ASC"
                )
            }
            NODES_WITH_SONGS -> {
                val setId = uri.pathSegments[2]
                val query = """
                    SELECT 
                        n.${DatabaseContract.SetNodeEntry.COLUMN_ID} as node_id,
                        n.${DatabaseContract.SetNodeEntry.COLUMN_SET_ID},
                        n.${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID},
                        n.${DatabaseContract.SetNodeEntry.COLUMN_NOTE},
                        s.*
                    FROM ${DatabaseContract.SetNodeEntry.TABLE_NAME} n
                    INNER JOIN ${DatabaseContract.SongEntry.TABLE_NAME} s
                    ON n.${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID} = s.${DatabaseContract.SongEntry.COLUMN_ID}
                    WHERE n.${DatabaseContract.SetNodeEntry.COLUMN_SET_ID} = ?
                    ORDER BY n.${DatabaseContract.SetNodeEntry.COLUMN_ID} ASC
                """
                db.rawQuery(query, arrayOf(setId))
            }
            START_NODES -> {
                val setId = uri.pathSegments[2]
                val query = """
                    SELECT 
                        n.${DatabaseContract.SetNodeEntry.COLUMN_ID} as node_id,
                        n.${DatabaseContract.SetNodeEntry.COLUMN_SET_ID},
                        n.${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID},
                        n.${DatabaseContract.SetNodeEntry.COLUMN_NOTE},
                        s.*
                    FROM ${DatabaseContract.SetNodeEntry.TABLE_NAME} n
                    INNER JOIN ${DatabaseContract.SongEntry.TABLE_NAME} s
                    ON n.${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID} = s.${DatabaseContract.SongEntry.COLUMN_ID}
                    WHERE n.${DatabaseContract.SetNodeEntry.COLUMN_SET_ID} = ?
                    AND n.${DatabaseContract.SetNodeEntry.COLUMN_ID} NOT IN (
                        SELECT DISTINCT ${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID}
                        FROM ${DatabaseContract.SetEdgeEntry.TABLE_NAME}
                        WHERE ${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ?
                    )
                    ORDER BY n.${DatabaseContract.SetNodeEntry.COLUMN_ID} ASC
                """
                db.rawQuery(query, arrayOf(setId, setId))
            }
            END_NODES -> {
                val setId = uri.pathSegments[2]
                val query = """
                    SELECT 
                        n.${DatabaseContract.SetNodeEntry.COLUMN_ID} as node_id,
                        n.${DatabaseContract.SetNodeEntry.COLUMN_SET_ID},
                        n.${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID},
                        n.${DatabaseContract.SetNodeEntry.COLUMN_NOTE},
                        s.*
                    FROM ${DatabaseContract.SetNodeEntry.TABLE_NAME} n
                    INNER JOIN ${DatabaseContract.SongEntry.TABLE_NAME} s
                    ON n.${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID} = s.${DatabaseContract.SongEntry.COLUMN_ID}
                    WHERE n.${DatabaseContract.SetNodeEntry.COLUMN_SET_ID} = ?
                    AND n.${DatabaseContract.SetNodeEntry.COLUMN_ID} NOT IN (
                        SELECT DISTINCT ${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID}
                        FROM ${DatabaseContract.SetEdgeEntry.TABLE_NAME}
                        WHERE ${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ?
                    )
                    ORDER BY n.${DatabaseContract.SetNodeEntry.COLUMN_ID} ASC
                """
                db.rawQuery(query, arrayOf(setId, setId))
            }
            NEXT_NODES -> {
                val setId = uri.pathSegments[2]
                val currentNodeId = uri.pathSegments[4]
                val query = """
                    SELECT 
                        n.${DatabaseContract.SetNodeEntry.COLUMN_ID} as node_id,
                        n.${DatabaseContract.SetNodeEntry.COLUMN_SET_ID},
                        n.${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID},
                        n.${DatabaseContract.SetNodeEntry.COLUMN_NOTE},
                        s.*,
                        e.${DatabaseContract.SetEdgeEntry.COLUMN_ORD},
                        e.${DatabaseContract.SetEdgeEntry.COLUMN_KIND}
                    FROM ${DatabaseContract.SetEdgeEntry.TABLE_NAME} e
                    INNER JOIN ${DatabaseContract.SetNodeEntry.TABLE_NAME} n
                    ON e.${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID} = n.${DatabaseContract.SetNodeEntry.COLUMN_ID}
                    INNER JOIN ${DatabaseContract.SongEntry.TABLE_NAME} s
                    ON n.${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID} = s.${DatabaseContract.SongEntry.COLUMN_ID}
                    WHERE e.${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ?
                    AND e.${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} = ?
                    ORDER BY e.${DatabaseContract.SetEdgeEntry.COLUMN_ORD} ASC
                """
                db.rawQuery(query, arrayOf(setId, currentNodeId))
            }
            PREVIOUS_NODES -> {
                val setId = uri.pathSegments[2]
                val currentNodeId = uri.pathSegments[4]
                val query = """
                    SELECT 
                        n.${DatabaseContract.SetNodeEntry.COLUMN_ID} as node_id,
                        n.${DatabaseContract.SetNodeEntry.COLUMN_SET_ID},
                        n.${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID},
                        n.${DatabaseContract.SetNodeEntry.COLUMN_NOTE},
                        s.*
                    FROM ${DatabaseContract.SetEdgeEntry.TABLE_NAME} e
                    INNER JOIN ${DatabaseContract.SetNodeEntry.TABLE_NAME} n
                    ON e.${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} = n.${DatabaseContract.SetNodeEntry.COLUMN_ID}
                    INNER JOIN ${DatabaseContract.SongEntry.TABLE_NAME} s
                    ON n.${DatabaseContract.SetNodeEntry.COLUMN_SONG_ID} = s.${DatabaseContract.SongEntry.COLUMN_ID}
                    WHERE e.${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ?
                    AND e.${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID} = ?
                    ORDER BY n.${DatabaseContract.SetNodeEntry.COLUMN_ID} ASC
                """
                db.rawQuery(query, arrayOf(setId, currentNodeId))
            }
            EDGES -> {
                db.query(
                    DatabaseContract.SetEdgeEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder ?: "${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} ASC, ${DatabaseContract.SetEdgeEntry.COLUMN_ORD} ASC"
                )
            }
            EDGE_ID -> {
                val id = ContentUris.parseId(uri)
                db.query(
                    DatabaseContract.SetEdgeEntry.TABLE_NAME,
                    projection,
                    "${DatabaseContract.SetEdgeEntry.COLUMN_ID} = ?",
                    arrayOf(id.toString()),
                    null,
                    null,
                    sortOrder
                )
            }
            EDGES_BY_SET -> {
                val setId = uri.pathSegments[2]
                db.query(
                    DatabaseContract.SetEdgeEntry.TABLE_NAME,
                    projection,
                    "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ?",
                    arrayOf(setId),
                    null,
                    null,
                    sortOrder ?: "${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} ASC, ${DatabaseContract.SetEdgeEntry.COLUMN_ORD} ASC"
                )
            }
            OUTGOING_EDGES -> {
                val setId = uri.pathSegments[2]
                val fromNodeId = uri.pathSegments[4]
                db.query(
                    DatabaseContract.SetEdgeEntry.TABLE_NAME,
                    projection,
                    "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ? AND ${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} = ?",
                    arrayOf(setId, fromNodeId),
                    null,
                    null,
                    sortOrder ?: "${DatabaseContract.SetEdgeEntry.COLUMN_ORD} ASC"
                )
            }
            INCOMING_EDGES -> {
                val setId = uri.pathSegments[2]
                val toNodeId = uri.pathSegments[4]
                db.query(
                    DatabaseContract.SetEdgeEntry.TABLE_NAME,
                    projection,
                    "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ? AND ${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID} = ?",
                    arrayOf(setId, toNodeId),
                    null,
                    null,
                    sortOrder ?: "${DatabaseContract.SetEdgeEntry.COLUMN_ORD} ASC"
                )
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            NODES, NODES_BY_SET, NODES_WITH_SONGS, START_NODES, END_NODES, NEXT_NODES, PREVIOUS_NODES ->
                "vnd.android.cursor.dir/vnd.$AUTHORITY.nodes"
            NODE_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.nodes"
            EDGES, EDGES_BY_SET, OUTGOING_EDGES, INCOMING_EDGES ->
                "vnd.android.cursor.dir/vnd.$AUTHORITY.edges"
            EDGE_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.edges"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        val id: Long
        val resultUri: Uri

        when (uriMatcher.match(uri)) {
            NODES -> {
                id = db.insert(DatabaseContract.SetNodeEntry.TABLE_NAME, null, values)
                resultUri = ContentUris.withAppendedId(NODES_URI, id)
            }
            EDGES -> {
                id = db.insert(DatabaseContract.SetEdgeEntry.TABLE_NAME, null, values)
                resultUri = ContentUris.withAppendedId(EDGES_URI, id)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        if (id > 0) {
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
            NODES -> db.update(
                DatabaseContract.SetNodeEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )
            NODE_ID -> {
                val id = ContentUris.parseId(uri)
                db.update(
                    DatabaseContract.SetNodeEntry.TABLE_NAME,
                    values,
                    "${DatabaseContract.SetNodeEntry.COLUMN_ID} = ?",
                    arrayOf(id.toString())
                )
            }
            EDGES -> db.update(
                DatabaseContract.SetEdgeEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )
            EDGE_ID -> {
                val id = ContentUris.parseId(uri)
                db.update(
                    DatabaseContract.SetEdgeEntry.TABLE_NAME,
                    values,
                    "${DatabaseContract.SetEdgeEntry.COLUMN_ID} = ?",
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
            NODES -> db.delete(DatabaseContract.SetNodeEntry.TABLE_NAME, selection, selectionArgs)
            NODE_ID -> {
                val id = ContentUris.parseId(uri)
                db.delete(
                    DatabaseContract.SetNodeEntry.TABLE_NAME,
                    "${DatabaseContract.SetNodeEntry.COLUMN_ID} = ?",
                    arrayOf(id.toString())
                )
            }
            EDGES -> db.delete(DatabaseContract.SetEdgeEntry.TABLE_NAME, selection, selectionArgs)
            EDGE_ID -> {
                val id = ContentUris.parseId(uri)
                db.delete(
                    DatabaseContract.SetEdgeEntry.TABLE_NAME,
                    "${DatabaseContract.SetEdgeEntry.COLUMN_ID} = ?",
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

