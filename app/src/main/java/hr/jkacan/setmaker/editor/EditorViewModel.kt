package hr.jkacan.setmaker.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.jkacan.setmaker.data.dao.SetGraphRepository
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.data.state.EditorState
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.computeGraphLayout
import hr.jkacan.setmaker.models.song.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel(
    private val setId: Int,
    private val setGraphRepository: SetGraphRepository,
    private val songRepository: SongRepository
) : ViewModel() {

    private val _graphState = MutableStateFlow<EditorState?>(null)
    val graphState: StateFlow<EditorState?> = _graphState.asStateFlow()

    init {
        loadGraph()
    }

    /**
     * Loads the graph data from the database and computes layout
     */
    fun loadGraph() {
        viewModelScope.launch {
            try {
                // Load nodes with songs and edges from database
                val nodesWithSongs = setGraphRepository.getNodesWithSongsBySet(setId)
                val edges = setGraphRepository.getEdgesBySet(setId)

                // Convert to domain models
                val setNodes = nodesWithSongs.map { it.node }
                val songs = nodesWithSongs.associate { it.node.songId to it.song }

                // Compute layout
                val newState = if (setNodes.isEmpty()) {
                    // Empty graph state
                    EditorState(
                        nodes = emptyMap(),
                        edges = emptyList()
                    )
                } else {
                    computeGraphLayout(
                        nodes = setNodes,
                        edges = edges,
                        songs = songs.values.toList()
                    )
                }

                _graphState.value = newState
            } catch (e: Exception) {
                // Handle error - could emit error state or log
                e.printStackTrace()
            }
        }
    }

    /**
     * Gets the current set ID
     */
    fun getSetId(): Int = setId

    /**
     * Checks if the graph is empty (no nodes)
     */
    fun isGraphEmpty(): Boolean {
        return _graphState.value?.nodes?.isEmpty() ?: true
    }

    /**
     * Adds first node to an empty graph
     */
    fun addFirstNode(song: Song) {
        // Validate song has valid ID
        val songId = song.id ?: 0
        if (songId <= 0) {
            return
        }

        // Insert the first node
        val newNodeId = setGraphRepository.insertNode(
            setId = setId,
            songId = songId,
            note = null
        )

        if (newNodeId > 0) {
            // Reload graph to show the new node
            loadGraph()
        }
    }

    fun addNode(song: Song, fromId: Int, toId: Int?) {
        // Validate song has valid ID
        val songId = song.id ?: 0
        if (songId <= 0) {
            return
        }

        // Insert new node
        val newNodeId = setGraphRepository.insertNode(
            setId = setId,
            songId = songId,
            note = null
        )

        if (newNodeId > 0) {
            if (toId == null) {
                // Adding to a leaf node (no toNodeId)
                // Create edge from fromNodeId to newNodeId
                setGraphRepository.insertEdge(
                    setId = setId,
                    fromNodeId = fromId,
                    toNodeId = newNodeId.toInt()
                )
            } else {
                // Inserting between two nodes
                // 1. Delete the old edge from fromNodeId to toNodeId
                setGraphRepository.deleteEdgeBetweenNodes(setId, fromId, toId)

                // 2. Create edge from fromNodeId to newNodeId
                setGraphRepository.insertEdge(
                    setId = setId,
                    fromNodeId = fromId,
                    toNodeId = newNodeId.toInt()
                )

                // 3. Create edge from newNodeId to toNodeId
                setGraphRepository.insertEdge(
                    setId = setId,
                    fromNodeId = newNodeId.toInt(),
                    toNodeId = toId
                )
            }

            // Refresh the editor to show the new node
            loadGraph()
        }
    }

    fun addBranchNode(song: Song, fromNodeId: Int) {
        // Validate song has valid ID
        val songId = song.id ?: 0
        if (songId <= 0) {
            return
        }

        // Insert new node
        val newNodeId = setGraphRepository.insertNode(
            setId = setId,
            songId = songId,
            note = null
        )

        if (newNodeId > 0) {
            // Create edge from parent to new branch node
            setGraphRepository.insertEdge(
                setId = setId,
                fromNodeId = fromNodeId,
                toNodeId = newNodeId.toInt()
            )

            // Refresh the editor to show the new branch
            loadGraph()
        }
    }

    fun swapNodes(node1Id: Int, node2Id: Int) {
        // Get all edges for the set
        val edges = setGraphRepository.getEdgesBySet(setId)

        // Find edges connected to node1
        val node1IncomingEdges = edges.filter { it.toNodeId == node1Id }
        val node1OutgoingEdges = edges.filter { it.fromNodeId == node1Id }

        // Find edges connected to node2
        val node2IncomingEdges = edges.filter { it.toNodeId == node2Id }
        val node2OutgoingEdges = edges.filter { it.fromNodeId == node2Id }

        // Delete all edges connected to both nodes
        node1IncomingEdges.forEach { setGraphRepository.deleteEdge(it.id) }
        node1OutgoingEdges.forEach { setGraphRepository.deleteEdge(it.id) }
        node2IncomingEdges.forEach { setGraphRepository.deleteEdge(it.id) }
        node2OutgoingEdges.forEach { setGraphRepository.deleteEdge(it.id) }

        // Recreate edges with swapped node IDs
        node1IncomingEdges.forEach { edge ->
            setGraphRepository.insertEdge(
                setId = setId,
                fromNodeId = edge.fromNodeId,
                toNodeId = node2Id,
                ord = edge.ord,
                kind = edge.kind
            )
        }

        node1OutgoingEdges.forEach { edge ->
            setGraphRepository.insertEdge(
                setId = setId,
                fromNodeId = node2Id,
                toNodeId = edge.toNodeId,
                ord = edge.ord,
                kind = edge.kind
            )
        }

        node2IncomingEdges.forEach { edge ->
            setGraphRepository.insertEdge(
                setId = setId,
                fromNodeId = edge.fromNodeId,
                toNodeId = node1Id,
                ord = edge.ord,
                kind = edge.kind
            )
        }

        node2OutgoingEdges.forEach { edge ->
            setGraphRepository.insertEdge(
                setId = setId,
                fromNodeId = node1Id,
                toNodeId = edge.toNodeId,
                ord = edge.ord,
                kind = edge.kind
            )
        }

        // Refresh the graph
        loadGraph()
    }

    fun insertNodeBetween(draggedId: Int, fromId: Int, toId: Int) {
        // Get all edges for the dragged node
        val edges = setGraphRepository.getEdgesBySet(setId)
        val draggedIncomingEdges = edges.filter { it.toNodeId == draggedId }
        val draggedOutgoingEdges = edges.filter { it.fromNodeId == draggedId }

        // Remove the dragged node from its current position
        draggedIncomingEdges.forEach { setGraphRepository.deleteEdge(it.id) }
        draggedOutgoingEdges.forEach { setGraphRepository.deleteEdge(it.id) }

        // Delete the edge between fromNode and toNode
        setGraphRepository.deleteEdgeBetweenNodes(setId, fromId, toId)

        // Insert the dragged node between fromNode and toNode
        setGraphRepository.insertEdge(
            setId = setId,
            fromNodeId = fromId,
            toNodeId = draggedId
        )

        setGraphRepository.insertEdge(
            setId = setId,
            fromNodeId = draggedId,
            toNodeId = toId
        )

        // Refresh the graph
        loadGraph()
    }
}
