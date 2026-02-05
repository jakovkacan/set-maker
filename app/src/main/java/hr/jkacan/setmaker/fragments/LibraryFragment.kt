package hr.jkacan.setmaker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import hr.jkacan.setmaker.adapters.SongAdapter
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import hr.jkacan.setmaker.activities.MainActivity
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.utils.AudioPreviewManager

class LibraryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var chipSpotify: Chip
    private lateinit var chipSoundcloud: Chip
    private lateinit var chipLocal: Chip
    private val audioPreviewManager = AudioPreviewManager()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        recyclerView = view.findViewById(R.id.library_recycler_view)
        fabAdd = view.findViewById(R.id.fab_add)
        chipSpotify = view.findViewById(R.id.chip_spotify)
        chipSoundcloud = view.findViewById(R.id.chip_soundcloud)
        chipLocal = view.findViewById(R.id.chip_local)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // Get the SongRepository from MainActivity
        val songRepository = (requireActivity() as MainActivity).songRepository

        // Load all saved songs
        val songs = songRepository.getAll().sortedByDescending { it.dateAdded }

        adapter = SongAdapter(
            songs,
            onItemClick = { song -> },
            onItemLongPress = { song -> showSongOptionsModal(song) },
            audioPreviewManager = audioPreviewManager,
            savedSongPlatformIds = null,
        )

        recyclerView.adapter = adapter

        chipSpotify.setOnCheckedChangeListener { _, _ -> filterSongs(songRepository) }
        chipSoundcloud.setOnCheckedChangeListener { _, _ -> filterSongs(songRepository) }
        chipLocal.setOnCheckedChangeListener { _, _ -> filterSongs(songRepository) }


        fabAdd.setOnClickListener {
            // Navigate to add song
        }

        return view
    }

    private fun filterSongs(songRepository: hr.jkacan.setmaker.data.dao.SongRepository) {
        val selectedProviders = mutableListOf<SongProvider>()

        if (chipSpotify.isChecked) selectedProviders.add(SongProvider.SPOTIFY)
        if (chipSoundcloud.isChecked) selectedProviders.add(SongProvider.SOUNDCLOUD)
        if (chipLocal.isChecked) selectedProviders.add(SongProvider.LOCAL)

        val filteredSongs = if (selectedProviders.isEmpty()) {
            songRepository.getAll()
        } else {
            selectedProviders.flatMap { provider ->
                songRepository.getSongsByProvider(provider)
            }
        }.sortedByDescending { it.dateAdded }

        adapter = SongAdapter(
            filteredSongs,
            onItemClick = { song -> },
            onItemLongPress = { song -> showSongOptionsModal(song) },
            audioPreviewManager = audioPreviewManager,
            savedSongPlatformIds = null
        )
        recyclerView.adapter = adapter
    }


    private fun showSongOptionsModal(song: Song) {
        val modalFragment = SongOptionsBottomSheet.newInstance(song)
        modalFragment.onSongDeleted = {
            refreshSongs()
        }
        modalFragment.show(parentFragmentManager, "SongOptions")
    }

    private fun refreshSongs() {
        val songRepository = (requireActivity() as MainActivity).songRepository
        filterSongs(songRepository)
    }
}