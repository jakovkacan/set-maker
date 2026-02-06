package hr.jkacan.setmaker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.utils.AudioPreviewManager
import hr.jkacan.setmaker.views.AnimatedCoverView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SongAdapter(
    private var songs: List<Song>,
    private var savedSongPlatformIds: List<String>?,
    private val onItemClick: (Song) -> Unit,
    private val onItemLongPress: (Song) -> Unit,
    private val audioPreviewManager: AudioPreviewManager,
    private val soundcloudService: SoundcloudService? = null

) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var currentPlayingPosition: Int = RecyclerView.NO_POSITION

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverView: AnimatedCoverView = itemView.findViewById(R.id.song_cover)
        val title: TextView = itemView.findViewById(R.id.song_title)
        val artist: TextView = itemView.findViewById(R.id.song_artist)
        val providerIcon: ImageView = itemView.findViewById(R.id.provider_icon)
        private val bufferingIndicator: com.google.android.material.progressindicator.CircularProgressIndicator =
            itemView.findViewById(R.id.buffering_indicator)

        init {
            coverView.setBufferingIndicator(bufferingIndicator)

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(songs[position])
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongPress(songs[position])
                }
                true
            }

            coverView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    handleCoverClick(songs[position], coverView)
                }
            }
        }

        private fun handleCoverClick(song: Song, coverView: AnimatedCoverView) {
            val previewUrl = song.previewUrl
            if (previewUrl.isNullOrBlank()) return

            if (audioPreviewManager.isPlaying(previewUrl)) {
                audioPreviewManager.stop()
                coverView.reset()
                currentPlayingPosition = RecyclerView.NO_POSITION
            } else {
                // Reset previously playing item's animation
                if (currentPlayingPosition != RecyclerView.NO_POSITION && currentPlayingPosition != position) {
                    notifyItemChanged(currentPlayingPosition)
                }

                currentPlayingPosition = position
                coverView.showBuffering()

                val scope = (itemView.context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope
                    ?: CoroutineScope(Dispatchers.Main)

                scope.launch {
                    val authToken = if (song.provider == SongProvider.SOUNDCLOUD) {
                        soundcloudService?.getToken()
                    } else {
                        null
                    }

                    audioPreviewManager.play(
                        url = previewUrl,
                        authToken = authToken,
                        onBuffering = { isBuffering ->
                            if (isBuffering) coverView.showBuffering() else coverView.hideBuffering()
                        },
                        onProgress = { progress ->
                            coverView.setProgress(progress)
                        },
                        onComplete = {
                            coverView.reset()
                            currentPlayingPosition = RecyclerView.NO_POSITION
                        }
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        holder.artist.text = song.artist

        // Set provider icon
        val providerIcon = when (song.provider) {
            SongProvider.SPOTIFY -> R.drawable.ic_spotify
            SongProvider.SOUNDCLOUD -> R.drawable.ic_soundcloud
            SongProvider.LOCAL -> R.drawable.ic_local_file
        }
        holder.providerIcon.setImageResource(
            if (savedSongPlatformIds == null) providerIcon
            else if (savedSongPlatformIds!!.contains(song.platformId)) R.drawable.ic_circle_check else R.drawable.ic_add
        )

        holder.coverView.reset()
        if (song.coverUrl.isNullOrBlank()) {
            holder.coverView.getBaseImageView().setImageResource(R.drawable.placeholder_album_cover)
            holder.coverView.getOverlayImageView()
                .setImageResource(R.drawable.placeholder_album_cover)
        } else {
            holder.coverView.getBaseImageView().load(song.coverUrl) {
                crossfade(true)
                placeholder(R.drawable.placeholder_album_cover)
                transformations(RoundedCornersTransformation(16f))
            }
            holder.coverView.getOverlayImageView().load(song.coverUrl) {
                crossfade(true)
                placeholder(R.drawable.placeholder_album_cover)
                transformations(RoundedCornersTransformation(16f))
            }
        }
    }

    override fun getItemCount(): Int = songs.size

    fun updateSongs(newSongs: List<Song>, newSavedSongsIds: List<String>? = null) {
        val diffCallback = SongDiffCallback(songs, newSongs)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        songs = newSongs
        savedSongPlatformIds = newSavedSongsIds
        diffResult.dispatchUpdatesTo(this)
    }

    fun stopCurrentPreview() {
        if (currentPlayingPosition != RecyclerView.NO_POSITION) {
            audioPreviewManager.stop()
            notifyItemChanged(currentPlayingPosition)
            currentPlayingPosition = RecyclerView.NO_POSITION
        }
    }

    fun updateSongSavedState(platformId: String, isSaved: Boolean) {
        val position = songs.indexOfFirst { it.platformId == platformId }
        if (position != RecyclerView.NO_POSITION) {
            val currentIds = savedSongPlatformIds?.toMutableList() ?: mutableListOf()
            if (isSaved) {
                if (!currentIds.contains(platformId)) {
                    currentIds.add(platformId)
                }
            } else {
                currentIds.remove(platformId)
            }
            savedSongPlatformIds = currentIds
            notifyItemChanged(position)
        }
    }


    private class SongDiffCallback(
        private val oldList: List<Song>,
        private val newList: List<Song>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
