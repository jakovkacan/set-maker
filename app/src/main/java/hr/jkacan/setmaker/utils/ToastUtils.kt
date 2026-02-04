package hr.jkacan.setmaker.utils

import android.content.Context
import android.widget.Toast

fun showError(message: String, context: Context) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}