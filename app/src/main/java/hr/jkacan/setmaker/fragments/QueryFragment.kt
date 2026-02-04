package hr.jkacan.setmaker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import hr.jkacan.setmaker.adapters.QueryPagerAdapter
import hr.jkacan.setmaker.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.viewmodels.QuerySharedViewModel

class QueryFragment : Fragment() {

    private lateinit var searchBar: EditText
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private val sharedViewModel: QuerySharedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_query, container, false)

        searchBar = view.findViewById(R.id.search_bar)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)

        // Setup ViewPager with tabs
        val pagerAdapter = QueryPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.spotify)
                1 -> getString(R.string.soundcloud)
                2 -> getString(R.string.local_files)
                else -> ""
            }
        }.attach()

        searchBar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // set initial colors for the currently selected page
        updateTabColors(viewPager.currentItem)

        // when a tab is selected by tap
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabColors(tab.position)
                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // when the page changes by swipe
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabColors(position)
            }
        })

        return view
    }

    private fun performSearch() {
        val query = searchBar.text.toString().trim()
        if (query.isNotEmpty()) {
            val currentProvider = when (viewPager.currentItem) {
                0 -> SongProvider.SPOTIFY
                1 -> SongProvider.SOUNDCLOUD
                2 -> SongProvider.LOCAL
                else -> SongProvider.SPOTIFY
            }
            // Trigger the search in the ViewModel
            sharedViewModel.search(query, currentProvider)

            // Hide keyboard
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(searchBar.windowToken, 0)

        }
    }

    private fun updateTabColors(index: Int) {
        val selectedColor = when (index) {
            0 -> ContextCompat.getColor(requireContext(), R.color.colorSpotify)
            1 -> ContextCompat.getColor(requireContext(), R.color.colorSoundCloud)
            2 -> ContextCompat.getColor(requireContext(), R.color.colorLocalFiles)
            else -> ContextCompat.getColor(requireContext(), R.color.gray)
        }
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        tabLayout.setSelectedTabIndicatorColor(selectedColor)
        tabLayout.setTabTextColors(unselectedColor, selectedColor)
    }
}