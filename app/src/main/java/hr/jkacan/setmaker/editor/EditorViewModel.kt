package hr.jkacan.setmaker.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.jkacan.setmaker.data.dao.SetGraphRepository
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.computeGraphLayout
import hr.jkacan.setmaker.models.song.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel(
    private val setId: Int,
    private val setGraphRepository: SetGraphRepository,
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
        // Use transaction-based swap to avoid cascade deletion issues
        setGraphRepository.swapNodesTransaction(setId, node1Id, node2Id)

        // Refresh the graph
        loadGraph()
    }

    fun insertNodeBetween(draggedId: Int, fromId: Int, toId: Int) {
        // Use transaction-based insert to avoid cascade deletion issues
        setGraphRepository.insertNodeBetweenTransaction(setId, draggedId, fromId, toId)

        // Refresh the graph
        loadGraph()
    }

    /**
     * Deletes a node from the graph and reconnects surrounding edges.
     * The first node (start node) cannot be deleted.
     * Children nodes are linked to the deleted node's parent.
     */
    fun deleteNode(nodeId: Int) {
        viewModelScope.launch {
            try {
                // Check if this is the first node (start node)
                val startNodes = setGraphRepository.getStartNodes(setId)
                val isFirstNode = startNodes.any { it.node.id == nodeId }

                if (isFirstNode) {
                    // Cannot delete the first node
                    return@launch
                }

                // Delete the node and reconnect edges
                setGraphRepository.deleteNodeTransaction(setId, nodeId)

                // Refresh the graph
                loadGraph()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Checks if a node can be deleted (i.e., it's not the first node).
     */
    fun canDeleteNode(nodeId: Int): Boolean {
        val startNodes = setGraphRepository.getStartNodes(setId)
        return !startNodes.any { it.node.id == nodeId }
    }

    /**
     * Connects a leaf node to another existing node by creating an edge.
     * This is used when dragging a leaf edge line onto another node to merge branches.
     */
    fun connectLeafToNode(leafNodeId: Int, targetNodeId: Int) {
        viewModelScope.launch {
            try {
                // Create edge from leaf node to target node
                setGraphRepository.insertEdge(
                    setId = setId,
                    fromNodeId = leafNodeId,
                    toNodeId = targetNodeId
                )

                // Refresh the graph
                loadGraph()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
