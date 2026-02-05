package hr.jkacan.setmaker.utils

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

class AudioPreviewManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressCallback: ((Float) -> Unit)? = null
    private var completionCallback: (() -> Unit)? = null

    fun play(url: String, onProgress: (Float) -> Unit, onComplete: () -> Unit) {
        stop()
        currentUrl = url
        progressCallback = onProgress
        completionCallback = onComplete

        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener {
                start()
                startProgressTracking()
            }
            setOnCompletionListener {
                onComplete()
                stop()
            }
            setOnErrorListener { _, _, _ ->
                onComplete()
                stop()
                true
            }
        }
    }

    private fun startProgressTracking() {
        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val progress = player.currentPosition.toFloat() / player.duration.toFloat()
                        progressCallback?.invoke(progress)
                        handler.postDelayed(this, 50)
                    }
                }
            }
        })
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentUrl = null
        progressCallback = null
        completionCallback = null
    }

    fun isPlaying(url: String): Boolean {
        return currentUrl == url && mediaPlayer?.isPlaying == true
    }
}
