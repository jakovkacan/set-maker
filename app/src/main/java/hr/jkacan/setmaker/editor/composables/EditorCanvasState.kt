package hr.jkacan.setmaker.editor.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Composable
fun rememberEditorCanvasState(): EditorCanvasState {
    return remember { EditorCanvasState() }
}

class EditorCanvasState {
    // Transform state
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    // Drag state
    var draggingNodeId by mutableStateOf<Int?>(null)
    var dragOffset by mutableStateOf(Offset.Zero)
    var highlightedNodeId by mutableStateOf<Int?>(null)
    var highlightedEdge by mutableStateOf<Pair<Int, Int>?>(null)

    // Debug state
    var lastTapScreenPos by mutableStateOf<Offset?>(null)
    var lastTapCanvasPos by mutableStateOf<Offset?>(null)
    var currentDragScreenPos by mutableStateOf<Offset?>(null)
    var currentDragCanvasPos by mutableStateOf<Offset?>(null)

    fun resetDragState() {
        draggingNodeId = null
        dragOffset = Offset.Zero
        highlightedNodeId = null
        highlightedEdge = null
    }
}