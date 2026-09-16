package com.zenplayer.tv

import java.util.Date

/** Compatibility overload for EPG timestamps stored as epoch milliseconds. */
fun formatTime(timestamp: Long): String = formatTime(Date(timestamp))
