package hr.jkacan.setmaker.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import hr.jkacan.setmaker.R

class AnimatedCoverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val baseImageView: AppCompatImageView = AppCompatImageView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0.2f
    }
    private val overlayImageView: ClippableImageView = ClippableImageView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    private var progress = 1f

    init {
        addView(baseImageView)
        addView(overlayImageView)
        overlayImageView.setClipProgress(1f)
    }

    fun getBaseImageView(): AppCompatImageView = baseImageView
    fun getOverlayImageView(): AppCompatImageView = overlayImageView

    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, 1f)
        overlayImageView.setClipProgress(this.progress)
    }

    fun reset() {
        progress = 0f
        overlayImageView.setClipProgress(1f)
    }

    private class ClippableImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : AppCompatImageView(context, attrs, defStyleAttr) {

        private var clipProgress = 1f
        private val clipRect = Rect()

        fun setClipProgress(progress: Float) {
            this.clipProgress = progress.coerceIn(0f, 1f)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            if (clipProgress >= 1f) {
                // Draw fully without clipping when progress is 1f
                super.onDraw(canvas)
            } else if (clipProgress > 0f) {
                val revealHeight = (height * clipProgress).toInt()
                clipRect.set(0, height - revealHeight, width, height)

                canvas.save()
                canvas.clipRect(clipRect)
                super.onDraw(canvas)
                canvas.restore()
            }
        }
    }
}
