package hr.jkacan.setmaker.utils

import androidx.appcompat.app.AppCompatActivity
import hr.jkacan.setmaker.R

object ThemeHelper {
    fun applyTheme(activity: AppCompatActivity) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity)
        val theme = prefs.getString("theme", "red")
        when (theme) {
            "cyan" -> activity.setTheme(R.style.Theme_SetMaker_Cyan)
            "purple" -> activity.setTheme(R.style.Theme_SetMaker_Purple)
            else -> activity.setTheme(R.style.Theme_SetMaker_Red)
        }
    }
}