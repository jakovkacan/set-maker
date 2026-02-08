package hr.jkacan.setmaker.editor.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun AnimatedCoverImage(
    coverUrl: String?,
    contentDescription: String,
    isBuffering: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val isPlaying = progress > 0f

    // Track if we've just transitioned from idle to playing
    var previousIsPlaying by remember { mutableStateOf(false) }

    // Animated progress that tweens from 1f to 0f when playback starts
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(isPlaying, progress) {
        if (isPlaying) {
            if (!previousIsPlaying) {
                // Just started playing - animate from full (1f) to empty (0f)
                previousIsPlaying = true
                animatedProgress.snapTo(1f)
                animatedProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 500)
                )
            } else {
                // Already playing - track actual audio progress
                animatedProgress.snapTo(progress)
            }
        } else {
            // Stopped playing - reset everything
            previousIsPlaying = false
            animatedProgress.snapTo(0f)
        }
    }

    val displayProgress = animatedProgress.value

    Box(
        modifier = modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Base image - dimmed when playing, full opacity when idle
        AsyncImage(
            model = coverUrl,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isPlaying) 0.2f else 1f),
            contentScale = ContentScale.Crop
        )

        // Overlay image with progress reveal (from bottom to top) - only when playing
        if (isPlaying) {
            // Use drawWithContent to apply a clipping mask that reveals from bottom to top
            AsyncImage(
                model = coverUrl,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(100.dp)
                    .drawWithContent {
                        // Calculate the reveal height in pixels using animated progress
                        val revealHeight = size.height * displayProgress

                        // Create a clipping path that shows only the bottom portion
                        val clipPath = Path().apply {
                            addRect(
                                Rect(
                                    left = 0f,
                                    top = size.height - revealHeight,  // Top of visible area
                                    right = size.width,
                                    bottom = size.height  // Bottom is always at the bottom
                                )
                            )
                        }

                        // Clip and draw the content
                        clipPath(clipPath, clipOp = ClipOp.Intersect) {
                            this@drawWithContent.drawContent()
                        }
                    },
                contentScale = ContentScale.Crop
            )
        }

        // Buffering indicator
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 4.dp,
                color = Color.White
            )
        }
    }
}


