package hr.jkacan.setmaker.editor.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * Handles tap detection for debug purposes, capturing screen and canvas coordinates.
 */
class DebugTapDetector {
    /**
     * Detects tap gestures and calculates both screen and canvas coordinates.
     *
     * @param getCurrentScale Function to get the current canvas scale
     * @param getCurrentOffset Function to get the current canvas offset
     * @param onTap Called with screen position and canvas position when a tap is detected
     */
    suspend fun PointerInputScope.detectDebugTap(
        getCurrentScale: () -> Float,
        getCurrentOffset: () -> Offset,
        onTap: (screenPos: Offset, canvasPos: Offset) -> Unit
    ) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val screenPos = down.position

            // Calculate canvas position (inverse transform)
            // Transform: screen = canvas * scale + offset
            // Inverse: canvas = (screen - offset) / scale
            val currentScale = getCurrentScale()
            val currentOffset = getCurrentOffset()

            val canvasPos = Offset(
                x = (screenPos.x - currentOffset.x) / currentScale,
                y = (screenPos.y - currentOffset.y) / currentScale
            )

            onTap(screenPos, canvasPos)
        }
    }
}

