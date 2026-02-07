package hr.jkacan.setmaker.data.state

import androidx.compose.ui.geometry.Offset
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode

data class EditorState(
    val nodes: Map<Long, UiNode>,
    val edges: List<UiEdge>,
    val draggingNodeId: Long? = null,
    val linkingFromId: Long? = null,
    val pointerWorld: Offset = Offset.Companion.Unspecified,
    val pan: Offset = Offset.Companion.Zero,
    val zoom: Float = 1f
)