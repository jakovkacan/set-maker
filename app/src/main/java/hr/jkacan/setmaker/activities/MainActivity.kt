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
import hr.jkacan.setmaker.databinding.ActivityMainBinding
import hr.jkacan.setmaker.fragments.LibraryFragment
import hr.jkacan.setmaker.fragments.SetsFragment
import hr.jkacan.setmaker.fragments.QueryFragment
import hr.jkacan.setmaker.data.dao.SetGraphRepository
import hr.jkacan.setmaker.data.dao.SetRepository
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.data.dao.getSetGraphRepository
import hr.jkacan.setmaker.data.dao.getSetRepository
import hr.jkacan.setmaker.data.dao.getSongRepository
import hr.jkacan.setmaker.utils.ThemeHelper

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    lateinit var songRepository: SongRepository
    lateinit var setRepository: SetRepository
    private lateinit var setGraphRepository: SetGraphRepository

    private var currentThemeValue: String? = null
    private var lastQueryTapTime = 0L
    private val DOUBLE_TAP_DELAY = 300L

    private val prefListener = OnSharedPreferenceChangeListener { _, key ->
        if (key == "theme") {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        currentThemeValue = prefs.getString("theme", null)

        songRepository = getSongRepository(this)
        setRepository = getSetRepository(this)
        setGraphRepository = getSetGraphRepository(this)

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(SetsFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_sets
        }

        // Bottom navigation listener
        binding.bottomNavigation.setOnItemSelectedListener { item ->
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
                        val currentFragment =
                            supportFragmentManager.findFragmentById(R.id.fragment_container)
                        if (currentFragment is QueryFragment) {
                            currentFragment.focusSearchBar()
                        }
                    } else {
                        loadFragment(QueryFragment())
                    }
                    true
                }

                else -> false
            }
        }

        // Toolbar menu
        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
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
}
