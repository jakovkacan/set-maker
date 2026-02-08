package hr.jkacan.setmaker.editor.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import hr.jkacan.setmaker.models.editor.UiNode

@Composable
fun LeafEdge(
    leafNode: UiNode,
    targetNode: UiNode?,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    hasValidTarget: Boolean = false,
    dragOffset: Offset = Offset.Zero,
    debugMode: Boolean = false
) {
    val density = LocalDensity.current
    val edgeLength = with(density) { 100.dp.toPx() }

    // Choose color based on state
    val edgeColor = when {
        isDragging && hasValidTarget -> Color.White
        isDragging -> Color(0xFFFFAA00)
        debugMode -> Color(0xFFFF00FF) // Magenta in debug mode
        else -> Color(0xFF888888)
    }
    val strokeWidth = if (isDragging) 6f else if (debugMode) 5f else 4f

    // Use a larger canvas to accommodate drag in any direction
    // This ensures the line doesn't get clipped when dragged
    Canvas(
        modifier = modifier
            .size(width = 80.dp, height = 180.dp)
    ) {
        // Start point is always at a fixed offset within the large canvas
        // Center it horizontally and give some vertical padding
        // Note: size.width is in pixels (the actual canvas pixel size)
        val verticalPaddingPx = 100.dp.toPx()
        val start = Offset(size.width / 2f, verticalPaddingPx)

        // End point (adjusted by drag if dragging)
        val end = if (isDragging) {
            Offset(
                size.width / 2f + dragOffset.x,
                verticalPaddingPx + edgeLength + dragOffset.y
            )
        } else {
            Offset(size.width / 2f, verticalPaddingPx + edgeLength)
        }

        // Draw curved line (like EdgeLayer) or straight line if pointing down
        if (isDragging) {
            // Calculate if the line is pointing significantly horizontally
            val horizontalDistance = kotlin.math.abs(end.x - start.x)
            val verticalDistance = end.y - start.y

            // Use curved line if there's horizontal displacement
            if (horizontalDistance > 10f || verticalDistance < edgeLength * 0.8f) {
                val controlPointOffset = verticalDistance * 0.5f

                val controlPoint1 = Offset(
                    start.x,
                    start.y + controlPointOffset
                )

                val controlPoint2 = Offset(
                    end.x,
                    end.y - controlPointOffset
                )

                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        end.x, end.y
                    )
                }

                drawPath(
                    path = path,
                    color = edgeColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            } else {
                // Straight vertical line when pointing down
                drawLine(
                    color = edgeColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth
                )
            }
        } else {
            // Normal state: straight vertical line
            drawLine(
                color = edgeColor,
                start = start,
                end = end,
                strokeWidth = strokeWidth
            )
        }

        // Draw arrow at the end
        val arrowSize = 20f
        val angle = Math.PI / 2 // 90 degrees (pointing down)
        val arrowAngle1 = angle + Math.PI * 5 / 6
        val arrowAngle2 = angle - Math.PI * 5 / 6

        val arrowPoint1 = Offset(
            x = end.x + (arrowSize * kotlin.math.cos(arrowAngle1)).toFloat(),
            y = end.y + (arrowSize * kotlin.math.sin(arrowAngle1)).toFloat()
        )

        val arrowPoint2 = Offset(
            x = end.x + (arrowSize * kotlin.math.cos(arrowAngle2)).toFloat(),
            y = end.y + (arrowSize * kotlin.math.sin(arrowAngle2)).toFloat()
        )

        drawLine(
            color = edgeColor,
            start = end,
            end = arrowPoint1,
            strokeWidth = strokeWidth
        )

        drawLine(
            color = edgeColor,
            start = end,
            end = arrowPoint2,
            strokeWidth = strokeWidth
        )

        // Debug mode: Draw hit detection area
        if (debugMode) {
            val hitRadius = 40f
            val startY = 100.dp.toPx()

            // Draw hit detection rectangle
            drawRect(
                color = Color(0x4DFF00FF), // Semi-transparent magenta
                topLeft = Offset(
                    size.width / 2f - hitRadius,
                    startY - hitRadius
                ),
                size = androidx.compose.ui.geometry.Size(
                    hitRadius * 2,
                    edgeLength + hitRadius * 2
                ),
                style = Stroke(width = 2f)
            )

            // Draw center line indicator
            drawLine(
                color = Color(0xAAFF00FF),
                start = Offset(size.width / 2f, startY - hitRadius),
                end = Offset(size.width / 2f, startY + edgeLength + hitRadius),
                strokeWidth = 1f
            )

            // Draw hit radius circles at start and end
            drawCircle(
                color = Color(0x66FF00FF),
                radius = hitRadius,
                center = Offset(size.width / 2f, startY),
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = Color(0x66FF00FF),
                radius = hitRadius,
                center = Offset(size.width / 2f, startY + edgeLength),
                style = Stroke(width = 1f)
            )
        }
    }
}




