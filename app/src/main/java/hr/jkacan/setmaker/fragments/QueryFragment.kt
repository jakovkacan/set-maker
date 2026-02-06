package hr.jkacan.setmaker.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import hr.jkacan.setmaker.adapters.QueryPagerAdapter
import hr.jkacan.setmaker.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.utils.showToast
import hr.jkacan.setmaker.viewmodels.QuerySharedViewModel

class QueryFragment : Fragment() {

    private lateinit var searchBar: EditText
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private val sharedViewModel: QuerySharedViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val songRepository = SongRepository(requireContext())
                return QuerySharedViewModel(songRepository, requireContext()) as T
            }
        }
    }

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
            val imm =
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, load local files
            sharedViewModel.loadAllLocalFiles()
        } else {
            showToast("Storage permission is required to access local music", requireContext())
        }
    }

    fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                sharedViewModel.loadAllLocalFiles()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                // Show explanation dialog
                showToast("Permission needed to access your music library", requireContext())
                requestPermissionLauncher.launch(permission)
            }

            else -> {
                // Request permission
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}