package hr.jkacan.setmaker.editor.composables

import android.os.Vibrator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import hr.jkacan.setmaker.editor.EditorCanvasState
import hr.jkacan.setmaker.editor.gestures.GestureDetector
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.calculateNodePosition
import hr.jkacan.setmaker.editor.layout.HORIZONTAL_SPACING
import hr.jkacan.setmaker.editor.layout.NODE_HEIGHT
import hr.jkacan.setmaker.editor.layout.NODE_WIDTH
import hr.jkacan.setmaker.editor.layout.VERTICAL_SPACING
import hr.jkacan.setmaker.editor.layout.withDensity
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode
import kotlin.math.roundToInt

@Composable
fun LeafEdgeLayer(
    edges: List<UiEdge>,
    nodes: Map<Int, UiNode>,
    canvasState: EditorCanvasState,
    debugMode: Boolean = false,
    draggingLeafEdgeNodeId: Int?,
    leafEdgeDragOffset: Offset,
    onDragStart: (Int) -> Unit,
    onDrag: (Offset, Int?) -> Unit,
    onDragEnd: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    val gestureDetector = remember(vibrator) {
        GestureDetector(vibrator)
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val nodeWidth = NODE_WIDTH.withDensity(density)
    val nodeHeight = NODE_HEIGHT.withDensity(density)
    val horizontalSpacing = HORIZONTAL_SPACING.withDensity(density)
    val verticalSpacing = VERTICAL_SPACING.withDensity(density)

    // Find leaf nodes (nodes with no outgoing edges)
    val nodesWithChildren = edges.map { it.fromId }.toSet()
    val leafNodes = nodes.values.filter { it.id !in nodesWithChildren }

    Box(modifier = Modifier.fillMaxSize()) {
        leafNodes.forEach { leafNode ->
            key(leafNode.id) {
                val fromPos = calculateNodePosition(
                    leafNode,
                    screenWidth,
                    horizontalSpacing,
                    verticalSpacing
                )

                // Position leaf edge at bottom center of the node
                // LeafEdge canvas is 400dp wide with start point at canvas center (200dp from left)
                // We want this start point to align with node's bottom center
                // So: canvas left edge = node center - 200dp
                val edgeCanvasWidthPx = with(density) { 400.dp.toPx() }
                val edgeCanvasVerticalPaddingPx = with(density) { 100.dp.toPx() }
                val nodeCenterX = fromPos.x + nodeWidth / 2f
                val edgeX = nodeCenterX - (edgeCanvasWidthPx / 2f)
                val edgeY = fromPos.y + nodeHeight - edgeCanvasVerticalPaddingPx

                val isDragging = draggingLeafEdgeNodeId == leafNode.id

                // Position is always fixed - we don't offset the composable during drag
                // Instead, we pass the dragOffset to LeafEdge which handles the visual drag
                val finalOffset = IntOffset(edgeX.roundToInt(), edgeY.roundToInt())

                Box(
                    modifier = Modifier.zIndex(if (isDragging) 1f else 0f)
                ) {
                    // Draw the leaf edge (400dp × 600dp canvas for drawing space)
                    LeafEdge(
                        leafNode = leafNode,
                        targetNode = canvasState.leafEdgeTargetNodeId?.let { nodes[it] },
                        modifier = Modifier.offset {
                            IntOffset(
                                (finalOffset.x + (edgeCanvasWidthPx / 2f) - 40.dp.toPx()).roundToInt(),
                                edgeY.roundToInt()
                            )
                        },
                        isDragging = isDragging,
                        hasValidTarget = canvasState.leafEdgeTargetNodeId != null,
                        dragOffset = if (isDragging) leafEdgeDragOffset else Offset.Zero,
                        debugMode = debugMode
                    )

                    // Hit test Box - only the center 80dp wide area is tappable
                    // This is positioned over the actual edge line area
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (finalOffset.x + (edgeCanvasWidthPx / 2f) - 40.dp.toPx()).roundToInt(),
                                    (finalOffset.y + edgeCanvasVerticalPaddingPx).roundToInt()
                                )
                            }
                            .size(
                                width = 80.dp,
                                height = with(density) { 100.dp + 80.dp } // edge length + buffer
                            )
                            .pointerInput(leafNode.id) {
                                with(gestureDetector) {
                                    detectLongPressDrag(
                                        id = leafNode.id,
                                        nodePosition = Offset(nodeCenterX, fromPos.y + nodeHeight),
                                        canvasState = canvasState,
                                        onDragStart = { onDragStart(it) },
                                        onDrag = { delta, canvasPos ->
                                            // Find which node is being hovered over
                                            val targetNodeId = findValidTargetNode(
                                                canvasPos,
                                                leafNode,
                                                nodes,
                                                screenWidth,
                                                nodeWidth,
                                                nodeHeight,
                                                horizontalSpacing,
                                                verticalSpacing
                                            )

                                            onDrag(delta, targetNodeId)
                                        },
                                        onDragEnd = { onDragEnd() }
                                    )
                                }
                            }
                    )

                    // Debug overlay for leaf edges
                    if (debugMode) {
                        LeafEdgeDebugInfo(
                            leafNode = leafNode,
                            position = Offset(edgeX, edgeY),
                            isDragging = isDragging,
                            dragOffset = leafEdgeDragOffset,
                            targetNodeId = canvasState.leafEdgeTargetNodeId,
                            finalOffset = finalOffset
                        )
                    }
                }
            }

//        // Draw curved preview lines for dragging (separate layer to avoid clipping)
//        if (draggingLeafEdgeNodeId != null && canvasState.leafEdgeTargetNodeId != null) {
//            val leafNode = nodes[draggingLeafEdgeNodeId]
//            val targetNode = nodes[canvasState.leafEdgeTargetNodeId]
//
//            if (leafNode != null && targetNode != null) {
//                Canvas(modifier = Modifier.fillMaxSize()) {
//                    val fromPos = calculateNodePosition(
//                        leafNode,
//                        screenWidth,
//                        horizontalSpacing,
//                        verticalSpacing
//                    )
//                    val targetPos = calculateNodePosition(
//                        targetNode,
//                        screenWidth,
//                        horizontalSpacing,
//                        verticalSpacing
//                    )
//
//                    val edgeLength = with(density) { 100.dp.toPx() }
//                    val startY = fromPos.y + nodeHeight
//                    val midX = fromPos.x + nodeWidth / 2f
//
//                    val end = Offset(
//                        midX + leafEdgeDragOffset.x,
//                        startY + edgeLength + leafEdgeDragOffset.y
//                    )
//
//                    val targetTopCenter = Offset(
//                        targetPos.x + nodeWidth / 2f,
//                        targetPos.y
//                    )
//
//                    // Draw curved preview line from drag end to target node
//                    val verticalDistance = targetTopCenter.y - end.y
//                    val controlPointOffset = verticalDistance * 0.5f
//
//                    val controlPoint1 = Offset(
//                        end.x,
//                        end.y + controlPointOffset
//                    )
//
//                    val controlPoint2 = Offset(
//                        targetTopCenter.x,
//                        targetTopCenter.y - controlPointOffset
//                    )
//
//                    val path = Path().apply {
//                        moveTo(end.x, end.y)
//                        cubicTo(
//                            controlPoint1.x, controlPoint1.y,
//                            controlPoint2.x, controlPoint2.y,
//                            targetTopCenter.x, targetTopCenter.y
//                        )
//                    }
//
//                    drawPath(
//                        path = path,
//                        color = Color.White.copy(alpha = 0.5f),
//                        style = Stroke(width = 4f)
//                    )
//                }
//            }
//        }
        }
    }
}

/**
 * Finds a valid target node for the leaf edge connection.
 * A valid target is any node whose row is below the source leaf node's row.
 */
fun findValidTargetNode(
    canvasPos: Offset,
    sourceLeafNode: UiNode,
    nodes: Map<Int, UiNode>,
    canvasWidth: Float,
    nodeWidth: Float,
    nodeHeight: Float,
    horizontalSpacing: Float,
    verticalSpacing: Float
): Int? {
    for (node in nodes.values) {
        // Must be in a row below the source leaf node
        if (node.row <= sourceLeafNode.row) continue

        // Don't allow connecting to itself
        if (node.id == sourceLeafNode.id) continue

        val nodePos = calculateNodePosition(
            node,
            canvasWidth,
            horizontalSpacing,
            verticalSpacing
        )

        // Check if canvas position is within the node bounds
        if (canvasPos.x >= nodePos.x && canvasPos.x <= nodePos.x + nodeWidth &&
            canvasPos.y >= nodePos.y && canvasPos.y <= nodePos.y + nodeHeight
        ) {
            return node.id
        }
    }
    return null
}