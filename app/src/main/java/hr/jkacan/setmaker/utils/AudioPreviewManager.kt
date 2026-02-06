package hr.jkacan.setmaker.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

class AudioPreviewManager(private val context: Context) {
    private var exoPlayer: ExoPlayer? = null
    private var currentUrl: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressCallback: ((Float) -> Unit)? = null
    private var completionCallback: (() -> Unit)? = null
    private var bufferingCallback: ((Boolean) -> Unit)? = null

    @OptIn(UnstableApi::class)
    fun play(
        url: String,
        authToken: String? = null,
        onBuffering: (Boolean) -> Unit,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit
    ) {
        stop()
        currentUrl = url
        progressCallback = onProgress
        completionCallback = onComplete
        bufferingCallback = onBuffering

        // Show buffering immediately
        onBuffering(true)


        val httpFactory = DefaultHttpDataSource.Factory().apply {
            authToken?.takeIf { it.isNotBlank() }?.let { token ->
                setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
            }
        }

        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(url)
                setMediaItem(mediaItem)
                prepare()

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                play()
                                onBuffering(false)
                                startProgressTracking()
                            }

                            Player.STATE_ENDED -> {
                                onBuffering(false)
                                onComplete()
                                stop()
                            }

                            Player.STATE_BUFFERING -> {
                                onBuffering(true)
                            }

                            Player.STATE_IDLE -> {
                                // Do nothing for idle state
                            }
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        onBuffering(false)
                        onComplete()
                        stop()
                    }
                })
            }
    }

    private fun startProgressTracking() {
        handler.post(object : Runnable {
            override fun run() {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val duration = player.duration
                        if (duration > 0) {
                            val progress = player.currentPosition.toFloat() / duration.toFloat()
                            progressCallback?.invoke(progress)
                        }
                        handler.postDelayed(this, 50)
                    }
                }
            }
        })
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.apply {
            stop()
            release()
        }
        exoPlayer = null
        currentUrl = null
        progressCallback = null
        completionCallback = null
        bufferingCallback = null
    }

    fun isPlaying(url: String): Boolean {
        return currentUrl == url && exoPlayer?.isPlaying == true
    }
}
