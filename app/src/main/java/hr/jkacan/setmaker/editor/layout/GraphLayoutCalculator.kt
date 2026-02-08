package hr.jkacan.setmaker.editor.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import hr.jkacan.setmaker.data.state.EditorState
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode
import hr.jkacan.setmaker.models.set.SetEdge
import hr.jkacan.setmaker.models.set.SetNode
import hr.jkacan.setmaker.models.song.Song
import kotlin.collections.forEach

object GraphLayoutCalculator {
    fun computeGraphLayout(
        nodes: List<SetNode>,
        edges: List<SetEdge>,
        songs: List<Song>
    ): EditorState {
        // Create a map of song ID to Song for quick lookup
        val songMap = songs.associateBy { it.id }

        // Build adjacency list
        val adjacencyList = mutableMapOf<Int, MutableList<Int>>()
        val inDegree = mutableMapOf<Int, Int>()

        nodes.forEach { node ->
            adjacencyList[node.id] = mutableListOf()
            inDegree[node.id] = 0
        }

        edges.forEach { edge ->
            adjacencyList[edge.fromNodeId]?.add(edge.toNodeId)
            inDegree[edge.toNodeId] = (inDegree[edge.toNodeId] ?: 0) + 1
        }

        // Find starting node (node with in-degree 0)
        val startNodes = nodes.filter { (inDegree[it.id] ?: 0) == 0 }

        // Compute row (layer) for each node using topological sorting
        val nodeRows = mutableMapOf<Int, Int>()
        val nodeColumns = mutableMapOf<Int, Int>()
        val queue = ArrayDeque<Pair<Int, Int>>() // (nodeId, row)

        // Initialize with start nodes at row 0, center column
        startNodes.forEach { node ->
            queue.add(Pair(node.id, 0))
            nodeRows[node.id] = 0
            nodeColumns[node.id] = 0 // Start in center
        }

        val processed = mutableSetOf<Int>()
        val tempInDegree = inDegree.toMutableMap()

        while (queue.isNotEmpty()) {
            val (nodeId, row) = queue.removeFirst()

            if (nodeId in processed) continue
            processed.add(nodeId)

            val children = adjacencyList[nodeId] ?: emptyList()

            if (children.isNotEmpty()) {
                // Assign columns to children
                val numChildren = children.size
                val parentCol = nodeColumns[nodeId] ?: 0

                children.forEachIndexed { index, childId ->
                    val childRow = row + 1

                    // Calculate column offset for branching
                    val columnOffset = if (numChildren == 1) {
                        parentCol
                    } else {
                        // For even numbers: spread around parent (-0.5, 0.5, -1.5, 1.5, etc.)
                        // For odd numbers: spread around parent (0, -1, 1, -2, 2, etc.)
                        if (numChildren % 2 == 0) {
                            // Even: alternate left and right, starting right
                            val step = (index + 1) / 2
                            if (index % 2 == 0) {
                                parentCol + step
                            } else {
                                parentCol - step
                            }
                        } else {
                            // Odd: center child at parent's column
                            val centerIndex = numChildren / 2
                            parentCol + (index - centerIndex)
                        }
                    }

                    // Only update if not yet assigned or if this gives a later row
                    if (childId !in nodeRows || nodeRows[childId]!! < childRow) {
                        nodeRows[childId] = childRow
                        nodeColumns[childId] = columnOffset
                    }

                    tempInDegree[childId] = (tempInDegree[childId] ?: 0) - 1
                    if (tempInDegree[childId] == 0) {
                        queue.add(Pair(childId, childRow))
                    }
                }
            }
        }

        // Create UiNodes
        val uiNodes = nodes.mapNotNull { node ->
            val song = songMap[node.songId]
            if (song != null) {
                UiNode(
                    id = node.id,
                    col = nodeColumns[node.id] ?: 0,
                    row = nodeRows[node.id] ?: 0,
                    song = song
                )
            } else null
        }.associateBy { it.id }

        // Create UiEdges
        val uiEdges = edges.map { edge ->
            UiEdge(
                fromId = edge.fromNodeId,
                toId = edge.toNodeId
            )
        }

        return EditorState(
            nodes = uiNodes,
            edges = uiEdges,
            pan = Offset.Zero,
            zoom = 1f
        )
    }

    fun calculateNodePosition(
        node: UiNode,
        canvasWidth: Float,
        horizontalSpacing: Float,
        verticalSpacing: Float,
        pan: Offset = Offset.Zero,
        zoom: Float = 1f
    ): Offset {
        val nodeWidth = 120f

        // Calculate horizontal position with centering
        // Center the node at canvas center, then apply column offset
        // Subtract half node width to center the node on its position
        val baseX = canvasWidth / 2f
        val x = baseX + (node.col * horizontalSpacing) - (nodeWidth / 2f)

        // Calculate vertical position
        val y = 100f + (node.row * verticalSpacing)

        return Offset(x, y)
    }

    @Composable
    fun calculateNodePositionDp(node: UiNode, pan: Offset, zoom: Float): Offset {
        val density = LocalDensity.current
        val horizontalSpacing = with(density) { 180.dp.toPx() }
        val verticalSpacing = with(density) { 220.dp.toPx() }
        val configuration = LocalConfiguration.current
        val canvasWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

        return calculateNodePosition(
            node,
            canvasWidth,
            horizontalSpacing,
            verticalSpacing,
            pan,
            zoom
        )
    }
}