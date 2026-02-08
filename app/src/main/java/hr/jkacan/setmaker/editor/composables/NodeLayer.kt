package hr.jkacan.setmaker.editor.composables

import android.os.Vibrator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
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
import hr.jkacan.setmaker.editor.gestures.UnifiedGestureDetector
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.calculateNodePositionDp
import hr.jkacan.setmaker.editor.layout.HORIZONTAL_SPACING
import hr.jkacan.setmaker.editor.layout.HitDetector
import hr.jkacan.setmaker.editor.layout.NODE_HEIGHT
import hr.jkacan.setmaker.editor.layout.NODE_WIDTH
import hr.jkacan.setmaker.editor.layout.VERTICAL_SPACING
import hr.jkacan.setmaker.editor.layout.withDensity
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode
import kotlin.math.roundToInt

@Composable
fun NodeLayer(
    nodes: List<UiNode>,
    edges: List<UiEdge>,
    canvasState: EditorCanvasState,
    draggingNodeId: Int?,
    dragOffset: Offset,
    debugMode: Boolean = false,
    highlightedNodeId: Int? = null,
    onDebugDragUpdate: (screenPos: Offset, canvasPos: Offset) -> Unit = { _, _ -> },
    onDragStart: (Int) -> Unit,
    onDrag: (Offset, Int?, Pair<Int, Int>?) -> Unit,
    onDragEnd: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    val gestureDetector = remember(vibrator) {
        UnifiedGestureDetector(vibrator)
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val canvasWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val nodeWidth = NODE_WIDTH.withDensity(density)
    val nodeHeight = NODE_HEIGHT.withDensity(density)
    val horizontalSpacing = HORIZONTAL_SPACING.withDensity(density)
    val verticalSpacing = VERTICAL_SPACING.withDensity(density)

    val hitDetector = remember(nodes, edges, canvasWidth, horizontalSpacing, verticalSpacing) {
        HitDetector(
            nodes = nodes,
            edges = edges,
            canvasWidth = canvasWidth,
            nodeWidth = nodeWidth,
            nodeHeight = nodeHeight,
            horizontalSpacing = horizontalSpacing,
            verticalSpacing = verticalSpacing
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        nodes.forEach { node ->
            val position = calculateNodePositionDp(node)

            val isDragging = draggingNodeId == node.id
            val finalOffset = if (isDragging) {
                IntOffset(
                    (position.x + dragOffset.x).roundToInt(),
                    (position.y + dragOffset.y).roundToInt()
                )
            } else {
                IntOffset(position.x.roundToInt(), position.y.roundToInt())
            }

            Box(
                modifier = Modifier.zIndex(if (isDragging) 1f else 0f)
            ) {
                SongNode(
                    song = node.song,
                    modifier = Modifier
                        .offset { finalOffset }
                        .pointerInput(node.id) {
                            with(gestureDetector) {
                                detectLongPressDrag(
                                    id = node.id,
                                    nodePosition = position,
                                    canvasState = canvasState,
                                    onDragStart = { onDragStart(it) },
                                    onDrag = { delta, canvasPos ->
                                        // Use HitDetector to find hovered node and edge
                                        val hoveredNodeId = hitDetector.findHoveredNode(
                                            canvasPos,
                                            excludeNodeId = node.id
                                        )
                                        val hoveredEdge = hitDetector.findHoveredEdge(canvasPos)

                                        onDrag(delta, hoveredNodeId, hoveredEdge)
                                    },
                                    onDragEnd = { onDragEnd() },
                                    onDebugDragUpdate = { screenPos, canvasPos ->
                                        onDebugDragUpdate(
                                            screenPos, canvasPos
                                        )
                                    }
                                )
                            }
                        },
                    isDragging = isDragging,
                    isHighlighted = highlightedNodeId == node.id
                )

                // Debug overlay for nodes
                if (debugMode) {
                    NodeDebugOverlay(node, position, canvasState.scale, finalOffset)
                }
            }
        }
    }
}