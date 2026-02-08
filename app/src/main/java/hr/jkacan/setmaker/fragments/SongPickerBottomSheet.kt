package hr.jkacan.setmaker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.SetMakerApplication
import hr.jkacan.setmaker.adapters.SongAdapter
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.utils.AudioPreviewManager
import hr.jkacan.setmaker.utils.getServiceOrNull

class SongPickerBottomSheet : BottomSheetDialogFragment() {

    private lateinit var adapter: SongAdapter
    private lateinit var songRepository: SongRepository
    private lateinit var audioPreviewManager: AudioPreviewManager
    private lateinit var soundcloudService: SoundcloudService

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var chipSpotify: Chip
    private lateinit var chipSoundcloud: Chip
    private lateinit var chipLocal: Chip

    var onSongSelected: ((Song) -> Unit)? = null

    companion object {
        fun newInstance(): SongPickerBottomSheet {
            return SongPickerBottomSheet()
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
    ): View? {
        return inflater.inflate(R.layout.sheet_song_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.song_picker_recycler_view)
        searchBar = view.findViewById(R.id.search_bar)
        chipSpotify = view.findViewById(R.id.chip_spotify)
        chipSoundcloud = view.findViewById(R.id.chip_soundcloud)
        chipLocal = view.findViewById(R.id.chip_local)

        recyclerView.layoutManager = LinearLayoutManager(context)

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
            null, // savedSongPlatformIds
            onItemClick = { song ->
                onSongSelected?.invoke(song)
                dismiss()
            },
            onItemLongPress = { },
            audioPreviewManager = audioPreviewManager,
            soundcloudService = scServiceOrNull
        )

        recyclerView.adapter = adapter

        // Set up filter chips
        chipSpotify.setOnCheckedChangeListener { _, _ -> filterSongs() }
        chipSoundcloud.setOnCheckedChangeListener { _, _ -> filterSongs() }
        chipLocal.setOnCheckedChangeListener { _, _ -> filterSongs() }

        // Set up search bar
        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterSongs()
                true
            } else {
                false
            }
        }
    }

    private fun filterSongs() {
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
    }
}




