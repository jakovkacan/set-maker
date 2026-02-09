package hr.jkacan.setmaker.data.dao

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.core.net.toUri
import hr.jkacan.setmaker.data.provider.SetGraphContentProvider
import hr.jkacan.setmaker.models.set.SetEdge
import hr.jkacan.setmaker.models.set.SetGraphPath
import hr.jkacan.setmaker.models.set.SetNode
import hr.jkacan.setmaker.models.set.SetNodeWithSong
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.utils.parseStringAsDate

class SetGraphRepository(private val context: Context) {
    private val contentResolver = context.contentResolver
    private val dbHelper = DatabaseHelper(context) // Still needed for transactions

    // ============ NODE OPERATIONS ============

    fun insertNode(setId: Int, songId: Int, note: String? = null): Long {
        val values = ContentValues().apply {
            put(DatabaseContract.SetNodeEntry.COLUMN_SET_ID, setId)
            put(DatabaseContract.SetNodeEntry.COLUMN_SONG_ID, songId)
            put(DatabaseContract.SetNodeEntry.COLUMN_NOTE, note)
        }
        val uri = contentResolver.insert(SetGraphContentProvider.NODES_URI, values)
        return uri?.let { ContentUris.parseId(it) } ?: -1
    }

    fun updateNode(nodeId: Int, note: String?): Int {
        val values = ContentValues().apply {
            put(DatabaseContract.SetNodeEntry.COLUMN_NOTE, note)
        }
        val uri = ContentUris.withAppendedId(SetGraphContentProvider.NODES_URI, nodeId.toLong())
        return contentResolver.update(uri, values, null, null)
    }

    fun deleteNode(nodeId: Int): Int {
        val uri = ContentUris.withAppendedId(SetGraphContentProvider.NODES_URI, nodeId.toLong())
        return contentResolver.delete(uri, null, null)
    }

