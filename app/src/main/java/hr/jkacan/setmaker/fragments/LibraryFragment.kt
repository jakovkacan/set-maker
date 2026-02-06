package hr.jkacan.setmaker.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import hr.jkacan.setmaker.adapters.SongAdapter
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.activities.MainActivity
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.databinding.FragmentLibraryBinding
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.utils.AudioPreviewManager
import hr.jkacan.setmaker.utils.getServiceOrNull

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SongAdapter
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
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.libraryRecyclerView.layoutManager = LinearLayoutManager(context)

        // Get the SongRepository from MainActivity
        val songRepository = (requireActivity() as MainActivity).songRepository

        // Load all saved songs
        val songs = songRepository.getAll().sortedByDescending { it.dateAdded }

        updateEmptyState(songs)

        if (songs.any { it.provider == SongProvider.SOUNDCLOUD }) {
            soundcloudService = SoundcloudService()
        }

        val scServiceOrNull = getServiceOrNull(::soundcloudService)

        adapter = SongAdapter(
            songs,
            onItemClick = { song -> },
            onItemLongPress = { song -> showSongOptionsModal(song) },
            audioPreviewManager = audioPreviewManager,
            savedSongPlatformIds = null,
            soundcloudService = scServiceOrNull
        )

        binding.libraryRecyclerView.adapter = adapter

        binding.chipSpotify.setOnCheckedChangeListener { _, _ -> filterSongs(songRepository) }
        binding.chipSoundcloud.setOnCheckedChangeListener { _, _ -> filterSongs(songRepository) }
        binding.chipLocal.setOnCheckedChangeListener { _, _ -> filterSongs(songRepository) }

        binding.searchBar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterSongs(songRepository)
                true
            } else {
                false
            }
        }

        binding.fabAdd.setOnClickListener {
            (requireActivity() as MainActivity).binding.bottomNavigation.selectedItemId =
                R.id.nav_query
        }
    }

    private fun updateEmptyState(songs: List<Song>) {
        if (songs.isEmpty()) {
            binding.libraryRecyclerView.visibility = View.GONE
            binding.emptyStateLibrary.root.visibility = View.VISIBLE
        } else {
            binding.libraryRecyclerView.visibility = View.VISIBLE
            binding.emptyStateLibrary.root.visibility = View.GONE
        }
    }

    private fun filterSongs(songRepository: SongRepository) {
        val selectedProviders = mutableListOf<SongProvider>()
        val filterQuery = binding.searchBar.text.toString().trim().lowercase()

        if (binding.chipSpotify.isChecked) selectedProviders.add(SongProvider.SPOTIFY)
        if (binding.chipSoundcloud.isChecked) selectedProviders.add(SongProvider.SOUNDCLOUD)
        if (binding.chipLocal.isChecked) selectedProviders.add(SongProvider.LOCAL)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
