package hr.jkacan.setmaker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.utils.AudioPreviewManager
import hr.jkacan.setmaker.views.AnimatedCoverView

class SongAdapter(
    private var songs: List<Song>,
    private val onItemClick: (Song) -> Unit,
    private val onItemLongPress: (Song) -> Unit,
    private val showAddButton: Boolean = false,
    private val audioPreviewManager: AudioPreviewManager

) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var currentPlayingPosition: Int = RecyclerView.NO_POSITION

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverView: AnimatedCoverView = itemView.findViewById(R.id.song_cover)
        val title: TextView = itemView.findViewById(R.id.song_title)
        val artist: TextView = itemView.findViewById(R.id.song_artist)
        val providerIcon: ImageView = itemView.findViewById(R.id.provider_icon)
//        val addIcon: ImageView = itemView.findViewById(R.id.add_icon)

        init {
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
                coverView.setProgress(0f)
                audioPreviewManager.play(
                    url = previewUrl,
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
        holder.providerIcon.setImageResource(if (showAddButton) R.drawable.ic_add else providerIcon)

        holder.coverView.reset()
        if (song.coverUrl.isNullOrBlank()) {
            holder.coverView.getBaseImageView().setImageResource(R.drawable.placeholder_album_cover)
            holder.coverView.getOverlayImageView().setImageResource(R.drawable.placeholder_album_cover)
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

    fun updateSongs(newSongs: List<Song>) {
        val diffCallback = SongDiffCallback(songs, newSongs)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        songs = newSongs
        diffResult.dispatchUpdatesTo(this)
    }

    fun stopCurrentPreview() {
        if (currentPlayingPosition != RecyclerView.NO_POSITION) {
            audioPreviewManager.stop()
            notifyItemChanged(currentPlayingPosition)
            currentPlayingPosition = RecyclerView.NO_POSITION
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
