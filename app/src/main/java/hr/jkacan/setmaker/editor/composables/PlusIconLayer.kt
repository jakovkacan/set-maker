package hr.jkacan.setmaker.editor.composables

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.calculateNodePosition
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import kotlin.math.roundToInt

@Composable
fun PlusIconLayer(
    edges: List<UiEdge>,
    nodes: Map<Int, UiNode>,
    pan: Offset,
    zoom: Float,
    onPlusClick: (fromId: Int, toId: Int?) -> Unit,
    onPlusLongPress: (fromId: Int, toId: Int?) -> Unit
) {
    val density = LocalDensity.current
    val nodeWidth = with(density) { 120.dp.toPx() }
    val nodeHeight = with(density) { 150.dp.toPx() }
    val horizontalSpacing = with(density) { 180.dp.toPx() }
    val verticalSpacing = with(density) { 220.dp.toPx() }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Plus icons for regular edges
        edges.forEach { edge ->
            // Use key to ensure proper identity tracking
            androidx.compose.runtime.key(edge.fromId, edge.toId) {
                val fromNode = nodes[edge.fromId]
                val toNode = nodes[edge.toId]

                if (fromNode != null && toNode != null) {
                    val fromPos = calculateNodePosition(
                        fromNode,
                        with(density) { screenWidth.toPx() },
                        horizontalSpacing,
                        verticalSpacing,
                        pan,
                        zoom
                    )
                    val toPos = calculateNodePosition(
                        toNode,
                        with(density) { screenWidth.toPx() },
                        horizontalSpacing,
                        verticalSpacing,
                        pan,
                        zoom
                    )

                    // Calculate middle point of the edge (approximation using simple midpoint)
                    val startY = fromPos.y + nodeHeight
                    val endY = toPos.y
                    val midX = (fromPos.x + toPos.x) / 2f + nodeWidth / 2f
                    val midY = (startY + endY) / 2f

                    PlusIcon(
                        x = midX,
                        y = midY,
                        onClick = { onPlusClick(edge.fromId, edge.toId) },
                        onLongClick = { onPlusLongPress(edge.fromId, edge.toId) }
                    )
                }
            }
        }

        // Plus icons for leaf nodes
        val nodesWithChildren = edges.map { it.fromId }.toSet()
        val leafNodes = nodes.values.filter { it.id !in nodesWithChildren }

        leafNodes.forEach { leafNode ->
            // Use key to ensure proper identity tracking
            androidx.compose.runtime.key(leafNode.id) {
                val fromPos = calculateNodePosition(
                    leafNode,
                    with(density) { screenWidth.toPx() },
                    horizontalSpacing,
                    verticalSpacing,
                    pan,
                    zoom
                )

                // Calculate middle point of the leaf edge
                val startY = fromPos.y + nodeHeight
                val edgeLength = with(density) { 100.dp.toPx() }
                val midX = fromPos.x + nodeWidth / 2f
                val midY = startY + edgeLength / 2f

                PlusIcon(
                    x = midX,
                    y = midY,
                    onClick = { onPlusClick(leafNode.id, null) },
                    onLongClick = { onPlusLongPress(leafNode.id, null) }
                )
            }
        }
    }
}

@Composable
private fun PlusIcon(
    x: Float,
    y: Float,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)
    val iconSizeDp = 32f
    val iconSizePx = with(density) { iconSizeDp.dp.toPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (x - iconSizePx / 2).roundToInt(),
                    (y - iconSizePx / 2).roundToInt()
                )
            }
            .size(iconSizeDp.dp)
            .clip(CircleShape)
            .background(Color(0xFF2a2a2a))
            .then(
                if (onLongClick != null) {
                    Modifier.pointerInput(Unit) {
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
                            }

                            // Wait for release
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })

                            // Clean up
                            longPressJob.cancel()

                            if (longPressTriggered) {
                                onLongClick()
                            } else {
                                onClick()
                            }
                        }
                    }
                } else {
                    Modifier.clickable { onClick() }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_add),
            contentDescription = "Add node",
            modifier = Modifier.size((iconSizeDp * 0.75f).dp),
            tint = Color(0xFF888888)
        )
    }
}