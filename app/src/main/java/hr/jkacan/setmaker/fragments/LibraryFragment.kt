package hr.jkacan.setmaker.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import hr.jkacan.setmaker.adapters.SongAdapter
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import hr.jkacan.setmaker.activities.MainActivity
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.utils.AudioPreviewManager
import hr.jkacan.setmaker.utils.getServiceOrNull

class LibraryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLibrary: View
    private lateinit var adapter: SongAdapter
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var searchBar: EditText
    private lateinit var chipSpotify: Chip
    private lateinit var chipSoundcloud: Chip
    private lateinit var chipLocal: Chip
    private lateinit var audioPreviewManager: AudioPreviewManager
    private lateinit var soundcloudService: SoundcloudService

    override fun onAttach(context: Context) {
        super.onAttach(context)
        audioPreviewManager = AudioPreviewManager(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        recyclerView = view.findViewById(R.id.library_recycler_view)
        emptyStateLibrary = view.findViewById(R.id.empty_state_library)
        fabAdd = view.findViewById(R.id.fab_add)
        searchBar = view.findViewById(R.id.search_bar)
        chipSpotify = view.findViewById(R.id.chip_spotify)
        chipSoundcloud = view.findViewById(R.id.chip_soundcloud)
        chipLocal = view.findViewById(R.id.chip_local)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // Get the SongRepository from MainActivity
        val songRepository = (requireActivity() as MainActivity).songRepository

        // Load all saved songs
        val songs = songRepository.getAll().sortedByDescending { it.dateAdded }

        updateEmptyState(songs)

        if (songs.any { it.provider == SongProvider.SOUNDCLOUD }) soundcloudService =
            SoundcloudService()

        val scServiceOrNull = getServiceOrNull(::soundcloudService)

        adapter = SongAdapter(
            songs,
            onItemClick = { song -> },
            onItemLongPress = { song -> showSongOptionsModal(song) },
            audioPreviewManager = audioPreviewManager,
            savedSongPlatformIds = null,
            soundcloudService = scServiceOrNull
        )

        recyclerView.adapter = adapter

        chipSpotify.setOnCheckedChangeListener { _, _ -> filterSongs(songRepository) }
        chipSoundcloud.setOnCheckedChangeListener { _, _ -> filterSongs(songRepository) }
        chipLocal.setOnCheckedChangeListener { _, _ -> filterSongs(songRepository) }

        searchBar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterSongs(songRepository)
                true
            } else {
                false
            }
        }

        fabAdd.setOnClickListener {
            (requireActivity() as MainActivity).findViewById<BottomNavigationView>(R.id.bottom_navigation)
                .selectedItemId = R.id.nav_query
        }

        return view
    }

    private fun updateEmptyState(songs: List<Song>) {
        if (songs.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateLibrary.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLibrary.visibility = View.GONE
        }
    }

    private fun filterSongs(songRepository: SongRepository) {
        val selectedProviders = mutableListOf<SongProvider>()
        val filterQuery = searchBar.text.toString().trim().lowercase()

        if (chipSpotify.isChecked) selectedProviders.add(SongProvider.SPOTIFY)
        if (chipSoundcloud.isChecked) selectedProviders.add(SongProvider.SOUNDCLOUD)
        if (chipLocal.isChecked) selectedProviders.add(SongProvider.LOCAL)

        // Start with all songs
        var filteredSongs = songRepository.getAll()

        // Filter by provider if any selected
        if (selectedProviders.isNotEmpty()) {
            filteredSongs = filteredSongs.filter { it.provider in selectedProviders }
        }

        // Apply text filter if query exists
        if (filterQuery.isNotEmpty()) {
            filteredSongs = filteredSongs.filter { song ->
                song.title.lowercase().contains(filterQuery) ||
                        song.artist.lowercase().contains(filterQuery)
            }
        }

        // Sort by date added
        filteredSongs = filteredSongs.sortedByDescending { it.dateAdded }

        adapter.updateSongs(filteredSongs, null)
        updateEmptyState(filteredSongs)
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