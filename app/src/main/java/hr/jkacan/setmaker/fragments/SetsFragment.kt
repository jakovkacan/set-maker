package hr.jkacan.setmaker.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.activities.EditorActivity
import hr.jkacan.setmaker.adapters.SetAdapter
import hr.jkacan.setmaker.models.Set

class SetsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sets, container, false)

        recyclerView = view.findViewById(R.id.sets_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 2) // 2 columns

        // Sample data
        val setsList = getSampleSets()

        adapter = SetAdapter(setsList) { setItem ->
            // On click listener
            val intent = Intent(activity, EditorActivity::class.java)
            intent.putExtra("SET_ID", setItem.id)
            intent.putExtra("SET_NAME", setItem.name)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        return view
    }

    private fun getSampleSets(): List<Set> {
        return listOf(
            Set(1, "sr sn", "https://image-cdn-ak.spotifycdn.com/image/ab67706c0000d72c1fa09190a44ba099ff9a232d"),
            Set(2, "Chill Vibes", null),
            Set(3, "Party Hits", null),
            Set(4, "Study Session", null),
            Set(5, "Road Trip", null),
            Set(6, "Relax & Sleep", null)
        )
    }
}
