package hr.jkacan.setmaker.editor.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.calculateNodePosition
import hr.jkacan.setmaker.editor.layout.HORIZONTAL_SPACING
import hr.jkacan.setmaker.editor.layout.NODE_HEIGHT
import hr.jkacan.setmaker.editor.layout.NODE_WIDTH
import hr.jkacan.setmaker.editor.layout.VERTICAL_SPACING
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode
import kotlin.collections.forEach
import kotlin.math.roundToInt

@Composable
fun EdgeLayer(
    edges: List<UiEdge>,
    nodes: Map<Int, UiNode>,
    debugMode: Boolean = false,
    highlightedEdge: Pair<Int, Int>? = null
) {
    val density = LocalDensity.current

    Canvas(modifier = Modifier.fillMaxSize()) {
        val nodeWidth = NODE_WIDTH.toPx()
        val nodeHeight = NODE_HEIGHT.toPx()
        val horizontalSpacing = HORIZONTAL_SPACING.toPx()
        val verticalSpacing = VERTICAL_SPACING.toPx()

        // Draw regular edges
        edges.forEach { edge ->
            val fromNode = nodes[edge.fromId]
            val toNode = nodes[edge.toId]

            if (fromNode != null && toNode != null) {
                val isHighlighted = highlightedEdge?.let {
                    it.first == edge.fromId && it.second == edge.toId
                } ?: false

                drawEdge(
                    fromNode = fromNode,
                    toNode = toNode,
                    nodeWidth = nodeWidth,
                    nodeHeight = nodeHeight,
                    horizontalSpacing = horizontalSpacing,
                    verticalSpacing = verticalSpacing,
                    canvasWidth = size.width,
                    isHighlighted = isHighlighted
                )
            }
        }

        // Find leaf nodes (nodes with no outgoing edges)
        val nodesWithChildren = edges.map { it.fromId }.toSet()
        val leafNodes = nodes.values.filter { it.id !in nodesWithChildren }

        // Draw edges for leaf nodes
        leafNodes.forEach { leafNode ->
            val fromPos = calculateNodePosition(
                leafNode,
                size.width,
                horizontalSpacing,
                verticalSpacing
            )

            // Start from bottom center of leaf node
            val start = Offset(
                fromPos.x + nodeWidth / 2,
                fromPos.y + nodeHeight
            )

            // End point is straight down, 100dp below
            val edgeLength = 100.dp.toPx()
            val end = Offset(
                start.x,
                start.y + edgeLength
            )

            // Draw straight vertical line
            drawLine(
                color = Color(0xFF888888),
                start = start,
                end = end,
                strokeWidth = 4f
            )

            // Draw arrow at the end
            val arrowSize = 20f
            val angle = Math.PI / 2 // 90 degrees (pointing down)
            val arrowAngle1 = angle + Math.PI * 5 / 6
            val arrowAngle2 = angle - Math.PI * 5 / 6

            val arrowPoint1 = Offset(
                x = end.x + (arrowSize * kotlin.math.cos(arrowAngle1)).toFloat(),
                y = end.y + (arrowSize * kotlin.math.sin(arrowAngle1)).toFloat()
            )

            val arrowPoint2 = Offset(
                x = end.x + (arrowSize * kotlin.math.cos(arrowAngle2)).toFloat(),
                y = end.y + (arrowSize * kotlin.math.sin(arrowAngle2)).toFloat()
            )

            drawLine(
                color = Color(0xFF888888),
                start = end,
                end = arrowPoint1,
                strokeWidth = 4f
            )

            drawLine(
                color = Color(0xFF888888),
                start = end,
                end = arrowPoint2,
                strokeWidth = 4f
            )
        }
    }

    // Debug overlay for edges
    if (debugMode) {
        edges.forEach { edge ->
            val fromNode = nodes[edge.fromId]
            val toNode = nodes[edge.toId]

            if (fromNode != null && toNode != null) {
                val nodeWidth = with(density) { NODE_WIDTH.toPx() }
                val nodeHeight = with(density) { NODE_HEIGHT.toPx() }
                val horizontalSpacing = with(density) { HORIZONTAL_SPACING.toPx() }
                val verticalSpacing = with(density) { VERTICAL_SPACING.toPx() }
                val configuration = LocalConfiguration.current
                val canvasWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

                val fromPos = calculateNodePosition(
                    fromNode,
                    canvasWidth,
                    horizontalSpacing,
                    verticalSpacing,
                )
                val toPos = calculateNodePosition(
                    toNode,
                    canvasWidth,
                    horizontalSpacing,
                    verticalSpacing,
                )

                val startY = fromPos.y + nodeHeight
                val endY = toPos.y
                val midX = (fromPos.x + toPos.x) / 2f + nodeWidth / 2f
                val midY = (startY + endY) / 2f

                Box(
                    modifier = Modifier
                        .offset { IntOffset(midX.roundToInt(), midY.roundToInt()) }
                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    Text(
                        text = "E: ${edge.fromId}→${edge.toId}",
                        color = Color.Yellow,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEdge(
    fromNode: UiNode,
    toNode: UiNode,
    nodeWidth: Float,
    nodeHeight: Float,
    horizontalSpacing: Float,
    verticalSpacing: Float,
    canvasWidth: Float,
    isHighlighted: Boolean = false
) {
    val fromPos = calculateNodePosition(
        fromNode,
        canvasWidth,
        horizontalSpacing,
        verticalSpacing,
    )
    val toPos = calculateNodePosition(
        toNode,
        canvasWidth,
        horizontalSpacing,
        verticalSpacing,
    )

    // Start from bottom center of source node
    val start = Offset(
        fromPos.x + nodeWidth / 2,
        fromPos.y + nodeHeight
    )

    // End at top center of target node
    val targetEnd = Offset(
        toPos.x + nodeWidth / 2,
        toPos.y
    )

    // End the curve slightly above the target node (20dp above)
    val curveEndOffset = 20f
    val curveEnd = Offset(
        targetEnd.x,
        targetEnd.y - curveEndOffset
    )

    // Calculate control points for cubic Bézier curve
    val verticalDistance = curveEnd.y - start.y
    val controlPointOffset = verticalDistance * 0.5f

    val controlPoint1 = Offset(
        start.x,
        start.y + controlPointOffset
    )

    val controlPoint2 = Offset(
        curveEnd.x,
        curveEnd.y - controlPointOffset
    )

    // Choose color based on highlight state
    val edgeColor = if (isHighlighted) Color.White else Color(0xFF888888)
    val strokeWidth = if (isHighlighted) 6f else 4f

    // Draw curved path using cubic Bézier
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(start.x, start.y)
        cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            curveEnd.x, curveEnd.y
        )
    }

    drawPath(
        path = path,
        color = edgeColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )

    // Draw straight vertical line from curve end to target node
    drawLine(
        color = edgeColor,
        start = curveEnd,
        end = targetEnd,
        strokeWidth = strokeWidth
    )

    // Arrow always points straight down
    val arrowSize = 20f
    val angle = Math.PI / 2 // 90 degrees (pointing down)
    val arrowAngle1 = angle + Math.PI * 5 / 6
    val arrowAngle2 = angle - Math.PI * 5 / 6

    val arrowPoint1 = Offset(
        x = targetEnd.x + (arrowSize * kotlin.math.cos(arrowAngle1)).toFloat(),
        y = targetEnd.y + (arrowSize * kotlin.math.sin(arrowAngle1)).toFloat()
    )

    val arrowPoint2 = Offset(
        x = targetEnd.x + (arrowSize * kotlin.math.cos(arrowAngle2)).toFloat(),
        y = targetEnd.y + (arrowSize * kotlin.math.sin(arrowAngle2)).toFloat()
    )

    drawLine(
        color = edgeColor,
        start = targetEnd,
        end = arrowPoint1,
        strokeWidth = strokeWidth
    )

    drawLine(
        color = edgeColor,
        start = targetEnd,
        end = arrowPoint2,
        strokeWidth = strokeWidth
    )
}