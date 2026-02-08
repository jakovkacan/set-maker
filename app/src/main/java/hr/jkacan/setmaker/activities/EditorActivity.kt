package hr.jkacan.setmaker.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.databinding.ActivityEditorBinding
import hr.jkacan.setmaker.editor.EditorViewModel
import hr.jkacan.setmaker.editor.composables.EditorCanvas
import hr.jkacan.setmaker.utils.ThemeHelper

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private lateinit var setGraphRepository: hr.jkacan.setmaker.data.dao.SetGraphRepository
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

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }

        // Get repositories from application
        val application = application as hr.jkacan.setmaker.SetMakerApplication
        setGraphRepository = application.setGraphRepository

        // Get set information from intent
        currentSetId = intent.getIntExtra("SET_ID", -1)
        val setName = intent.getStringExtra("SET_NAME")

        supportActionBar?.title = setName

        if (currentSetId == -1) {
            // Handle error case
            finish()
            return
        }

        setupEditor()
    }

    private fun showSongPicker(fromNodeId: Int, toNodeId: Int?) {
        val songPicker = hr.jkacan.setmaker.fragments.SongPickerBottomSheet.newInstance()
        songPicker.onSongSelected = { song ->
            viewModel.addNode(song, fromNodeId, toNodeId)
        }
        songPicker.show(supportFragmentManager, "SongPicker")
    }

    private fun showSongPickerForBranch(fromNodeId: Int) {
        val songPicker = hr.jkacan.setmaker.fragments.SongPickerBottomSheet.newInstance()
        songPicker.onSongSelected = { song ->
            viewModel.addBranchNode(song, fromNodeId)
        }
        songPicker.show(supportFragmentManager, "SongPicker")
    }

    private fun showSongPickerForFirstNode() {
        val songPicker = hr.jkacan.setmaker.fragments.SongPickerBottomSheet.newInstance()
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
                onConnectLeafToNode = viewModel::connectLeafToNode
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


    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Modern way to handle back
        return true
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_debug -> {
                debugMode = !debugMode
                item.isChecked = debugMode
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}

class EditorViewModelFactory(
    private val setId: Int,
    private val setGraphRepository: hr.jkacan.setmaker.data.dao.SetGraphRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            return EditorViewModel(setId, setGraphRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



