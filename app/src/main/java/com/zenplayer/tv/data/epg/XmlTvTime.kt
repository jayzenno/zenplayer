package com.zenplayer.tv.data.epg

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** XMLTV timestamps are normally YYYYMMDDHHMMSS with an optional numeric offset. */
internal object XmlTvTime {
    fun parse(value: String): Long? {
        val raw = value.trim()
        if (raw.isBlank()) return null

        val withZone = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
            isLenient = false
        }
        runCatching { withZone.parse(raw)?.time }.getOrNull()?.let { return it }

        // XMLTV permits timestamps without an offset. Treat these as UTC rather than
        // silently depending on the Android device's timezone.
        val utc = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return runCatching { utc.parse(raw)?.time }.getOrNull()
    }
}
