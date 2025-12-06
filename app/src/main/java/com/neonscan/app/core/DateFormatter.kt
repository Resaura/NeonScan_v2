package com.neonscan.app.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    fun format(timestamp: Long): String = dateFormat.format(Date(timestamp))
}
