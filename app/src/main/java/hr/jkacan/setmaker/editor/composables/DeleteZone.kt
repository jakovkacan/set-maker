package hr.jkacan.setmaker.editor.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import hr.jkacan.setmaker.R
import kotlin.math.pow
import kotlin.math.sqrt

// Delete zone configuration
private const val DELETE_ZONE_RADIUS = 80f // Radius in dp
private const val DELETE_ZONE_BOTTOM_MARGIN = 48f // Margin from bottom in dp

/**
 * Delete zone that appears at the bottom center of screen when dragging a node.
 * Shows a trash can icon that highlights when a node is dragged over it.
 */
@Composable
fun DeleteZone(
    isVisible: Boolean,
    isHighlighted: Boolean
) {
    if (!isVisible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(9f), // Below debug overlay (10f) but above everything else
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 48.dp)
                .size(65.dp)
                .background(
                    color = if (isHighlighted) {
                        Color(0xFFFF5252) // Red when highlighted
                    } else {
                        Color(0xFF424242) // Dark gray normally
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "Delete node",
                modifier = Modifier.size(30.dp),
                tint = if (isHighlighted) {
                    Color.White
                } else {
                    Color(0xFFBDBDBD) // Light gray
                }
            )
        }
    }
}

/**
 * Checks if a screen position is within the delete zone (circular area at bottom center)
 */
fun isInDeleteZone(
    screenPos: Offset,
    screenWidth: Float,
    screenHeight: Float,
    density: Float
): Boolean {
    val deleteZoneRadius = DELETE_ZONE_RADIUS * density
    val bottomMargin = DELETE_ZONE_BOTTOM_MARGIN * density

    // Delete zone center position
    val deleteZoneCenterX = screenWidth / 2f
    val deleteZoneCenterY = screenHeight - bottomMargin - deleteZoneRadius

    // Calculate distance from drag position to delete zone center
    val dx = screenPos.x - deleteZoneCenterX
    val dy = screenPos.y - deleteZoneCenterY
    val distance = sqrt(dx.pow(2) + dy.pow(2))

    return distance <= deleteZoneRadius
}