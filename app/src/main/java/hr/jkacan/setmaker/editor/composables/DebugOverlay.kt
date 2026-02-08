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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import hr.jkacan.setmaker.data.state.EditorState
import hr.jkacan.setmaker.models.editor.UiNode

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
                    val horizontalSpacing = with(density) { 180.dp.toPx() }
                    val verticalSpacing = with(density) { 220.dp.toPx() }
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
    scale: Float,
    offset: IntOffset
) {
    Box(
        modifier = Modifier
            .offset { offset }
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
                            position.x * scale + offset.x
                        )
                    }, ${
                        String.format(
                            "%.0f",
                            position.y * scale + offset.y
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
