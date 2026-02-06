package hr.jkacan.setmaker.activities

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.databinding.ActivitySetFormBinding

class SetFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetFormBinding
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.setCoverPreview.load(it) {
                crossfade(true)
            }
            binding.cameraIcon.alpha = 0f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.create_set)
        }
    }

    private fun setupClickListeners() {
        binding.coverImageCard.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

//        binding.saveButton.setOnClickListener {
//            saveSet()
//        }
    }

    private fun saveSet() {
//        val setName = binding.setNameInput.text?.toString()?.trim()

//        if (setName.isNullOrBlank()) {
//            binding.setNameInput.error = getString(R.string.error_set_name_required)
//            return
//        }

        // TODO: Implement saving logic
        // - Upload image to storage
        // - Create SetItem with name and coverUrl
        // - Save to database/repository

//        Toast.makeText(this, "Set saved: $setName", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
