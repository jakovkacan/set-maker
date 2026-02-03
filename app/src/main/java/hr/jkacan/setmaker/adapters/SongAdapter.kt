package hr.jkacan.setmaker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.models.Song
import hr.jkacan.setmaker.models.SongProvider

class SongAdapter(
    private val songs: List<Song>,
    private val onItemClick: (Song) -> Unit,
    private val onItemLongPress: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImage: ImageView = itemView.findViewById(R.id.song_cover)
        val title: TextView = itemView.findViewById(R.id.song_title)
        val artist: TextView = itemView.findViewById(R.id.song_artist)
        val providerIcon: ImageView = itemView.findViewById(R.id.provider_icon)
        val pinIcon: ImageView = itemView.findViewById(R.id.pin_icon)

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
        holder.providerIcon.setImageResource(providerIcon)

        // Show pin icon if pinned
        holder.pinIcon.visibility = if (song.isPinned) View.VISIBLE else View.GONE

        // Load cover image
        // Glide.with(holder.itemView.context).load(song.coverUrl).into(holder.coverImage)
    }

    override fun getItemCount(): Int = songs.size
}
