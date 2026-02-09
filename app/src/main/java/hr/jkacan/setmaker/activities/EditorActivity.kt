package hr.jkacan.setmaker.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.SetMakerApplication
import hr.jkacan.setmaker.data.dao.SetGraphRepository
import hr.jkacan.setmaker.databinding.ActivityEditorBinding
import hr.jkacan.setmaker.editor.EditorViewModel
import hr.jkacan.setmaker.editor.EditorViewModelFactory
import hr.jkacan.setmaker.editor.composables.EditorCanvas
import hr.jkacan.setmaker.fragments.SongPickerBottomSheet
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.services.soundcloud.SoundcloudService
import hr.jkacan.setmaker.utils.AudioPreviewManager
import hr.jkacan.setmaker.utils.ThemeHelper

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var setGraphRepository: SetGraphRepository
    private lateinit var audioPreviewManager: AudioPreviewManager
    private lateinit var soundcloudService: SoundcloudService
    private var currentSetId: Int = -1
    private val viewModel: EditorViewModel by viewModels {
        EditorViewModelFactory(currentSetId, setGraphRepository)
    }
    private var debugMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get repositories from application
        val application = application as SetMakerApplication
        setGraphRepository = application.setGraphRepository
        audioPreviewManager = application.audioPreviewManager
        soundcloudService = application.soundcloudService

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Get set information from intent
        currentSetId = intent.getIntExtra("SET_ID", -1)
        val setName = intent.getStringExtra("SET_NAME")

        // Set up the toolbar
        binding.toolbar.inflateMenu(R.menu.menu_editor)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }
        supportActionBar?.title = setName

        if (currentSetId == -1) {
            // Handle error case
            finish()
            return
        }

        setupEditor()
    }

    private fun showSongPicker(fromNodeId: Int, toNodeId: Int?) {
        val insertedSongIds = getAllInsertedSongs().mapNotNull { it.platformId }
        val songPicker = SongPickerBottomSheet.newInstance(insertedSongIds)
        songPicker.onSongSelected = { song ->
            viewModel.addNode(song, fromNodeId, toNodeId)
        }
        songPicker.show(supportFragmentManager, "SongPicker")
    }

    private fun showSongPickerForBranch(fromNodeId: Int) {
        val insertedSongIds = getAllInsertedSongs().mapNotNull { it.platformId }
        val songPicker = SongPickerBottomSheet.newInstance(insertedSongIds)
        songPicker.onSongSelected = { song ->
            viewModel.addBranchNode(song, fromNodeId)
        }
        songPicker.show(supportFragmentManager, "SongPicker")
    }

    private fun showSongPickerForFirstNode() {
        val songPicker = SongPickerBottomSheet.newInstance()
        songPicker.onSongSelected = { song ->
            viewModel.addFirstNode(song)
        }
        songPicker.show(supportFragmentManager, "SongPicker")
    }

    private fun setupEditor() {
        binding.composeView.setContent {
            val graphState by viewModel.graphState.collectAsState()
            EditorCanvas(
                state = graphState,
                debugMode = debugMode,
                onAddNode = { from, to -> showSongPicker(from, to) },
                onAddNodeBranch = { from -> showSongPickerForBranch(from) },
                onSwapNodes = viewModel::swapNodes,
                onInsertNode = viewModel::insertNodeBetween,
                onDeleteNode = viewModel::deleteNode,
                onConnectLeafToNode = viewModel::connectLeafToNode,
                audioPreviewManager = audioPreviewManager,
                soundcloudService = soundcloudService,
                prefs = prefs
            )

            // Update FAB visibility based on graph state
            androidx.compose.runtime.LaunchedEffect(graphState) {
                binding.fabAddSong.visibility = if (viewModel.isGraphEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }

        // Initial FAB setup
        updateFabVisibility()
        binding.fabAddSong.setOnClickListener {
            showSongPickerForFirstNode()
        }
    }

    private fun updateFabVisibility() {
        binding.fabAddSong.visibility = if (viewModel.isGraphEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPreviewManager.stop()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Modern way to handle back
        return true
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        // Hide menu items when debug is disabled
        val enableDebug = prefs.getBoolean("debug", false)
        menu?.setGroupVisible(0, enableDebug)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_debug -> {
                debugMode = !debugMode
                item.isChecked = debugMode
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getAllInsertedSongs(): List<Song> {
        val allNodes = setGraphRepository.getNodesWithSongsBySet(currentSetId)
        return allNodes.map { it.song }.distinct()
    }
}