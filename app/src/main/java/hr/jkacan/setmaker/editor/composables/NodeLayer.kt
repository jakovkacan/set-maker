package hr.jkacan.setmaker.editor.composables

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.calculateNodePosition
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.calculateNodePositionDp
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import kotlin.math.roundToInt

@Composable
fun NodeLayer(
    nodes: List<UiNode>,
    edges: List<UiEdge>,
    pan: Offset,
    zoom: Float,
    scale: Float,
    offset: Offset,
    draggingNodeId: Int?,
    dragOffset: Offset,
    debugMode: Boolean = false,
    highlightedNodeId: Int? = null,
    onDebugDragUpdate: (screenPos: Offset, canvasPos: Offset) -> Unit = { _, _ -> },
    onDragStart: (Int) -> Unit,
    onDrag: (Offset, Int?, Pair<Int, Int>?) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)
    val configuration = LocalConfiguration.current
    val canvasWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val nodeWidth = with(density) { 120.dp.toPx() }
    val nodeHeight = with(density) { 150.dp.toPx() }
    val horizontalSpacing = with(density) { 180.dp.toPx() }
    val verticalSpacing =
        with(density) { 160.dp.toPx() } // 160dp ensures no overlap (160dp*3=480px > 150dp*3=450px node height)

    Box(modifier = Modifier.fillMaxSize()) {
        nodes.forEach { node ->
            val position = with(density) {
                calculateNodePositionDp(node, pan, zoom)
            }

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
                            var longPressJob: kotlinx.coroutines.Job? = null

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var longPressTriggered = false

                                // Launch coroutine for long press detection
                                @OptIn(DelicateCoroutinesApi::class)
                                longPressJob = GlobalScope.launch {
                                    delay(500) // 500ms long press threshold
                                    longPressTriggered = true

                                    // Trigger vibration
                                    @Suppress("MissingPermission")
                                    vibrator?.vibrate(
                                        VibrationEffect.createOneShot(
                                            50,
                                            VibrationEffect.DEFAULT_AMPLITUDE
                                        )
                                    )

                                    // Trigger visual feedback immediately
                                    onDragStart(node.id)
                                }

                                // Handle drag gestures
                                var dragStarted = false
                                do {
                                    val event = awaitPointerEvent()

                                    if (longPressTriggered) {
                                        // Once long press is triggered, handle drag
                                        event.changes.forEach { change ->
                                            if (change.pressed) {
                                                val dragAmount = change.position - down.position
                                                if (!dragStarted) {
                                                    dragStarted = true
                                                    android.util.Log.d(
                                                        "DragDebug",
                                                        "=== DRAG STARTED ==="
                                                    )
                                                    android.util.Log.d(
                                                        "DragDebug",
                                                        "down.position (initial touch): $down.position"
                                                    )
                                                }

                                                android.util.Log.d(
                                                    "DragDebug", """
                                                        === DRAG EVENT ===
                                                        change.position: ${change.position}
                                                        down.position: ${down.position}
                                                        dragAmount (change - down): $dragAmount
                                                        current dragOffset state: $dragOffset
                                                        delta to send (dragAmount - dragOffset): ${
                                                        Offset(
                                                            dragAmount.x - dragOffset.x,
                                                            dragAmount.y - dragOffset.y
                                                        )
                                                    }
                                                    """.trimIndent()
                                                )

                                                // Calculate world position in the canvas coordinate system
                                                // The node's ORIGINAL position in canvas space is 'position'
                                                // The node's CURRENT position includes dragOffset (via .offset modifier)
                                                // change.position is relative to node's CURRENT position (after offset)

                                                android.util.Log.d(
                                                    "DragDebug",
                                                    "=== WORLD POS CALCULATION ==="
                                                )
                                                android.util.Log.d(
                                                    "DragDebug",
                                                    "position (original canvas): $position"
                                                )
                                                android.util.Log.d(
                                                    "DragDebug",
                                                    "dragOffset (accumulated): $dragOffset"
                                                )

                                                // Node's current canvas position (base + drag)
                                                val currentNodeCanvasPos = Offset(
                                                    position.x + dragOffset.x,
                                                    position.y + dragOffset.y
                                                )
                                                android.util.Log.d(
                                                    "DragDebug",
                                                    "currentNodeCanvasPos (pos + dragOff): $currentNodeCanvasPos"
                                                )

                                                // change.position is in screen pixels relative to current node position
                                                // We need to convert it to canvas space
                                                android.util.Log.d(
                                                    "DragDebug",
                                                    "change.position (relative to node): ${change.position}"
                                                )
                                                android.util.Log.d("DragDebug", "scale: $scale")

                                                val touchOffsetCanvas = Offset(
                                                    change.position.x / scale,
                                                    change.position.y / scale
                                                )
                                                android.util.Log.d(
                                                    "DragDebug",
                                                    "touchOffsetCanvas (change.pos / scale): $touchOffsetCanvas"
                                                )

                                                // Absolute canvas position of touch
                                                val worldPos = Offset(
                                                    currentNodeCanvasPos.x + touchOffsetCanvas.x,
                                                    currentNodeCanvasPos.y + touchOffsetCanvas.y
                                                )
                                                android.util.Log.d(
                                                    "DragDebug",
                                                    "worldPos (currentNode + touchOffset): $worldPos"
                                                )
                                                android.util.Log.d(
                                                    "DragDebug",
                                                    "==========================="
                                                )

                                                // Absolute screen position (for debug display)
                                                val absoluteScreenPos = Offset(
                                                    worldPos.x * scale + offset.x,
                                                    worldPos.y * scale + offset.y
                                                )

                                                // Update debug coordinates during drag
                                                if (debugMode) {
                                                    android.util.Log.d(
                                                        "DragDebug", """
                                                            Node ${node.id} drag calculation:
                                                            - Original canvas pos: $position
                                                            - Drag offset: $dragOffset
                                                            - Current canvas pos: $currentNodeCanvasPos
                                                            - change.position (screen, relative): ${change.position}
                                                            - Touch offset (canvas): $touchOffsetCanvas
                                                            - World pos (canvas): $worldPos
                                                            - Screen pos: $absoluteScreenPos
                                                            - Scale: $scale, Offset: $offset
                                                        """.trimIndent()
                                                    )

                                                    onDebugDragUpdate(
                                                        absoluteScreenPos,
                                                        worldPos
                                                    )
                                                }

                                                // Hit detection for nodes
                                                var hoveredNode: Int? = null
                                                for (otherNode in nodes) {
                                                    if (otherNode.id == node.id) continue

                                                    val otherNodePos = calculateNodePosition(
                                                        otherNode,
                                                        canvasWidth,
                                                        horizontalSpacing,
                                                        verticalSpacing,
                                                        pan,
                                                        zoom
                                                    )

                                                    val xMin = otherNodePos.x
                                                    val xMax = otherNodePos.x + nodeWidth
                                                    val yMin = otherNodePos.y
                                                    val yMax = otherNodePos.y + nodeHeight

                                                    val xIn =
                                                        worldPos.x >= xMin && worldPos.x <= xMax
                                                    val yIn =
                                                        worldPos.y >= yMin && worldPos.y <= yMax
                                                    val inBounds = xIn && yIn

                                                    if (inBounds) {
                                                        hoveredNode = otherNode.id
                                                        break
                                                    }
                                                }


                                                // Hit detection for edges (only if not hovering a node)
                                                var hoveredEdge: Pair<Int, Int>? = null
                                                if (hoveredNode == null) {
                                                    for (edge in edges) {
                                                        val fromNode =
                                                            nodes.find { it.id == edge.fromId }
                                                        val toNode =
                                                            nodes.find { it.id == edge.toId }

                                                        if (fromNode != null && toNode != null) {
                                                            val fromPos = calculateNodePosition(
                                                                fromNode,
                                                                canvasWidth,
                                                                horizontalSpacing,
                                                                verticalSpacing,
                                                                pan,
                                                                zoom
                                                            )
                                                            val toPos = calculateNodePosition(
                                                                toNode,
                                                                canvasWidth,
                                                                horizontalSpacing,
                                                                verticalSpacing,
                                                                pan,
                                                                zoom
                                                            )

                                                            val startY = fromPos.y + nodeHeight
                                                            val endY = toPos.y
                                                            val midX =
                                                                (fromPos.x + toPos.x) / 2f + nodeWidth / 2f
                                                            val midY = (startY + endY) / 2f

                                                            // Check distance from edge midpoint
                                                            val hitRadius = 60f
                                                            val dx = worldPos.x - midX
                                                            val dy = worldPos.y - midY
                                                            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                                                                hoveredEdge =
                                                                    Pair(edge.fromId, edge.toId)
                                                                break
                                                            }
                                                        }
                                                    }
                                                }


                                                onDrag(
                                                    Offset(
                                                        dragAmount.x - dragOffset.x,
                                                        dragAmount.y - dragOffset.y
                                                    ),
                                                    hoveredNode,
                                                    hoveredEdge
                                                )
                                                change.consume()
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                // Clean up
                                longPressJob.cancel()
                                if (longPressTriggered) {
                                    onDragEnd()
                                }
                            }
                        },
                    zoom = zoom,
                    isDragging = isDragging,
                    isHighlighted = highlightedNodeId == node.id
                )

                // Debug overlay for nodes
                if (debugMode) {
                    Box(
                        modifier = Modifier
                            .offset { finalOffset }
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
            }
        }
    }
}