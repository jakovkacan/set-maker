package hr.jkacan.setmaker.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import hr.jkacan.setmaker.adapters.SongAdapter
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.activities.MainActivity
import hr.jkacan.setmaker.data.state.SearchResultState
import hr.jkacan.setmaker.databinding.FragmentQueryTabBinding
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.utils.AudioPreviewManager
import hr.jkacan.setmaker.utils.getServiceOrNull
import hr.jkacan.setmaker.utils.showToast
import hr.jkacan.setmaker.viewmodels.QuerySharedViewModel

class QueryTabFragment : Fragment() {

    private var _binding: FragmentQueryTabBinding? = null
    private val binding get() = _binding!!

    private var emptyStateInitial: View? = null
    private var emptyStateQuery: View? = null
    private var isFirstSearch = true
    private lateinit var adapter: SongAdapter
    private var provider: SongProvider = SongProvider.SPOTIFY

    private val sharedViewModel: QuerySharedViewModel by viewModels({ requireParentFragment() }) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuerySharedViewModel(
                    (requireActivity() as MainActivity).songRepository,
                    requireContext()
                ) as T
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
    ): View {
        _binding = FragmentQueryTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyStateInitial = binding.emptyStateInitialStub.inflate()
        binding.queryResultsRecyclerView.layoutManager = LinearLayoutManager(context)

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

        binding.queryResultsRecyclerView.adapter = adapter

        // Load all local files immediately when LOCAL tab is created
        if (provider == SongProvider.LOCAL) {
            (requireParentFragment() as QueryFragment).checkAndRequestPermission()
        }

        // Observe the search results from the shared ViewModel
        sharedViewModel.searchResults.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SearchResultState.Loading -> {
                    isFirstSearch = false
                    binding.loadingIndicator.visibility = View.VISIBLE
                    binding.queryResultsRecyclerView.visibility = View.GONE
                    emptyStateInitial?.visibility = View.GONE
                    emptyStateQuery?.visibility = View.GONE
                }

                is SearchResultState.Success -> {
                    binding.loadingIndicator.visibility = View.GONE

                    // Filter results for this specific tab's provider
                    val providerSongs = state.songs.filter { it.provider == provider }

                    if (providerSongs.isEmpty()) {
                        binding.queryResultsRecyclerView.visibility = View.GONE
                        emptyStateInitial?.visibility = View.GONE

                        if (emptyStateQuery == null) {
                            emptyStateQuery = binding.emptyStateStub.inflate()
                        }
                        emptyStateQuery?.visibility = View.VISIBLE
                    } else {
                        binding.queryResultsRecyclerView.visibility = View.VISIBLE
                        emptyStateInitial?.visibility = View.GONE
                        emptyStateQuery?.visibility = View.GONE
                    }

                    adapter.updateSongs(providerSongs, state.savedSongsIds)
                }

                is SearchResultState.Error -> {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.queryResultsRecyclerView.visibility = View.GONE
                    emptyStateInitial?.visibility = View.GONE

                    if (emptyStateQuery == null) {
                        emptyStateQuery = binding.emptyStateStub.inflate()
                    }

                    emptyStateQuery?.visibility = View.VISIBLE
                    showToast(state.message, requireContext())
                    Log.e("QueryTabFragment", "Error: ${state.message}")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
