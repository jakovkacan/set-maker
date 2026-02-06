package hr.jkacan.setmaker.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hr.jkacan.setmaker.adapters.SongAdapter
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.activities.MainActivity
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.utils.AudioPreviewManager
import hr.jkacan.setmaker.utils.getServiceOrNull
import hr.jkacan.setmaker.utils.showToast
import hr.jkacan.setmaker.viewmodels.QuerySharedViewModel
import hr.jkacan.setmaker.viewmodels.SearchResultState

class QueryTabFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var adapter: SongAdapter
    private var provider: SongProvider = SongProvider.SPOTIFY

    private val sharedViewModel: QuerySharedViewModel by viewModels({ requireParentFragment() }) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuerySharedViewModel((requireActivity() as MainActivity).songRepository) as T
            }
        }
    }

    private lateinit var audioPreviewManager: AudioPreviewManager
    private lateinit var soundcloudService: SoundcloudService

    companion object {
        private const val ARG_PROVIDER = "provider"

        fun newInstance(provider: SongProvider): QueryTabFragment {
            return QueryTabFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PROVIDER, provider)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        audioPreviewManager = AudioPreviewManager(context)
        if (provider == SongProvider.SOUNDCLOUD) {
            soundcloudService = SoundcloudService()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            provider = it.getSerializable(ARG_PROVIDER) as SongProvider
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_query_tab, container, false)

        recyclerView = view.findViewById(R.id.query_results_recycler_view)
        loadingIndicator = view.findViewById(R.id.loading_indicator)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val songRepository = (requireActivity() as MainActivity).songRepository
        val scServiceOrNull = getServiceOrNull(::soundcloudService)

        adapter = SongAdapter(
            emptyList(),
            onItemClick = { song ->
                val added = songRepository.toggleSavedSong(song)
                adapter.updateSongSavedState(song.platformId!!, added)
            },
            onItemLongPress = { song -> },
            audioPreviewManager = audioPreviewManager,
            savedSongPlatformIds = emptyList(),
            soundcloudService = scServiceOrNull
        )

        recyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe the search results from the shared ViewModel
        sharedViewModel.searchResults.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SearchResultState.Loading -> {
                    loadingIndicator.isVisible = true
                    recyclerView.isVisible = false
                }

                is SearchResultState.Success -> {
                    loadingIndicator.isVisible = false
                    recyclerView.isVisible = true
                    // Filter results for this specific tab's provider
                    val providerSongs = state.songs.filter { it.provider == provider }
                    val savedSongsIds = state.savedSongsIds
                    adapter.updateSongs(providerSongs, savedSongsIds)
                }

                is SearchResultState.Error -> {
                    loadingIndicator.isVisible = false
                    recyclerView.isVisible = false
                    showToast(state.message, requireContext())
                    Log.e("QueryTabFragment", "Error: ${state.message}")
                }
            }
        }
    }
}