    fun getNodeById(nodeId: Int): SetNode? {
        val uri = ContentUris.withAppendedId(SetGraphContentProvider.NODES_URI, nodeId.toLong())
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) cursorToNode(it) else null
        }
    }

    fun getNodesBySet(setId: Int): List<SetNode> {
        val nodes = mutableListOf<SetNode>()
        val uri = "content://${SetGraphContentProvider.AUTHORITY}/nodes/set/$setId".toUri()
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                nodes.add(cursorToNode(it))
            }
        }

        return nodes
    }

    fun getNodesWithSongsBySet(setId: Int): List<SetNodeWithSong> {
        val nodesWithSongs = mutableListOf<SetNodeWithSong>()
        val uri = "content://${SetGraphContentProvider.AUTHORITY}/nodes/set/$setId/with_songs".toUri()
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
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
        val values = ContentValues().apply {
            put(DatabaseContract.SetEdgeEntry.COLUMN_SET_ID, setId)
            put(DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID, fromNodeId)
            put(DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID, toNodeId)
            put(DatabaseContract.SetEdgeEntry.COLUMN_ORD, ord)
            put(DatabaseContract.SetEdgeEntry.COLUMN_KIND, kind)
        }
        val uri = contentResolver.insert(SetGraphContentProvider.EDGES_URI, values)
        return uri?.let { ContentUris.parseId(it) } ?: -1
    }

    fun updateEdge(edgeId: Int, ord: Int, kind: String?): Int {
        val values = ContentValues().apply {
            put(DatabaseContract.SetEdgeEntry.COLUMN_ORD, ord)
            put(DatabaseContract.SetEdgeEntry.COLUMN_KIND, kind)
        }
        val uri = ContentUris.withAppendedId(SetGraphContentProvider.EDGES_URI, edgeId.toLong())
        return contentResolver.update(uri, values, null, null)
    }

    fun deleteEdge(edgeId: Int): Int {
        val uri = ContentUris.withAppendedId(SetGraphContentProvider.EDGES_URI, edgeId.toLong())
        return contentResolver.delete(uri, null, null)
    }

    fun deleteEdgeBetweenNodes(setId: Int, fromNodeId: Int, toNodeId: Int): Int {
        val selection = "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ? AND " +
                "${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} = ? AND " +
                "${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID} = ?"
        val selectionArgs = arrayOf(setId.toString(), fromNodeId.toString(), toNodeId.toString())
        return contentResolver.delete(SetGraphContentProvider.EDGES_URI, selection, selectionArgs)
    }

    /**
     * Swaps two nodes by swapping all their edges in a single transaction.
     * This avoids cascade deletion issues by collecting all edge data first,
     * then deleting and recreating edges atomically.
     * Uses direct database access within transaction to avoid ANR.
     */
    fun swapNodesTransaction(setId: Int, node1Id: Int, node2Id: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Query all edges for the set directly from database
            val cursor = db.query(
                DatabaseContract.SetEdgeEntry.TABLE_NAME,
                null,
                "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ?",
                arrayOf(setId.toString()),
                null,
                null,
                null
            )

            val edges = mutableListOf<SetEdge>()
            cursor.use {
                while (it.moveToNext()) {
                    edges.add(cursorToEdge(it))
                }
            }

            // Find edges connected to node1 (excluding edges between node1 and node2)
            val node1IncomingEdges = edges.filter { it.toNodeId == node1Id && it.fromNodeId != node2Id }
            val node1OutgoingEdges = edges.filter { it.fromNodeId == node1Id && it.toNodeId != node2Id }

            // Find edges connected to node2 (excluding edges between node2 and node1)
            val node2IncomingEdges = edges.filter { it.toNodeId == node2Id && it.fromNodeId != node1Id }
            val node2OutgoingEdges = edges.filter { it.fromNodeId == node2Id && it.toNodeId != node1Id }

            // Find edge between node1 and node2 (if any)
            val edge1to2 = edges.firstOrNull { it.fromNodeId == node1Id && it.toNodeId == node2Id }
            val edge2to1 = edges.firstOrNull { it.fromNodeId == node2Id && it.toNodeId == node1Id }

            // Delete all edges connected to both nodes using direct SQL
            val edgesToDelete = (node1IncomingEdges + node1OutgoingEdges + node2IncomingEdges + node2OutgoingEdges)
                .map { it.id }
                .distinct()
                .toMutableList()

            edge1to2?.let { edgesToDelete.add(it.id) }
            edge2to1?.let { edgesToDelete.add(it.id) }

            for (edgeId in edgesToDelete) {
                db.delete(
                    DatabaseContract.SetEdgeEntry.TABLE_NAME,
                    "${DatabaseContract.SetEdgeEntry.COLUMN_ID} = ?",
                    arrayOf(edgeId.toString())
                )
            }

            // Helper function to insert edge directly into database
            fun insertEdgeDirect(fromId: Int, toId: Int, ord: Int, kind: String?) {
                val values = ContentValues().apply {
                    put(DatabaseContract.SetEdgeEntry.COLUMN_SET_ID, setId)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID, fromId)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID, toId)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_ORD, ord)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_KIND, kind)
                }
                db.insert(DatabaseContract.SetEdgeEntry.TABLE_NAME, null, values)
            }

            // Recreate edges with swapped node IDs
            node1IncomingEdges.forEach { edge ->
                insertEdgeDirect(edge.fromNodeId, node2Id, edge.ord, edge.kind)
            }

            node1OutgoingEdges.forEach { edge ->
                insertEdgeDirect(node2Id, edge.toNodeId, edge.ord, edge.kind)
            }

            node2IncomingEdges.forEach { edge ->
                insertEdgeDirect(edge.fromNodeId, node1Id, edge.ord, edge.kind)
            }

            node2OutgoingEdges.forEach { edge ->
                insertEdgeDirect(node1Id, edge.toNodeId, edge.ord, edge.kind)
            }

            // Recreate edge between nodes (swapped)
            edge1to2?.let { edge ->
                insertEdgeDirect(node2Id, node1Id, edge.ord, edge.kind)
            }

            edge2to1?.let { edge ->
                insertEdgeDirect(node1Id, node2Id, edge.ord, edge.kind)
            }

            db.setTransactionSuccessful()

            // Notify ContentResolver of changes after transaction completes
            context.contentResolver.notifyChange(SetGraphContentProvider.EDGES_URI, null)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Inserts a node between two nodes in a single transaction.
     * This avoids cascade deletion issues.
     * Uses direct database access within transaction to avoid ANR.
     */
    fun insertNodeBetweenTransaction(setId: Int, draggedId: Int, fromId: Int, toId: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Get all edges for the set directly from database
            val cursor = db.query(
                DatabaseContract.SetEdgeEntry.TABLE_NAME,
                null,
                "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ?",
                arrayOf(setId.toString()),
                null,
                null,
                null
            )

            val edges = mutableListOf<SetEdge>()
            cursor.use {
                while (it.moveToNext()) {
                    edges.add(cursorToEdge(it))
                }
            }

            // Find edges connected to the dragged node
            val draggedIncomingEdges = edges.filter { it.toNodeId == draggedId }
            val draggedOutgoingEdges = edges.filter { it.fromNodeId == draggedId }

            // Store edge data before deletion
            val draggedIncomingData = draggedIncomingEdges.map { Triple(it.fromNodeId, it.ord, it.kind) }
            val draggedOutgoingData = draggedOutgoingEdges.map { Triple(it.toNodeId, it.ord, it.kind) }

            // Helper function to delete edge directly from database
            fun deleteEdgeDirect(edgeId: Int) {
                db.delete(
                    DatabaseContract.SetEdgeEntry.TABLE_NAME,
                    "${DatabaseContract.SetEdgeEntry.COLUMN_ID} = ?",
                    arrayOf(edgeId.toString())
                )
            }

            // Helper function to insert edge directly into database
            fun insertEdgeDirect(fromNodeId: Int, toNodeId: Int, ord: Int = 0, kind: String? = null) {
                val values = ContentValues().apply {
                    put(DatabaseContract.SetEdgeEntry.COLUMN_SET_ID, setId)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID, fromNodeId)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID, toNodeId)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_ORD, ord)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_KIND, kind)
                }
                db.insert(DatabaseContract.SetEdgeEntry.TABLE_NAME, null, values)
            }

            // Remove the dragged node from its current position
            draggedIncomingEdges.forEach { deleteEdgeDirect(it.id) }
            draggedOutgoingEdges.forEach { deleteEdgeDirect(it.id) }

            // Delete the edge between fromNode and toNode
            db.delete(
                DatabaseContract.SetEdgeEntry.TABLE_NAME,
                "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ? AND " +
                        "${DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID} = ? AND " +
                        "${DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID} = ?",
                arrayOf(setId.toString(), fromId.toString(), toId.toString())
            )

            // Reconnect orphaned nodes from dragged node's previous position
            // If dragged node had incoming edges, connect them to its outgoing targets
            if (draggedIncomingData.isNotEmpty() && draggedOutgoingData.isNotEmpty()) {
                for (incoming in draggedIncomingData) {
                    for (outgoing in draggedOutgoingData) {
                        insertEdgeDirect(incoming.first, outgoing.first, outgoing.second, outgoing.third)
                    }
                }
            }

            // Insert the dragged node between fromNode and toNode
            insertEdgeDirect(fromId, draggedId)
            insertEdgeDirect(draggedId, toId)

            db.setTransactionSuccessful()

            // Notify ContentResolver of changes after transaction completes
            context.contentResolver.notifyChange(SetGraphContentProvider.EDGES_URI, null)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Deletes a node and reconnects its parent to all its children in a single transaction.
     * This preserves the graph structure by maintaining connectivity.
     * For branching situations, all children are linked to the parent node.
     * Uses direct database access within transaction to avoid ANR.
     */
    fun deleteNodeTransaction(setId: Int, nodeId: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Get all edges for the set directly from database
            val cursor = db.query(
                DatabaseContract.SetEdgeEntry.TABLE_NAME,
                null,
                "${DatabaseContract.SetEdgeEntry.COLUMN_SET_ID} = ?",
                arrayOf(setId.toString()),
                null,
                null,
                null
            )

            val edges = mutableListOf<SetEdge>()
            cursor.use {
                while (it.moveToNext()) {
                    edges.add(cursorToEdge(it))
                }
            }

            // Find edges connected to the node being deleted
            val incomingEdges = edges.filter { it.toNodeId == nodeId }
            val outgoingEdges = edges.filter { it.fromNodeId == nodeId }

            // Store parent nodes data (nodes pointing to this node)
            val parentNodes = incomingEdges.map { Triple(it.fromNodeId, it.ord, it.kind) }

            // Store children nodes data (nodes this node points to)
            val childrenNodes = outgoingEdges.map { Triple(it.toNodeId, it.ord, it.kind) }

            // Helper function to delete edge directly from database
            fun deleteEdgeDirect(edgeId: Int) {
                db.delete(
                    DatabaseContract.SetEdgeEntry.TABLE_NAME,
                    "${DatabaseContract.SetEdgeEntry.COLUMN_ID} = ?",
                    arrayOf(edgeId.toString())
                )
            }

            // Helper function to insert edge directly into database
            fun insertEdgeDirect(fromNodeId: Int, toNodeId: Int, ord: Int = 0, kind: String? = null) {
                val values = ContentValues().apply {
                    put(DatabaseContract.SetEdgeEntry.COLUMN_SET_ID, setId)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_FROM_NODE_ID, fromNodeId)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_TO_NODE_ID, toNodeId)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_ORD, ord)
                    put(DatabaseContract.SetEdgeEntry.COLUMN_KIND, kind)
                }
                db.insert(DatabaseContract.SetEdgeEntry.TABLE_NAME, null, values)
            }

            // Delete all edges connected to the node
            incomingEdges.forEach { deleteEdgeDirect(it.id) }
            outgoingEdges.forEach { deleteEdgeDirect(it.id) }

            // Reconnect parent nodes to children nodes
            // For each parent, create edges to all children
            for (parent in parentNodes) {
                for (child in childrenNodes) {
                    insertEdgeDirect(
                        parent.first,
                        child.first,
                        child.second, // Preserve the child's edge order
                        child.third   // Preserve the child's edge kind
                    )
                }
            }

            // Delete the node itself using direct database access
            db.delete(
                DatabaseContract.SetNodeEntry.TABLE_NAME,
                "${DatabaseContract.SetNodeEntry.COLUMN_ID} = ?",
                arrayOf(nodeId.toString())
            )

            db.setTransactionSuccessful()

            // Notify ContentResolver of changes after transaction completes
            context.contentResolver.notifyChange(SetGraphContentProvider.EDGES_URI, null)
            context.contentResolver.notifyChange(SetGraphContentProvider.NODES_URI, null)
        } finally {
            db.endTransaction()
        }
    }

    fun getEdgesBySet(setId: Int): List<SetEdge> {
        val edges = mutableListOf<SetEdge>()
        val uri = "content://${SetGraphContentProvider.AUTHORITY}/edges/set/$setId".toUri()
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                edges.add(cursorToEdge(it))
            }
        }

        return edges
    }

    fun getOutgoingEdges(setId: Int, fromNodeId: Int): List<SetEdge> {
        val edges = mutableListOf<SetEdge>()
        val uri = "content://${SetGraphContentProvider.AUTHORITY}/edges/set/$setId/outgoing/$fromNodeId".toUri()
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                edges.add(cursorToEdge(it))
            }
        }

        return edges
    }

    fun getIncomingEdges(setId: Int, toNodeId: Int): List<SetEdge> {
        val edges = mutableListOf<SetEdge>()
        val uri = "content://${SetGraphContentProvider.AUTHORITY}/edges/set/$setId/incoming/$toNodeId".toUri()
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                edges.add(cursorToEdge(it))
            }
        }

        return edges
    }

    // ============ GRAPH TRAVERSAL OPERATIONS ============

    fun getStartNodes(setId: Int): List<SetNodeWithSong> {
        val startNodes = mutableListOf<SetNodeWithSong>()
        val uri = "content://${SetGraphContentProvider.AUTHORITY}/nodes/set/$setId/start".toUri()
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                startNodes.add(cursorToNodeWithSong(it))
            }
        }

        return startNodes
    }

    fun getEndNodes(setId: Int): List<SetNodeWithSong> {
        val endNodes = mutableListOf<SetNodeWithSong>()
        val uri = "content://${SetGraphContentProvider.AUTHORITY}/nodes/set/$setId/end".toUri()
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                endNodes.add(cursorToNodeWithSong(it))
            }
        }

        return endNodes
    }

    fun getNextNodes(setId: Int, currentNodeId: Int): List<SetNodeWithSong> {
        val nextNodes = mutableListOf<SetNodeWithSong>()
        val uri = "content://${SetGraphContentProvider.AUTHORITY}/nodes/set/$setId/next/$currentNodeId".toUri()
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                nextNodes.add(cursorToNodeWithSong(it))
            }
        }

        return nextNodes
    }

    fun getPreviousNodes(setId: Int, currentNodeId: Int): List<SetNodeWithSong> {
        val prevNodes = mutableListOf<SetNodeWithSong>()
        val uri = "content://${SetGraphContentProvider.AUTHORITY}/nodes/set/$setId/previous/$currentNodeId".toUri()
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
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
