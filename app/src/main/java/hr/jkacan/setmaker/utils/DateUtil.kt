package hr.jkacan.setmaker.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun parseStringAsDate(dateString: String?): Date {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateString?.let { dateFormat.parse(it) } ?: Date()
}

fun formatDate(date: Date?): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateFinal = date ?: Date()
    return dateFinal.let { dateFormat.format(it) }
}