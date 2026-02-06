package hr.jkacan.setmaker.activities

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.fragments.LibraryFragment
import hr.jkacan.setmaker.fragments.SetsFragment
import hr.jkacan.setmaker.fragments.QueryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import hr.jkacan.setmaker.data.dao.SetGraphRepository
import hr.jkacan.setmaker.data.dao.SetRepository
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.data.dao.getSetGraphRepository
import hr.jkacan.setmaker.data.dao.getSetRepository
import hr.jkacan.setmaker.data.dao.getSongRepository
import hr.jkacan.setmaker.utils.ThemeHelper

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var prefs: SharedPreferences

    lateinit var songRepository: SongRepository
    lateinit var setRepository: SetRepository
    private lateinit var setGraphRepository: SetGraphRepository

    private var currentThemeValue: String? = null
    private var lastQueryTapTime = 0L
    private val DOUBLE_TAP_DELAY = 300L // milliseconds

    private val prefListener = OnSharedPreferenceChangeListener { _, key ->
        if (key == "theme") {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        currentThemeValue = prefs.getString("theme", null)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        songRepository = getSongRepository(this)
        setRepository = getSetRepository(this)
        setGraphRepository = getSetGraphRepository(this)

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(SetsFragment())
            bottomNavigation.selectedItemId = R.id.nav_sets
        }

        // Bottom navigation listener
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_library -> {
                    loadFragment(LibraryFragment())
                    true
                }

                R.id.nav_sets -> {
                    loadFragment(SetsFragment())
                    true
                }

                R.id.nav_query -> {
                    val currentTime = System.currentTimeMillis()
                    val isDoubleTap = currentTime - lastQueryTapTime < DOUBLE_TAP_DELAY
                    lastQueryTapTime = currentTime

                    if (isDoubleTap) {
                        // Double tap detected - focus search bar
                        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                        if (currentFragment is QueryFragment) {
                            currentFragment.focusSearchBar()
                        }
                    } else {
                        // Single tap - load fragment normally
                        loadFragment(QueryFragment())
                    }
                    true
                }

                else -> false
            }
        }

        // Toolbar menu
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply top inset as padding to the toolbar
            view.updatePadding(top = insets.top)
            windowInsets
        }
    }

    override fun onResume() {
        super.onResume()
        val newTheme = prefs.getString("theme", null)
        if (newTheme != currentThemeValue) {
            currentThemeValue = newTheme
            recreate()
            return
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        songRepository.close()
        setRepository.close()
        setGraphRepository.close()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

//    private fun createExampleSetWithGraph() {
//        // Create a set
//        val setId = setRepository.insert(SetItem(0, "My DJ Set", null))
//
//        // Add nodes (same song can appear multiple times)
//        val node1Id = setGraphRepository.insertNode(setId.toInt(), song1Id.toInt(), "Intro track")
//        val node2Id = setGraphRepository.insertNode(setId.toInt(), song2Id.toInt(), "Build up")
//        val node3Id = setGraphRepository.insertNode(setId.toInt(), song3Id.toInt(), "Peak")
//        val node4Id = setGraphRepository.insertNode(setId.toInt(), song4Id.toInt(), "Cool down")
//        val node5Id = setGraphRepository.insertNode(setId.toInt(), song1Id.toInt(), "Reprise")
//
//        // Create edges (transitions)
//        // Default path: node1 -> node2 -> node3 -> node4
//        setGraphRepository.insertEdge(
//            setId.toInt(),
//            node1Id.toInt(),
//            node2Id.toInt(),
//            ord = 0,
//            kind = "default"
//        )
//        setGraphRepository.insertEdge(
//            setId.toInt(),
//            node2Id.toInt(),
//            node3Id.toInt(),
//            ord = 0,
//            kind = "default"
//        )
//        setGraphRepository.insertEdge(
//            setId.toInt(),
//            node3Id.toInt(),
//            node4Id.toInt(),
//            ord = 0,
//            kind = "default"
//        )
//
//        // Alternative path: node2 -> node5 (instead of node3)
//        setGraphRepository.insertEdge(
//            setId.toInt(),
//            node2Id.toInt(),
//            node5Id.toInt(),
//            ord = 1,
//            kind = "alt"
//        )
//        setGraphRepository.insertEdge(
//            setId.toInt(),
//            node5Id.toInt(),
//            node4Id.toInt(),
//            ord = 0,
//            kind = "default"
//        )
//
//        // Traverse the graph
//        val startNodes = setGraphRepository.getStartNodes(setId.toInt())
//        Log.d("SetGraph", "Start nodes: ${startNodes.size}")
//
//        val defaultPath = setGraphRepository.getDefaultPath(setId.toInt())
//        defaultPath?.let { path ->
//            Log.d("SetGraph", "Default path has ${path.nodes.size} nodes")
//            path.nodes.forEach { nodeWithSong ->
//                Log.d("SetGraph", "Node: ${nodeWithSong.song.title} - ${nodeWithSong.node.note}")
//            }
//        }
//
//        // Get alternatives from node2
//        val alternatives = setGraphRepository.getNextNodes(setId.toInt(), node2Id.toInt())
//        Log.d("SetGraph", "Node 2 has ${alternatives.size} outgoing paths")
//    }
}
