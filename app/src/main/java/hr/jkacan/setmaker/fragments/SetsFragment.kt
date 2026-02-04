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
import hr.jkacan.setmaker.activities.MainActivity
import hr.jkacan.setmaker.adapters.SetAdapter
import hr.jkacan.setmaker.models.set.SetItem

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

        // Get the SongRepository from MainActivity
        val setRepository = (requireActivity() as MainActivity).setRepository

        // Load all saved songs
        val setsList = setRepository.getAll()

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
}
