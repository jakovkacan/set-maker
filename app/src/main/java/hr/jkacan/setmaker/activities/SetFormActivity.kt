package hr.jkacan.setmaker.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import coil.load
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.SetMakerApplication
import hr.jkacan.setmaker.data.dao.SetRepository
import hr.jkacan.setmaker.databinding.ActivitySetFormBinding
import hr.jkacan.setmaker.models.set.SetItem
import hr.jkacan.setmaker.utils.ThemeHelper
import hr.jkacan.setmaker.utils.showToast
import java.util.Date
import kotlin.toString
import androidx.core.net.toUri

class SetFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetFormBinding
    private lateinit var setRepository: SetRepository
    private var selectedImageUri: Uri? = null
    private var existingSet: SetItem? = null
    private var isEditMode = false

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            if (selectedImageUri != null) {
                try {
                    contentResolver.releasePersistableUriPermission(
                        selectedImageUri!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    // Permission was already released or never persisted
                    Log.w("Playlist", "Could not release old URI permission", e)
                }
            }
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedImageUri = it
            binding.setCoverPreview.load(it) {
                crossfade(true)
            }
            binding.cameraIcon.alpha = 0f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySetFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setRepository = (application as SetMakerApplication).setRepository

        loadSetData()
        setupViews()
        setupClickListeners()
    }

    private fun loadSetData() {
        val setId = intent.getIntExtra("SET_ID", -1)
        if (setId != -1) {
            existingSet = setRepository.getById(setId)
            existingSet?.let { set ->
                isEditMode = true
                binding.setNameInput.setText(set.name)
                if (!set.coverPath.isNullOrBlank()) {
                    set.coverPath.let { path ->
                        selectedImageUri = path.toUri()
                        binding.setCoverPreview.load(selectedImageUri) {
                            crossfade(true)
                        }
                        binding.cameraIcon.alpha = 0f
                    }
                } else {
                    binding.cameraIcon.alpha = 1f
                }
            }
        }
    }

    private fun setupViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(if (isEditMode) R.string.edit_set else R.string.create_set)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }
    }

    private fun setupClickListeners() {
        binding.coverImageCard.setOnClickListener {
            imagePickerLauncher.launch(arrayOf("image/*"))
        }

        binding.saveButton.setOnClickListener {
            saveSet()
        }
    }

    private fun saveSet() {
        val setName = binding.setNameInput.text?.toString()?.trim()

        if (setName.isNullOrBlank()) {
            binding.setNameInput.error = getString(R.string.error_set_name_required)
            return
        }

        val coverPath = if (selectedImageUri != null) selectedImageUri.toString() else null

        val set = if (isEditMode && existingSet != null) {
            existingSet!!.copy(
                name = setName,
                coverPath = coverPath,
                dateUpdated = Date()
            )
        } else {
            SetItem(
                id = null,
                name = setName,
                coverPath = coverPath,
                dateAdded = Date(),
                dateUpdated = Date()
            )
        }

        if (isEditMode) {
            setRepository.update(set)
            showToast("Set updated: $setName", this)
        } else {
            setRepository.insert(set)
            showToast("Set saved: $setName", this)
        }

        setResult(RESULT_OK)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
