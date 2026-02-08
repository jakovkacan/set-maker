package hr.jkacan.setmaker.editor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import hr.jkacan.setmaker.editor.EditorState
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.calculateNodePosition
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.canvasToScreenCoordinates
import hr.jkacan.setmaker.editor.layout.HORIZONTAL_SPACING
import hr.jkacan.setmaker.editor.layout.NODE_HEIGHT
import hr.jkacan.setmaker.editor.layout.NODE_WIDTH
import hr.jkacan.setmaker.editor.layout.VERTICAL_SPACING
import hr.jkacan.setmaker.editor.layout.withDensity
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode
import kotlin.math.roundToInt

@Composable
fun DebugOverlay(
    state: EditorState,
    scale: Float,
    offset: Offset,
    lastTapScreenPos: Offset?,
    lastTapCanvasPos: Offset?,
    currentDragScreenPos: Offset?,
    currentDragCanvasPos: Offset?,
    draggingNodeId: Int?,
    highlightedNodeId: Int?,
    highlightedEdge: Pair<Int, Int>?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = "🐛 DEBUG MODE",
                color = Color.Yellow,
                fontSize = 14.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transform info
            Text(
                text = "Transform:",
                color = Color.Cyan,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "  Scale: ${String.format("%.2f", scale)}x",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "  Pan: (${
                    String.format(
                        "%.0f",
                        offset.x
                    )
                }, ${String.format("%.0f", offset.y)})",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Last tap info
            if (lastTapScreenPos != null) {
                Text(
                    text = "Last Tap:",
                    color = Color.Cyan,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "  Screen: (${
                        String.format(
                            "%.0f",
                            lastTapScreenPos!!.x
                        )
                    }, ${String.format("%.0f", lastTapScreenPos!!.y)})",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                if (lastTapCanvasPos != null) {
                    Text(
                        text = "  Canvas: (${
                            String.format(
                                "%.0f",
                                lastTapCanvasPos!!.x
                            )
                        }, ${String.format("%.0f", lastTapCanvasPos!!.y)})",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            // Current drag info
            if (draggingNodeId != null && currentDragCanvasPos != null) {
                Spacer(modifier = Modifier.height(8.dp))

                // Find the dragged node to show its canvas position
                val draggedNode = state.nodes[draggingNodeId]

                Text(
                    text = "Dragging Node $draggingNodeId:",
                    color = Color.Green,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                if (draggedNode != null) {
                    // Calculate the node's base canvas position (without drag offset)
                    val density = LocalDensity.current
                    val configuration = LocalConfiguration.current
                    val horizontalSpacing = HORIZONTAL_SPACING.withDensity(density)
                    val verticalSpacing = VERTICAL_SPACING.withDensity(density)
                    val canvasWidth =
                        with(density) { configuration.screenWidthDp.dp.toPx() }
                    val nodeWidth = 120f

                    val nodeCanvasX =
                        canvasWidth / 2f + (draggedNode.col * horizontalSpacing) - (nodeWidth / 2f)
                    val nodeCanvasY = 100f + (draggedNode.row * verticalSpacing)

                    Text(
                        text = "  Node Base: (${
                            String.format(
                                "%.0f",
                                nodeCanvasX
                            )
                        }, ${String.format("%.0f", nodeCanvasY)})",
                        color = Color.Cyan,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                if (currentDragScreenPos != null) {
                    Text(
                        text = "  Screen: (${
                            String.format(
                                "%.0f",
                                currentDragScreenPos!!.x
                            )
                        }, ${String.format("%.0f", currentDragScreenPos!!.y)})",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                Text(
                    text = "  Canvas: (${
                        String.format(
                            "%.0f",
                            currentDragCanvasPos!!.x
                        )
                    }, ${String.format("%.0f", currentDragCanvasPos!!.y)})",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                if (highlightedNodeId != null) {
                    Text(
                        text = "  Hover: Node $highlightedNodeId",
                        color = Color.Yellow,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                if (highlightedEdge != null) {
                    Text(
                        text = "  Hover: Edge ${highlightedEdge!!.first}->${highlightedEdge!!.second}",
                        color = Color.Yellow,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Node count
            Text(
                text = "Nodes: ${state.nodes.size}",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "Edges: ${state.edges.size}",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun NodeDebugOverlay(
    node: UiNode,
    position: Offset,
    canvasState: EditorCanvasState,
    visualOffset: IntOffset
) {
    // Calculate screen coordinates using center-based transformation
    // This matches the graphicsLayer transformation: (canvas - center) * scale + center + offset
    val screenCoords = canvasToScreenCoordinates(position, canvasState)

    Box(
        modifier = Modifier
            .offset { visualOffset }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                .padding(4.dp)
        ) {
            Column {
                Text(
                    text = "ID: ${node.id}",
                    color = Color.Cyan,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Col: ${node.col}",
                    color = Color.Green,
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Row: ${node.row}",
                    color = Color.Magenta,
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Canvas: (${
                        String.format(
                            "%.0f",
                            position.x
                        )
                    }, ${String.format("%.0f", position.y)})",
                    color = Color.Yellow,
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Screen: (${
                        String.format(
                            "%.0f",
                            screenCoords.x
                        )
                    }, ${
                        String.format(
                            "%.0f",
                            screenCoords.y
                        )
                    })",
                    color = Color(0xFF00FF00),
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun EdgeDebugOverlay(edges: List<UiEdge>, nodes: Map<Int, UiNode>, density: Density) {
    edges.forEach { edge ->
        val fromNode = nodes[edge.fromId]
        val toNode = nodes[edge.toId]

        if (fromNode != null && toNode != null) {
            val nodeWidth = NODE_WIDTH.withDensity(density)
            val nodeHeight = NODE_HEIGHT.withDensity(density)
            val horizontalSpacing = HORIZONTAL_SPACING.withDensity(density)
            val verticalSpacing = VERTICAL_SPACING.withDensity(density)
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

@Composable
fun LeafEdgeDebugInfo(
    leafNode: UiNode,
    position: Offset,
    isDragging: Boolean,
    dragOffset: Offset,
    targetNodeId: Int?,
    finalOffset: IntOffset
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(finalOffset.x + 40, finalOffset.y + 50) }
            .background(
                Color(0xCC000000),
                RoundedCornerShape(4.dp)
            )
            .padding(4.dp)
    ) {
        androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Text(
                text = "LEAF: ${leafNode.id}",
                color = Color(0xFFFF00FF),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            androidx.compose.material3.Text(
                text = "Pos: (${String.format("%.0f", position.x)}, ${
                    String.format(
                        "%.0f",
                        position.y
                    )
                })",
                color = Color(0xFFFFAA00),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            if (isDragging) {
                androidx.compose.material3.Text(
                    text = "DRAGGING",
                    color = Color.Red,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                androidx.compose.material3.Text(
                    text = "Δ(${String.format("%.0f", dragOffset.x)}, ${
                        String.format(
                            "%.0f",
                            dragOffset.y
                        )
                    })",
                    color = Color.Yellow,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (targetNodeId != null) {
                    androidx.compose.material3.Text(
                        text = "Target: $targetNodeId",
                        color = Color.Green,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}