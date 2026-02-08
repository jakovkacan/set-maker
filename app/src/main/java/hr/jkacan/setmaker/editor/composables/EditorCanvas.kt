package hr.jkacan.setmaker.editor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.data.state.EditorState

@Composable
fun EditorCanvas(
    state: EditorState?,
    debugMode: Boolean,
    onAddNode: (Int, Int?) -> Unit,
    onAddNodeBranch: (Int) -> Unit,
    onSwapNodes: (Int, Int) -> Unit,
    onInsertNode: (Int, Int, Int) -> Unit,
) {
    when {
        state == null -> LoadingState()
        state.nodes.isEmpty() -> EmptyGraphState()
        else -> {

            val canvasState = rememberEditorCanvasState()

            // Get screen/canvas center
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val screenHeight = configuration.screenHeightDp.dp
            val density = LocalDensity.current

            val centerX = with(density) { screenWidth.toPx() / 2f }
            val centerY = with(density) { screenHeight.toPx() / 2f }

            // Outer container for both transformed canvas and untransformed HUD
            Box(modifier = Modifier.fillMaxSize()) {
                // Transformed canvas Box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorResource(id = R.color.background))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()

                                // Capture tap coordinates for debug
                                if (debugMode) {
                                    canvasState.lastTapScreenPos = down.position
                                    // Calculate canvas position (inverse transform)
                                    // Transform: screen = canvas * scale + offset
                                    // Inverse: canvas = (screen - offset) / scale
                                    canvasState.lastTapCanvasPos = Offset(
                                        (down.position.x - canvasState.offset.x) / canvasState.scale,
                                        (down.position.y - canvasState.offset.y) / canvasState.scale
                                    )
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                // 1. Apply zoom limiting
                                val newScale = (canvasState.scale * zoom).coerceIn(0.5f, 3f)

                                // 2. Zoom towards centroid
                                canvasState.offset += Offset(
                                    x = (1 - zoom) * (centroid.x - canvasState.offset.x - centerX),
                                    y = (1 - zoom) * (centroid.y - canvasState.offset.y - centerY)
                                )

                                // 3. Update scale after offset adjustment
                                canvasState.scale = newScale

                                // 4. Apply pan
                                canvasState.offset += pan
                            }
                        }
                        .graphicsLayer(
                            scaleX = canvasState.scale,
                            scaleY = canvasState.scale,
                            translationX = canvasState.offset.x,
                            translationY = canvasState.offset.y
                        )

                ) {
                    // Draw edges behind nodes
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0f)
                    ) {
                        EdgeLayer(
                            edges = state.edges,
                            nodes = state.nodes,
                            pan = state.pan,
                            zoom = state.zoom,
                            debugMode = debugMode,
                            highlightedEdge = canvasState.highlightedEdge
                        )
                    }

                    // Draw plus icons on edges
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0.5f)
                    ) {
                        PlusIconLayer(
                            edges = state.edges,
                            nodes = state.nodes,
                            pan = state.pan,
                            zoom = state.zoom,
                            onPlusClick = onAddNode,
                            onPlusLongPress = { fromId, toId ->
                                if (toId != null)
                                    onAddNodeBranch(fromId)
                                else
                                    onAddNode(fromId, toId)
                            }
                        )
                    }

                    // Draw nodes
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                    ) {
                        NodeLayer(
                            nodes = state.nodes.values.toList(),
                            edges = state.edges,
                            pan = state.pan,
                            zoom = state.zoom,
                            scale = canvasState.scale,
                            offset = canvasState.offset,
                            draggingNodeId = canvasState.draggingNodeId,
                            dragOffset = canvasState.dragOffset,
                            debugMode = debugMode,
                            highlightedNodeId = canvasState.highlightedNodeId,
                            onDebugDragUpdate = { screenPos, canvasPos ->
                                canvasState.currentDragScreenPos = screenPos
                                canvasState.currentDragCanvasPos = canvasPos
                            },
                            onDragStart = { nodeId ->
                                canvasState.draggingNodeId = nodeId
                                canvasState.dragOffset = Offset.Zero
                            },
                            onDrag = { delta, hoveredNode, hoveredEdge ->
                                canvasState.dragOffset += delta
                                canvasState.highlightedNodeId = hoveredNode
                                canvasState.highlightedEdge = hoveredEdge
                            },
                            onDragEnd = {
                                val draggedId = canvasState.draggingNodeId

                                if (draggedId != null) {
                                    // Handle drop on node (swap positions)
                                    if (canvasState.highlightedNodeId != null) {
                                        onSwapNodes(
                                            draggedId,
                                            canvasState.highlightedNodeId!!
                                        )
                                    }
                                    // Handle drop on edge (insert between)
                                    else if (canvasState.highlightedEdge != null) {
                                        val (fromId, toId) = canvasState.highlightedEdge!!
                                        onInsertNode(
                                            draggedId,
                                            fromId,
                                            toId
                                        )
                                    }
                                }

                                canvasState.draggingNodeId = null
                                canvasState.dragOffset = Offset.Zero
                                canvasState.highlightedNodeId = null
                                canvasState.highlightedEdge = null
                            }
                        )
                    }
                }

                // Debug HUD overlay - NOT transformed, stays pinned to screen
                if (debugMode) {
                    DebugOverlay(
                        state = state,
                        scale = canvasState.scale,
                        offset = canvasState.offset,
                        lastTapScreenPos = canvasState.lastTapScreenPos,
                        lastTapCanvasPos = canvasState.lastTapCanvasPos,
                        currentDragScreenPos = canvasState.currentDragScreenPos,
                        currentDragCanvasPos = canvasState.currentDragCanvasPos,
                        draggingNodeId = canvasState.draggingNodeId,
                        highlightedNodeId = canvasState.highlightedNodeId,
                        highlightedEdge = canvasState.highlightedEdge
                    )
                }
            }
        }
    }
}