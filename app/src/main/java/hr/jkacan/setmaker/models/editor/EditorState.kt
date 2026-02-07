package hr.jkacan.setmaker.models.editor

import androidx.compose.ui.geometry.Offset

data class EditorState(
    val nodes: Map<Long, UiNode>,
    val edges: List<UiEdge>,
    val draggingNodeId: Long? = null,
    val linkingFromId: Long? = null,
    val pointerWorld: Offset = Offset.Unspecified,
    val pan: Offset = Offset.Zero,
    val zoom: Float = 1f
)