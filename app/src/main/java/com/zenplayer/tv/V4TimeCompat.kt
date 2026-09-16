package com.zenplayer.tv

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Compatibility formatter for EPG timestamps stored as epoch milliseconds. */
fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
