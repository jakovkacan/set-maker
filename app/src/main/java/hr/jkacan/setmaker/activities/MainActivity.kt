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
import hr.jkacan.setmaker.activities.SettingsActivity
import hr.jkacan.setmaker.fragments.LibraryFragment
import hr.jkacan.setmaker.fragments.SetsFragment
import hr.jkacan.setmaker.fragments.QueryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import hr.jkacan.setmaker.utils.ThemeHelper

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var prefs: SharedPreferences

    private var currentThemeValue: String? = null

    private val prefListener = OnSharedPreferenceChangeListener { _, key ->
        if (key == "theme") {
            // Recreate activity so toolbar and other themed UI update immediately
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
                    loadFragment(QueryFragment())
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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
