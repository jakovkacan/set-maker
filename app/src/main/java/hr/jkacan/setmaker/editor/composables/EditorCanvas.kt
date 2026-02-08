package hr.jkacan.setmaker.editor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.editor.EditorState
import hr.jkacan.setmaker.editor.gestures.canvasTransformGestures
import hr.jkacan.setmaker.editor.gestures.debugTapDetection
import hr.jkacan.setmaker.editor.rememberEditorCanvasState

@Composable
fun EditorCanvas(
    state: EditorState?,
    debugMode: Boolean,
    onAddNode: (Int, Int?) -> Unit,
    onAddNodeBranch: (Int) -> Unit,
    onSwapNodes: (Int, Int) -> Unit,
    onInsertNode: (Int, Int, Int) -> Unit,
    onDeleteNode: (Int) -> Unit,
    onConnectLeafToNode: (Int, Int) -> Unit,
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

            canvasState.centerX = with(density) { screenWidth.toPx() / 2f }
            canvasState.centerY = with(density) { screenHeight.toPx() / 2f }

            // Outer container for both transformed canvas and untransformed HUD
            Box(modifier = Modifier.fillMaxSize()) {
                // Transformed canvas Box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorResource(id = R.color.background))
                        .debugTapDetection(
                            enabled = debugMode,
                            getCurrentScale = { canvasState.scale },
                            getCurrentOffset = { canvasState.offset },
                            onTap = { screenPos, canvasPos ->
                                canvasState.lastTapScreenPos = screenPos
                                canvasState.lastTapCanvasPos = canvasPos
                            }
                        )
                        .canvasTransformGestures(
                            centerX = canvasState.centerX,
                            centerY = canvasState.centerY,
                            minScale = 0.5f,
                            maxScale = 3f,
                            getCurrentScale = { canvasState.scale },
                            getCurrentOffset = { canvasState.offset },
                            onTransform = { newScale, newOffset ->
                                canvasState.scale = newScale
                                canvasState.offset = newOffset
                            }
                        )
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
                            debugMode = debugMode,
                            highlightedEdge = canvasState.highlightedEdge
                        )
                    }

                    // Draw draggable leaf edges
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0.3f)
                    ) {
                        LeafEdgeLayer(
                            edges = state.edges,
                            nodes = state.nodes,
                            canvasState = canvasState,
                            debugMode = debugMode,
                            draggingLeafEdgeNodeId = canvasState.draggingLeafEdgeNodeId,
                            leafEdgeDragOffset = canvasState.leafEdgeDragOffset,
                            onDragStart = { leafNodeId ->
                                canvasState.draggingLeafEdgeNodeId = leafNodeId
                                canvasState.leafEdgeDragOffset = Offset.Zero
                                canvasState.leafEdgeTargetNodeId = null
                            },
                            onDrag = { dragOffset, targetNodeId ->
                                canvasState.leafEdgeDragOffset = dragOffset
                                canvasState.leafEdgeTargetNodeId = targetNodeId
                            },
                            onDragEnd = {
                                val leafNodeId = canvasState.draggingLeafEdgeNodeId
                                val targetNodeId = canvasState.leafEdgeTargetNodeId

                                if (leafNodeId != null && targetNodeId != null) {
                                    // Connect the leaf node to the target node
                                    onConnectLeafToNode(leafNodeId, targetNodeId)
                                }

                                canvasState.resetLeafEdgeDragState()
                            }
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
                            onPlusClick = onAddNode,
                            onPlusLongPress = { fromId, toId ->
                                if (toId != null)
                                    onAddNodeBranch(fromId)
                                else
                                    onAddNode(fromId, toId)
                            },
                            draggingLeafEdgeNodeId = canvasState.draggingLeafEdgeNodeId
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
                            canvasState = canvasState,
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
                                canvasState.isOverDeleteZone = false
                            },
                            onDrag = { delta, hoveredNode, hoveredEdge ->
                                canvasState.dragOffset += delta
                                canvasState.highlightedNodeId = hoveredNode
                                canvasState.highlightedEdge = hoveredEdge

                                // Check if drag position is over delete zone
                                val dragScreenPos = canvasState.currentDragScreenPos
                                if (dragScreenPos != null) {
                                    val screenWidthPx = with(density) { screenWidth.toPx() }
                                    val screenHeightPx = with(density) { screenHeight.toPx() }
                                    canvasState.isOverDeleteZone = isInDeleteZone(
                                        dragScreenPos,
                                        screenWidthPx,
                                        screenHeightPx,
                                        density.density
                                    )
                                }
                            },
                            onDragEnd = {
                                val draggedId = canvasState.draggingNodeId

                                if (draggedId != null) {
                                    // Check if dropped on delete zone first
                                    if (canvasState.isOverDeleteZone) {
                                        onDeleteNode(draggedId)
                                    }
                                    // Handle drop on node (swap positions)
                                    else if (canvasState.highlightedNodeId != null) {
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

                                canvasState.resetDragState()
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

                // Delete zone - shows when dragging a node
                DeleteZone(
                    isVisible = canvasState.draggingNodeId != null,
                    isHighlighted = canvasState.isOverDeleteZone
                )
            }
        }
    }
}