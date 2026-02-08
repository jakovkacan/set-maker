package hr.jkacan.setmaker.editor.gestures

import androidx.compose.ui.geometry.Offset

interface DragGestureHandler {
    fun onDragStart(nodeId: Int)
    fun onDrag(delta: Offset, hoveredNodeId: Int?, hoveredEdge: Pair<Int, Int>?)
    fun onDragEnd()
}