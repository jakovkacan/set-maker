package hr.jkacan.setmaker.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hr.jkacan.setmaker.R

class EditorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Editor"

        // Get set information from intent
        val setId = intent.getIntExtra("SET_ID", -1)
        val setName = intent.getStringExtra("SET_NAME")

        // TODO: Implement editor functionality
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
