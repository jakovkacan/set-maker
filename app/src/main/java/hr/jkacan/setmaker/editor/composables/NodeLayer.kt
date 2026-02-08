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
import hr.jkacan.setmaker.editor.gestures.UnifiedGestureDetector
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.calculateNodePositionDp
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

            Box {
                SongNode(
                    song = node.song,
                    modifier = Modifier
                        .offset { finalOffset }
                        .pointerInput(node.id) {
                            with(gestureDetector) {
                                detectLongPressDrag(
                                    id = node.id,
                                    onDragStart = { onDragStart(it) },
                                    onDrag = { delta -> onDrag(delta, null, null) },
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