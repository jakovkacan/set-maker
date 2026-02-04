package hr.jkacan.setmaker.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hr.jkacan.setmaker.databinding.ActivityEditorBinding // Auto-generated
import hr.jkacan.setmaker.utils.ThemeHelper

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        setSupportActionBar(binding.toolbar) // Assuming you have a toolbar in XML
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get set information from intent
        val setId = intent.getIntExtra("SET_ID", -1)
        val setName = intent.getStringExtra("SET_NAME")

        supportActionBar?.title = "Editing: $setName"

        if (setId == -1) {
            // Handle error or new set creation
        }

        setupEditor()
    }

    private fun setupEditor() {
        // Example: If you use ViewPager2 for different edit modes
        // val adapter = EditorPagerAdapter(this)
        // binding.viewPager.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Modern way to handle back
        return true
    }
}