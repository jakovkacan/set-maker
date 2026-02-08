package hr.jkacan.setmaker.data.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import hr.jkacan.setmaker.models.set.SetEdge
import hr.jkacan.setmaker.models.set.SetGraphPath
import hr.jkacan.setmaker.models.set.SetNode
import hr.jkacan.setmaker.models.set.SetNodeWithSong
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.utils.parseStringAsDate

class SetGraphRepository(private val context: Context) {
    private val dbHelper = DatabaseHelper(context)

    // ============ NODE OPERATIONS ============

    fun insertNode(setId: Int, songId: Int, note: String? = null): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.SetNodeEntry.COLUMN_SET_ID, setId)
            put(DatabaseContract.SetNodeEntry.COLUMN_SONG_ID, songId)
            put(DatabaseContract.SetNodeEntry.COLUMN_NOTE, note)
        }
        return db.insert(DatabaseContract.SetNodeEntry.TABLE_NAME, null, values)
    }

    fun updateNode(nodeId: Int, note: String?): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.SetNodeEntry.COLUMN_NOTE, note)
        }
        val selection = "${DatabaseContract.SetNodeEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(nodeId.toString())
        return db.update(
            DatabaseContract.SetNodeEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
    }

    fun deleteNode(nodeId: Int): Int {
        val db = dbHelper.writableDatabase
        val selection = "${DatabaseContract.SetNodeEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(nodeId.toString())
        return db.delete(
            DatabaseContract.SetNodeEntry.TABLE_NAME,
            selection,
            selectionArgs
        )
    }

    fun getNodeById(nodeId: Int): SetNode? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseContract.SetNodeEntry.TABLE_NAME,
            null,
            "${DatabaseContract.SetNodeEntry.COLUMN_ID} = ?",
            arrayOf(nodeId.toString()),
            null,
            null,
            null
        )

        return cursor.use {
            if (it.moveToFirst()) cursorToNode(it) else null
        }
    }

    fun getNodesBySet(setId: Int): List<SetNode> {
        val nodes = mutableListOf<SetNode>()
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            DatabaseContract.SetNodeEntry.TABLE_NAME,
            null,
            "${DatabaseContract.SetNodeEntry.COLUMN_SET_ID} = ?",
            arrayOf(setId.toString()),
            null,
            null,
            "${DatabaseContract.SetNodeEntry.COLUMN_ID} ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                nodes.add(cursorToNode(it))
            }
        }

        return nodes
    }

    fun getNodesWithSongsBySet(setId: Int): List<SetNodeWithSong> {
        val nodesWithSongs = mutableListOf<SetNodeWithSong>()
        val db = dbHelper.readableDatabase

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

        val cursor = db.rawQuery(query, arrayOf(setId.toString()))

        cursor.use {
            while (it.moveToNext()) {
                val node = SetNode(
                    id = it.getInt(it.getColumnIndexOrThrow("node_id")),
                    setId = it.getInt(it.getColumnIndexOrThrow(DatabaseContract.SetNodeEntry.COLUMN_SET_ID)),
                    songId = it.getInt(it.getColumnIndexOrThrow(DatabaseContract.SetNodeEntry.COLUMN_SONG_ID)),
                    note = it.getString(it.getColumnIndexOrThrow(DatabaseContract.SetNodeEntry.COLUMN_NOTE))
                )

                val song = Song(
                    id = it.getInt(it.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_ID)),
                    platformId = it.getString(it.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_PLATFORM_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_TITLE)),
                    artist = it.getString(it.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_ARTIST)),
                    coverUrl = it.getString(it.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_COVER_URL)),
                    provider = SongProvider.valueOf(
                        it.getString(it.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_PROVIDER))
                    ),
                    previewUrl = it.getString(it.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_PREVIEW_URL)),
                    songUrl = it.getString(it.getColumnIndexOrThrow(DatabaseContract.SongEntry.COLUMN_SONG_URL)),
                    dateAdded = parseStringAsDate(
                        it.getString(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.SongEntry.COLUMN_DATE_ADDED
                            )
                        )
                    )
                )

                nodesWithSongs.add(SetNodeWithSong(node, song))
            }
        }

        return nodesWithSongs
    }

    // ============ EDGE OPERATIONS ============

    fun insertEdge(
        setId: Int,
        fromNodeId: Int,
        toNodeId: Int,
        ord: Int = 0,
        kind: String? = null
    ): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.SetEdgeEntry.COLUMN_SET_ID, setId)
            put(DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID, fromNodeId)
            put(DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID, toNodeId)
            put(DatabaseContract.SetEdgeEntry.COLUMN_ORD, ord)
            put(DatabaseContract.SetEdgeEntry.COLUMN_KIND, kind)
        }
        return db.insertWithOnConflict(
            DatabaseContract.SetEdgeEntry.TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun updateEdge(edgeId: Int, ord: Int, kind: String?): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.SetEdgeEntry.COLUMN_ORD, ord)
            put(DatabaseContract.SetEdgeEntry.COLUMN_KIND, kind)
        }
        val selection = "${DatabaseContract.SetEdgeEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(edgeId.toString())
        return db.update(
            DatabaseContract.SetEdgeEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
    }

    fun deleteEdge(edgeId: Int): Int {
        val db = dbHelper.writableDatabase
        val selection = "${DatabaseContract.SetEdgeEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(edgeId.toString())
        return db.delete(
            DatabaseContract.SetEdgeEntry.TABLE_NAME,
            selection,
            selectionArgs
        )
    }

    fun deleteEdgeBetweenNodes(setId: Int, fromNodeId: Int, toNodeId: Int): Int {
        val db = dbHelper.writableDatabase
        val selection = "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ? AND " +
                "${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} = ? AND " +
                "${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID} = ?"
        val selectionArgs = arrayOf(setId.toString(), fromNodeId.toString(), toNodeId.toString())
        return db.delete(
            DatabaseContract.SetEdgeEntry.TABLE_NAME,
            selection,
            selectionArgs
        )
    }

    /**
     * Swaps two nodes by swapping all their edges in a single transaction.
     * This avoids cascade deletion issues by collecting all edge data first,
     * then deleting and recreating edges atomically.
     */
    fun swapNodesTransaction(setId: Int, node1Id: Int, node2Id: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Get all edges for the set
            val edges = getEdgesBySet(setId)

            // Find edges connected to node1 (excluding edges between node1 and node2)
            val node1IncomingEdges = edges.filter { it.toNodeId == node1Id && it.fromNodeId != node2Id }
            val node1OutgoingEdges = edges.filter { it.fromNodeId == node1Id && it.toNodeId != node2Id }

            // Find edges connected to node2 (excluding edges between node2 and node1)
            val node2IncomingEdges = edges.filter { it.toNodeId == node2Id && it.fromNodeId != node1Id }
            val node2OutgoingEdges = edges.filter { it.fromNodeId == node2Id && it.toNodeId != node1Id }

            // Find edge between node1 and node2 (if any)
            val edge1to2 = edges.firstOrNull { it.fromNodeId == node1Id && it.toNodeId == node2Id }
            val edge2to1 = edges.firstOrNull { it.fromNodeId == node2Id && it.toNodeId == node1Id }

            // Delete all edges connected to both nodes
            val edgesToDelete = (node1IncomingEdges + node1OutgoingEdges + node2IncomingEdges + node2OutgoingEdges)
                .map { it.id }
                .distinct()
                .toMutableList()

            edge1to2?.let { edgesToDelete.add(it.id) }
            edge2to1?.let { edgesToDelete.add(it.id) }

            for (edgeId in edgesToDelete) {
                deleteEdge(edgeId)
            }

            // Recreate edges with swapped node IDs
            node1IncomingEdges.forEach { edge ->
                insertEdge(
                    setId = setId,
                    fromNodeId = edge.fromNodeId,
                    toNodeId = node2Id,
                    ord = edge.ord,
                    kind = edge.kind
                )
            }

            node1OutgoingEdges.forEach { edge ->
                insertEdge(
                    setId = setId,
                    fromNodeId = node2Id,
                    toNodeId = edge.toNodeId,
                    ord = edge.ord,
                    kind = edge.kind
                )
            }

            node2IncomingEdges.forEach { edge ->
                insertEdge(
                    setId = setId,
                    fromNodeId = edge.fromNodeId,
                    toNodeId = node1Id,
                    ord = edge.ord,
                    kind = edge.kind
                )
            }

            node2OutgoingEdges.forEach { edge ->
                insertEdge(
                    setId = setId,
                    fromNodeId = node1Id,
                    toNodeId = edge.toNodeId,
                    ord = edge.ord,
                    kind = edge.kind
                )
            }

            // Recreate edge between nodes (swapped)
            edge1to2?.let { edge ->
                insertEdge(
                    setId = setId,
                    fromNodeId = node2Id,
                    toNodeId = node1Id,
                    ord = edge.ord,
                    kind = edge.kind
                )
            }

            edge2to1?.let { edge ->
                insertEdge(
                    setId = setId,
                    fromNodeId = node1Id,
                    toNodeId = node2Id,
                    ord = edge.ord,
                    kind = edge.kind
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Inserts a node between two nodes in a single transaction.
     * This avoids cascade deletion issues.
     */
    fun insertNodeBetweenTransaction(setId: Int, draggedId: Int, fromId: Int, toId: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Get all edges for the set
            val edges = getEdgesBySet(setId)

            // Find edges connected to the dragged node
            val draggedIncomingEdges = edges.filter { it.toNodeId == draggedId }
            val draggedOutgoingEdges = edges.filter { it.fromNodeId == draggedId }

            // Store edge data before deletion
            val draggedIncomingData = draggedIncomingEdges.map { Triple(it.fromNodeId, it.ord, it.kind) }
            val draggedOutgoingData = draggedOutgoingEdges.map { Triple(it.toNodeId, it.ord, it.kind) }

            // Remove the dragged node from its current position
            draggedIncomingEdges.forEach { deleteEdge(it.id) }
            draggedOutgoingEdges.forEach { deleteEdge(it.id) }

            // Delete the edge between fromNode and toNode
            deleteEdgeBetweenNodes(setId, fromId, toId)

            // Reconnect orphaned nodes from dragged node's previous position
            // If dragged node had incoming edges, connect them to its outgoing targets
            if (draggedIncomingData.isNotEmpty() && draggedOutgoingData.isNotEmpty()) {
                for (incoming in draggedIncomingData) {
                    for (outgoing in draggedOutgoingData) {
                        insertEdge(
                            setId = setId,
                            fromNodeId = incoming.first,
                            toNodeId = outgoing.first,
                            ord = outgoing.second,
                            kind = outgoing.third
                        )
                    }
                }
            }

            // Insert the dragged node between fromNode and toNode
            insertEdge(
                setId = setId,
                fromNodeId = fromId,
                toNodeId = draggedId
            )

            insertEdge(
                setId = setId,
                fromNodeId = draggedId,
                toNodeId = toId
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Deletes a node and reconnects its parent to all its children in a single transaction.
     * This preserves the graph structure by maintaining connectivity.
     * For branching situations, all children are linked to the parent node.
     */
    fun deleteNodeTransaction(setId: Int, nodeId: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Get all edges for the set
            val edges = getEdgesBySet(setId)

            // Find edges connected to the node being deleted
            val incomingEdges = edges.filter { it.toNodeId == nodeId }
            val outgoingEdges = edges.filter { it.fromNodeId == nodeId }

            // Store parent nodes data (nodes pointing to this node)
            val parentNodes = incomingEdges.map { Triple(it.fromNodeId, it.ord, it.kind) }

            // Store children nodes data (nodes this node points to)
            val childrenNodes = outgoingEdges.map { Triple(it.toNodeId, it.ord, it.kind) }

            // Delete all edges connected to the node
            incomingEdges.forEach { deleteEdge(it.id) }
            outgoingEdges.forEach { deleteEdge(it.id) }

            // Reconnect parent nodes to children nodes
            // For each parent, create edges to all children
            for (parent in parentNodes) {
                for (child in childrenNodes) {
                    insertEdge(
                        setId = setId,
                        fromNodeId = parent.first,
                        toNodeId = child.first,
                        ord = child.second, // Preserve the child's edge order
                        kind = child.third   // Preserve the child's edge kind
                    )
                }
            }

            // Delete the node itself
            deleteNode(nodeId)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getEdgesBySet(setId: Int): List<SetEdge> {
        val edges = mutableListOf<SetEdge>()
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            DatabaseContract.SetEdgeEntry.TABLE_NAME,
            null,
            "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ?",
            arrayOf(setId.toString()),
            null,
            null,
            "${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} ASC, ${DatabaseContract.SetEdgeEntry.COLUMN_ORD} ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                edges.add(cursorToEdge(it))
            }
        }

        return edges
    }

    fun getOutgoingEdges(setId: Int, fromNodeId: Int): List<SetEdge> {
        val edges = mutableListOf<SetEdge>()
        val db = dbHelper.readableDatabase

        val selection = "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ? AND " +
                "${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} = ?"
        val selectionArgs = arrayOf(setId.toString(), fromNodeId.toString())

        val cursor = db.query(
            DatabaseContract.SetEdgeEntry.TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "${DatabaseContract.SetEdgeEntry.COLUMN_ORD} ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                edges.add(cursorToEdge(it))
            }
        }

        return edges
    }

    fun getIncomingEdges(setId: Int, toNodeId: Int): List<SetEdge> {
        val edges = mutableListOf<SetEdge>()
        val db = dbHelper.readableDatabase

        val selection = "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ? AND " +
                "${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID} = ?"
        val selectionArgs = arrayOf(setId.toString(), toNodeId.toString())

        val cursor = db.query(
            DatabaseContract.SetEdgeEntry.TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "${DatabaseContract.SetEdgeEntry.COLUMN_ORD} ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                edges.add(cursorToEdge(it))
            }
        }

        return edges
    }

    // ============ GRAPH TRAVERSAL OPERATIONS ============

    fun getStartNodes(setId: Int): List<SetNodeWithSong> {
        val startNodes = mutableListOf<SetNodeWithSong>()
        val db = dbHelper.readableDatabase

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

        val cursor = db.rawQuery(query, arrayOf(setId.toString(), setId.toString()))

        cursor.use {
            while (it.moveToNext()) {
                startNodes.add(cursorToNodeWithSong(it))
            }
        }

        return startNodes
    }

    fun getEndNodes(setId: Int): List<SetNodeWithSong> {
        val endNodes = mutableListOf<SetNodeWithSong>()
        val db = dbHelper.readableDatabase

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

        val cursor = db.rawQuery(query, arrayOf(setId.toString(), setId.toString()))

        cursor.use {
            while (it.moveToNext()) {
                endNodes.add(cursorToNodeWithSong(it))
            }
        }

        return endNodes
    }

    fun getNextNodes(setId: Int, currentNodeId: Int): List<SetNodeWithSong> {
        val nextNodes = mutableListOf<SetNodeWithSong>()
        val db = dbHelper.readableDatabase

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

        val cursor = db.rawQuery(query, arrayOf(setId.toString(), currentNodeId.toString()))

        cursor.use {
            while (it.moveToNext()) {
                nextNodes.add(cursorToNodeWithSong(it))
            }
        }

        return nextNodes
    }

    fun getPreviousNodes(setId: Int, currentNodeId: Int): List<SetNodeWithSong> {
        val prevNodes = mutableListOf<SetNodeWithSong>()
        val db = dbHelper.readableDatabase

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

        val cursor = db.rawQuery(query, arrayOf(setId.toString(), currentNodeId.toString()))

        cursor.use {
            while (it.moveToNext()) {
                prevNodes.add(cursorToNodeWithSong(it))
            }
        }

        return prevNodes
    }

    fun getDefaultPath(setId: Int): SetGraphPath? {
        val nodes = mutableListOf<SetNodeWithSong>()
        val edges = mutableListOf<SetEdge>()

        // Get the first start node
        val startNodes = getStartNodes(setId)
        if (startNodes.isEmpty()) return null

        var currentNode: SetNodeWithSong? = startNodes.first()

        while (currentNode != null) {
            nodes.add(currentNode)

            // Get outgoing edges ordered by ord (0 = default)
            val outgoingEdges = getOutgoingEdges(setId, currentNode.node.id)
            if (outgoingEdges.isEmpty()) break

            // Take the edge with ord = 0 (default path)
            val defaultEdge = outgoingEdges.firstOrNull { it.ord == 0 } ?: outgoingEdges.first()
            edges.add(defaultEdge)

            // Get the next node
            val nextNodeId = defaultEdge.toNodeId
            val nextNode = getNodeById(nextNodeId)

            if (nextNode != null) {
                val nodesWithSongs = getNodesWithSongsBySet(setId)
                currentNode = nodesWithSongs.firstOrNull { it.node.id == nextNodeId }
            } else {
                break
            }
        }

        return SetGraphPath(nodes, edges)
    }

    // ============ HELPER METHODS ============

    private fun cursorToNode(cursor: Cursor): SetNode {
        return SetNode(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetNodeEntry.COLUMN_ID)),
            setId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetNodeEntry.COLUMN_SET_ID)),
            songId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetNodeEntry.COLUMN_SONG_ID)),
            note = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SetNodeEntry.COLUMN_NOTE))
        )
    }

    private fun cursorToEdge(cursor: Cursor): SetEdge {
        return SetEdge(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetEdgeEntry.COLUMN_ID)),
            setId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetEdgeEntry.COLUMN_SET_ID)),
            fromNodeId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID)),
            toNodeId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID)),
            ord = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetEdgeEntry.COLUMN_ORD)),
            kind = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SetEdgeEntry.COLUMN_KIND))
        )
    }

    private fun cursorToNodeWithSong(cursor: Cursor): SetNodeWithSong {
        val node = SetNode(
            id = cursor.getInt(cursor.getColumnIndexOrThrow("node_id")),
            setId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetNodeEntry.COLUMN_SET_ID)),
            songId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SetNodeEntry.COLUMN_SONG_ID)),
            note = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SetNodeEntry.COLUMN_NOTE))
        )

        val song = Song(
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

        return SetNodeWithSong(node, song)
    }

    fun close() {
        dbHelper.close()
    }
}
