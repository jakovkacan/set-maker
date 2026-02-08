package hr.jkacan.setmaker.editor

import androidx.compose.ui.geometry.Offset
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode

data class EditorState(
    val nodes: Map<Int, UiNode>,
    val edges: List<UiEdge>,
    val draggingNodeId: Int? = null,
    val linkingFromId: Int? = null,
    val pointerWorld: Offset = Offset.Companion.Unspecified
)