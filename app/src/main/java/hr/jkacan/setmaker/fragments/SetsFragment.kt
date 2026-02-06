package hr.jkacan.setmaker.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import hr.jkacan.setmaker.SetMakerApplication
import hr.jkacan.setmaker.activities.EditorActivity
import hr.jkacan.setmaker.activities.MainActivity
import hr.jkacan.setmaker.activities.SetFormActivity
import hr.jkacan.setmaker.adapters.SetAdapter
import hr.jkacan.setmaker.data.dao.SetRepository
import hr.jkacan.setmaker.databinding.FragmentSetsBinding
import hr.jkacan.setmaker.models.set.SetItem
import hr.jkacan.setmaker.models.song.Song

class SetsFragment : Fragment() {

    private var _binding: FragmentSetsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SetAdapter
    private lateinit var setRepository: SetRepository

    private val setFormLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            setRepository = (requireActivity().application as SetMakerApplication).setRepository

            val updatedSets = setRepository.getAll()
            adapter = SetAdapter(updatedSets, { setItem ->
                val intent = Intent(activity, EditorActivity::class.java)
                intent.putExtra("SET_ID", setItem.id)
                intent.putExtra("SET_NAME", setItem.name)
                startActivity(intent)
            }, { set -> showSetOptionsModal(set) })
            binding.setsRecyclerView.adapter = adapter
            updateEmptyState(updatedSets)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRepository = (requireActivity().application as SetMakerApplication).setRepository
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.setsRecyclerView.layoutManager = GridLayoutManager(context, 2)

        val setsList = setRepository.getAll()

        updateEmptyState(setsList)

        adapter = SetAdapter(
            setsList, { setItem ->
                val intent = Intent(activity, EditorActivity::class.java)
                intent.putExtra("SET_ID", setItem.id)
                intent.putExtra("SET_NAME", setItem.name)
                startActivity(intent)
            },
            onItemLongPress = { set -> showSetOptionsModal(set) })

        binding.setsRecyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireActivity(), SetFormActivity::class.java)
            setFormLauncher.launch(intent)
        }
    }

    private fun updateEmptyState(songs: List<SetItem>) {
        if (songs.isEmpty()) {
            binding.setsRecyclerView.visibility = View.GONE
            binding.emptyStateSets.root.visibility = View.VISIBLE
        } else {
            binding.setsRecyclerView.visibility = View.VISIBLE
            binding.emptyStateSets.root.visibility = View.GONE
        }
    }

    private fun editSet(setItem: SetItem) {
        val intent = Intent(requireActivity(), SetFormActivity::class.java)
        intent.putExtra("SET_ID", setItem.id)
        setFormLauncher.launch(intent)
    }

    private fun showSetOptionsModal(set: SetItem) {
        val modalFragment = SetOptionsBottomSheet.newInstance(set)
        modalFragment.onSetDeleted = {
            refreshSets()
        }
        modalFragment.onEditSet = {
            editSet(set)
        }
        modalFragment.show(parentFragmentManager, "SetOptions")
    }

    private fun refreshSets() {
//        filterSets(setRepository)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
