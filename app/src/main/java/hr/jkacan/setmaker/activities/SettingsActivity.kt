package hr.jkacan.setmaker.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.SetMakerApplication
import hr.jkacan.setmaker.data.dao.SongRepository
import hr.jkacan.setmaker.databinding.ActivitySettingsBinding
import hr.jkacan.setmaker.utils.ThemeHelper
import hr.jkacan.setmaker.utils.showToast

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "theme") {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        private lateinit var songRepository: SongRepository

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val application = requireActivity().application as SetMakerApplication
            songRepository = application.songRepository
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Set up prune unused songs preference click listener
            findPreference<Preference>("prune_unused_songs")?.setOnPreferenceClickListener {
                showPruneConfirmationDialog()
                true
            }
        }

        private fun showPruneConfirmationDialog() {
            val context = requireContext()
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.prune_unused_songs))
                .setMessage(getString(R.string.prune_unused_songs_confirmation))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    pruneUnusedSongs()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        private fun pruneUnusedSongs() {
            val context = requireContext()
            val songRepository = songRepository

            try {
                val deletedCount = songRepository.pruneUnusedSongs(context)
                songRepository.close()

                // Show result to user
                showToast(
                    getString(R.string.deleted_unused_songs, deletedCount),
                    context
                )
            } catch (e: Exception) {
                songRepository.close()
                showToast(
                    getString(R.string.error_pruning_songs, e.message),
                    context
                )
            }
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            if (key == "theme") {
                activity?.recreate()
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }
    }
}
