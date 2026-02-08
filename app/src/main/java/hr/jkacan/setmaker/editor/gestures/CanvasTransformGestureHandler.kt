package hr.jkacan.setmaker.editor.gestures

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * Handles pan and zoom gestures for the canvas with scale limiting and centroid-based zooming.
 */
class CanvasTransformGestureHandler(
    private val minScale: Float = 0.5f,
    private val maxScale: Float = 3f
) {
    /**
     * Detects pan and zoom gestures on the canvas.
     *
     * @param centerX The X coordinate of the canvas center (used for zoom pivot)
     * @param centerY The Y coordinate of the canvas center (used for zoom pivot)
     * @param onTransform Called with the new scale and offset values
     */
    suspend fun PointerInputScope.detectCanvasTransform(
        centerX: Float,
        centerY: Float,
        getCurrentScale: () -> Float,
        getCurrentOffset: () -> Offset,
        onTransform: (newScale: Float, newOffset: Offset) -> Unit
    ) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            val currentScale = getCurrentScale()
            val currentOffset = getCurrentOffset()

            // 1. Apply zoom limiting
            val newScale = (currentScale * zoom).coerceIn(minScale, maxScale)

            // 2. Calculate zoom offset to zoom towards centroid
            val zoomOffset = Offset(
                x = (1 - zoom) * (centroid.x - currentOffset.x - centerX),
                y = (1 - zoom) * (centroid.y - currentOffset.y - centerY)
            )

            // 3. Combine zoom offset and pan
            val newOffset = currentOffset + zoomOffset + pan

            // 4. Apply the transformation
            onTransform(newScale, newOffset)
        }
    }
}

