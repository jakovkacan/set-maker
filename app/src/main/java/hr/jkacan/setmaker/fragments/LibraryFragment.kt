package hr.jkacan.setmaker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hr.jkacan.setmaker.adapters.SongAdapter
import hr.jkacan.setmaker.models.Song
import hr.jkacan.setmaker.models.SongProvider
import hr.jkacan.setmaker.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LibraryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        recyclerView = view.findViewById(R.id.library_recycler_view)
        fabAdd = view.findViewById(R.id.fab_add)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // Sample data
        val songs = getSampleSongs()

        adapter = SongAdapter(
            songs,
            onItemClick = { song ->
                // Handle song click
            },
            onItemLongPress = { song ->
                showSongOptionsModal(song)
            }
        )

        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            // Navigate to add song
        }

        return view
    }

    private fun showSongOptionsModal(song: Song) {
        val modalFragment = SongOptionsBottomSheet.newInstance(song)
        modalFragment.show(parentFragmentManager, "SongOptions")
    }

    private fun getSampleSongs(): List<Song> {
        return listOf(
            Song(
                1,
                "Blinding Lights",
                "The Weeknd",
                "After Hours",
                "cover1",
                SongProvider.SPOTIFY,
                false
            ),
            Song(
                2,
                "Levitating",
                "Dua Lipa",
                "Future Nostalgia",
                "cover2",
                SongProvider.SPOTIFY,
                true
            ),
            Song(
                3,
                "Heat Waves",
                "Glass Animals",
                "Dreamland",
                "cover3",
                SongProvider.SOUNDCLOUD,
                false
            ),
            Song(
                4,
                "Astronaut In The Ocean",
                "Masked Wolf",
                "Single",
                "cover4",
                SongProvider.LOCAL,
                true
            ),
            Song(
                5,
                "Save Your Tears",
                "The Weeknd",
                "After Hours",
                "cover5",
                SongProvider.SPOTIFY,
                false
            )
        )
    }
}