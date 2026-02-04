package hr.jkacan.setmaker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hr.jkacan.setmaker.adapters.SongAdapter
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.R

class QueryTabFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private var provider: SongProvider = SongProvider.SPOTIFY

    companion object {
        private const val ARG_PROVIDER = "provider"

        fun newInstance(provider: SongProvider): QueryTabFragment {
            val fragment = QueryTabFragment()
            val args = Bundle()
            args.putSerializable(ARG_PROVIDER, provider)
            fragment.arguments = args
            return fragment
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
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Sample search results
        val songs = getSearchResults()

        adapter = SongAdapter(
            songs,
            onItemClick = { song ->
                // Add song to library
            },
            onItemLongPress = { song ->
                // Show options
            }
        )

        recyclerView.adapter = adapter

        return view
    }

    private fun getSearchResults(): List<Song> {
        // Return filtered results based on provider
        return emptyList()
    }
}