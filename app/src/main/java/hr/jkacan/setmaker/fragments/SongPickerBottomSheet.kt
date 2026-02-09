package hr.jkacan.setmaker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import hr.jkacan.setmaker.SetMakerApplication
import hr.jkacan.setmaker.adapters.SongAdapter
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.databinding.SheetSongPickerBinding
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.utils.AudioPreviewManager
import hr.jkacan.setmaker.utils.getServiceOrNull

class SongPickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SheetSongPickerBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SongAdapter
    private lateinit var songRepository: SongRepository
    private lateinit var audioPreviewManager: AudioPreviewManager
    private lateinit var soundcloudService: SoundcloudService


    var onSongSelected: ((Song) -> Unit)? = null

    companion object {
        private const val ARG_ADDED_SONG_IDS = "added_song_ids"

        fun newInstance(savedSongIds: List<String>? = null): SongPickerBottomSheet {
            return SongPickerBottomSheet().apply {
                arguments = Bundle().apply {
                    savedSongIds?.let {
                        putStringArrayList(ARG_ADDED_SONG_IDS, ArrayList(it))
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = requireActivity().application as SetMakerApplication
        songRepository = application.songRepository
        audioPreviewManager = application.audioPreviewManager
        soundcloudService = application.soundcloudService
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetSongPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        binding.songPickerRecyclerView.layoutManager = LinearLayoutManager(context)

        // Get saved song IDs from arguments
        val addedSongIds = arguments?.getStringArrayList(ARG_ADDED_SONG_IDS)

        // Load all saved songs
        val songs = songRepository.getAll().sortedByDescending { it.dateAdded }

        // Load Soundcloud if needed
        if (songs.any { it.provider == SongProvider.SOUNDCLOUD }) {
            soundcloudService =
                (requireActivity().application as SetMakerApplication).soundcloudService
        }

        val scServiceOrNull = getServiceOrNull(::soundcloudService)

        adapter = SongAdapter(
            songs,
            addedSongIds,
            onItemClick = { song ->
                onSongSelected?.invoke(song)
                dismiss()
            },
            onItemLongPress = { },
            audioPreviewManager = audioPreviewManager,
            soundcloudService = scServiceOrNull,
            prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        binding.songPickerRecyclerView.adapter = adapter

        // Set up filter chips
//        binding.chipSpotify.setOnCheckedChangeListener { _, _ -> filterSongs() }
//        binding.chipSoundcloud.setOnCheckedChangeListener { _, _ -> filterSongs() }
//        binding.chipLocal.setOnCheckedChangeListener { _, _ -> filterSongs() }

        // Set up search bar
        binding.searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterSongs()
                true
            } else {
                false
            }
        }
    }

    private fun filterSongs() {
//        val selectedProviders = mutableListOf<SongProvider>()
        val filterQuery = binding.searchBar.text.toString().trim().lowercase()

//        if (binding.chipSpotify.isChecked) selectedProviders.add(SongProvider.SPOTIFY)
//        if (binding.chipSoundcloud.isChecked) selectedProviders.add(SongProvider.SOUNDCLOUD)
//        if (binding.chipLocal.isChecked) selectedProviders.add(SongProvider.LOCAL)

        // Start with all songs
        var filteredSongs = songRepository.getAll()

        // Filter by provider if any selected
//        if (selectedProviders.isNotEmpty()) {
//            filteredSongs = filteredSongs.filter { it.provider in selectedProviders }
//        }

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioPreviewManager.stop()
        _binding = null
    }
}




