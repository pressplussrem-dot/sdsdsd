package com.smartcalc.ai.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatters {
    private val formatter = SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale("sr", "RS"))

    fun format(timestamp: Long): String = formatter.format(Date(timestamp))
}
