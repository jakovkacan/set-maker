package hr.jkacan.setmaker.editor.gestures

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import hr.jkacan.setmaker.editor.composables.EditorCanvasState
import hr.jkacan.setmaker.editor.layout.GraphLayoutCalculator.canvasToScreenCoordinates
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gesture detector that supports click, long press, and drag operations.
 */
class GestureDetector(
    private val vibrator: Vibrator?,
    private val longPressDuration: Long = 500L,
    private val vibrationDuration: Long = 50L
) {
    /**
     * Detects long press and drag gestures.
     * Used for draggable elements like nodes.
     *
     * @param id Identifier for the element being dragged
     * @param onDragStart Called when long press is completed and drag begins
     * @param onDrag Called during drag with the offset delta from initial touch point, canvas position, hovered node, and hovered edge
     * @param onDragEnd Called when drag ends
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun PointerInputScope.detectLongPressDrag(
        id: Int,
        nodePosition: Offset,
        canvasState: EditorCanvasState,
        onDragStart: (Int) -> Unit,
        onDrag: (Offset, Offset) -> Unit,
        onDragEnd: () -> Unit,
        onDebugDragUpdate: (screenPos: Offset, canvasPos: Offset) -> Unit = { _, _ -> },
    ) {
        var cumulativeDrag: Offset
        var canvasCoordinates: Offset

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            cumulativeDrag = Offset.Zero
            canvasCoordinates = nodePosition + down.position
            var longPressTriggered = false

            val longPressJob: Job = GlobalScope.launch {
                delay(longPressDuration)
                longPressTriggered = true

                triggerVibration()
                onDragStart(id)
            }

            do {
                val event = awaitPointerEvent()

                if (longPressTriggered) {
                    event.changes.forEach { change ->
                        if (change.pressed) {
                            val delta = change.position - change.previousPosition

                            val dragAmount = change.position - down.position
                            cumulativeDrag += delta
                            canvasCoordinates += delta

                            onDrag(dragAmount, canvasCoordinates)

                            onDebugDragUpdate(
                                canvasToScreenCoordinates(canvasCoordinates, canvasState),
                                canvasCoordinates
                            )

                            change.consume()
                        }
                    }
                }
            } while (event.changes.any { it.pressed })

            longPressJob.cancel()
            if (longPressTriggered) {
                onDragEnd()
            }
        }
    }

    /**
     * Detects click and long press gestures without dragging.
     * Used for interactive elements like buttons.
     *
     * @param onClick Called when a short press is detected
     * @param onLongPress Called when a long press is detected
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun PointerInputScope.detectClickAndLongPress(
        onClick: () -> Unit,
        onLongPress: () -> Unit
    ) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var longPressTriggered = false
            val longPressJob: Job

            longPressJob = GlobalScope.launch {
                delay(longPressDuration)
                longPressTriggered = true

                triggerVibration()
            }

            do {
                val event = awaitPointerEvent()
            } while (event.changes.any { it.pressed })

            longPressJob.cancel()

            if (longPressTriggered) {
                onLongPress()
            } else {
                onClick()
            }
        }
    }

    @Suppress("MissingPermission")
    private fun triggerVibration() {
        vibrator?.vibrate(
            VibrationEffect.createOneShot(
                vibrationDuration,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }
}

/**
 * Modifier extension for click and long press gestures.
 */
fun Modifier.clickAndLongPress(
    detector: GestureDetector,
    onClick: () -> Unit,
    onLongPress: () -> Unit
): Modifier = this.pointerInput(Unit) {
    with(detector) {
        detectClickAndLongPress(onClick, onLongPress)
    }
}




