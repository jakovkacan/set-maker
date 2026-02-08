package hr.jkacan.setmaker.editor.layout

import androidx.compose.ui.geometry.Offset
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode

class HitDetector(
    private val nodes: List<UiNode>,
    private val edges: List<UiEdge>,
    private val canvasWidth: Float,
    private val nodeWidth: Float,
    private val nodeHeight: Float,
    private val horizontalSpacing: Float,
    private val verticalSpacing: Float,
) {
    fun findHoveredNode(worldPos: Offset, excludeNodeId: Int): Int? {
        for (node in nodes) {
            if (node.id == excludeNodeId) continue

            val nodePos = GraphLayoutCalculator.calculateNodePosition(
                node, canvasWidth, horizontalSpacing, verticalSpacing
            )

            if (worldPos.x >= nodePos.x && worldPos.x <= nodePos.x + nodeWidth &&
                worldPos.y >= nodePos.y && worldPos.y <= nodePos.y + nodeHeight
            ) {
                return node.id
            }
        }
        return null
    }

    fun findHoveredEdge(worldPos: Offset, hitRadius: Float = 60f): Pair<Int, Int>? {
        for (edge in edges) {
            val fromNode = nodes.find { it.id == edge.fromId } ?: continue
            val toNode = nodes.find { it.id == edge.toId } ?: continue

            val fromPos = GraphLayoutCalculator.calculateNodePosition(
                fromNode, canvasWidth, horizontalSpacing, verticalSpacing
            )
            val toPos = GraphLayoutCalculator.calculateNodePosition(
                toNode, canvasWidth, horizontalSpacing, verticalSpacing
            )

            val midX = (fromPos.x + toPos.x) / 2f + nodeWidth / 2f
            val midY = (fromPos.y + nodeHeight + toPos.y) / 2f

            val dx = worldPos.x - midX
            val dy = worldPos.y - midY

            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                return Pair(edge.fromId, edge.toId)
            }
        }
        return null
    }
}