package hr.jkacan.setmaker.editor.gestures

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Modifier extension for canvas pan and zoom gestures.
 *
 * @param centerX The X coordinate of the canvas center (used for zoom pivot)
 * @param centerY The Y coordinate of the canvas center (used for zoom pivot)
 * @param minScale Minimum allowed scale (default 0.5f)
 * @param maxScale Maximum allowed scale (default 3f)
 * @param getCurrentScale Function to get the current canvas scale
 * @param getCurrentOffset Function to get the current canvas offset
 * @param onTransform Called with the new scale and offset values when transform occurs
 */
fun Modifier.canvasTransformGestures(
    centerX: Float,
    centerY: Float,
    minScale: Float = 0.5f,
    maxScale: Float = 3f,
    getCurrentScale: () -> Float,
    getCurrentOffset: () -> Offset,
    onTransform: (newScale: Float, newOffset: Offset) -> Unit
): Modifier = this.pointerInput(centerX, centerY) {
    val handler = CanvasTransformGestureHandler(minScale, maxScale)
    with(handler) {
        detectCanvasTransform(
            centerX = centerX,
            centerY = centerY,
            getCurrentScale = getCurrentScale,
            getCurrentOffset = getCurrentOffset,
            onTransform = onTransform
        )
    }
}

/**
 * Modifier extension for debug tap detection on canvas.
 *
 * @param enabled Whether debug tap detection is enabled
 * @param getCurrentScale Function to get the current canvas scale
 * @param getCurrentOffset Function to get the current canvas offset
 * @param onTap Called with screen position and canvas position when a tap is detected
 */
fun Modifier.debugTapDetection(
    enabled: Boolean,
    getCurrentScale: () -> Float,
    getCurrentOffset: () -> Offset,
    onTap: (screenPos: Offset, canvasPos: Offset) -> Unit
): Modifier = if (enabled) {
    this.pointerInput(Unit) {
        val detector = DebugTapDetector()
        with(detector) {
            detectDebugTap(
                getCurrentScale = getCurrentScale,
                getCurrentOffset = getCurrentOffset,
                onTap = onTap
            )
        }
    }
} else {
    this
}

